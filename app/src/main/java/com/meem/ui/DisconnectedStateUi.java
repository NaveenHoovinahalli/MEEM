package com.meem.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meem.androidapp.AppPreferences;
import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;

import java.util.Date;

import static com.meem.androidapp.R.string.mins;

/**
 * Created by arun on 22/7/16.
 */
public class DisconnectedStateUi implements View.OnClickListener {
    UiContext mUiCtxt = UiContext.getInstance();

    FrameLayout mViewRoot;
    View mMainLayout;
    TextView mPromtTextView;
    ImageView mPromptImgView;
    TextView mLastBackupTime;

    LinearLayout mSearchLayout;
    ImageView mWifiIcon;
    TextView mWiFiSearchTV;

    String mDaysString = mUiCtxt.getAppContext().getResources().getString(R.string.days);
    String mDayString = mUiCtxt.getAppContext().getResources().getString(R.string.day);
    String mHrString = mUiCtxt.getAppContext().getResources().getString(R.string.hr);
    String mHoursString = mUiCtxt.getAppContext().getResources().getString(R.string.hrs);
    String mMinString = mUiCtxt.getAppContext().getResources().getString(R.string.min);
    String mMinsString = mUiCtxt.getAppContext().getResources().getString(mins);
    String mJustNowString = mUiCtxt.getAppContext().getResources().getString(R.string.just_now);
    String mAgoString = mUiCtxt.getAppContext().getResources().getString(R.string.ago);

    boolean mShowingCableConnected;
    /*ProgressDialog mInitProgress;*/
    boolean mShowingConnectMEEMCable;

    DisconnectedStateUiListener mListener;
    LayoutInflater inflater;
    private AlphaAnimation mAnimIn, mAnimOut;
    boolean mAnimateWifiIcon = false;


    public DisconnectedStateUi(FrameLayout viewRoot, DisconnectedStateUiListener listener) {
        mViewRoot = viewRoot;
        mListener = listener;
    }

    public void create() {
        inflater = (LayoutInflater) mUiCtxt.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMainLayout = inflater.inflate(R.layout.waiting_for_cable, mViewRoot, false);

        mPromtTextView = (TextView) mMainLayout.findViewById(R.id.prompt_for_connect_textview);
        mPromptImgView = (ImageView) mMainLayout.findViewById(R.id.prompt_for_connect_imageview);
        mLastBackupTime = (TextView) mMainLayout.findViewById(R.id.lastBackupTime);
        mSearchLayout = (LinearLayout) mMainLayout.findViewById(R.id.wifiSearchLL);
        mWifiIcon = (ImageView) mMainLayout.findViewById(R.id.wifiIcon);
        mWiFiSearchTV = (TextView) mMainLayout.findViewById(R.id.wifiSearchText);

        Typeface tf = Typeface.createFromAsset(mUiCtxt.getAppContext().getAssets(), "fonts/GMC-1.ttf");
        mPromtTextView.setTypeface(tf);
        mPromtTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f);

        String mStringWithoutColor = mUiCtxt.getAppContext().getResources().getString(R.string.connect_meem_cable);
        int mStartingPosition = mStringWithoutColor.indexOf("MEEM");
        Spannable wordtoSpan = new SpannableString(mStringWithoutColor);
        wordtoSpan.setSpan(new ForegroundColorSpan(mUiCtxt.getAppContext().getResources().getColor(R.color.meemGreen)), mStartingPosition, mStartingPosition + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mPromtTextView.setText(wordtoSpan);

        mShowingConnectMEEMCable = true;
        updateBackupTimeStamp();
        mViewRoot.addView(mMainLayout);

        mSearchLayout.setOnClickListener(this);


    }

    private void convertTestWIthColor(String mTextToConvert) {


    }

