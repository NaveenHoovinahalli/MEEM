package com.meem.v2.net;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.net.ssl.SSLSocket;


/**
 * Created by arun on 20/6/17.
 * Manages MeemNet broadcast stuff.
 */

public class MeemNetService extends Service {
    static String TAG = "MeemNetService";

    String mUpid = "notset";

    public interface MeemNetServiceListener {
        boolean onMNetSvcDiscoveryBCastReceived(String senderIp, String message);
        boolean MNetSvcTCPConnection(SSLSocket clientSocket);
    }

    public class LocalBinder extends Binder {
        public MeemNetService getInstance(){
            return MeemNetService.this;
        }
    }

    private final IBinder mIBinder = new LocalBinder();

    private Boolean mStopFlag = false;
    DatagramSocket mUdpBCastSocket;
    Thread UDPBroadcastListenerThread;

    MNetTCPServer mMNetTCPServer;

    MeemNetServiceListener mServiceListener;

    private void listenAndProcessClientRequestBCast(InetAddress broadcastIP, Integer port){
        boolean error = false;
        Log.d(TAG, "listenAndProcessClientRequestBCast");

        if (mUdpBCastSocket == null || mUdpBCastSocket.isClosed()) {
            try {
                mUdpBCastSocket = new DatagramSocket(port, broadcastIP);
                mUdpBCastSocket.setReuseAddress(true);
            } catch(Exception e) {
                Log.e(TAG, "Unable to reuse address: " + e.getMessage());
            }
        }

        byte[] recvBuf = new byte[MNetConstants.NW_BC_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

        Log.d(TAG, "Waiting for UDP broadcast");

        // take care of wifi being ON all the time and broadcast packet filtering.
        WifiManager wifiMgr = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock wifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "meemsrvwifilock");
        wifiLock.acquire();

        PowerManager powerMgr = (PowerManager) getApplication().getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "meemsrvwakelock");
        wakeLock.acquire();

        WifiManager.MulticastLock mcastLock = wifiMgr.createMulticastLock("meemsrvmcastlock");
        mcastLock.acquire();

        try {
            mUdpBCastSocket.receive(packet);
        } catch(Exception e) {
            Log.e(TAG, "Socket receive exception: " + e.getMessage());
            error = true;
        } finally {
            mUdpBCastSocket.close();

            mcastLock.release();
            wakeLock.release();
            wifiLock.release();
        }

        if(error) {
            Log.w(TAG, "Broadcast listening has encountred an error.");
            return;
        }

        String senderIP = packet.getAddress().getHostAddress();
        String message = new String(packet.getData()).trim();

        Log.d(TAG, "Got UDP broadcast from " + senderIP + ", message: " + message);
        if(mServiceListener != null) {
            Log.w(TAG, "Invoking listener on broadcast");
            mServiceListener.onMNetSvcDiscoveryBCastReceived(senderIP, message);
            sendResponseToClient(senderIP);
        } else {
            Log.w(TAG, "No listener for broadcast!");
        }
    }

    private void sendResponseToClient(String senderIP) {
        Log.d(TAG, "sendResponseToClient");

        try {
            InetAddress clientAddr = InetAddress.getByName(senderIP);
            String resp = MNetConstants.NW_SERVER_RES + mUpid;
            DatagramPacket respPacket = new DatagramPacket(resp.getBytes(), resp.getBytes().length, clientAddr, MNetConstants.NW_BCAST_RCV_PORT_CLIENT);
            DatagramSocket socket = new DatagramSocket();
            socket.send(respPacket);
        }catch(Exception e) {
            Log.e(TAG, "Error sending response to client: ", e);
        }
    }

    private void startListenForUDPBroadcast() {
        Log.d(TAG, "startListenForUDPBroadcast");
        UDPBroadcastListenerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    InetAddress broadcastIP = MNetUtils.getBroadcastAddress(getApplicationContext());
                    Integer port = MNetConstants.NW_BCAST_RCV_PORT_MASTER;
                    while (!mStopFlag) {
                        listenAndProcessClientRequestBCast(broadcastIP, port);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "No longer listening for UDP broadcasts cause of exception: " + e.getMessage());
                }
            }
        });

        UDPBroadcastListenerThread.start();
        Log.d(TAG, "UDPBroadcastListenerThread started");
    }

    private void stopBCastListener() {
        Log.d(TAG, "stopBCastListener");
        mStopFlag = true;
        mUdpBCastSocket.close();
        Log.d(TAG, "BCast listener stopped");
    }

    private void startTCPServer() {
        Log.d(TAG, "startTCPServer");

        Context appCtxt = getApplication().getApplicationContext();
        mMNetTCPServer = new MNetTCPServer(appCtxt, new MNetTCPServer.MeemNetTCPServerListener() {
            @Override
            public void onClientConnect(SSLSocket clientSocket) {
                if(mServiceListener != null) {
                    Log.d(TAG, "onClientConnect: Invoking listener");
                    mServiceListener.MNetSvcTCPConnection(clientSocket);
                } else {
                    Log.d(TAG, "onClientConnect: No listener!");
                    try {
                        Log.d(TAG, "onClientConnect: Closing client socket");
                        clientSocket.close();
                    } catch (Exception e) {
                        Log.d(TAG, "onClientConnect: Exception on closing client socket: " + e.getMessage());
                    }
                }
            }
        });

        mMNetTCPServer.start();
    }

    private void stopTCPServer() {
        Log.d(TAG, "stopTCPServer");
        mMNetTCPServer.stop();
    }

    // ===========================================================================================
    // ------------------------ Android service related overrides
    // ===========================================================================================

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopBCastListener();
        stopTCPServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        mStopFlag = false;
        startListenForUDPBroadcast();

        startTCPServer();

        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mIBinder;
    }

    // ===========================================================================================
    // ------------------------ Public methods for Meem App
    // ===========================================================================================

    public void setOnServiceListener(MeemNetServiceListener serviceListener, String upid){
        mServiceListener = serviceListener;
        mUpid = upid;
    }
}
