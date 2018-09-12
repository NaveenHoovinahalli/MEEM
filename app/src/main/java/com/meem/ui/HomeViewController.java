package com.meem.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.MeemSegmentUi.MeemSegmentListener;
import com.meem.ui.MiddleSegmentUi.MiddleSegmentListener;
import com.meem.ui.PhoneSegmentUi.PhoneSegmentListener;
import com.meem.ui.utils.DragNDropTracker;
import com.meem.utils.DebugTracer;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.PhoneInfo;
import com.meem.viewmodel.VaultInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 * This class will act as event listener for view segments in the GUI. In this way, it can act as the single controller who coordinate the
 * whole GUI views.
 *
 * @author arun
 */
public class HomeViewController implements PhoneSegmentListener, MiddleSegmentListener, MeemSegmentListener {
    private static final String TAG = "HomeViewController";
    DebugTracer mDbg = new DebugTracer(TAG, "HomeViewController.log");

    DragNDropTracker mDndTracker;

    PhoneSegmentUi mPhoneSegment;
    MeemSegmentUi mMeemSegment;
    MiddleSegmentUi mMiddleSegment;

    // The following 3 members are for managing category-wise operations.
    CountDownTimer mCatOpTimer;
    ArrayList<CategoryInfo> mCatOpInfoList = new ArrayList<>();
    String mCatOpSrcUpid;
    boolean mCatOpIsBackup, mIsCatOpInProgress;

    HomeViewControllerListener mListener;

    String mSessionUpid;
    ArrayList<Byte> mSessionCatsForAnim;

    boolean mLockState;
    boolean mPinSetupInProgress, mPinAuthInProgress, mAutoBackupCountDownInProgress;
    boolean mStartAutoBackup;

    FrameLayout mParentLayout;

    boolean mCatListItemTapped;

    public HomeViewController(FrameLayout parentLayout, FrameLayout phoneLayout, FrameLayout middleLayout, FrameLayout meemLayout) {
        mParentLayout = parentLayout;

        mMiddleSegment = new MiddleSegmentUi(middleLayout);
        mMiddleSegment.setEventListener(this);
        mMiddleSegment.create();

        mPhoneSegment = new PhoneSegmentUi(phoneLayout);
        mPhoneSegment.setEventListener(this);
        mPhoneSegment.create();

        mMeemSegment = new MeemSegmentUi(meemLayout);
        mMeemSegment.setEventListener(this);
        mMeemSegment.create(true);

        mDndTracker = new DragNDropTracker(parentLayout);

        mMiddleSegment.showDisconnectedStateUi();
    }

    /**
     * ********************************************************
     * <p/>
     * Public interfaces
     * <p/>
     * ********************************************************
     */

    public void setEventListener(HomeViewControllerListener listener) {
        mDbg.trace();
        mListener = listener;
    }

    public void update(PhoneInfo phoneInfo, CableInfo cableInfo) {
        mDbg.trace();

        // Update phoneInfo's vault reference (Mirror vault) as a precautionary measure.
        phoneInfo.setVaultInfo(cableInfo.getVaultInfo(phoneInfo.getmUpid()));

        mPhoneSegment.updatePhoneInfo(phoneInfo);
        mMeemSegment.updateCableInfo(cableInfo);
    }

    // --------------------------------------------------------
    // ----------- Start: AutoBackupUi related ----------------
    // --------------------------------------------------------

    public void showAutobackupCountDown() {
        mDbg.trace();

        mMiddleSegment.showAutoBackupCountDownUi();
        mAutoBackupCountDownInProgress = true;
    }

    @Override
    public void onAutoBackupCountDownEnd(boolean startAutoBackup) {
        mDbg.trace();

        mStartAutoBackup = startAutoBackup;
        mListener.onAutoBackupCountDownEnd();
    }

    @Override
    public void onAutoBackupCountDownUiFinish() {
        mDbg.trace();

        mListener.onAutoBackupCountDownUiFinish();
        mAutoBackupCountDownInProgress = false;
    }

