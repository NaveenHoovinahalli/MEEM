package com.meem.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.meem.androidapp.MainActivity;
import com.meem.events.ResponseCallback;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by arun on 14/11/16.
 */

public class UnregisteredPhoneAuthUi extends PinEntryUi {
    UnregisteredPhoneAuthUiListener mListener;

    public UnregisteredPhoneAuthUi(FrameLayout viewRoot, UnregisteredPhoneAuthUiListener listener) {
        super(viewRoot, (MainActivity) listener.getActivity());
        mListener = listener;
    }


    protected void showCableSetupMessage() {
        mPinAuthTV.setVisibility(View.VISIBLE);
        mForgotPassword.setVisibility(View.VISIBLE);
        isForgotPassword = true;

    }

    protected void processPassword() {
        mNewPassword = mPasswordET.getText().toString();

        mainActivity.setBackPressDisable(true);
        mListener.onUnregisteredPhoneAuthEntry(mNewPassword, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mainActivity.setBackPressDisable(false);
                if (result) {
                    View view = mListener.getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    } else {
                        // TODO
                    }
                } else {
                    mPasswordET.setText(null);
                    setCircaleInvisible();
                    showSoftKey();
                }
                return false;
            }
        });
    }

    protected void validateFromCable() {

        LinkedHashMap<Integer, String> recoveryQuestions = new LinkedHashMap<>();

        recoveryQuestions.put(1, mFirstAnswer.getText().toString().trim().toLowerCase());
        recoveryQuestions.put(2, mSecondAnswer.getText().toString().trim().toLowerCase());
        recoveryQuestions.put(3, mThirdAnswer.getText().toString().trim().toLowerCase());

        mainActivity.setBackPressDisable(true);
        mListener.onValidateRecoveryQuestions(recoveryQuestions, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mainActivity.setBackPressDisable(false);
                if (result) {
                    View view = mListener.getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    } else {
                        // TODO
                    }
                } else {
//                    recovery answers are wrong
                    showRecoveryQuestions();
                }
                return false;
            }
        });

    }

    // Is implemented by MiddleSegmentUi
    public interface UnregisteredPhoneAuthUiListener {
        void onUnregisteredPhoneAuthEntry(String pin, ResponseCallback responseCallback);
        void onValidateRecoveryQuestions(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
        Activity getActivity();
    }
}
