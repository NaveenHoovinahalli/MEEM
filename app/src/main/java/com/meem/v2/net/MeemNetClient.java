package com.meem.v2.net;

import android.os.CountDownTimer;

import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.utils.DebugTracer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * This class sends out the master search broadcast, waits for response / timeout. Once got connected to the server, it creates the
 * accessory object with socket and give it to main activity where it will be used with driver and in turn meem core. Created by arun on
 * 2/8/17.
 */

public class MeemNetClient {
    private static final int MASTER_SEARCH_TIME = 30000; //ms
    private static final int MASTER_SEARCH_IVAL = 2000; //ms

    private DebugTracer mDbg = new DebugTracer("MeemNetClient", "MeemNetClient.log");
    private UiContext mUiCtxt = UiContext.getInstance();

    private String mPhoneUpid;

    private CountDownTimer mMasterSearchTimer;
    private Thread mMasterDiscoveryThread;
    DatagramSocket mUdpBCastSocket;
    private boolean mStopFlag;

    String mMasterUpid;

    Runnable mRunOnFinish;

    public MeemNetClient(String phoneUpid) {
        mPhoneUpid = phoneUpid;
    }

    public void startSearchForMaster(Runnable onFinish) {
        mDbg.trace();

        mRunOnFinish = onFinish;

        mMasterUpid = null;
        startMasterDiscoveryThread();

        mMasterSearchTimer = new CountDownTimer(MASTER_SEARCH_TIME, MASTER_SEARCH_IVAL) {
            public void onTick(long millisUntilFinished) {
                MNetUtils.broadcast(MNetConstants.NW_CLIENT_REQ + mPhoneUpid, MNetConstants.NW_BCAST_RCV_PORT_MASTER);
                mDbg.trace("Master search broadcasted.");
            }

            public void onFinish() {
                stopMasterDiscoveryThread();

                if(null != mRunOnFinish) {
                    mRunOnFinish.run();
                }

                mDbg.trace("Search for master finished");
            }
        }.start();
    }

    public void stopSearchForMaster() {
        mDbg.trace();

        if (null != mMasterSearchTimer) {
            mMasterSearchTimer.cancel();
        }

        stopMasterDiscoveryThread();

        if(null != mRunOnFinish) {
            mRunOnFinish.run();
        }

        mDbg.trace("Search for master stopped");
        mMasterUpid = null;
    }

    private void listenAndProcessServerResponse(Integer port) {
        mDbg.trace();

        byte[] recvBuf = new byte[MNetConstants.NW_BC_PACKET_SIZE];
        if (mUdpBCastSocket == null || mUdpBCastSocket.isClosed()) {
            try {
                mUdpBCastSocket = new DatagramSocket(port);
                mUdpBCastSocket.setReuseAddress(true);
            } catch (Exception e) {
                mDbg.trace("Unable to reuse address: " + e.getMessage());
            }
        }

        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
        mDbg.trace("Waiting for server response");

        try {
            mUdpBCastSocket.receive(packet);
        } catch (Exception e) {
            mDbg.trace("Socket receive exception: " + e.getMessage());
        } finally {
            mUdpBCastSocket.close();
        }

        String senderIP = packet.getAddress().getHostAddress();
        String message = new String(packet.getData()).trim();

        // find master upid
        String masterUpid = null;
        int dot = message.lastIndexOf(".");
        if (dot > 0) {
            try {
                masterUpid = message.substring(dot);
            } catch (Exception e) {
                // below
            }
        }

        if (null == masterUpid) {
            mDbg.trace("Master response parse error: " + message);
            return;
        }

        if (null != mMasterUpid && mMasterUpid.equals(masterUpid)) {
            mDbg.trace("Ignoring duplicate master response: " + message);
            return;
        }

        // keep the master upid
        mMasterUpid = masterUpid;

        mDbg.trace("Got udp response from master at:" + senderIP + ", message: " + message);

        // connect to master and instantiate a new MNet accessory. The classes MNetTCPClient (and from a design perspective MNetAccessory)
        // offloads all the heavy work.
        MNetTCPClient client = new MNetTCPClient(mUiCtxt.getApplicationContext());
        if (!client.connect(senderIP)) {
            mDbg.trace("Failed to establish connection to master!");
        } else {
            MNetAccessory remoteAccessory = new MNetAccessory(client);
            MeemEvent remoteAccConnectedEvent = new MeemEvent(EventCode.MNET_REMOTE_ACCESSORY_CONNECTED, remoteAccessory);
            mUiCtxt.postEvent(remoteAccConnectedEvent);

            mDbg.trace("Connected to master. Remote accessory connected event posted.");
        }
    }

    private void startMasterDiscoveryThread() {
        mDbg.trace();

        mStopFlag = false;
        mMasterDiscoveryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Integer port = MNetConstants.NW_BCAST_RCV_PORT_CLIENT;
                    while (!mStopFlag) {
                        listenAndProcessServerResponse(port);
                    }
                } catch (Exception e) {
                    mDbg.trace("No longer listening for UDP broadcasts cause of exception: " + e.getMessage());
                }
            }
        });

        mMasterDiscoveryThread.start();
    }

    private void stopMasterDiscoveryThread() {
        mDbg.trace();

        if (null != mMasterDiscoveryThread) {
            mStopFlag = true;
            mMasterDiscoveryThread.interrupt();
        }
    }
}
