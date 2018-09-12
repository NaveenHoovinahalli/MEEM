package com.meem.ui;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.utils.JustifiedTextView;
import com.meem.utils.GenUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

@SuppressLint("ClickableViewAccessibility")
public class LegalDetailsFragment extends Fragment {
    private static final String tag = "LegalDetailsFragment";
    final String TYPE = "type";
    MainActivity mActivity;
    LinearLayout mFragLayout;
    JustifiedTextView mLegalTextView;
    ScrollView mScroller;
    String mType;
    LegalAgreementType mAgreementType;
    UiContext mUiCtxt = UiContext.getInstance();
    private GestureDetectorCompat gDetect;

    public void setAgreementType(LegalAgreementType type) {
        mAgreementType = type;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dbgTrace();

        mType = getArguments().getString(TYPE);
        if (mType.equals("EULA")) setAgreementType(LegalAgreementType.EULA);
        else if (mType.equals("WARRANTY")) setAgreementType(LegalAgreementType.WARRANTY);
        else setAgreementType(LegalAgreementType.FIRMWARE);

        View v = inflater.inflate(R.layout.legaldetailsfrag, container, false);
        mScroller = (ScrollView) v.findViewById(R.id.legal_scroller);
        mLegalTextView = (JustifiedTextView) v.findViewById(R.id.legal_text);

        mLegalTextView.setAlignment(Align.LEFT);
        mLegalTextView.setLineSpacing(15);
        mLegalTextView.setTextColor(Color.WHITE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        dbgTrace();
        super.onActivityCreated(savedInstanceState);

        mActivity = (MainActivity) getActivity();

		/* mLegalTextView.setTypeface(mActivity.getTypeface("DM"));*/

        // Need to show the legal stuff, USA has a special one.
        Locale defLocale = Locale.getDefault();
        boolean isUSA = Locale.US.equals(defLocale);

        String legalTextFile;


        switch (mAgreementType) {

            case EULA:
                legalTextFile = isUSA ? "text/eula_usa.txt" : "text/eula_world.txt";
                break;
            case FIRMWARE:
                legalTextFile = isUSA ? "text/fw_license_usa.txt" : "text/fw_license_world.txt";
                break;
            case WARRANTY:
                Log.d("legal", "warranty");
                legalTextFile = isUSA ? "text/warranty_usa.txt" : "text/warranty_world.txt";
                break;
            default:
                Log.d("legal", "default");

                legalTextFile = "text/eula_usa.txt"; // funny...
                break;
        }

        // Programmatically load text from an asset and place it into the
        // text view.  Note that the text we are loading is UTF-8, so we
        // need to convert it to UTF-16.
        try {
            InputStream is = mUiCtxt.getAppContext().getAssets().open(legalTextFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            // LOL... last line cut off issue solution on Nexus 6 :/
            sb.append("\n \n ");

            mLegalTextView.setText(sb.toString());
        } catch (IOException e) {
            // Should never happen!
            dbgTrace("Error loading legal doc: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(new Bundle());
    }

    // ================= debug support =============
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("LegalDetailsFragment.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("LegalDetailsFragment.log");
    }

    public enum LegalAgreementType {
        EULA, FIRMWARE, WARRANTY
    }
}
