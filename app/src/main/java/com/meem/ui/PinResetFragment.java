package com.meem.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTextView;


/**
 * Created by Naveen on 7/18/2016.
 *
 */
@SuppressLint("ValidFragment")
public class PinResetFragment extends Fragment implements TextWatcher, View.OnClickListener, MainActivity.ToolBarItemClickListener {


    EditText mPasswordET;
    ImageView mCircle1, mCircle2, mCircle3, mCircle4;
    View mRootView;
    MainActivity mainActivity;
    RelativeLayout mMainView;
    MeemTextView mHeaderTV, mPinSetupTV, mPinAuthTV;

    PinChangeFragment.PinChangeInterface mListener;
    View mViewOne, mViewTwo, mViewThree, mViewFour;
    LinearLayout mTextEntryView;
    private ProgressDialog mInitProgress;


    public PinResetFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
        setEventListener(mainActivity.getCablePresenter());
    }

    private void setEventListener(PinChangeFragment.PinChangeInterface pinChangeInterface) {
        mListener = pinChangeInterface;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.pinchangefragment, null);
        init();
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showSoftKey();
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.setEventListner(this);
        setEventListner(mainActivity.getCablePresenter());
        mainActivity.setAppTitle(getResources().getString(R.string.reset_cable));

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    private void showSoftKey() {
        mPasswordET.removeTextChangedListener(this);

        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        mPasswordET.requestFocus();
        mPasswordET.addTextChangedListener(this);
        mMainView.setClickable(true);
    }

    private void init() {
        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewId();
        mPasswordET.requestFocus();
        mMainView.setOnClickListener(this);
        mPinAuthTV.setVisibility(View.VISIBLE);
        mPinAuthTV.setText(getString(R.string.reset_warning));
    }

    private void findViewId() {

        mMainView = (RelativeLayout) mRootView.findViewById(R.id.mainLL);
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

    @Override
    public void afterTextChanged(Editable s) {

    }

    private void validatePassword() {
        mainActivity.showSimpleProgressBar(0, mInitProgress, "");

        mListener.onValidatePassword(mPasswordET.getText().toString(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                if (result) {
                    mInitProgress.dismiss();
                    showAlertDialoge();
                } else {

                    mInitProgress.dismiss();

                    Animation shake;
                    shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_animation);
                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(300);
                    mTextEntryView.startAnimation(shake);
                    mPasswordET.setText(null);
                    setCircaleInvisible();
                    showSoftKey();
                }
                return false;
            }
        });


    }

    private void showAlertDialoge() {
        AlertDialog.Builder alertDialoge = new AlertDialog.Builder(getActivity());
        alertDialoge.setMessage(getString(R.string.reset_warning));
        alertDialoge.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetCable();
            }
        });
        alertDialoge.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mInitProgress.dismiss();
                mainActivity.onBackPressed();
            }
        });

        AlertDialog alertDialog = alertDialoge.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();

    }

    private void resetCable() {

/*        showSimpleProgressBar(0,mInitProgress,"");

        mListener.onResetCable(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                if (!result) {
                    mInitProgress.dismiss();
                    mainActivity.showToast(getString(R.string.failed));
                }


                return false;
            }
        });*/
    }


    private void showInitializingProgressBar(int i, ProgressDialog mInitProgress, String s) {
        mInitProgress.setMessage("Test");
        mInitProgress.setTitle("Hello");
        mInitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mInitProgress.setIndeterminate(true);
        mInitProgress.setCancelable(false);
        mInitProgress.setProgress(i);
        mInitProgress.show();
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mInitProgress != null)
            mInitProgress.dismiss();
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
    public void onClick(View v) {

        showSoftKey();
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


}

/*mPasswordET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
@Override
public void onFocusChange(View mMainView, boolean hasFocus) {
        if(hasFocus){
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mPasswordET, InputMethodManager.SHOW_IMPLICIT);
        Toast.makeText(getActivity(), "got the focus", Toast.LENGTH_LONG).show();
        }else {
        Toast.makeText(getActivity(), "lost the focus", Toast.LENGTH_LONG).show();
        hideSoftKey();
        }
        }
        });*/