    public void showCableConnected() {

        String mStringWithoutColor = mUiCtxt.getAppContext().getResources().getString(R.string.meem_connected);
        int mStartingPosition = mStringWithoutColor.indexOf("MEEM");
        Spannable wordtoSpan = new SpannableString(mStringWithoutColor);
        wordtoSpan.setSpan(new ForegroundColorSpan(mUiCtxt.getAppContext().getResources().getColor(R.color.meemGreen)), mStartingPosition, mStartingPosition + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mPromtTextView.setText(wordtoSpan);


        mShowingConnectMEEMCable = false;
        mLastBackupTime.setVisibility(View.GONE);
        mSearchLayout.setVisibility(View.GONE);

        mShowingCableConnected = true;
    }

    public void showCableDisconnected() {
        /*if (mInitProgress != null) {
            mInitProgress.dismiss();
        }
*/
        String mStringWithoutColor = mUiCtxt.getAppContext().getResources().getString(R.string.connect_meem_cable);
        int mStartingPosition = mStringWithoutColor.indexOf("MEEM");
        Spannable wordtoSpan = new SpannableString(mStringWithoutColor);
        wordtoSpan.setSpan(new ForegroundColorSpan(mUiCtxt.getAppContext().getResources().getColor(R.color.meemGreen)), mStartingPosition, mStartingPosition + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mPromtTextView.setText(wordtoSpan);

        mShowingConnectMEEMCable = true;
        updateBackupTimeStamp();

        mShowingCableConnected = false;

        mSearchLayout.setVisibility(View.VISIBLE);
    }

    public void setNetworkSearchTextNormal() {
        mWiFiSearchTV.setText("Search Network");
    }

    public void setNetworkSearchTextBusy() {
        mWiFiSearchTV.setText("Searching Network");
    }

    private void updateBackupTimeStamp() {
        if (mShowingCableConnected || !mShowingConnectMEEMCable) {
            mLastBackupTime.setVisibility(View.GONE);
            return;
        }

        new CountDownTimer(60000, 60000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                updateBackupTimeStamp();

            }
        }.start();

        String timedifference;
        Date now = new Date();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mUiCtxt.getAppContext());
        long timestamp = sharedPreferences.getLong(AppPreferences.LAST_BACKUP_TIME, 0);

        long nowtime = now.getTime() / 1000;
        long difftime = nowtime - (timestamp / 1000);

        int sec = 1;
        int minute = 60 * sec;
        int hour = 60 * minute;
        int day = 24 * hour;

        int hrspassed = 0;
        int dayspassed = 0;
        int minutespassed = (int) Math.abs(difftime / minute);

        if (mLastBackupTime == null) {
            return;
        }

        if (timestamp == 0) {
            mLastBackupTime.setVisibility(View.GONE);
            return;
        }

        mLastBackupTime.setVisibility(View.VISIBLE);

        if (minutespassed < 5) {
            timedifference = mJustNowString;
        } else {
            minutespassed = 5 * (minutespassed / 5);

            if (minutespassed < 60) {
                timedifference = minutespassed + mMinsString + " " + mAgoString;
            } else {
                hrspassed = (int) Math.abs(difftime / hour);
                if (hrspassed < 24) {
                    if (hrspassed == 1) {
                        minutespassed = minutespassed - hrspassed * 60;
                        if (minutespassed >= 1) {
                            timedifference = hrspassed + mHrString + " " + minutespassed + (minutespassed <= 0 ? mMinString : mMinsString) + " " + mAgoString;
                        } else {
                            timedifference = hrspassed + mHrString + " " + mAgoString;
                        }

                    } else {
                        minutespassed = minutespassed - hrspassed * 60;
                        if (minutespassed >= 1) {
                            timedifference = hrspassed + mHoursString + " " + minutespassed + (minutespassed <= 0 ? mMinString : mMinsString) + " " + mAgoString;
                        } else {
                            timedifference = hrspassed + mHrString + " " + mAgoString;
                        }
                    }
                } else {
                    dayspassed = (int) Math.abs(difftime / day);
                    if (dayspassed < 7) {
                        if (dayspassed == 1) {
                            hrspassed = hrspassed - dayspassed * 24;
                            if (hrspassed == 1) {
                                timedifference = dayspassed + mDayString + " " + hrspassed + " " + mHrString + " " + mAgoString;
                            } else if (hrspassed > 1) {
                                timedifference = dayspassed + mDayString + " " + hrspassed + mHoursString + " " + mAgoString;
                            } else {
                                timedifference = dayspassed + mDayString + " " + mAgoString;
                            }

                        } else {
                            hrspassed = hrspassed - dayspassed * 24;
                            if (hrspassed == 1) {
                                timedifference = dayspassed + mDaysString + " " + hrspassed + mHrString + " " + mAgoString;
                            } else if (hrspassed > 1) {
                                timedifference = dayspassed + mDaysString + " " + hrspassed + mHoursString + " " + mAgoString;
                            } else {
                                timedifference = dayspassed + mDaysString + " " + mAgoString;
                            }
                        }

                    } else {
                        timedifference = dayspassed + mDaysString + " " + mAgoString;
                    }
                }
            }
        }

        mLastBackupTime.setText("Last backed up: " + timedifference); //TODO: Translation

    }