    public void removeAutoBackupcountDown() {
        mDbg.trace();
        mMiddleSegment.removeAutoBackupCountDownUi();
    }

    // --------------------------------------------------------
    // ----------- End: AutoBackupUi related ------------------
    // --------------------------------------------------------

    // --------------------------------------------------------
    // ---------- Start: DisconnectStateUi related stuff
    // --------------------------------------------------------

    public void showCableConnected() {
        mDbg.trace();
        mMiddleSegment.showCableConnected();
    }

   /* public void showInitializingProgressBar(int percent) {
        mDbg.trace();
        mMiddleSegment.showInitializingProgressBar(percent);
    }

    public void hideInitializingProgressBar() {
        mDbg.trace();
        mMiddleSegment.hideInitializingProgressBar();
    }*/

    public void removeDisconnectedStateUi() {
        mDbg.trace();
        mMiddleSegment.removeDisconnectedStateUi();
    }

    public void showCableConnectionSequence() {
        mDbg.trace();
        startMainScreenAnimationOnConnect();
    }

    public void showCableDisconnectionSequence() {
        mDbg.trace();

        if (mPinSetupInProgress) {
            removeVirginCablePinSetupUi();
        } else if (mPinAuthInProgress) {
            removeUnregisteredPhoneAuthUi();
        } else if (mAutoBackupCountDownInProgress) {
            removeAutoBackupcountDown();
        }

        startMainScreenAnimationOnDisconnect();
    }

    @Override
    public void onDisconnectedStateUiFinish() {
        mDbg.trace();
        mListener.onDisconnectedStateUiFinished();
    }


    // --------------------------------------------------------
    // ---------- End: DisconnectStateUi related stuff
    // --------------------------------------------------------

    // --------------------------------------------------------
    // ---------- Start: VirginCablePinSetupUi related stuff
    // --------------------------------------------------------

    public void showVirginCablePinSetupUi() {
        mDbg.trace();

        mMiddleSegment.showVirginCablePinSetupUi();
        mPinSetupInProgress = true;
    }

    @Override
    public void onVirginCablePinSetupEntry(String pin, LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        mDbg.trace();
        mListener.onVirginCablePinSetupEntry(pin, recoveryAnswers, responseCallback);
    }

    @Override
    public void onVirginCablePinSetupUiFinish() {
        mDbg.trace();

        mListener.onVirginCablePinSetupUiFinished();
        mPinSetupInProgress = false;
    }

    public void removeVirginCablePinSetupUi() {
        mDbg.trace();
        mMiddleSegment.removeVirginCablePinSetupUi();
    }

    // --------------------------------------------------------
    // ---------- End: VirginCablePinSetupUi related stuff
    // --------------------------------------------------------


    // --------------------------------------------------------
    // ---------- Start: UnregisteredPhoneAuthUi related stuff
    // --------------------------------------------------------

    public void showUnregisteredPhoneAuthUi() {
        mDbg.trace();

        mMiddleSegment.showUnregisteredPhoneAuthUi();
        mPinAuthInProgress = true;
    }

    @Override
    public void onUnregisteredPhoneAuthEntry(String pin, ResponseCallback responseCallback) {
        mDbg.trace();
        mListener.onUnregisteredPhoneAuthEntry(pin, responseCallback);
    }

    @Override
    public void onUnregisteredPhoneAuthUiFinish() {
        mDbg.trace();

        mListener.onUnregisteredPhoneAuthUiFinished();
        mPinAuthInProgress = false;
    }

