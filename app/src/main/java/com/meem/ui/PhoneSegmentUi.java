package com.meem.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.meem.androidapp.UiContext;
import com.meem.ui.PhoneAvatarUi.PhoneAvatarEventListener;
import com.meem.viewmodel.PhoneInfo;

import java.util.ArrayList;
import java.util.Vector;

/**
 * @author arun
 */
public class PhoneSegmentUi implements PhoneAvatarEventListener {
    @SuppressWarnings("unused")
    private static final String TAG = "PhoneSegmentUi";
    protected PhoneSegmentListener mListener;
    FrameLayout mRootView;
    PhoneAvatarUi mPhoneAvatar;
    UiContext mUiCtxt = UiContext.getInstance();
    int mMemGradHeight = 500;

    float mTransX;

    PhoneInfo mPhoneInfo;

    public PhoneSegmentUi(FrameLayout viewRoot) {
        mRootView = viewRoot;
    }

    public void setEventListener(PhoneSegmentListener listener) {
        mListener = listener;
    }

    public FrameLayout getView() {
        return mRootView;
    }

    public PhoneAvatarUi getAvatarUi() {
        return mPhoneAvatar;
    }

    public boolean create() {
        FrameLayout.LayoutParams llp = (FrameLayout.LayoutParams) mRootView.getLayoutParams();
        llp.width = (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.PHONE_SIDE_WIDTH);
        mRootView.setLayoutParams(llp);

        int leftPadding = (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.VAULT_LEFT_PADDING);
        int rightPadding = (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.VAULT_RIGHT_PADDING);

        mRootView.setPadding(leftPadding, 0, rightPadding, 0);

        mRootView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                int action = event.getAction();
                switch (action) {
                    case DragEvent.ACTION_DROP:
                        onDnDDropEvent(event);
                        break;
                    default:
                        break;
                }

                return true;
            }
        });

        // make it hidden beyond the left edge
        mTransX = llp.width;
        mRootView.setTranslationX(-1 * mTransX);
        return true;
    }

    // TODO: Experimental
    private ShapeDrawable getMemoryGradient() {
        ShapeDrawable sd = new ShapeDrawable(new RectShape());
        // screen: left bottom x, left bottom y, left top x, left top y, bottom color, top color, CLAMP continues top color.
        sd.getPaint().setShader(new LinearGradient(0, mUiCtxt.getScreenHeightPix(), 0, mMemGradHeight, Color.parseColor("#5587D300"), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        return sd;
    }

    private void onDnDDropEvent(DragEvent event) {
        mListener.onDropOnPhoneSegment(event.getX(), event.getY());
    }

    public void updatePhoneInfo(PhoneInfo phoneInfo) {
        mRootView.removeAllViews();

        mPhoneInfo = phoneInfo;

        // creating phone avatar
        mPhoneAvatar = new PhoneAvatarUi(mRootView, mPhoneInfo);
        mPhoneAvatar.create();

        // center the whole vault layout in parent. Note: Do NOT use gravity for this - which will have
        // terrible impact on animation performance.
        float defTransY = mUiCtxt.getScreenHeightPix() / 2 - mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEIGHT) / 2;
        mPhoneAvatar.setDefTransY(defTransY);

        Log.d(TAG, "Default phone avatar transY: " + defTransY);

        mPhoneAvatar.setEventListener(this);
    }

    /**
     * Listener functionality for PhoneAvatarUi class. Here, our listener is ViewController
     */
    @Override
    public void onAvatarOpen() {
        mListener.onPhoneAvatarOpen();
    }

    @Override
    public void onAvatarClose() {
        mListener.onPhoneAvatarClose();
    }

    @Override
    public void onCategoryTap(byte cat) {
        mListener.onPhoneCategoryTap(cat);
    }

    @Override
    public void onCategorySwipe(PhoneAvatarUi avatarUi, byte cat) {
        mListener.onPhoneCategorySwipe(avatarUi, cat);
    }

    @Override
    public void onPhoneAvatarDragStart() {
        mListener.onPhoneAvatarDragStart(mPhoneAvatar);
    }

    @Override
    public void onValidDropOnPhoneAvatar(String dropUpid, float x, float y) {
        mListener.onValidDropOnPhoneAvatar(mPhoneAvatar, dropUpid, x, y);
    }

    @Override
    public void onInvalidDropOnPhoneAvatar(float x, float y) {
        mListener.onInvalidDropOnPhoneAvatar(mPhoneAvatar, x, y);
    }

    /**
     * ********************************************************
     * <p/>
     * Public interfaces
     * <p/>
     * ********************************************************
     */

    public void setLockState(boolean lockState) {
        mPhoneAvatar.setLockState(lockState);
    }

    public ValueAnimator getSlideAnimator(boolean in) {
        ValueAnimator slide;

        if (in) {
            slide = ValueAnimator.ofFloat(mRootView.getTranslationX(), 0);
        } else {
            slide = ValueAnimator.ofFloat(mRootView.getTranslationX(), -1 * mTransX);
        }

        slide.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRootView.setTranslationX((float) animation.getAnimatedValue());
            }
        });

        return slide;
    }

    public Vector<Animator> getPhoneAvatarOpenCloseAnimatorList(boolean open, float shift) {
        return mPhoneAvatar.getAvatarOpenCloseAnimatorList(open, shift);
    }

    public void startSessionAnimations(ArrayList<Byte> cats) {
        mPhoneAvatar.startSessionAnimations(cats);
    }

    public void stopSessionAnimations(ArrayList<Byte> cats) {
        mPhoneAvatar.stopSessionAnimations(cats);
    }

    public void stopSessionAnimationForCat(byte cat) {
        mPhoneAvatar.stopSessionAnimationForCat(cat);
    }

    // interface to be implemented by view controller

    public interface PhoneSegmentListener {
        void onPhoneAvatarOpen();

        void onPhoneAvatarClose();

        void onPhoneCategoryTap(byte cat);

        void onPhoneCategorySwipe(PhoneAvatarUi avatarUi, byte cat);

        void onPhoneAvatarDragStart(PhoneAvatarUi phAvatar);

        void onValidDropOnPhoneAvatar(PhoneAvatarUi phAvatar, String dropUpid, float x, float y);

        void onInvalidDropOnPhoneAvatar(PhoneAvatarUi phAvatar, float x, float y);

        void onDropOnPhoneSegment(float x, float y);
    }
}
