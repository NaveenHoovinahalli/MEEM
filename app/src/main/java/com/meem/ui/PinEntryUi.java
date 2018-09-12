package com.meem.ui;

/**
 * Created by arun on 14/11/16.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.utils.MeemTextView;

/**
 * Created by arun on 10/11/16.
 */

public class PinEntryUi implements TextWatcher, View.OnClickListener {
    UiContext mUiCtxt = UiContext.getInstance();

    FrameLayout mViewRoot;

    EditText mPasswordET,mFirstAnswer,mSecondAnswer,mThirdAnswer;
    ImageView mCircle1, mCircle2, mCircle3, mCircle4;

    RelativeLayout mPinEntryLayout,mRecoveryQuestionsLayout;

    MainActivity mainActivity;
    String mNewPassword;
    boolean isValidCablePassword = false, isFirstAttempt = false;
    MeemTextView mHeaderTV, mPinSetupTV, mPinAuthTV,mForgotPassword;
    RelativeLayout mMainView;
    ProgressBar mPbar;
    View mViewOne, mViewTwo, mViewThree, mViewFour;
    Button mdone;

    boolean isForgotPassword=false;
    public PinEntryUi(FrameLayout viewRoot, MainActivity activity) {
        mViewRoot = viewRoot;
        mainActivity = activity;
    }

