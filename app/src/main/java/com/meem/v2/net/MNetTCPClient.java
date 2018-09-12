package com.meem.v2.net;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.meem.androidapp.R;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by arun on 20/6/17.
 */

public class MNetTCPClient {
    private static final String TAG = "MNetTCPClient";
    private Context mAppContext;

    private InputStream mInStream;
    private OutputStream mOutStream;

    public MNetTCPClient(Context appContext) {
        mAppContext = appContext;
    }

    public boolean connect(String serverIp) {
        Log.d(TAG, "connect");

        boolean result = true;

        try {
            // Load the mServerSocket keystore
            int bksResource = R.raw.trustedcertstore;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Log.w(TAG, "Loading compatibility bks");
                bksResource = R.raw.trustedcertstore_compat;
            }

            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(mAppContext.getResources().openRawResource(bksResource), "meemindia".toCharArray());

            // Create a custom trust manager that accepts the mServerSocket self-signed certificate
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            // Create the SSLContext for the SSLSocket to use
            SSLContext sslctx = SSLContext.getInstance("TLS");
            sslctx.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            // Create SSLSocketFactory
            SSLSocketFactory factory = sslctx.getSocketFactory();

            // Create mUdpBCastSocket using SSLSocketFactory
            SSLSocket client = (SSLSocket) factory.createSocket(serverIp, MNetConstants.NW_TCP_PORT);

            // Print system information
            Log.d(TAG, "Connected to mServerSocket " + client.getInetAddress() + ": " + client.getPort());

            // Writer and Reader
            mOutStream = client.getOutputStream();
            mInStream = client.getInputStream();
        } catch (Exception e) {
            Log.e(TAG, "Exception while connecting: " + e.getMessage());
            result = false;
        }

        return result;
    }

    public InputStream getInputStream() {
        return mInStream;
    }

    public OutputStream getOutputStream() {
        return mOutStream;
    }

    public boolean close() {
        Log.d(TAG, "close");

        boolean result = true;

        try {
            if (mOutStream != null) mOutStream.close();
            if (mInStream != null) mInStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception during close: " + e.getMessage());
            result = false;
        }

        return result;
    }

    public boolean send(ByteBuffer data) {
        Log.d(TAG, "send");
        boolean result = true;

        if (mOutStream == null) {
            return false;
        }

        try {
            mOutStream.write(data.array());
        } catch (Exception e) {
            Log.e(TAG, "Exception in data write: " + e.getMessage());
            result = false;
        }

        return result;
    }
}
