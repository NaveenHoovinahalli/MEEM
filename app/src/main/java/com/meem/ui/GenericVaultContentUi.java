package com.meem.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.meem.androidapp.UiContext;
import com.meem.ui.utils.AnimDotsView;

/**
 * Essentially an array of views, arranged vertically. This will install a gesture handler on all views and provide basic gesture handling
 * functions, which derived classes can override. This way, we can avoid duplicating gesture handling code in all the derived classes like
 * CategoryListView, UsageListView, MeemInfoListView, FirmwareListView etc.
 *
 * @author arun
 */

@SuppressLint("ClickableViewAccessibility")
public class GenericVaultContentUi extends MeemGestureAdapter {
    @SuppressWarnings("unused")
    private static final String TAG = "GenericVaultContentUi";
    /*
     * ArrayMap is used to get additional features like indexOfKey() that may be useful later.
     */
    protected ArrayMap<Integer, ViewGroup> mViewMap;
    protected boolean mIsMirror, mLockState;
    Context mContext;
    FrameLayout mViewRoot;

    public GenericVaultContentUi(Context context, FrameLayout viewRoot) {
        mContext = context;
        mViewRoot = viewRoot;

        mViewMap = new ArrayMap<Integer, ViewGroup>();
    }

    public boolean getLockState() {
        return mLockState;
    }

    public void setLockState(boolean lockState) {
        mLockState = lockState;
    }

    public void setProperties(boolean isMirror) {
        mIsMirror = isMirror;
    }

    protected void addView(ViewGroup v, boolean isDummy) {
        mViewRoot.addView(v);

        if(!isDummy) {
		/*
         * Install a gesture detector for this view.
		 * In this way many detector instances will be there in app.
		 * To be solved later. 
		 * by the way, compatibility version is used to support buggy older devices.
		 */
            final GestureDetectorCompat gd = new GestureDetectorCompat(mContext, new MeemGestureDetector(this, mViewRoot, v));
            v.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mLockState) {
                        return true;
                    }

                    return gd.onTouchEvent(event);
                }
            });
        }

        UiContext ctx = UiContext.getInstance();
        int offsetFromTop = mViewMap.size() * (int) ctx.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.CATEGORY_LIST_HEIGHT);
        v.setY(offsetFromTop);

        mViewMap.put(new Integer((byte) v.getTag()), v);
    }

    public void animCatDots(byte cat, boolean start) {
        ViewGroup vg = mViewMap.get(new Integer(cat));

        if (null == vg) {
            // This may happen if the category is not visible in ui.
            // Note: these issues are expected for model changes in v2
            return;
        }

        AnimDotsView adv = (AnimDotsView) vg.getChildAt(1);

        if (null == adv) {
            return;
        }

        if (start) {
            adv.setVisibility(View.VISIBLE);
            adv.startAnimation();
        } else {
            adv.stopAnimation();
            adv.setVisibility(View.GONE);
        }
    }

    public ViewGroup getContentViewGroupItemByTag(int tag) {
        ViewGroup vg = mViewMap.get(tag);
        if (vg == null) {
            Log.e("GenericVaultContentUi", "Viewgroup not found in map for tag: " + tag);
            return null;
        }

        return vg;
    }
}
