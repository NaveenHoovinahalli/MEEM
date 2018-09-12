package com.meem.ui;

import android.view.View;

import com.meem.ui.MeemGestureDetector.MeemGestureInterface;

/**
 * Utility class to avoid littering a class that uses MeemGestureDetector with all interfaces of MeemGestureDetector. You can override only
 * what you need.
 *
 * @author arun
 */
public class MeemGestureAdapter implements MeemGestureInterface {
    @Override
    public void onGestureStart(View v, float x, float y) {
    }

    @Override
    public void onSingleTap(View v, float x, float y) {
    }

    @Override
    public void onLongPress(View v, float x, float y) {
    }

    @Override
    public void onLeftToRightSwipe(View v) {
    }

    @Override
    public void onRightToLeftSwipe(View v) {
    }

    @Override
    public void onTopToBottomSwipe(View v) {
    }

    @Override
    public void onBottomToTopSwipe(View v) {
    }

    @Override
    public void onLeftToRightScroll(View v, float x, float y, float distx) {
    }

    @Override
    public void onRightToLeftScroll(View v, float x, float y, float distx) {
    }

    @Override
    public void onTopToBottomScroll(View v, float x, float y, float disty) {
    }

    @Override
    public void onBottomToTopScroll(View v, float x, float y, float disty) {
    }
}