    public void create() {
        LayoutInflater inflater = (LayoutInflater) mUiCtxt.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMainView = (RelativeLayout) inflater.inflate(R.layout.pinentryuifragment, mViewRoot, false);

        mPbar = (ProgressBar) mMainView.findViewById(R.id.pBar);

        mPasswordET = (EditText) mMainView.findViewById(R.id.passwordET);
        mCircle1 = (ImageView) mMainView.findViewById(R.id.password1);
        mCircle2 = (ImageView) mMainView.findViewById(R.id.password2);
        mCircle3 = (ImageView) mMainView.findViewById(R.id.password3);
        mCircle4 = (ImageView) mMainView.findViewById(R.id.password4);
        mHeaderTV = (MeemTextView) mMainView.findViewById(R.id.pinHeaderTV);

        mPinSetupTV = (MeemTextView) mMainView.findViewById(R.id.pinsetupTV);
        mPinAuthTV = (MeemTextView) mMainView.findViewById(R.id.pinauthTV);

        mViewOne = mMainView.findViewById(R.id.viewOne);
        mViewTwo = mMainView.findViewById(R.id.viewTwo);
        mViewThree = mMainView.findViewById(R.id.viewThree);
        mViewFour = mMainView.findViewById(R.id.viewFour);

        mPinEntryLayout= (RelativeLayout) mMainView.findViewById(R.id.pin_setup_rl);
        mRecoveryQuestionsLayout= (RelativeLayout) mMainView.findViewById(R.id.recovery_questions_rl);
        mPinEntryLayout.setVisibility(View.VISIBLE);
        mRecoveryQuestionsLayout.setVisibility(View.GONE);

        mFirstAnswer= (EditText) mMainView.findViewById(R.id.first_answer);
        mSecondAnswer= (EditText) mMainView.findViewById(R.id.second_answer);
        mThirdAnswer= (EditText) mMainView.findViewById(R.id.third_answer);
        mdone= (Button) mMainView.findViewById(R.id.done);


        mForgotPassword= (MeemTextView) mMainView.findViewById(R.id.forgotpassword);
        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecoveryQuestions();
            }
        });

        mMainView.setOnClickListener(this);
        mdone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mdone.setClickable(false);
                validateQuestions();

            }
        });


        mViewRoot.addView(mMainView);

        showCableSetupMessage();
        showKeyPad();

    }

    protected void showCableSetupMessage() {

    }

    @Override
    public void onClick(View v) {
        showSoftKey();
    }

    protected void showKeyPad() {

        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        mPasswordET.requestFocus();
        mPasswordET.addTextChangedListener(this);
    }

    protected void showSoftKey() {
        mPasswordET.removeTextChangedListener(this);

        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        mPasswordET.requestFocus();
        mPasswordET.addTextChangedListener(this);
        mMainView.setClickable(true);

    }

    protected void hideSoftKey() {
        View view = mainActivity.getCurrentFocus();
        if (mPasswordET!=null) {
            InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPasswordET.getWindowToken(), 0);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing.
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        if (mPasswordET.getText().length() == 0) {
            setCircaleInvisible();
        } else if (mPasswordET.getText().length() == 1) {
            mCircle1.setVisibility(View.VISIBLE);
            mCircle2.setVisibility(View.GONE);
            mCircle3.setVisibility(View.GONE);
            mCircle4.setVisibility(View.GONE);

            mViewOne.setVisibility(View.GONE);
            mViewTwo.setVisibility(View.VISIBLE);
            mViewThree.setVisibility(View.VISIBLE);
            mViewFour.setVisibility(View.VISIBLE);
        } else if (mPasswordET.getText().length() == 2) {
            mCircle1.setVisibility(View.VISIBLE);
            mCircle2.setVisibility(View.VISIBLE);
            mCircle3.setVisibility(View.GONE);
            mCircle4.setVisibility(View.GONE);

            mViewOne.setVisibility(View.GONE);
            mViewTwo.setVisibility(View.GONE);
            mViewThree.setVisibility(View.VISIBLE);
            mViewFour.setVisibility(View.VISIBLE);
        } else if (mPasswordET.getText().length() == 3) {
            mCircle1.setVisibility(View.VISIBLE);
            mCircle2.setVisibility(View.VISIBLE);
            mCircle3.setVisibility(View.VISIBLE);
            mCircle4.setVisibility(View.GONE);

            mViewOne.setVisibility(View.GONE);
            mViewTwo.setVisibility(View.GONE);
            mViewThree.setVisibility(View.GONE);
            mViewFour.setVisibility(View.VISIBLE);
        } else if (mPasswordET.getText().length() == 4) {
            mMainView.setClickable(false);
            hideSoftKey();
            mCircle1.setVisibility(View.VISIBLE);
            mCircle2.setVisibility(View.VISIBLE);
            mCircle3.setVisibility(View.VISIBLE);
            mCircle4.setVisibility(View.VISIBLE);

            mViewOne.setVisibility(View.GONE);
            mViewTwo.setVisibility(View.GONE);
            mViewThree.setVisibility(View.GONE);
            mViewFour.setVisibility(View.GONE);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    processPassword();
                }
            }, 500);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Nothing
    }

    /**
     * Must be overriden by derived classes
     */
    protected void processPassword() {
        // Override and use!
    }

    protected void updateToCable() {
        // Override and use!
    }

    protected void validateFromCable() {
        // Override and use!
    }

    protected void setCircaleInvisible() {
        mCircle1.setVisibility(View.GONE);
        mCircle2.setVisibility(View.GONE);
        mCircle3.setVisibility(View.GONE);
        mCircle4.setVisibility(View.GONE);

        mViewOne.setVisibility(View.VISIBLE);
        mViewTwo.setVisibility(View.VISIBLE);
        mViewThree.setVisibility(View.VISIBLE);
        mViewFour.setVisibility(View.VISIBLE);
    }

    public void destroy(final Runnable mRunnableOnFinish) {
        ValueAnimator alphaAnim = ValueAnimator.ofFloat(1.0f, 0.0f);

        alphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                mMainView.setAlpha(val);
            }
        });

        alphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                mViewRoot.removeView(mMainView);
                /*mViewRoot.removeAllViews();*/

                if (null != mRunnableOnFinish) {
                    mRunnableOnFinish.run();
                }
            }
        });

        alphaAnim.setDuration(ProductSpecs.DEFAULT_ANIM_DURATION);
        alphaAnim.start();
    }

    public void validateQuestions(){
        if(mFirstAnswer.getText().toString().isEmpty() && mSecondAnswer.getText().toString().isEmpty() && mThirdAnswer.getText().toString().isEmpty()){
            mainActivity.showToast("Please enter the answers");
            mdone.setClickable(true);

            return;
        }


//        Atleast two answers should be entered
         if(isForgotPassword) {
             if((mFirstAnswer.getText().toString().isEmpty() && mSecondAnswer.getText().toString().isEmpty() ) || (mFirstAnswer.getText().toString().isEmpty() && mThirdAnswer.getText().toString().isEmpty()) || (mSecondAnswer.getText().toString().isEmpty() && mThirdAnswer.getText().toString().isEmpty())) {
                 mainActivity.showToast("Please enter at least two answers");
                 mdone.setClickable(true);
                 return;
             }
             validateFromCable();
         }
        else {
             //    All answer should be entered

             if(mFirstAnswer.getText().toString().isEmpty() ||  mSecondAnswer.getText().toString().isEmpty() || mThirdAnswer.getText().toString().isEmpty()){
                 mainActivity.showToast("Please enter all the answers");
                 mdone.setClickable(true);
                 return;
             }
             updateToCable();
         }
    }

    public void showRecoveryQuestions(){
        mdone.setClickable(true);
        mPinEntryLayout.setVisibility(View.GONE);
        mRecoveryQuestionsLayout.setVisibility(View.VISIBLE);
        mFirstAnswer.requestFocus();

    }
}