    @Override
    public void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        mDbg.trace();
        mListener.onValidateRecoveryAnswers(recoveryAnswers, responseCallback);
    }

    public void removeUnregisteredPhoneAuthUi() {
        mDbg.trace();
        mMiddleSegment.removeUnregisteredPhoneAuthUi();
    }

    // --------------------------------------------------------
    // ---------- End: UnregisteredPhoneAuthUi related stuff
    // --------------------------------------------------------

    @Override
    public Activity getActivity() {
        mDbg.trace();
        return mListener.getMainActivity();
    }

    // =========================================================

    private void startSynchronizedMeemUiOpenCloseAnimation(MeemUi anchor, final boolean open, final Runnable runOnAnimEnd) {
        mDbg.trace();

        AnimatorSet animSet = new AnimatorSet();
        Vector<Animator> animList = new Vector<>();

        animList.addAll(mPhoneSegment.getPhoneAvatarOpenCloseAnimatorList(open, anchor.getVerticalShiftOnAnchoring()));
        animList.addAll(mMeemSegment.getAllMeemUiOpenCloseAnimatorList(anchor, open));

        animSet.playTogether(animList);
        animSet.setDuration(GuiSpec.ANIM_TEST_DURATION_SLOW);

        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!open) {
                    mMeemSegment.onMeemUiOpenCloseAnimationEnd();
                    if (runOnAnimEnd != null) {
                        runOnAnimEnd.run();
                    }
                }
            }
        });

        animSet.start();
    }

    // =========================================================

    // This function takes apparently unnecessary arguments every time. Trust me, it is OK./ This is done mainly to avoid too many
    // state information regarding category-wise swipe gestures in this class.
    private boolean processCatOpGesture(String srcUpid, CategoryInfo catInfo, boolean catOpIsBackup) {
        mDbg.trace();

        if (!mIsCatOpInProgress) {
            mDbg.trace("New category operation begins.");

            mCatOpSrcUpid = srcUpid;
            mCatOpInfoList.clear();

            mIsCatOpInProgress = true;
        } else {
            // restart the timer on a new category swipe.
            mDbg.trace("Cancelling running countdown timer");
            mCatOpTimer.cancel();
        }

        mCatOpIsBackup = catOpIsBackup;

        if (!mCatOpInfoList.contains(catInfo)) {
            mCatOpInfoList.add(catInfo);
        } else {
            // No need to process this or animate this.
            return false;
        }

        if (catOpIsBackup) {
            mMeemSegment.setLockState(true);
        } else {
            mPhoneSegment.setLockState(true);
        }

        mDbg.trace("Starting new countdown timer");
        mCatOpTimer = new CountDownTimer(ProductSpecs.CATEGORY_WISE_OPERATIONS_COUNTDOWN_MS, ProductSpecs.CATEGORY_WISE_OPERATIONS_COUNTDOWN_TICK_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Nothing.
            }

            @Override
            public void onFinish() {
                mIsCatOpInProgress = false;
                mDbg.trace("*** Category operation begins for : " + mCatOpSrcUpid + "\nCatInfos: " + mCatOpInfoList);

                if (mMeemSegment.isAnyMeemUiOpen()) {
                    MeemUi mui = mMeemSegment.getMeemUi(mCatOpSrcUpid);
                    startSynchronizedMeemUiOpenCloseAnimation(mui, false, new Runnable() {
                        @Override
                        public void run() {
                            if (mCatOpIsBackup) {
                                mListener.onCategorySetSwipeToMirror(mCatOpInfoList);
                            } else {
                                mListener.onCategorySetSwipeToPhone(mCatOpSrcUpid, mCatOpInfoList);
                            }
                        }
                    });
                } else {
                    if (mCatOpIsBackup) {
                        mListener.onCategorySetSwipeToMirror(mCatOpInfoList);
                    } else {
                        mListener.onCategorySetSwipeToPhone(mCatOpSrcUpid, mCatOpInfoList);
                    }
                }
            }
        }.start();

        return true;
    }

    private int[] getLocationRelativeToFragmentRoot(View view) {
        Rect childVisibleRect = new Rect();
        view.getGlobalVisibleRect(childVisibleRect);

        Rect rootVisibleRect = new Rect();
        mParentLayout.getGlobalVisibleRect(rootVisibleRect);

        int[] loc = new int[2];
        loc[0] = childVisibleRect.left - rootVisibleRect.left;
        loc[1] = childVisibleRect.top - rootVisibleRect.top;

        return loc;
    }

    private void animateCatOpGesture(final ViewGroup srcViewGroup, final ViewGroup dstViewGroup, final CategoryInfo catInfo, final boolean catOpIsBackup) {
        mDbg.trace();

        srcViewGroup.setDrawingCacheEnabled(true);
        srcViewGroup.buildDrawingCache();
        WeakReference<Bitmap> shadowBitmapWeakRef = new WeakReference<>(srcViewGroup.getDrawingCache());

        UiContext uiCtxt = UiContext.getInstance();

        final ImageView imageView = new ImageView(uiCtxt.getAppContext());
        Bitmap shadowBitmap = shadowBitmapWeakRef.get();

        if (shadowBitmap != null) {
            imageView.setImageBitmap(shadowBitmap);

            int w = shadowBitmap.getWidth();
            int h = shadowBitmap.getHeight();

            final FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(w, h);
            float transX = 0;

            int[] relLocSrc = getLocationRelativeToFragmentRoot(srcViewGroup);
            int[] relLocDst = getLocationRelativeToFragmentRoot(dstViewGroup);

            mDbg.trace("IsBackup: " + mCatOpIsBackup + ", srcLoc: " + relLocSrc[0] + ", " + relLocSrc[1] + ", destLoc: " + relLocDst[0] + ", " + relLocDst[1]);

            flp.leftMargin = relLocSrc[0];
            flp.topMargin = relLocSrc[1];

            transX = relLocDst[0] - relLocSrc[0];

            imageView.setLayoutParams(flp);
            mParentLayout.addView(imageView);

            final float finalTransX = transX;
            final float finalTransX1 = transX;
            final float finalTransX2 = transX;
            imageView.animate().translationX(transX + transX).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    imageView.getDrawable().setCallback(null);
                    imageView.setImageBitmap(null);
                    mParentLayout.removeView(imageView);

                    dstViewGroup.setDrawingCacheEnabled(true);
                    dstViewGroup.buildDrawingCache();
                    WeakReference<Bitmap> shadowBitmapWeakRef = new WeakReference<>(dstViewGroup.getDrawingCache());

                    UiContext uiCtxt = UiContext.getInstance();

                    final ImageView imageViewTwo = new ImageView(uiCtxt.getAppContext());
                    Bitmap shadowBitmap = shadowBitmapWeakRef.get();
                    if (shadowBitmap != null) {
                        imageViewTwo.setImageBitmap(shadowBitmap);

                        int w = shadowBitmap.getWidth();
                        int h = shadowBitmap.getHeight();

                        final FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(w, h);

                        int[] relLocSrc = getLocationRelativeToFragmentRoot(srcViewGroup);
                        int[] relLocDst = getLocationRelativeToFragmentRoot(dstViewGroup);

                        mDbg.trace("IsBackup: " + mCatOpIsBackup + ", srcLoc: " + relLocSrc[0] + ", " + relLocSrc[1] + ", destLoc: " + relLocDst[0] + ", " + relLocDst[1]);

                        flp.leftMargin = relLocDst[0];
                        flp.topMargin = relLocDst[1];

                        imageViewTwo.setLayoutParams(flp);
                        mParentLayout.addView(imageViewTwo);
                        imageViewTwo.setTranslationX(finalTransX1);

                        imageViewTwo.animate().translationX(0).setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                imageViewTwo.getDrawable().setCallback(null);
                                imageViewTwo.setImageBitmap(null);

                                mParentLayout.removeView(imageViewTwo);

                                if (catOpIsBackup) {
                                    mMeemSegment.setLockState(false);
                                } else {
                                    mPhoneSegment.setLockState(false);
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        }).setDuration(ProductSpecs.DEFAULT_ANIM_DURATION / 3).start();
                    }
                }
            }).setDuration(ProductSpecs.DEFAULT_ANIM_DURATION / 2).start();
        }
    }

    // =========================================================

    @Override
    public void onPhoneAvatarOpen() {
        mDbg.trace("Phone avatar opened");
        startSynchronizedMeemUiOpenCloseAnimation(mMeemSegment.getPrimaryMeemUi(), true, null);
    }

    @Override
    public void onPhoneAvatarClose() {
        mDbg.trace("Phone avatar closed");
        startSynchronizedMeemUiOpenCloseAnimation(mMeemSegment.getCurrentAnchor(), false, new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    MeemUi mirrorUi = mMeemSegment.getPrimaryMeemUi();
                    if (mirrorUi != null) {
                        if (!mLockState) {
                            // go for updating vault only if a category item is tapped.
                            if (mCatListItemTapped) {
                                mCatListItemTapped = false;
                                mListener.onUpdateVault(mirrorUi.getVaultInfo(), null);
                            }
                        } else {
                            mDbg.trace("No vault updates allowed now (UI is locked. May be session ongoing.)");
                        }
                    } else {
                        mDbg.trace("Mirror meem ui is null. 4 phone scenario?");
                    }
                }
            }
        });
    }

    @Override
    public void onPhoneCategoryTap(byte cat) {
        mDbg.trace("Phone category tapped: " + cat);
        mCatListItemTapped = true;
        mMeemSegment.onVirtualCategoryTap(cat);
    }

    @Override
    public void onPhoneCategorySwipe(PhoneAvatarUi avatarUi, byte cat) {
        mDbg.trace("Phone category swiped: " + cat);

        PhoneInfo phInfo = avatarUi.getPhoneInfo();
        String upid = phInfo.getmUpid();
        CategoryInfo catInfo = phInfo.getVaultInfo().getmCategoryInfoMap().get(cat);

        if (catInfo.getmBackupMode() == CategoryInfo.BackupMode.DISABLED) {
            mDbg.trace("Ignoring disabled category swipe for: " + cat);
            return;
        }

        // Process the gesture = update information for backend preperation
        if (processCatOpGesture(upid, catInfo, true)) {
            // Now do the animation for gesture.
            ViewGroup phoneCatListViewGroup = avatarUi.getSuperlistItemViewGroup(cat);

            MeemUi meemUi = mMeemSegment.getMeemUi(upid);
            ViewGroup meemCatListViewGroup = meemUi.getSuperlistItemViewGroup(cat);

            animateCatOpGesture(phoneCatListViewGroup, meemCatListViewGroup, catInfo, true);
        }
    }

    @Override
    public void onPhoneAvatarDragStart(PhoneAvatarUi pv) {
        mDbg.trace("User started dragging phone avatar");
        mDndTracker.onDragStarted(pv.getView());
    }

    @Override
    public void onValidDropOnPhoneAvatar(PhoneAvatarUi pv, final String dropUpid, float x, float y) {
        mDbg.trace("Valid drop on phone avatar");
        mDndTracker.onValidDrop(pv.getView(), (int) x, (int) y, new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onDropOnPhone(dropUpid);
                }
            }
        });
    }

    @Override
    public void onInvalidDropOnPhoneAvatar(PhoneAvatarUi pv, float x, float y) {
        mDbg.trace("Invalid drop on phone avatar");
        mDndTracker.onInvalidDrop(pv.getView(), (int) x, (int) y);
    }

    @Override
    public void onDropOnPhoneSegment(float x, float y) {
        mDbg.trace("Drop on phone segment");
        mDndTracker.onInvalidDrop(mPhoneSegment.getView(), (int) x, (int) y);
    }

    // -----------------------------

    @Override
    public void onMeemUiOpen(MeemUi mv) {
        mDbg.trace("Meem opened");
        startSynchronizedMeemUiOpenCloseAnimation(mv, true, null);
    }

    @Override
    public void onMeemUiClose(final MeemUi mv) {
        mDbg.trace("Meem closed");
        startSynchronizedMeemUiOpenCloseAnimation(mv, false, new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    if (!mLockState) {
                        // go for updating vault only if a category item is tapped.
                        if (mCatListItemTapped) {
                            mCatListItemTapped = false;
                            mListener.onUpdateVault(mv.getVaultInfo(), null);
                        }
                    } else {
                        mDbg.trace("No vault updates allowed now (UI is locked. May be session ongoing.)");
                    }
                }
            }
        });
    }

    @Override
    public void onMeemCategoryTap(MeemUi mv, byte cat) {
        mDbg.trace("Meem category tapped: " + cat);
        mCatListItemTapped = true;
    }

    @Override
    public void onMeemCategorySwipe(MeemUi mv, byte cat) {
        mDbg.trace("Meem category swiped: " + cat);

        VaultInfo vaultInfo = mv.getVaultInfo();
        String upid = vaultInfo.getmUpid();
        CategoryInfo catInfo = vaultInfo.getmCategoryInfoMap().get(cat);

        if (catInfo.getmBackupMode() == CategoryInfo.BackupMode.DISABLED) {
            mDbg.trace("Ignoring disabled category swipe for: " + cat);
            return;
        }

        // Process the gesture = update information for backend preperation
        if (processCatOpGesture(upid, catInfo, false)) {
            // Now do the animation for gesture.
            ViewGroup meemCatListViewGroup = mv.getSuperlistItemViewGroup(cat);

            PhoneAvatarUi avatarUi = mPhoneSegment.getAvatarUi();
            ViewGroup phoneCatListViewGroup = avatarUi.getSuperlistItemViewGroup(cat);

            animateCatOpGesture(meemCatListViewGroup, phoneCatListViewGroup, catInfo, false);
        }
    }

    @Override
    public void onMeemUiDragStart(MeemUi mv) {
        mDbg.trace("User started dragging meem view");
        mDndTracker.onDragStarted(mv.getView());
    }

    @Override
    public void onValidDropOnMeemUi(MeemUi mv, float x, float y) {
        mDbg.trace("Valid drop on meem view");
        mDndTracker.onValidDrop(mv.getView(), (int) x, (int) y, new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onDropOnMirror();
                }
            }
        });
    }

    @Override
    public void onInvalidDropOnMeemUi(MeemUi mv, float x, float y) {
        mDbg.trace("Invalid drop on meem view");
        mDndTracker.onInvalidDrop(mv.getView(), (int) x, (int) y);
    }

    @Override
    public void onDropOnMeemSegment(float x, float y) {
        mDbg.trace("Drop on meem segment");
        mDndTracker.onInvalidDrop(mMeemSegment.getView(), (int) x, (int) y);
    }

    // ----------------------------------------------------------------
    // Other public functions
    // ----------------------------------------------------------------

    public void startMainScreenAnimationOnConnect() {
        mDbg.trace();

        startMainScreenAnimation(true, new Runnable() {
            @Override
            public void run() {
                if (mStartAutoBackup) {
                    mStartAutoBackup = false;
                    mListener.onDropOnMirror();
                }
            }
        });
    }

    public void startMainScreenAnimationOnDisconnect() {
        mDbg.trace();

        // Does not really matter is both segments were there or not.
        // If they are not there, then also, this code will work beautifully.
        startMainScreenAnimation(false, new Runnable() {
            @Override
            public void run() {
                mLockState = false;
                mMiddleSegment.showDisconnectedStateUi();
            }
        });
    }

    private void startMainScreenAnimation(boolean show, final Runnable onFinish) {
        mDbg.trace();

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(mPhoneSegment.getSlideAnimator(show), mMeemSegment.getSlideInAnimator(show));
        animSet.setDuration(GuiSpec.ANIM_TEST_DURATION_SLOW);

        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (onFinish != null) onFinish.run();
            }
        });

        animSet.start();
    }

    public boolean getUiLockState() {
        mDbg.trace();

        return mLockState;
    }

    public void setUiLockState(boolean lockState) {
        mDbg.trace();

        mLockState = lockState;

        mPhoneSegment.setLockState(lockState);
        mMeemSegment.setLockState(lockState);
    }

    public void startSessionAnimations(VaultInfo vaultInfo, ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        mSessionUpid = new String(vaultInfo.getmUpid());
        mSessionCatsForAnim = new ArrayList<>();

        // lets keep cats to be animated in view controller.
        if (catInfos == null) {
            // this means whole vault is involved. we must take care of backup modes.
            LinkedHashMap<Byte, CategoryInfo> catInfoList = vaultInfo.getmCategoryInfoMap();

            for (Map.Entry<Byte, CategoryInfo> entry : catInfoList.entrySet()) {
                CategoryInfo catInfo = entry.getValue();
                if (catInfo.getmBackupMode() != CategoryInfo.BackupMode.DISABLED) {
                    mSessionCatsForAnim.add(catInfo.mMmpCode);
                }
            }
        } else {
            // this is a category-wise session. just keep all selected cats for the session
            for (int i = 0; i < catInfos.size(); i++) {
                byte catCode = catInfos.get(i).mMmpCode;

                // TODO: This is ugly. SDCard related stuff is supposed to be totally hidden UI classes.
                // Ideally, the whole GUI crap (especially superlist) must change. Or, we can do the filtering in
                // CablePresenter who is supposed to be a single point of such conversions.
                if (!isSdCardMappedCat(catCode)) {
                    mSessionCatsForAnim.add(catCode);
                }
            }
        }

        mPhoneSegment.startSessionAnimations(mSessionCatsForAnim);
        mMeemSegment.startSessionAnimations(mSessionUpid, mSessionCatsForAnim);
    }

    private boolean isSdCardMappedCat(byte viewModelCat) {
        return (viewModelCat == MMPConstants.MMP_CATCODE_PHOTO_CAM ||
                viewModelCat == MMPConstants.MMP_CATCODE_VIDEO_CAM ||
                viewModelCat == MMPConstants.MMP_CATCODE_FILE ||
                viewModelCat == MMPConstants.MMP_CATCODE_DOCUMENTS_SD);
    }


    public void stopSessionAnimations() {
        mDbg.trace();

        mPhoneSegment.stopSessionAnimations(mSessionCatsForAnim);
        mMeemSegment.stopSessionAnimations(mSessionUpid, mSessionCatsForAnim);
    }

    public void stopSessionAnimForCat(byte cat) {
        mDbg.trace();

        mPhoneSegment.stopSessionAnimationForCat(cat);
        mMeemSegment.stopSessionAnimationForCat(mSessionUpid, cat);
        if (mSessionCatsForAnim.contains(cat)) {
            mSessionCatsForAnim.remove(mSessionCatsForAnim.indexOf(cat));
        }
    }

    public void animateNetworkSearch(boolean start) {
        mMiddleSegment.animateNetworkSearch(start);
    }

    public void onCableAcquireRequest(String upid) {
        mMeemSegment.onCableAcquireRequest(upid);
    }

    public void onCableReleaseRequest(String upid) {
        mMeemSegment.onCableReleaseRequest(upid);
    }

    /**
     * upid null means this phone is the net client. So, all meem ui will be updated.
     *
     * @param upid
     */
    public void onRemoteClientStart(String upid) {
        mMeemSegment.onRemoteClientStart(upid);
    }

    /**
     * upid null means this phone is the net client. So, all meem ui will be updated.
     *
     * @param upid
     */
    public void onRemoteClientQuit(String upid) {
        mMeemSegment.onRemoteClientQuit(upid);
    }

    // Should be implemented by home fragment
    public interface HomeViewControllerListener {
        Activity getMainActivity();

        void onDropOnPhone(String srcUpid);
        void onDropOnMirror();
        void onCategorySetSwipeToMirror(ArrayList<CategoryInfo> catInfoList);
        void onCategorySetSwipeToPhone(String srcUpid, ArrayList<CategoryInfo> catInfoList);
        void onUpdateVault(VaultInfo vault, ResponseCallback responseCallback);
        void onAbortRequest();

        void onDisconnectedStateUiFinished();

        void onAutoBackupCountDownEnd();
        void onAutoBackupCountDownUiFinish();

        void onVirginCablePinSetupEntry(String pin, LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
        void onVirginCablePinSetupUiFinished();

        void onUnregisteredPhoneAuthEntry(String pin, ResponseCallback responseCallback);
        void onUnregisteredPhoneAuthUiFinished();

        void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
    }
}
