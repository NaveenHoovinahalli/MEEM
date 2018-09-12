package com.meem.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.utils.MeemTextView;

/**
 * Created by arun on 3/10/16.
 */

public class AutoBackupCountDownUi {
    CountDownTimer mTimer;
    UiContext mUiCtxt;
    AutoBackupCountDownUiListener mListener;
    private ViewGroup mParentLayout;
    private View mMainLayout;
    private MeemTextView mCountDownTv, mPrompt;

    public AutoBackupCountDownUi(ViewGroup parent, AutoBackupCountDownUiListener listener) {
        mUiCtxt = UiContext.getInstance();
        mParentLayout = parent;
        mListener = listener;
    }

    public void create() {
        LayoutInflater inflater = (LayoutInflater) mUiCtxt.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMainLayout = inflater.inflate(R.layout.autobackup_countdown, mParentLayout, false);

        mCountDownTv = (MeemTextView) mMainLayout.findViewById(R.id.autobackup_count_text);
        mCountDownTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40f);
        mCountDownTv.setText("21");

        //  TODO: For testing only.
        mCountDownTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimer != null) {
                    mTimer.cancel();
                }

                mListener.onAutoBackupCountDownEnd(false);
            }
        });

        mPrompt = (MeemTextView) mMainLayout.findViewById(R.id.autobackup_prompt);
        mPrompt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f);

        mParentLayout.addView(mMainLayout);

        startTimer();
    }

    private void startTimer() {
        if (mTimer != null) {
            throw new IllegalStateException("Countdown timer already exists!");
        }

        mTimer = new CountDownTimer(ProductSpecs.AUTOBACKUP_COUNTDOWN_MS, ProductSpecs.AUTOBACKUP_COUNTDOWN_TICK_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                mCountDownTv.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                mListener.onAutoBackupCountDownEnd(true);
            }
        }.start();
    }

    public void destroy() {
        stopTimer();

        ValueAnimator alphaAnim = ValueAnimator.ofFloat(1.0f, 0.0f);

        alphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                mCountDownTv.setAlpha(val);
                mPrompt.setAlpha(val);
            }
        });

        alphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mParentLayout.removeView(mMainLayout);
                mListener.onAutoBackupCountDownUiFinish();
            }
        });

        alphaAnim.setDuration(ProductSpecs.DEFAULT_ANIM_DURATION);
        alphaAnim.start();
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        mTimer = null;
    }

    public interface AutoBackupCountDownUiListener {
        void onAutoBackupCountDownEnd(boolean startAutoBackup);
        void onAutoBackupCountDownUiFinish();
    }

    ;
}
