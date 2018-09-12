package com.meem.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Arun T A
 */

public class AsyncDownload extends AsyncTask<String, String, String> {
    private UiContext mUiCtxt = UiContext.getInstance();

    private String mDestPath = "";
    private int mId = 0;
    private boolean mResult = false;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    public void setExtraParams(String dPath, int id) {
        mDestPath = dPath;
        mId = id;
    }

    @SuppressWarnings("unused")
    @Override
    protected String doInBackground(String... urls) {
        try {
            Log.d("AsyncDownload", "Starting download: " + urls[0]);

            URL url = new URL(urls[0]);
            URLConnection conection = url.openConnection();

            try {
                conection.addRequestProperty("Cache-Control", "no-cache");
            } catch (Exception e) {
                // no problem.
            }

            conection.connect();

            // getting file length
            int lenghtOfFile = conection.getContentLength();

            // input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            // output stream to write file
            OutputStream output = new FileOutputStream(mDestPath);

            byte data[] = new byte[1024];
            long total = 0;
            int count = 0, progress = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                progress = (int) ((total * 100l) / lenghtOfFile);

                // writing data to file
                output.write(data, 0, count);

                // Post progress. TODO: ACTION_ is obsolete. Define events for
                // this.
                // mUiCtxt.execute(UiContext.ACTION_PROGRESS_UPDATE,
                // Integer.valueOf(progress));

                if (isCancelled()) break;
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

            if (!isCancelled()) {
                mResult = true;
                mUiCtxt.log(UiContext.INFO, "Download completed: " + urls[0]);
            } else {
                mUiCtxt.log(UiContext.INFO, "Download cancelled");
            }
        } catch (Exception e) {
            mResult = false;
            mUiCtxt.log(UiContext.EXCEPTION, "Download error: " + urls[0] + ": " + e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(String url) {
        MeemEvent evt;
        if (mResult) {
            evt = new MeemEvent(EventCode.WWW_DOWNLOAD_COMPLETED);
        } else {
            evt = new MeemEvent(EventCode.WWW_DOWNLOAD_FAILED);
        }

        evt.setInfo(Integer.valueOf(mId));
        mUiCtxt.postEvent(evt);
    }

    @Override
    protected void onCancelled() {
        MeemEvent evt = new MeemEvent(EventCode.WWW_DOWNLOAD_FAILED);
        evt.setInfo(Integer.valueOf(mId));
        mUiCtxt.postEvent(evt);
    }
}
