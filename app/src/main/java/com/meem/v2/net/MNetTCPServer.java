package com.meem.v2.net;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.meem.androidapp.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Enumeration;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


/**
 * Created by arun on 20/6/17.
 */

public class MNetTCPServer {
    private static final String TAG = "MNetTCPServer";
    private Context mAppContext;

    private boolean mStop = false;

    private Thread mServerThread;
    SSLServerSocket mServerSocket;

    public interface MeemNetTCPServerListener {
        void onClientConnect(SSLSocket client);
    }

    MeemNetTCPServerListener mListener;

    public MNetTCPServer(Context appContext, MeemNetTCPServerListener listener) {
        mAppContext = appContext;
        mListener = listener;
    }

    public void start() {
        Log.d(TAG, "start");

        try {
            // Load the mServerSocket keystore: but take care of older android versions which uses bks-v1.
            int bksResource = R.raw.trustedcertstore;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Log.w(TAG, "Loading compatibility bks");
                bksResource = R.raw.trustedcertstore_compat;
            }

            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(mAppContext.getResources().openRawResource(bksResource), "meemindia".toCharArray());
            // debugPrintKeyStoreInfo(keyStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "meemindia".toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            SSLContext sslctx = SSLContext.getInstance("TLS");
            sslctx.init(keyManagers, trustManagers, new SecureRandom());
            // debugPrintSSLInfo(sslctx);

            SSLServerSocketFactory factory = sslctx.getServerSocketFactory();
            mServerSocket = (SSLServerSocket) factory.createServerSocket(MNetConstants.NW_TCP_PORT);
        } catch (Exception e) {
            Log.e(TAG, "Exception while preparing for ssl connections: " + e.getMessage());
            return;
        }

        mServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mStop) {
                    try {

                        final SSLSocket clientSocket = (SSLSocket) mServerSocket.accept();
                        Log.d(TAG, "New client: " + clientSocket.getRemoteSocketAddress().toString());

                        clientSocket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                            public void handshakeCompleted(HandshakeCompletedEvent arg0) {
                                Log.d(TAG, "handshakeCompleted");
                                mListener.onClientConnect(clientSocket);
                            }
                        });

                        clientSocket.startHandshake();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while accepting connections: " + getStackTrace(e));
                        mStop = true;
                    }
                }
            }
        });

        mServerThread.start();
        Log.d(TAG, "Server thread started");
    }

    public void stop() {
        Log.d(TAG, "stop");

        mStop = true;

        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Exception while closing server mUdpBCastSocket: " + e.getMessage());
            }
        }
    }

    private void debugPrintSSLInfo(SSLContext sslctx) {
        String[] ciphers = sslctx.getDefaultSSLParameters().getCipherSuites();
        for (String cipher : ciphers) {
            Log.w(TAG, "Cipher: " + cipher + "\n");
        }

        String[] protos = sslctx.getDefaultSSLParameters().getProtocols();
        for (String proto : protos) {
            Log.w(TAG, "Protocol: " + proto + "\n");
        }
    }

    private void debugPrintKeyStoreInfo(KeyStore keyStore) {
        try {
            int size = keyStore.size();
            Log.d(TAG, "keystore size: " + size);

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Log.w(TAG, "alias: " + alias);

                Key key = keyStore.getKey(alias, "meemindia".toCharArray());
                if (key != null) {
                    Log.w(TAG, "key algo: " + key.getAlgorithm() + ", fmt: " + key.getFormat() + ", encdata: " + key.getEncoded());
                } else {
                    Log.e(TAG, "key algo: " + "null!" + ", fmt: " + "null!" + ", encdata: " + "null!");
                }

                Certificate cert = keyStore.getCertificate(alias);
                Log.w(TAG, "cert: " + cert.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "exception while analysing key store: " + getStackTrace(e));
        }
    }

    private String getStackTrace(Throwable ex) {
        String stackTrace = "";
        if (ex != null) {
            stackTrace += ((Exception) ex).getMessage() + "\n";

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ((Exception) ex).printStackTrace(pw);
            stackTrace += sw.toString();
        }

        return stackTrace;
    }
}
