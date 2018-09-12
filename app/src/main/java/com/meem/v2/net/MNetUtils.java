package com.meem.v2.net;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.util.Log;

import com.meem.androidapp.UiContext;
import com.meem.utils.DebugTracer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


/**
 * Created by arun on 7/8/17.
 */

public class MNetUtils {
    static String TAG = "MNetUtils";

    //172.16.238.255 //172.16.238.42 //192.168.1.255
    public static InetAddress getBroadcastAddress(Context appContext) {
        Log.d(TAG, "getBroadcastAddress");

        WifiManager wifi = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        if (null == dhcp) {
            Log.e(TAG, "Unable to get dhcp info");
            return null;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];

        for (int k = 0; k < 4; k++)
            /*quads[k] = (byte) (broadcast >> (k * 8));*/ //TODO: Cross check
            quads[k] = (byte) (0xFF);

        InetAddress bca = null;

        try {
            bca = InetAddress.getByAddress(quads);
            Log.d(TAG, "Broadcast address: " + bca.getHostAddress());
        } catch (IOException e) {
            Log.e(TAG, "Address conversion failed: " + e.getMessage());
        }

        return bca;
    }

    public static InetAddress getAllcastAddress() {
        InetAddress bca = null;

        try {
            bca = InetAddress.getByName("0.0.0.0");
            Log.d(TAG, "Allcast address: " + bca.getHostAddress());
        } catch (IOException e) {
            Log.e(TAG, "Address conversion failed: " + e.getMessage());
        }

        return bca;
    }

    public static boolean broadcast(String data, int port) {
        Log.d(TAG, "broadcast");
        boolean result = true;

        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            // Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] sendData = data.getBytes();
            InetAddress bca = getBroadcastAddress(UiContext.getInstance().getApplicationContext());

            if(null == bca) {
                Log.e(TAG, "could not get bcast address!");
                result = false;
            } else {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, bca, port);
                socket.send(sendPacket);
                Log.d(TAG, "Broadcast packet sent to: " + bca.getHostAddress());
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            result = false;
        }

        return result;
    }
}
