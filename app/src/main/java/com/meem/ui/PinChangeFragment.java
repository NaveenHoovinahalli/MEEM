package com.meem.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.ui.utils.MeemTextView;

import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * Created by naveen on 7/18/2016.
 * class is used to change the cable pin
 */
@SuppressLint("ValidFragment")
public class PinChangeFragment extends Fragment implements TextWatcher, View.OnClickListener, MainActivity.ToolBarItemClickListener {


    EditText mPasswordET, mFirstAnswer, mSecondAnswer, mThirdAnswer;
    RelativeLayout mPinEntryLayout, mRecoveryQuestionsLayout;


    ImageView mCircle1, mCircle2, mCircle3, mCircle4;
    View mRootView;
    MainActivity mainActivity;
    String mNewPassword;
    boolean isValidCablePassword = false, isFirstAttempt = true;
    MeemTextView mHeaderTV, mPinSetupTV, mPinAuthTV;
    RelativeLayout mMainView;
    PinChangeInterface mListener;
    RelativeLayout mProgressBar;
    TextView mProgressMsg;
    View mViewOne, mViewTwo, mViewThree, mViewFour;
    LinearLayout mTextEntryView;
    Button mdone;

    public PinChangeFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
        setEventListener(mainActivity.getCablePresenter());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.pinchangefragment, null);
        init();
        return mRootView;
    }


    public void setEventListener(PinChangeInterface listener) {
        mListener = listener;
    }

    private void init() {

        findViewId();
        setClickListener();
        mPinSetupTV.setVisibility(View.VISIBLE);

    }

    private void findViewId() {

        mMainView = (RelativeLayout) mRootView.findViewById(R.id.mainLL);
        mProgressBar = (RelativeLayout) mRootView.findViewById(R.id.lProbressBar);
        mProgressMsg = (TextView) mRootView.findViewById(R.id.progressMsg);

        mPasswordET = (EditText) mRootView.findViewById(R.id.passwordET);
        mCircle1 = (ImageView) mRootView.findViewById(R.id.password1);
        mCircle2 = (ImageView) mRootView.findViewById(R.id.password2);
        mCircle3 = (ImageView) mRootView.findViewById(R.id.password3);
        mCircle4 = (ImageView) mRootView.findViewById(R.id.password4);
        mHeaderTV = (MeemTextView) mRootView.findViewById(R.id.pinHeaderTV);

        mPinSetupTV = (MeemTextView) mRootView.findViewById(R.id.pinsetupTV);
        mPinAuthTV = (MeemTextView) mRootView.findViewById(R.id.pinauthTV);

        mViewOne = mRootView.findViewById(R.id.viewOne);
        mViewTwo = mRootView.findViewById(R.id.viewTwo);
        mViewThree = mRootView.findViewById(R.id.viewThree);
        mViewFour = mRootView.findViewById(R.id.viewFour);

        mTextEntryView = (LinearLayout) mRootView.findViewById(R.id.linearLayout22);

        mPinEntryLayout = (RelativeLayout) mMainView.findViewById(R.id.pin_setup_rl);
        mRecoveryQuestionsLayout = (RelativeLayout) mMainView.findViewById(R.id.recovery_questions_rl);
        mPinEntryLayout.setVisibility(View.VISIBLE);
        mRecoveryQuestionsLayout.setVisibility(View.GONE);

        mFirstAnswer = (EditText) mMainView.findViewById(R.id.first_answer);
        mSecondAnswer = (EditText) mMainView.findViewById(R.id.second_answer);
        mThirdAnswer = (EditText) mMainView.findViewById(R.id.third_answer);
        mdone = (Button) mMainView.findViewById(R.id.done);

        mdone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mdone.setClickable(false);
                validateQuestions();

            }
        });
    }


    private void setClickListener() {
        mMainView.setOnClickListener(this);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showKeyPad();
    }

    private void showKeyPad() {
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        mPasswordET.requestFocus();
        mPasswordET.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

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
                    validatePassword();
                }
            }, 500);


        }


    }

    private void validatePassword() {
        mProgressBar.setVisibility(View.VISIBLE);
//        setting the default value as true ,because after entering the valid password only he can come to this page
        isValidCablePassword = true;

        if (!isValidCablePassword) {
            mainActivity.setBackPressDisable(true);
            mListener.onValidatePassword(mPasswordET.getText().toString(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mProgressBar.setVisibility(View.GONE);
                    mainActivity.setBackPressDisable(false);

                    if (result) {
                        isValidCablePassword = true;
                        isFirstAttempt = true;
                        mPasswordET.setText(null);
                        setCircaleInvisible();
                        mHeaderTV.setText(getString(R.string.enterpin));
                        showSoftKey();
                    } else {
                        Animation shake;
                        shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_animation);
                        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                        if(null != vibrator) vibrator.vibrate(300);
                        mTextEntryView.startAnimation(shake);
                        mPasswordET.setText(null);
                        setCircaleInvisible();
                        showSoftKey();
                    }
                    return false;
                }
            });
        } else if (isFirstAttempt) {
            mProgressBar.setVisibility(View.GONE);
            mNewPassword = mPasswordET.getText().toString();
            mPasswordET.setText(null);
            setCircaleInvisible();
            isFirstAttempt = false;
            mHeaderTV.setText(getString(R.string.confirmnewpin));
            Toast.makeText(mainActivity, R.string.confirmnewpin, Toast.LENGTH_SHORT).show();
            showSoftKey();

        } else {
            if (mNewPassword.equals(mPasswordET.getText().toString())) {
                mProgressBar.setVisibility(View.GONE);
                showRecoveryQuestions();
            } else {
                Animation shake;
                shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_animation);
                Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                if(null != vibrator) vibrator.vibrate(300);
                mTextEntryView.startAnimation(shake);
                mProgressBar.setVisibility(View.GONE);
                mHeaderTV.setText(getString(R.string.enterpin));
                isFirstAttempt = true;
                mPasswordET.setText(null);
                setCircaleInvisible();
                showSoftKey();
            }
        }
    }

    private void setCircaleInvisible() {

        mCircle1.setVisibility(View.GONE);
        mCircle2.setVisibility(View.GONE);
        mCircle3.setVisibility(View.GONE);
        mCircle4.setVisibility(View.GONE);

        mViewOne.setVisibility(View.VISIBLE);
        mViewTwo.setVisibility(View.VISIBLE);
        mViewThree.setVisibility(View.VISIBLE);
        mViewFour.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();

    }


    @Override
    public void onResume() {
        super.onResume();
        mainActivity.setEventListner(this);
        setEventListner(mainActivity.getCablePresenter());
        mainActivity.setAppTitle(getResources().getString(R.string.pin_change));

    }


    public void showRecoveryQuestions() {
        mPinEntryLayout.setVisibility(View.GONE);
        mRecoveryQuestionsLayout.setVisibility(View.VISIBLE);
        mFirstAnswer.requestFocus();

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        showSoftKey();

    }


    public void validateQuestions() {
        //    All answer should be entered

        if (mFirstAnswer.getText().toString().isEmpty() || mSecondAnswer.getText().toString().isEmpty() || mThirdAnswer.getText().toString().isEmpty()) {
            mainActivity.showToast("Please enter all the answers");
            mdone.setClickable(true);
            return;
        }
        updateToCable();
    }


    private void updateToCable() {

        mProgressBar.setVisibility(View.VISIBLE);

        LinkedHashMap<Integer, String> recoveryQuestions = new LinkedHashMap<>();
        recoveryQuestions.put(1, mFirstAnswer.getText().toString().trim().toLowerCase());
        recoveryQuestions.put(2, mSecondAnswer.getText().toString().trim().toLowerCase());
        recoveryQuestions.put(3, mThirdAnswer.getText().toString().trim().toLowerCase());

        mainActivity.setBackPressDisable(true);
        mListener.onUpdatePassword(mNewPassword, recoveryQuestions, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mainActivity.setBackPressDisable(false);
                mProgressBar.setVisibility(View.GONE);
                if (result) {
                    mainActivity.showToast(getString(R.string.success));

                    View view = getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        mainActivity.onBackPressed();
                    } else {
                        mainActivity.showToast(getString(R.string.failed));
                    }
                }
                return false;
            }
        });
    }


    private void showSoftKey() {
        mPasswordET.removeTextChangedListener(this);
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        mPasswordET.requestFocus();
        mPasswordET.addTextChangedListener(this);
        mMainView.setClickable(true);
    }

    public void hideSoftKey() {

        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onEditOrCancelClickListner(String text) {

    }

    @Override
    public void onSaveClickListner() {

    }

    @Override
    public void onShareIconClickListener() {

    }

    @Override
    public void onRestoreIconListener() {

    }

    @Override
    public void onSelectIconClickListener() {

    }

    @Override
    public void onBackButtonPressed() {

    }


    @Override
    public void onDeleteIconClickListener() {

    }

    public interface PinChangeInterface {

        void onValidatePassword(String password, ResponseCallback responseCallback);
        void onUpdatePassword(String newPassword, LinkedHashMap<Integer, String> recoveryQuestions, ResponseCallback responseCallback);
    }
}