    public boolean isShowingCableConnected() {
        return mShowingCableConnected;
    }

   /* public void showInitializingProgressBar(int percent) {
        mInitProgress = new ProgressDialog(mListener.getActivity());

        mInitProgress.setMessage(mUiCtxt.getAppContext().getResources().getString(R.string.please_wait_initilizing_cable)); // TODO: Translation
        mInitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mInitProgress.setIndeterminate(true);
        mInitProgress.setCancelable(false);
        mInitProgress.setProgress(percent);

        mInitProgress.show();
    }

    public void hideInitializingProgressBar() {
        if (mInitProgress != null) {
            mInitProgress.dismiss();
            mInitProgress = null;
        }
    }*/

    public void destroy(final Runnable mRunnableOnFinish) {
        /*if (mInitProgress != null) {
            mInitProgress.dismiss();
        }*/

        // TODO: This is hacking. Revisit. See comments in DisconnectedStateUi too.
        if (null != mRunnableOnFinish) {
            mViewRoot.removeView(mMainLayout);
            mShowingCableConnected = false;

            mRunnableOnFinish.run();
            return;
        }

        ValueAnimator alphaAnim = ValueAnimator.ofFloat(1.0f, 0.0f);

        alphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                mPromtTextView.setAlpha(val);
                mPromptImgView.setAlpha(val);
            }
        });

        alphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mMainLayout != null)
                    mViewRoot.removeView(mMainLayout);
                mShowingCableConnected = false;

                mListener.onDisconnectedStateUiFinish();
            }
        });

        alphaAnim.setDuration(ProductSpecs.DEFAULT_ANIM_DURATION);
        alphaAnim.start();
    }

    public void startWifiIconAnimation() {
        mAnimateWifiIcon = true;
        startAnimationInLoop();
    }

    public void stopWifiIconAnimation() {
        mAnimateWifiIcon = false;
    }

    public void enableWifiIconClick(boolean allowClick) {
        mSearchLayout.setClickable(allowClick);
    }

    private void startAnimationInLoop() {
        mAnimIn = new AlphaAnimation(0.1f, 1.0f);
        mAnimIn.setDuration(500);
        mAnimIn.setStartOffset(500);

        //animation1 AnimationListener
        mAnimIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                if (mAnimateWifiIcon)
                    mWifiIcon.startAnimation(mAnimOut);
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {

            }

            @Override
            public void onAnimationStart(Animation arg0) {
            }

        });

        mAnimOut = new AlphaAnimation(1.0f, 0.1f);
        mAnimOut.setDuration(500);
        mAnimOut.setStartOffset(500);

        mAnimOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                mWifiIcon.startAnimation(mAnimIn);
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationStart(Animation arg0) {
            }

        });

        mWifiIcon.startAnimation(mAnimIn);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.wifiSearchLL) {
            onClickOfSearchIcon();
        }
    }

    private void onClickOfSearchIcon() {
        MeemEvent startNwMasterSearch = new MeemEvent(EventCode.MNET_USER_REQ_SEARCH_MASTER);
        mUiCtxt.postEvent(startNwMasterSearch);

        startWifiIconAnimation();
    }

    public interface DisconnectedStateUiListener {
        void onDisconnectedStateUiFinish();
        Activity getActivity();
    }
}
