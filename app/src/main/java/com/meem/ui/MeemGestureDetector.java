package com.meem.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.meem.androidapp.UiContext;

/**
 * Custom onTouchListener exclusively for MEEM application. May be re-usable, but you need to test.
 * <p/>
 * Author Arun T. A.
 */

@SuppressLint("ClickableViewAccessibility")
public class MeemGestureDetector implements GestureDetector.OnGestureListener {
    private static final String TAG = "MeemGestureDetector";
    // from source code of ViewPager
    private static final float MIN_DISTANCE_FOR_FLING = 25f; // 50f?
    MeemGestureInterface mListener;
    ViewGroup mParentViewGroup;
    View mView;
    private float mFlingMinDistance; // = 100 works well in hdpi;
    private float mFlingMinVelocity; // = 100 works well in hdpi;
    private float mScrollMinDist; //  = 10 works well in hdpi;

    public MeemGestureDetector(MeemGestureInterface listener, ViewGroup parent, View view) {
        mListener = listener;
        mParentViewGroup = parent;
        mView = view;

        UiContext uiCtx = UiContext.getInstance();
        Context ctx = uiCtx.getAppContext();
        mFlingMinDistance = MIN_DISTANCE_FOR_FLING * uiCtx.getDensity();
        mFlingMinVelocity = ViewConfiguration.get(ctx).getScaledMinimumFlingVelocity();
        mScrollMinDist = ViewConfiguration.get(ctx).getScaledTouchSlop();

        /**
         * Observed values:
         * XXXHDPI: mFlingMinDistance: 100.0, mFlingMinVelocity: 200.0, mScrollMinDist: 64.0
         * HDPI:    mFlingMinDistance: 37.5,  mFlingMinVelocity: 75.0,  mScrollMinDist: 24.0
         *
         */
        // Log.d(TAG, "mFlingMinDistance: " + mFlingMinDistance + ", mFlingMinVelocity: " + mFlingMinVelocity + ", mScrollMinDist: " + mScrollMinDist);
    }

    // implement onDown in case other gesture events are ignored
    @Override
    public boolean onDown(MotionEvent event) {
        mParentViewGroup.requestDisallowInterceptTouchEvent(true);
        mListener.onGestureStart(mView, event.getX(), event.getY());
        return true;
    }


    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "onLongPress");
        mListener.onLongPress(mView, e.getX(), e.getY());
    }

    // handle fling gesture
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        Log.d(TAG, "onFling");

        boolean swipeToLeft = false;
        boolean swipeToRight = false;
        boolean swipeToUp = false;
        boolean swipeToDown = false;

        float horizontalDiff = event2.getX() - event1.getX();
        float verticalDiff = event2.getY() - event1.getY();

        // work out absolute values
        float absHDiff = Math.abs(horizontalDiff);
        float absVDiff = Math.abs(verticalDiff);
        float absVelocityX = Math.abs(velocityX);
        float absVelocityY = Math.abs(velocityY);

        // is horizontal difference greater and are values valid
        if (absHDiff > absVDiff && absHDiff > mFlingMinDistance && absVelocityX > mFlingMinVelocity) {
            if (horizontalDiff > 0) swipeToRight = true;
            else swipeToLeft = true;
        } else if (absHDiff < absVDiff && absVDiff > mFlingMinDistance && absVelocityY > mFlingMinVelocity) {
            if (verticalDiff > 0)
                // Important: Y in increasing downwards!
                swipeToDown = true;
            else swipeToUp = true;
        }

        if (swipeToLeft) {
            //Log.d(TAG, "<< Swipe <<");
            mListener.onRightToLeftSwipe(mView);
        } else if (swipeToRight) {
            //Log.d(TAG, ">> Swipe >>");
            mListener.onLeftToRightSwipe(mView);
        }

        if (swipeToUp) {
            //Log.d(TAG, "^^ Swipe ^^");
            mListener.onBottomToTopSwipe(mView);
        }

        if (swipeToDown) {
            //Log.d(TAG, "vv Swipe vv");
            mListener.onTopToBottomSwipe(mView);
        }

        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // No need
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG, "onSingleTapUp");
        mListener.onSingleTap(mView, e.getX(), e.getY());
        return true;
    }

    /*
     * Duplicated code from swipe with different present minimums for scroll
     * Duplicated for clarity.
     *
     */
    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        Log.d(TAG, "onScroll");

        boolean scrollToLeft = false;
        boolean scrollToRight = false;
        boolean scrollToUp = false;
        boolean scrollToDown = false;

        float horizontalDiff = event2.getX() - event1.getX();
        float verticalDiff = event2.getY() - event1.getY();

        // work out absolute values
        float absHDiff = Math.abs(horizontalDiff);
        float absVDiff = Math.abs(verticalDiff);

        // is horizontal difference greater and are values valid
        if (absHDiff > absVDiff && absHDiff > mScrollMinDist) {
            if (horizontalDiff > 0) scrollToRight = true;
            else scrollToLeft = true;
        } else if (absHDiff < absVDiff && absVDiff > mScrollMinDist) {
            if (verticalDiff > 0)
                // Important: Y in increasing downwards!
                scrollToDown = true;
            else scrollToUp = true;
        }

        if (scrollToLeft) {
            //Log.d(TAG, "<< Scroll <<");
            mListener.onRightToLeftScroll(mView, event2.getX(), event2.getY(), distanceX);
        } else if (scrollToRight) {
            //Log.d(TAG, ">> Scroll >>");
            mListener.onLeftToRightScroll(mView, event2.getX(), event2.getY(), distanceX);
        }

        if (scrollToUp) {
            //Log.d(TAG, "^^ Scroll ^^");
            mListener.onBottomToTopScroll(mView, event2.getX(), event2.getY(), distanceY);
        }

        if (scrollToDown) {
            //Log.d(TAG, "vv Scroll vv");
            mListener.onTopToBottomScroll(mView, event2.getX(), event2.getY(), distanceY);
        }

        return true;
    }

    public interface MeemGestureInterface {
        void onGestureStart(View v, float x, float y);

        void onSingleTap(View v, float x, float y);

        void onLongPress(View v, float x, float y);

        void onLeftToRightSwipe(View v);

        void onRightToLeftSwipe(View v);

        void onTopToBottomSwipe(View v);

        void onBottomToTopSwipe(View v);

        void onLeftToRightScroll(View v, float x, float y, float dist);

        void onRightToLeftScroll(View v, float x, float y, float dist);

        void onTopToBottomScroll(View v, float x, float y, float dist);

        void onBottomToTopScroll(View v, float x, float y, float dist);
    }
}