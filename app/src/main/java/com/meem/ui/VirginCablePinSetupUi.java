package com.meem.ui;

import android.app.Activity;
import android.content.Context;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by arun on 10/11/16.
 */

public class VirginCablePinSetupUi extends PinEntryUi implements TextWatcher, View.OnClickListener {
    VirginCablePinSetupUiListener mListener;

    public VirginCablePinSetupUi(FrameLayout viewRoot, VirginCablePinSetupUiListener listener) {
        super(viewRoot, (MainActivity) listener.getActivity());
        mListener = listener;

        isFirstAttempt = true;
    }


    protected void showCableSetupMessage() {
        mPinSetupTV.setVisibility(View.VISIBLE);
        mPinAuthTV.setVisibility(View.VISIBLE);
    }

    protected void processPassword() {
        if (isFirstAttempt) {
            mNewPassword = mPasswordET.getText().toString();
            mPasswordET.setText(null);
            setCircaleInvisible();
            isFirstAttempt = false;
            mHeaderTV.setText(R.string.confirmnewpin);
            showSoftKey();
        } else {
            String repeatPassword = mPasswordET.getText().toString();
            if (mNewPassword.equals(repeatPassword)) {
//                here we are adding code to get the answer for recovery questions
                showRecoveryQuestions();

            } else {
                mHeaderTV.setText(R.string.cable_setup);
                isFirstAttempt = true;
                mPasswordET.setText(null);
                setCircaleInvisible();
                showSoftKey();
            }
        }
    }


    public void updateToCable() {
        LinkedHashMap<Integer, String> recoveryQuestions = new LinkedHashMap<>();
        recoveryQuestions.put(1, mFirstAnswer.getText().toString().trim().toLowerCase());
        recoveryQuestions.put(2, mSecondAnswer.getText().toString().trim().toLowerCase());
        recoveryQuestions.put(3, mThirdAnswer.getText().toString().trim().toLowerCase());

        mainActivity.setBackPressDisable(true);
        mListener.onVirginCablePinEntry(mNewPassword, recoveryQuestions, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mainActivity.setBackPressDisable(false);
                if (result) {
                    View view = mListener.getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    } else {
                        mainActivity.onCriticalError("PIN setup failed!");
                    }
                }
                return result;
            }
        });
    }

    /**
     * MiddleSegmentUi must implement this.
     */
    public interface VirginCablePinSetupUiListener {
        void onVirginCablePinEntry(String pin, LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
        Activity getActivity();
    }
}
