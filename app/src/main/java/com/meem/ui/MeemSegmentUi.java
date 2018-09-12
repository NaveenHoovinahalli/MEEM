package com.meem.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.MeemUi.MeemViewEventListener;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.VaultInfo;

import java.util.ArrayList;
import java.util.Vector;

/**
 * The class that manages right side pane of the GUI. This handles the placement of meems and also the settings related UI things.
 * <p/>
 * Note: We no longer use green meen and blue/copy meem terminology in UI - as this wll be problematic in many scenarios. So, from a UI
 * perspective, the MeemUis are managed based upon their ID, which is simply an index into an array of MeemUi.
 *
 * @author arun
 */
public class MeemSegmentUi implements MeemViewEventListener {
    private static final String TAG = "MeemSegmentUi";
    protected MeemSegmentListener mListener;
    UiContext mUiCtxt = UiContext.getInstance();
    float mTransX;
    private CableInfo mCableInfo;
    private FrameLayout mRootView;
    private MeemUi[] mMeemUiArray;
    // Important: Current Anchor is the MeemUI object which shall do the height animation. This
    // is named as "Anchor" because this object will decide phone avatar's Y translation.
    // Anchor object will be selected by ViewController based upon whether the user tapped on
    // phone avatar or any meem uis.
    private MeemUi mCurrentAnchor;

    public MeemSegmentUi(FrameLayout viewRoot) {
        mRootView = viewRoot;
    }

    public void setEventListener(MeemSegmentListener listener) {
        mListener = listener;
    }

    public FrameLayout getView() {
        return mRootView;
    }

    /**
     * TODO: Pass MeemCable model object here.
     *
     * @param isCableVirgin
     *
     * @return
     */
    public boolean create(boolean isCableVirgin) {
        FrameLayout.LayoutParams llp = (FrameLayout.LayoutParams) mRootView.getLayoutParams();
        llp.width = (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.MEEM_SIDE_WIDTH);
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

        mTransX = mUiCtxt.getScreenWidthPix();
        mRootView.setTranslationX(mTransX);

        mMeemUiArray = new MeemUi[ProductSpecs.LIMIT_MAX_VAULTS];
        return true;
    }

    private void onDnDDropEvent(DragEvent event) {
        mListener.onDropOnMeemSegment(event.getX(), event.getY());
    }

    public void updateCableInfo(CableInfo cableInfo) {
        if (cableInfo == null) {
            return;
        }

        // cleanup all previous views
        mRootView.removeAllViews();
        for (int i = 0; i < mMeemUiArray.length; i++) {
            mMeemUiArray[i] = null;
        }

        mCableInfo = cableInfo;

        for (String upid : mCableInfo.getmVaultInfoMap().keySet()) {
            VaultInfo vi = mCableInfo.getmVaultInfoMap().get(upid);
            addMeem(vi, vi.ismIsMirror());
        }

        // Very important to call this function.
        finalizeSegmentUi();
    }

    /**
     * Add a new meem view to the segment.
     */
    private void addMeem(VaultInfo vaultInfo, boolean isMirror) {
        MeemUi mv = new MeemUi(mRootView, vaultInfo);
        mv.setEventListener(this);
        mv.create();

        // assign ID, color and mirror information to MeemUi
        if (isMirror) {
            Log.d(TAG, "Adding mirror meem");
            setupMirrorMeemUi(mv);
        } else {
            if (mMeemUiArray[GuiConstants.SECOND_MEEM_IDX] == null) {
                Log.d(TAG, "Adding second meem");
                setupSecondMeemUi(mv);
            } else if (mMeemUiArray[GuiConstants.THIRD_MEEM_IDX] == null) {
                setupThirdMeemUi(mv);
            } else {
                if (mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX] == null) {
                    Log.d(TAG, "4th phone scenario");
                    // TODO: Handle 4th phone scenario
                } else {
                    Log.d(TAG, "Vault limit reached");
                    // TODO: Too many phones connected.
                }
            }
        }

