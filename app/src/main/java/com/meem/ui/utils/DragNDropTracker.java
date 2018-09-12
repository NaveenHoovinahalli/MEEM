package com.meem.ui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;

import java.lang.ref.WeakReference;

/**
 * This class manages the drag and drop after the drop is made on a wrong target - basically it animates back the drag shadow back to the
 * source.
 * <p/>
 * Important: In this class, we should not keep a strong reference to the bitmap cache of the view which initiates the drag. So a weak
 * reference is used, and is always checked for null before "animating it back". If we keep a strong reference to any bitmap cache objects,
 * random crashes are observed about using recycled bitmaps, which makes sense as cache objects are not supposed to be kept in our class
 * with a string reference to it.
 *
 * @author Arun T A
 */

public class DragNDropTracker {
    private static String TAG = "DnDTracker";
    ViewGroup mRootLayout;

    WeakReference<Bitmap> mShadowBitmapWeakRef;

    int[] mDragSrcLoc = new int[2];

    UiContext mUiContext = UiContext.getInstance();

    public DragNDropTracker(ViewGroup rootLayout) {
        mRootLayout = rootLayout;
    }

    private int[] getLocationRelativeToRoot(View view) {
        Rect childVisibleRect = new Rect();
        view.getGlobalVisibleRect(childVisibleRect);

        Rect rootVisibleRect = new Rect();
        mRootLayout.getGlobalVisibleRect(rootVisibleRect);

        int[] loc = new int[2];
        loc[0] = childVisibleRect.left - rootVisibleRect.left;
        loc[1] = childVisibleRect.top - rootVisibleRect.top;

        return loc;
    }

    public void onDragStarted(View srcView) {
        createSrcShadow(srcView);

        int[] relativeLoc = getLocationRelativeToRoot(srcView);
        mDragSrcLoc[0] = relativeLoc[0];
        mDragSrcLoc[1] = relativeLoc[1];
    }

    private void createSrcShadow(View srcView) {
        srcView.setDrawingCacheEnabled(true);
        srcView.buildDrawingCache();
        mShadowBitmapWeakRef = new WeakReference<Bitmap>(srcView.getDrawingCache());
    }

    /**
     * The X and Y position of the drop point is relative to the View object's bounding box.
     *
     * @param dropTarget
     * @param dropAtX
     * @param dropAtY
     */
    public void onInvalidDrop(View dropTarget, int dropAtX, int dropAtY) {
        Bitmap shadowBitmap = mShadowBitmapWeakRef.get();
        if (shadowBitmap == null) {
            Log.e(TAG, "Warning: onInvalidDrop: shadowBitmap is null");
            return;
        }

        final ImageView imageView = new ImageView(mUiContext.getAppContext());
        imageView.setImageBitmap(shadowBitmap);

        int w = shadowBitmap.getWidth();
        int h = shadowBitmap.getHeight();

        int[] relativeLocOfDropView = getLocationRelativeToRoot(dropTarget);
        int relDropAtX = relativeLocOfDropView[0] + dropAtX;
        int relDropAtY = relativeLocOfDropView[1] + dropAtY;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        // drop point is at the center of drop shadow
        lp.leftMargin = relDropAtX - w / 2;
        lp.topMargin = relDropAtY - h / 2;

        imageView.setLayoutParams(lp);

        imageView.offsetTopAndBottom(relDropAtY - h / 2);
        imageView.offsetLeftAndRight(relDropAtX - w / 2);
        imageView.setAlpha(0.75f);

        mRootLayout.addView(imageView);

        AnimatorSet animSetXY = new AnimatorSet();

        ObjectAnimator xTrans = ObjectAnimator.ofFloat(imageView, "x", mDragSrcLoc[0]);
        ObjectAnimator yTrans = ObjectAnimator.ofFloat(imageView, "y", mDragSrcLoc[1]);

        animSetXY.playTogether(xTrans, yTrans);
        animSetXY.setInterpolator(new LinearInterpolator());
        animSetXY.setDuration(500);
        animSetXY.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                imageView.animate().alpha(0).setDuration(ProductSpecs.TIME_VAULT_SPIRIT_RETURN_ON_INVALID_DROP).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        imageView.getDrawable().setCallback(null);
                        imageView.setImageBitmap(null);
                        mRootLayout.removeView(imageView);
                    }
                });

            }
        });

        animSetXY.start();
    }

    /**
     * The X and Y position of the drop point is relative to the View object's bounding box.
     *
     * @param dropTarget
     * @param dropAtX
     * @param dropAtY
     * @param runAtEndOfAnim
     */
    public void onValidDrop(View dropTarget, int dropAtX, int dropAtY, final Runnable runAtEndOfAnim) {
        Bitmap shadowBitmap = mShadowBitmapWeakRef.get();
        if (shadowBitmap == null) {
            Log.w(TAG, "Warning: onValidDrop: shadowBitmap is null");
            return;
        }

        final ImageView imageView = new ImageView(mUiContext.getAppContext());
        imageView.setImageBitmap(shadowBitmap);

        int w = shadowBitmap.getWidth();
        int h = shadowBitmap.getHeight();

        int[] relativeLocOfDropView = getLocationRelativeToRoot(dropTarget);
        int relDropAtX = relativeLocOfDropView[0] + dropAtX;
        int relDropAtY = relativeLocOfDropView[1] + dropAtY;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = relDropAtX - w / 2;
        lp.topMargin = relDropAtY - h / 2;

        imageView.setLayoutParams(lp);

        imageView.offsetTopAndBottom(relDropAtY - h / 2);
        imageView.offsetLeftAndRight(relDropAtX - w / 2);
        imageView.setAlpha(0.75f);

        mRootLayout.addView(imageView);
        AnimatorSet animSetXY = new AnimatorSet();

        ObjectAnimator xTrans = ObjectAnimator.ofFloat(imageView, "x", relativeLocOfDropView[0]);
        ObjectAnimator yTrans = ObjectAnimator.ofFloat(imageView, "y", relativeLocOfDropView[1]);

        animSetXY.playTogether(xTrans, yTrans);
        animSetXY.setInterpolator(new LinearInterpolator());
        animSetXY.setDuration(500);
        animSetXY.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                imageView.animate().alpha(0).setDuration(ProductSpecs.TIME_VAULT_SPIRIT_RETURN_ON_VALID_DROP).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        imageView.getDrawable().setCallback(null);
                        imageView.setImageBitmap(null);
                        mRootLayout.removeView(imageView);
                        if (null != runAtEndOfAnim) {
                            runAtEndOfAnim.run();
                        }
                    }
                });
            }
        });
        animSetXY.start();
    }
}
