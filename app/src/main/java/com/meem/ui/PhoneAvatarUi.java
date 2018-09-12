package com.meem.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.PhoneCategoryListUi.PhoneCategoryListEventListener;
import com.meem.viewmodel.PhoneInfo;

import java.util.ArrayList;
import java.util.Vector;

/**
 * getLocationOnScreen is the key
 *
 * @author arun
 */

@SuppressLint("ClickableViewAccessibility")
public class PhoneAvatarUi extends GenericVaultUi implements PhoneCategoryListEventListener {
    private static final String TAG = "PhoneAvatarUi";
    protected PhoneAvatarEventListener mListener;

    PhoneInfo mPhoneInfo;

    FrameLayout mParentLayout;
    UiContext mUiCtxt = UiContext.getInstance();
    PhoneCategoryListUi mCatListView;

    private float mDefTransY;

    private boolean mAvatarOpened, mIsAnimating;

    public PhoneAvatarUi(FrameLayout parentLayout, PhoneInfo phone) {
        super(parentLayout);
        mParentLayout = parentLayout;
        mPhoneInfo = phone;
    }

    public void setEventListener(PhoneAvatarEventListener listener) {
        mListener = listener;
    }

    public void setDefTransY(float defTransY) {
        mDefTransY = defTransY;
        mVaultLayout.setTranslationY(defTransY);
    }

    @Override
    public boolean create() {
        super.create(); // important.
        setResources(R.color.meemGreen);
        return true;
    }

    public Vector<Animator> getAvatarOpenCloseAnimatorList(boolean open, float shift) {

        final RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) mContentFrameLayout.getLayoutParams();

        ValueAnimator vaHeight, vaShift;

        if (open) {
            vaHeight = ValueAnimator.ofInt(rlp.height, mCatListView.getHeight());
            vaShift = ValueAnimator.ofFloat(mDefTransY, shift);
        } else {
            vaHeight = ValueAnimator.ofInt(rlp.height, 0);
            vaShift = ValueAnimator.ofFloat(shift, mDefTransY);
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
                mAvatarOpened = !mAvatarOpened;
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsAnimating = false;
            }
        });

        vaShift.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                mVaultLayout.setTranslationY(val);
            }
        });

        Vector<Animator> animList = new Vector<>(2);
        animList.add(vaShift);
        animList.add(vaHeight);

        return animList;
    }

    public float getDefaultVerticalShift() {
        return mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_LEFT_PADDING);
    }

    public PhoneInfo getPhoneInfo() {
        return mPhoneInfo;
    }

    @Override
    void setupHeaderView() {
        mHeaderNameTv.setText(mPhoneInfo.getmBaseModel());

        // Arun: 23April2018: To support vault name editing.
        if(null != mPhoneInfo.getVaultInfo()) {
            if (null != mPhoneInfo.getVaultInfo().mName) {
                mHeaderNameTv.setText(mPhoneInfo.getVaultInfo().mName);
            }
        }

        mHeaderTimeStampTv.setText("");
        mWifiIcon.setVisibility(View.GONE);
    }

    @Override
    void setupFooterView() {
        // TODO
    }

    @Override
    void setupContentView() {
        mCatListView = new PhoneCategoryListUi(mUiCtxt.getAppContext(), mContentFrameLayout);
        mCatListView.create();
        mCatListView.setEventListener(this);
    }

    @Override
    void setContentViewLockState(boolean lockState) {
        mCatListView.setLockState(lockState);
    }

    /**
     * Overrides for GenericVaultUi to handle header clicks and all
     */
    @Override
    protected void onSingleTapOnHeader() {
        Log.d(TAG, "Avatar clicked");

        // Just uncomment the following line to disable tap during animation.
        if (mIsAnimating) return;

        if (!mAvatarOpened) {
            mListener.onAvatarOpen();
        } else {
            mListener.onAvatarClose();
        }
    }

    /**
     * Override of the GenericVaultUi's header drag event handler.
     */
    @Override
    protected void onDnDDragStart(float x, float y) {
        Log.d(TAG, "Start dragging on phone header: " + x + ", " + y);

        if (mAvatarOpened) {
            Log.d(TAG, "Drag ignored in opened state");
            return;
        }

        // Construct drag shadow for view
        DragShadowBuilder shadowBuilder = new DragShadowBuilder(mVaultLayout);
        ClipData data = ClipData.newPlainText("PHONE", mPhoneInfo.mUpid);
        mVaultLayout.startDrag(data, shadowBuilder, mVaultLayout, 0);

        mListener.onPhoneAvatarDragStart();
    }

    /**
     * Override of GenericVaultUi's drop event (of drag and drop)
     */
    @Override
    protected void onDnDDropEvent(DragEvent event) {
        Log.d(TAG, "Drop on phone header");

        if (mAvatarOpened) {
            Log.d(TAG, "Drop ignored in opened state");
            return;
        }

        String clipTextUpid = event.getClipData().getItemAt(0).getText().toString();
        String clipTextDesc = event.getClipDescription().toString();

        if (!clipTextDesc.contains("MEEM")) {
            mListener.onInvalidDropOnPhoneAvatar(event.getX(), event.getY());
        } else {
            mListener.onValidDropOnPhoneAvatar(clipTextUpid, event.getX(), event.getY());
        }
    }

    /**
     * *****************************************************
     * <p/>
     * This class's Listener functionality for category list. The listener of this class is PhoneSegmentUi.
     * <p/>
     * *****************************************************
     */

    @Override
    public void onListItemSingleTap(byte cat) {
        mListener.onCategoryTap(cat);
    }

    @Override
    public void onListItemRightSwipe(byte cat) {
        mListener.onCategorySwipe(this, cat);
    }


    @Override
    public void onListItemLeftSwipe(byte cat) {
        // Ignored in phone avatar
    }

    // ------------------------------------------------------------
    // ----------------- Public methods ---------------------------
    // ------------------------------------------------------------

    public void startSessionAnimations(ArrayList<Byte> cats) {
        animFooterDots(true);
        for (Byte cat : cats) {
            mCatListView.animCatDots(cat, true);
        }
    }

    public void stopSessionAnimations(ArrayList<Byte> cats) {
        animFooterDots(false);
        for (Byte cat : cats) {
            mCatListView.animCatDots(cat, false);
        }
    }

    public void stopSessionAnimationForCat(Byte cat) {
        mCatListView.animCatDots(cat, false);
    }


    public ViewGroup getSuperlistItemViewGroup(byte cat) {
        return mCatListView.getContentViewGroupItemByTag(cat);
    }

    /**
     * To be implemented by PhoneSegmentUi
     */
    public interface PhoneAvatarEventListener {
        void onAvatarOpen();

        void onAvatarClose();

        void onCategoryTap(byte cat);

        void onCategorySwipe(PhoneAvatarUi avatarUi, byte cat);

        void onPhoneAvatarDragStart();

        void onValidDropOnPhoneAvatar(String dropUpid, float x, float y);

        void onInvalidDropOnPhoneAvatar(float x, float y);
    }
}