        resetZordering();
    }

    public void finalizeSegmentUi() {
        prepareMeemUiVerticalShiftsForAnim();
    }

    public MeemUi getCurrentAnchor() {
        return mCurrentAnchor;
    }

    public void resetZordering() {
        // bring primary meem to front - if it is there.
        if (null != mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX]) {
            mRootView.bringChildToFront(mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX].getView());
        }
    }

    public MeemUi getPrimaryMeemUi() {
        return mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX];
    }

    public MeemUi getMeemUi(String upid) {
        if(upid == null) {
            return null;
        }

        MeemUi mui = null;
        for (int i = 0; i < mMeemUiArray.length; i++) {
            if (mMeemUiArray[i] != null) {
                if (mMeemUiArray[i].mVaultInfo.getmUpid().equals(upid)) {
                    mui = mMeemUiArray[i];
                    break;
                }
            }
        }

        return mui;
    }

    public boolean isAnyMeemUiOpen() {
        boolean result = false;

        result = mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX].isOpen();

        if (null != mMeemUiArray[GuiConstants.SECOND_MEEM_IDX]) {
            result |= mMeemUiArray[GuiConstants.SECOND_MEEM_IDX].isOpen();
        }

        if (null != mMeemUiArray[GuiConstants.THIRD_MEEM_IDX]) {
            result |= mMeemUiArray[GuiConstants.THIRD_MEEM_IDX].isOpen();
        }

        return result;
    }

    public ValueAnimator getSlideInAnimator(final boolean in) {
        int transX = (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.PHONE_SIDE_WIDTH) + (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.MEMORY_TUBE_WIDTH);

        ValueAnimator slide;

        if (in) {
            mRootView.setVisibility(View.VISIBLE);//Naveen:For hiding the M pattern while fragment transaction
            slide = ValueAnimator.ofFloat(mRootView.getTranslationX(), transX);
        } else {
            slide = ValueAnimator.ofFloat(mRootView.getTranslationX(), mTransX);
        }

        slide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRootView.setTranslationX((float) animation.getAnimatedValue());
            }

        });

        slide.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!in) {
                    mRootView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        return slide;
    }

    public Vector<Animator> getAllMeemUiOpenCloseAnimatorList(MeemUi anchor, boolean open) {
        MeemUi mirrorMeem = mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX];
        MeemUi secondMeem = mMeemUiArray[GuiConstants.SECOND_MEEM_IDX];
        MeemUi thirdMeem = mMeemUiArray[GuiConstants.THIRD_MEEM_IDX];

        Vector<Animator> animList = new Vector<>();

        // Very important for the logic
        if (open) {
            anchor.setAnimAnchor(true);
            mCurrentAnchor = anchor;
        }

        prepareMeemUiVerticalShiftsForAnim();

        if (mirrorMeem != null) {
            animList.addAll(mirrorMeem.getOpenCloseAnimatorList(open));
        }

        if (secondMeem != null) {
            animList.addAll(secondMeem.getOpenCloseAnimatorList(open));
        }

        if (thirdMeem != null) {
            animList.addAll(thirdMeem.getOpenCloseAnimatorList(open));
        }

        return animList;
    }

    public void onMeemUiOpenCloseAnimationEnd() {
        mCurrentAnchor.setAnimAnchor(false);
        resetZordering();
    }

    public void onVirtualCategoryTap(byte catCode) {
        MeemUi mirrorMeem = mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX];
        if (null == mirrorMeem) {
            Log.w(TAG, "No mirror meem for connected phone!");
            return;
        }

        mirrorMeem.onVirtualCategoryTap(catCode);
    }

    public void setLockState(boolean lockState) {
        for (int i = 0; i < mMeemUiArray.length; i++) {
            if (mMeemUiArray[i] != null) {
                mMeemUiArray[i].setLockState(lockState);
            }
        }
    }

    public void startSessionAnimations(String upid, ArrayList<Byte> cats) {
        for (int i = 0; i < mMeemUiArray.length; i++) {
            MeemUi mv = mMeemUiArray[i];
            if (mv != null && mv.getVaultInfo().getmUpid().equals(upid)) {
                mv.startSessionAnimations(cats);
            }
        }
    }

    public void stopSessionAnimations(String upid, ArrayList<Byte> cats) {
        for (int i = 0; i < mMeemUiArray.length; i++) {
            MeemUi mv = mMeemUiArray[i];
            if (mv != null && mv.getVaultInfo().getmUpid().equals(upid)) {
                mv.stopSessionAnimations(cats);
                break;
            }
        }
    }

    public void stopSessionAnimationForCat(String upid, byte cat) {
        for (int i = 0; i < mMeemUiArray.length; i++) {
            MeemUi mv = mMeemUiArray[i];
            if (mv != null && mv.getVaultInfo().getmUpid().equals(upid)) {
                mv.stopSessionAnimationForCat(cat);
                break;
            }
        }
    }

    // Helper functions for arranging MeemUis in the segment.
    // ------------------------------------------------------

    // Mirror meem
    private void setupMirrorMeemUi(MeemUi mv) {
        if (mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX] != null) {
            throw new IllegalStateException("BUG: Mirror meem ui already exists");
        }

        mv.setProperties(GuiConstants.MIRROR_MEEM_IDX, R.color.meemGreen, true);
        mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX] = mv;

        // Mirror MeemUi is centered in the segment.
        float defTransY = getMirrorMeemUiDefTransY();
        mv.setDefTransY(defTransY);
    }

    // Mirror meem Y location
    private float getMirrorMeemUiDefTransY() {
        return mUiCtxt.getScreenHeightPix() / 2 - mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEIGHT) / 2;
    }

    // Second meem
    private void setupSecondMeemUi(MeemUi mv) {
        mv.setProperties(GuiConstants.SECOND_MEEM_IDX, R.color.meemBlue, false);
        mMeemUiArray[GuiConstants.SECOND_MEEM_IDX] = mv;

        // Assign location
        float defTransY = getSecondMeemUiDefTransY();
        mv.setDefTransY(defTransY);
    }

    // Second meem Y location
    private float getSecondMeemUiDefTransY() {
        return getMirrorMeemUiDefTransY() / 2 - mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEIGHT) / 2;
    }

    // Third meem
    private void setupThirdMeemUi(MeemUi mv) {
        mv.setProperties(GuiConstants.THIRD_MEEM_IDX, R.color.meemBlue, false);
        mMeemUiArray[GuiConstants.THIRD_MEEM_IDX] = mv;

        // Assign location
        float defTransY = getThirdMeemUiDefTransY();
        mv.setDefTransY(defTransY);
    }

    // Third meem Y location
    private float getThirdMeemUiDefTransY() {
        return mUiCtxt.getScreenHeightPix() / 2 + mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEIGHT);
    }

    // Set vertical shift for all MeemUi for open/close animations
    private void prepareMeemUiVerticalShiftsForAnim() {
        MeemUi mirrorMeem = mMeemUiArray[GuiConstants.MIRROR_MEEM_IDX];
        MeemUi secondMeem = mMeemUiArray[GuiConstants.SECOND_MEEM_IDX];
        MeemUi thirdMeem = mMeemUiArray[GuiConstants.THIRD_MEEM_IDX];

        if (null == mirrorMeem) {
            throw new IllegalStateException("BUG: No mirror meem during setup");
        }

        float vaultHeight = mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEIGHT);
        float marginHeight = mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_LEFT_PADDING);
        float footerHeight = mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_FOOTER_HEIGHT);

        // case 1: Only mirror meem
        if (secondMeem == null && thirdMeem == null) {
            mirrorMeem.setVerticalShiftAsAnchor(marginHeight);
            mirrorMeem.setVerticalShiftInGroup(marginHeight);
            return;
        }

        // case 2: Mirror and second meem (blue)
        if (secondMeem != null && thirdMeem == null) {
            secondMeem.setVerticalShiftAsAnchor(marginHeight);
            secondMeem.setVerticalShiftInGroup(marginHeight);

            mirrorMeem.setVerticalShiftAsAnchor(footerHeight);
            if (secondMeem.isAnimAnchor()) {
                mirrorMeem.setVerticalShiftInGroup(mUiCtxt.getScreenHeightPix() - (vaultHeight * 2f));
            } else {
                mirrorMeem.setVerticalShiftInGroup(footerHeight);
            }
            return;
        }

        // case 3: Mirror, second and third meems
        if (secondMeem != null && thirdMeem != null) {
            secondMeem.setVerticalShiftAsAnchor(marginHeight);
            secondMeem.setVerticalShiftInGroup(marginHeight);

            mirrorMeem.setVerticalShiftAsAnchor(footerHeight);
            if (secondMeem.isAnimAnchor()) {
                mirrorMeem.setVerticalShiftInGroup(mUiCtxt.getScreenHeightPix() - (vaultHeight * 2f) + footerHeight / 2 - 16);
            }
            if (thirdMeem.isAnimAnchor()) {
                mirrorMeem.setVerticalShiftInGroup(footerHeight);
            }

            thirdMeem.setVerticalShiftAsAnchor(footerHeight * 2);
            if (mirrorMeem.isAnimAnchor()) {
                thirdMeem.setVerticalShiftInGroup(mUiCtxt.getScreenHeightPix() - vaultHeight / 2);
            }
            if (secondMeem.isAnimAnchor()) {
                thirdMeem.setVerticalShiftInGroup(thirdMeem.getDefTransY());
            }
        }
    }

    // Listener functionality for MeemUi class.
    // ----------------------------------------

    @Override
    public void onMeemUiOpen(MeemUi mv) {
        if (isAnyMeemUiOpen()) {
            if (!mCurrentAnchor.isEqual(mv)) {
                return;
            }
        }

        mCurrentAnchor = mv;
        mRootView.bringChildToFront(mv.getView());
        mListener.onMeemUiOpen(mv);
    }

    @Override
    public void onMeemUiClose(MeemUi mv) {
        mListener.onMeemUiClose(mv);
    }

    @Override
    public void onCategoryTap(MeemUi mv, byte cat) {
        mListener.onMeemCategoryTap(mv, cat);
    }

    @Override
    public void onCategorySwipe(MeemUi mv, byte cat) {
        mListener.onMeemCategorySwipe(mv, cat);
    }

    @Override
    public void onMeemUiDragStart(MeemUi mv) {
        mListener.onMeemUiDragStart(mv);
    }

    @Override
    public void onValidDropOnMeemUi(MeemUi mv, float x, float y) {
        mListener.onValidDropOnMeemUi(mv, x, y);
    }

    @Override
    public void onInvalidDropOnMeemUi(MeemUi mv, float x, float y) {
        mListener.onInvalidDropOnMeemUi(mv, x, y);
    }

    public void onCableAcquireRequest(String upid) {
        MeemUi meemUi = getMeemUi(upid);
        if(meemUi != null) {
            meemUi.startWifiIconAnimation();
        }
    }

    public void onCableReleaseRequest(String upid) {
        MeemUi meemUi = getMeemUi(upid);
        if(meemUi != null) {
            meemUi.stopWifiIconAnimation();
        }
    }

    /**
     * upid null means this phone is the net client. So, all meem ui will be updated.
     * @param upid
     */
    public void onRemoteClientStart(String upid) {
        if(upid == null) {
            for (int i = 0; i < mMeemUiArray.length; i++) {
                MeemUi mv = mMeemUiArray[i];
                if (mv != null) {
                    mv.showWifiIcon(true);
                }
            }
        } else {
            MeemUi meemUi = getMeemUi(upid);
            if (meemUi != null) {
                meemUi.showWifiIcon(true);
            }
        }
    }

    /**
     * upid null means this phone is the net client. So, all meem ui will be updated.
     * @param upid
     */
    public void onRemoteClientQuit(String upid) {
        if(upid == null) {
            for (int i = 0; i < mMeemUiArray.length; i++) {
                MeemUi mv = mMeemUiArray[i];
                if (mv != null) {
                    mv.showWifiIcon(false);
                }
            }
        } else {
            MeemUi meemUi = getMeemUi(upid);
            if (meemUi != null) {
                meemUi.showWifiIcon(false);
            }
        }
    }

    // Interface that must be implemented by view controller

    public interface MeemSegmentListener {
        void onMeemUiOpen(MeemUi mv);

        void onMeemUiClose(MeemUi mv);

        void onMeemCategoryTap(MeemUi mv, byte cat);

        void onMeemCategorySwipe(MeemUi mv, byte cat);

        void onMeemUiDragStart(MeemUi mv);

        void onValidDropOnMeemUi(MeemUi mv, float x, float y);

        void onInvalidDropOnMeemUi(MeemUi mv, float x, float y);

        void onDropOnMeemSegment(float x, float y);
    }
}
