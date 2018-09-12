package com.meem.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ClipData;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.MeemCategoryListUi.MeemCategoryListEventListener;
import com.meem.v2.mmp.MMPV2Constants;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

import static com.meem.androidapp.R.string.mins;

/**
 * This is the Meem a.k.a Mirror (what you see in MeemSegmentUi for registered phones).
 * <p/>
 * For many animation aspects, using getLocationOnScreen is the key
 *
 * @author arun
 */

public class MeemUi extends GenericVaultUi implements MeemCategoryListEventListener {
    private static final String TAG = "Meem";
    protected MeemViewEventListener mListener;
    FrameLayout mParentLayout;
    private AlphaAnimation mAnimIn,mAnimOut;
    boolean  mAnimateWifiIcon=false;

    VaultInfo mVaultInfo;

    UiContext mUiCtxt = UiContext.getInstance();

    String mDayString = mUiCtxt.getAppContext().getResources().getString(R.string.day);
    String mDaysString = mUiCtxt.getAppContext().getResources().getString(R.string.days);
    String mHrString = mUiCtxt.getAppContext().getResources().getString(R.string.hr);
    String mHoursString = mUiCtxt.getAppContext().getResources().getString(R.string.hrs);
    String mMinString = mUiCtxt.getAppContext().getResources().getString(R.string.min);
    String mMinsString = mUiCtxt.getAppContext().getResources().getString(mins);
    String mJustNowString = mUiCtxt.getAppContext().getResources().getString(R.string.just_now);
    String mAgoString = mUiCtxt.getAppContext().getResources().getString(R.string.ago);

    MeemCategoryListUi mCatListView;

    private boolean mIsMirror;
    private int mId = -1;
    private int mColor = 0;

    private float mDefTransY, mVerticalShiftOnAnchoring, mVerticalShiftInGroup;

    private boolean mIsOpen, mIsAnimating, mIsAnimAnchor, mIsWiFiIconVisible;
    private Runnable timeStampUpdater = new Runnable() {
        @Override
        public void run() {
            updateBackupTimeStamp(mVaultInfo.getmLastBackupTime());

            Handler handler = new Handler();
            handler.removeCallbacks(timeStampUpdater);
            handler.postDelayed(timeStampUpdater, GuiSpec.TIME_DELAY_TIMESTAMP_CHANGE);
        }
    };

    public MeemUi(FrameLayout parentLayout, VaultInfo vaultInfo) {
        super(parentLayout);
        mParentLayout = parentLayout;
        mVaultInfo = vaultInfo;
    }

    public void setProperties(int id, int color, boolean isMirror) {
        mIsMirror = isMirror;
        mId = id;
        mColor = color;

        if (mIsMirror) mirrorUi();
        setResources(mColor);

        mCatListView.setProperties(isMirror);
    }

    public boolean isEqual(MeemUi other) {
        return (this.mId == other.mId);
    }

    public float getDefTransY() {
        return mDefTransY;
    }

    public void setDefTransY(float defTransY) {
        mDefTransY = defTransY;
        mVaultLayout.setTranslationY(defTransY);
    }

    public void setVerticalShiftAsAnchor(float verticalShift) {
        mVerticalShiftOnAnchoring = verticalShift;
    }

    public float getVerticalShiftOnAnchoring() {
        return mVerticalShiftOnAnchoring;
    }

    public float getVerticalShiftInGroup() {
        return mVerticalShiftInGroup;
    }

    public void setVerticalShiftInGroup(float verticalShift) {
        mVerticalShiftInGroup = verticalShift;
    }

    public boolean isAnimAnchor() {
        return mIsAnimAnchor;
    }

    public void setAnimAnchor(boolean value) {
        mIsAnimAnchor = value;
    }

    public void setEventListener(MeemViewEventListener listener) {
        mListener = listener;
    }

    @Override
    public boolean create() {
        return super.create(); // important.
    }

    public Vector<Animator> getOpenCloseAnimatorList(boolean open) {
        Vector<Animator> animList = new Vector<>();

        if (mIsAnimAnchor) {
            animList.add(createHeightAnimator(open));
        }
        animList.add(createVerticalShiftAnimator(open));
        return animList;
    }

