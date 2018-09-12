package com.meem.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.R;

/**
 * Created by SCS on 8/12/2016.
 */
public class MeemWebsiteView extends Fragment {

    View mRootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.websitewebview, null, false);
        init();
        return mRootView;
    }

    private void init() {
        WebView mWebview = (WebView) mRootView.findViewById(R.id.meemWebsite);
        mWebview.loadUrl(ProductSpecs.MEEM_WEB_SITE_URL);
        mWebview.setWebViewClient(new WebViewClient());

/*
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });*/


    }
}