    public VaultInfo getVaultInfo() {
        return mVaultInfo;
    }

    private void mirrorUi() {
        // make the phone icon left aligned in parent.
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int iconMarginHoriz = (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.VAULT_HEADER_ICON_ALL_MARGIN);
        int iconMarginVert = (int) mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEADER_ICON_ALL_MARGIN);
        rlp.setMargins(iconMarginHoriz, iconMarginVert, iconMarginHoriz, iconMarginVert);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mPhoneIcon.setLayoutParams(rlp);

        // make name text view mirrored and aligned to right of icon
        mHeaderNameTv.setMirrorMode(true);
        rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int nameMarginTop = (int) mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEADER_NAME_TOP_MARGIN);
        rlp.setMargins(0, nameMarginTop, 0, 0);
        rlp.addRule(RelativeLayout.RIGHT_OF, R.id.vault_header_phone_icon);
        mHeaderNameTv.setLayoutParams(rlp);

        // make timestamp (not mirrored) but aligned to right of icon
        rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.RIGHT_OF, R.id.vault_header_phone_icon);
        rlp.addRule(RelativeLayout.BELOW, R.id.vault_header_name_text);
        mHeaderTimeStampTv.setLayoutParams(rlp);
    }

    private ValueAnimator createHeightAnimator(boolean open) {
        final RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) mContentFrameLayout.getLayoutParams();

        ValueAnimator vaHeight;

        if (open) {
            vaHeight = ValueAnimator.ofInt(rlp.height, mCatListView.getHeight());
        } else {
            vaHeight = ValueAnimator.ofInt(rlp.height, 0);
        }

        vaHeight.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rlp.height = (int) animation.getAnimatedValue();
                mContentFrameLayout.setLayoutParams(rlp);
            }
        });

        vaHeight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mIsAnimating = true;
                mIsOpen = !mIsOpen;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsAnimating = false;
            }
        });

        return vaHeight;
    }

    private ValueAnimator createVerticalShiftAnimator(boolean open) {
        ValueAnimator vaShift;

        if (open) {
            vaShift = ValueAnimator.ofFloat(mDefTransY, mIsAnimAnchor ? mVerticalShiftOnAnchoring : mVerticalShiftInGroup);
        } else {
            vaShift = ValueAnimator.ofFloat(mIsAnimAnchor ? mVerticalShiftOnAnchoring : mVerticalShiftInGroup, mDefTransY);
        }

        vaShift.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                mVaultLayout.setTranslationY(val);
            }
        });

        vaShift.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsAnimating = false;
            }
        });

        return vaShift;
    }

    public boolean isMirror() {
        return mIsMirror;
    }

    public boolean isAnimating() {
        return mIsAnimating;
    }

    public boolean isOpen() {
        return mIsOpen;
    }

    public void onVirtualCategoryTap(byte catCode) {
        if (!mIsMirror) {
            Log.w(TAG, "Ignoring phone side category tap as I'm not its mirror (should not happen!)");
            return;
        }

        mCatListView.onVirtualSingleTap(catCode);
    }

    @Override
    void setupHeaderView() {
        mHeaderNameTv.setText(mVaultInfo.getmName());
        updateBackupTimeStamp(mVaultInfo.getmLastBackupTime());

        Handler handler = new Handler();
        handler.postDelayed(timeStampUpdater, GuiSpec.TIME_DELAY_TIMESTAMP_CHANGE);
    }

    @Override
    void setupFooterView() {
        // TODO
    }

    @Override
    protected void setupContentView() {
        mCatListView = new MeemCategoryListUi(mUiCtxt.getAppContext(), mContentFrameLayout);

        LinkedHashMap<Byte, CategoryInfo> catMap = mVaultInfo.getmCategoryInfoMap();

        // handle ios vault!
        if(mVaultInfo.mPlatform.equals("Android")) {
            mCatListView.create(catMap);
        } else {
            LinkedHashMap<Byte, CategoryInfo> appleShowCats = new LinkedHashMap<>();

            // contacts
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_CONTACT, catMap.get(MMPV2Constants.MMP_CATCODE_CONTACT));


            CategoryInfo catInfo = new CategoryInfo();

            // messages - dummy
            catInfo.mMmpCode = MMPV2Constants.MMP_CATCODE_MESSAGE;
            catInfo.setmDummy(true);
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_MESSAGE, catInfo);

            // calendar
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_CALENDER, catMap.get(MMPV2Constants.MMP_CATCODE_CALENDER));

            // photo - internal
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_PHOTO, catMap.get(MMPV2Constants.MMP_CATCODE_PHOTO));
            // photo sdcard
            catInfo = new CategoryInfo();
            catInfo.mMmpCode = MMPV2Constants.MMP_CATCODE_PHOTO_CAM;
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_PHOTO_CAM, catInfo);

            // videos
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_VIDEO, catMap.get(MMPV2Constants.MMP_CATCODE_VIDEO));
            catInfo = new CategoryInfo();
            // videos sdcard
            catInfo.mMmpCode = MMPV2Constants.MMP_CATCODE_VIDEO_CAM;
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_VIDEO_CAM, catInfo);

            // music - dummy
            catInfo = new CategoryInfo();
            catInfo.mMmpCode = MMPV2Constants.MMP_CATCODE_MUSIC;
            catInfo.setmDummy(true);
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_MUSIC, catInfo);

            // music sdcard - dummy
            catInfo = new CategoryInfo();
            catInfo.mMmpCode = MMPV2Constants.MMP_CATCODE_FILE;
            catInfo.setmDummy(true);
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_FILE, catInfo);

            // documents
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_DOCUMENTS, catMap.get(MMPV2Constants.MMP_CATCODE_DOCUMENTS));
            // documents sdcard
            catInfo = new CategoryInfo();
            catInfo.mMmpCode = MMPV2Constants.MMP_CATCODE_DOCUMENTS_SD;
            appleShowCats.put(MMPV2Constants.MMP_CATCODE_DOCUMENTS_SD, catInfo);

            mCatListView.create(appleShowCats);
        }

        mCatListView.setEventListener(this);
    }

    @Override
    void setContentViewLockState(boolean lockState) {
        mCatListView.setLockState(lockState);
    }

    /**
     * Gesture overrides for GenericVaultUi to handle header clicks and all
     */
    @Override
    protected void onSingleTapOnHeader() {
        Log.d(TAG, "Meem clicked");

        // Just uncomment the following line to disable tap during animation.
        if (mIsAnimating) return;

        if (!mIsOpen) {
            mListener.onMeemUiOpen(this);
        } else {
            mListener.onMeemUiClose(this);
        }
    }

    /**
     * Override of the GenericVaultUi's header drag event handler. Remember: The listener of this class is MeemSegmentUi.
     */
    @Override
    protected void onDnDDragStart(float x, float y) {
        Log.d(TAG, "Start dragging on meem header: " + x + ", " + y);

        if (mIsOpen) {
            Log.d(TAG, "Drag ignored in opened state");
            return;
        }

        // Construct drag shadow for view
        DragShadowBuilder shadowBuilder = new DragShadowBuilder(mVaultLayout);
        ClipData data = ClipData.newPlainText("MEEM", mVaultInfo.mUpid);
        mVaultLayout.startDrag(data, shadowBuilder, mVaultLayout, 0);

        mListener.onMeemUiDragStart(this);
    }

    /**
     * Override of GenericVaultUi's drop event (of drag and drop)
     */
    @Override
    protected void onDnDDropEvent(DragEvent event) {
        Log.d(TAG, "Drop on meem view");

        if (mIsOpen) {
            Log.d(TAG, "Drop ignored in opened state");
            return;
        }

        String clipTextUpid = event.getClipData().getItemAt(0).getText().toString();
        String clipTextDesc = event.getClipDescription().toString();

        if (clipTextUpid.equals(mVaultInfo.mUpid) && clipTextDesc.contains("PHONE")) {
            mListener.onValidDropOnMeemUi(this, event.getX(), event.getY());
        } else {
            mListener.onInvalidDropOnMeemUi(this, event.getX(), event.getY());
        }
    }

    /**
     * *****************************************************
     * <p/>
     * This class's listener functionality for category list. Remember: The listener of this class is MeemSegmentUi.
     * <p/>
     * *****************************************************
     */
    @Override
    public void onListItemSingleTap(byte cat) {
        mListener.onCategoryTap(this, cat);
    }

    @Override
    public void onListItemRightSwipe(byte cat) {
        // Ignored in Meem view
    }

    // ------------------------------------------------------------
    // ----------------- Public methods ---------------------------
    // ------------------------------------------------------------

    @Override
    public void onListItemLeftSwipe(byte cat) {
        mListener.onCategorySwipe(this, cat);
    }

    public void startSessionAnimations(ArrayList<Byte> cats) {
        animFooterDots(true);
        for (Byte cat : cats) {
            mCatListView.animCatDots(cat, true);
        }

        updateBackupTimeStamp(0);
    }

    public void stopSessionAnimations(ArrayList<Byte> cats) {
        animFooterDots(false);
        for (Byte cat : cats) {
            mCatListView.animCatDots(cat, false);
        }

        updateBackupTimeStamp(mVaultInfo.getmLastBackupTime());
    }

    // -------------------------------------------------------

    public void stopSessionAnimationForCat(Byte cat) {
        mCatListView.animCatDots(cat, false);
    }

    private void updateBackupTimeStamp(long timestamp) {
        String timedifference;
        Date now = new Date();

        long nowtime = now.getTime() / 1000;
        long difftime = nowtime - timestamp;

        int sec = 1;
        int minute = 60 * sec;
        int hour = 60 * minute;
        int day = 24 * hour;

        int hrspassed = 0;
        int dayspassed = 0;
        int minutespassed = (int) Math.abs(difftime / minute);

        if (mHeaderTimeStampTv == null) {
            return;
        }

        if (timestamp == 0) {
            timedifference = "";
            mHeaderTimeStampTv.setText(timedifference);
            return;
        }

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

        mHeaderTimeStampTv.setText(timedifference);

        Log.d(TAG, "Updating backup timestamp: " + timedifference);
    }


    public void showWifiIcon(boolean isVisible) {
        if (isVisible) {
            mWifiIcon.setVisibility(View.VISIBLE);
        } else {
            mWifiIcon.setVisibility(View.GONE);
        }

        mIsWiFiIconVisible = isVisible;
    }

    public  void  startWifiIconAnimation(){
        mAnimateWifiIcon=true;
        startAnimationInLoop();
    }

    public  void stopWifiIconAnimation() {
        mAnimateWifiIcon=false;
    }

    public  void startAnimationInLoop(){
        mAnimIn = new AlphaAnimation(0.1f, 1.0f);
        mAnimIn.setDuration(500);
        mAnimIn.setStartOffset(500);

        //animation1 AnimationListener
        mAnimIn.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationEnd(Animation arg0) {
                if(mAnimateWifiIcon)
                    mWifiIcon.startAnimation(mAnimOut);
                mWifiIcon.setVisibility(mIsWiFiIconVisible ? View.VISIBLE : View.GONE);
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

        mAnimOut.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationEnd(Animation arg0) {
                mWifiIcon.startAnimation(mAnimIn);
                mWifiIcon.setVisibility(mIsWiFiIconVisible ? View.VISIBLE : View.GONE);
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


    public ViewGroup getSuperlistItemViewGroup(byte cat) {
        return mCatListView.getContentViewGroupItemByTag(cat);
    }

    /**
     * To be implemented by MeemSegmentUi
     */

    public interface MeemViewEventListener {
        void onMeemUiOpen(MeemUi mv);

        void onMeemUiClose(MeemUi mv);

        void onCategoryTap(MeemUi m, byte cat);

        void onCategorySwipe(MeemUi mv, byte cat);

        void onMeemUiDragStart(MeemUi mv);

        void onValidDropOnMeemUi(MeemUi mv, float x, float y);

        void onInvalidDropOnMeemUi(MeemUi mv, float x, float y);
    }
}
