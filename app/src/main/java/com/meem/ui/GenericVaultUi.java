package com.meem.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.utils.AnimDotsView;
import com.meem.ui.utils.MeemFooterTextView;
import com.meem.ui.utils.MeemHeaderTextView;

/**
 * The generic vault implementation which is a rounded rectangle view group which has a header, footer and content view. The content view is
 * hidden if the vault view is 'closed'.
 * <p/>
 * This class is abstract and the methods setupContentView() methods must be implemented in derived classes.
 * <p/>
 * A listener object is strongly recommended in this design for derived classes. The listener object must be set immediately after
 * constructing the derived objects. the order of using this class or derived classes is:
 * <p/>
 * 1) constructor 2) set event listener 2) call create(), which will internally call populatecontentView() method. The reason for
 * recommending the listener object is that, in derived objects, when setupContentView() is getting called, some information may need to be
 * obtained from listener object to create additional views to populate the contents.
 *
 * @author arun
 */
abstract public class GenericVaultUi extends MeemGestureAdapter {
    private static final String TAG = "GenericVaultUi";

    protected UiContext mUiCtxt = UiContext.getInstance();

    protected RelativeLayout mVaultLayout;
    protected RelativeLayout mHeaderLayout, mFooterLayout;
    protected FrameLayout mContentFrameLayout;

    protected MeemHeaderTextView mHeaderNameTv, mHeaderTimeStampTv;
    protected MeemFooterTextView mFooterTv;
    protected AnimDotsView mFooterDots;

    protected ImageView mPhoneIcon,mWifiIcon;

    protected FrameLayout mParentLayout;

    protected int mColor;

    protected boolean mLockState;

    /**
     * Must call setResources after this to set header and footer image resource IDs. Used for creating meem avatar view.
     */
    public GenericVaultUi(FrameLayout parent) {
        mParentLayout = parent;
    }

    public boolean create() {
        LayoutInflater inflater = (LayoutInflater) mUiCtxt.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          /*
         * Very important to pass the last argument as false as we are reusing this base class multiple times
		 * for creating phone avatar, meem avatar, meem views etc. If we use the other overloaded  argument
		 * version of inflate(id, parent), then second time call to this will just mess up the previously
		 * added view. I must pass false here and call addView() down below.
		 *
		 * Long back I wondered why we need to do the same with inflate() call from a ListView
		 * adapter's getView() method... Now I know.
		 */
        View v = inflater.inflate(R.layout.generic_vault, mParentLayout, false);

        mVaultLayout = (RelativeLayout) v.findViewById(R.id.generic_vault_relativelayout);

        mHeaderLayout = (RelativeLayout) mVaultLayout.findViewById(R.id.vault_header_layout);
        mContentFrameLayout = (FrameLayout) mVaultLayout.findViewById(R.id.vault_content_layout);
        mFooterLayout = (RelativeLayout) mVaultLayout.findViewById(R.id.vault_footer_layout);

        mHeaderNameTv = (MeemHeaderTextView) mVaultLayout.findViewById(R.id.vault_header_name_text);
        mHeaderTimeStampTv = (MeemHeaderTextView) mVaultLayout.findViewById(R.id.vault_header_timestamp_text);

        mFooterTv = (MeemFooterTextView) mVaultLayout.findViewById(R.id.vault_footer_text);
        mFooterDots = (AnimDotsView) mVaultLayout.findViewById(R.id.vault_footer_dots);

        mPhoneIcon = (ImageView) mVaultLayout.findViewById(R.id.vault_header_phone_icon);
        mWifiIcon= (ImageView) mVaultLayout.findViewById(R.id.wifiIcon);

        createHeaderView();
        createContentView();
        createFooterView();

		/*
         * Install a gesture detected on the header. The tap event and scroll
		 * event (and even swipe event) are to be overridden by derived classes 
		 * like PhoneAvatarUi and Meem (view).
		 */
        final GestureDetectorCompat gd = new GestureDetectorCompat(mUiCtxt.getAppContext(), new MeemGestureDetector(this, mParentLayout, mHeaderLayout));
        mHeaderLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gd.onTouchEvent(event);
            }
        });

        mVaultLayout.setOnDragListener(new View.OnDragListener() {
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

        // One generic vault layout is born in the parent!
        mParentLayout.addView(mVaultLayout);

        return true;
    }

    public void setResources(int color) {
        mColor = color;
        GradientDrawable drawable = (GradientDrawable) mVaultLayout.getBackground();
        drawable.setColor(ContextCompat.getColor(mUiCtxt.getAppContext(), color));
    }

    void createHeaderView() {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) mHeaderLayout.getLayoutParams();
        rlp.height = (int) mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEADER_HEIGHT);
        Log.w(TAG, "Header height: " + rlp.height);
        mHeaderLayout.setLayoutParams(rlp);

        // ----------- arrange phone icon image view, name & timestamp text views -------------------

        rlp = (RelativeLayout.LayoutParams) mPhoneIcon.getLayoutParams();
        int iconMarginHoriz = (int) mUiCtxt.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.VAULT_HEADER_ICON_ALL_MARGIN);
        int iconMarginVert = (int) mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEADER_ICON_ALL_MARGIN);
        rlp.setMargins(iconMarginHoriz, iconMarginVert, iconMarginHoriz, iconMarginVert);
        mPhoneIcon.setLayoutParams(rlp);

        Typeface tf = Typeface.createFromAsset(mUiCtxt.getAppContext().getAssets(), "fonts/GMC-1.ttf");
        mHeaderNameTv.setTypeface(tf);
        mHeaderNameTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f);

        int nameMarginTop = (int) mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_HEADER_NAME_TOP_MARGIN);
        rlp = (RelativeLayout.LayoutParams) mHeaderNameTv.getLayoutParams();
        rlp.setMargins(0, nameMarginTop, 0, 0);
        mHeaderNameTv.setLayoutParams(rlp);

        tf = Typeface.createFromAsset(mUiCtxt.getAppContext().getAssets(), "fonts/GMC-2.ttf");
        mHeaderTimeStampTv.setTypeface(tf);
        mHeaderTimeStampTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);

        // further things to be done by derived classes
        setupHeaderView();
    }

    void createContentView() {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) mContentFrameLayout.getLayoutParams();
        rlp.height = 0;
        mContentFrameLayout.setLayoutParams(rlp);

        // further things to be done by derived classes
        setupContentView();
    }

    void createFooterView() {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) mFooterLayout.getLayoutParams();
        rlp.height = (int) mUiCtxt.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.VAULT_FOOTER_HEIGHT);
        Log.w(TAG, "Footer height: " + rlp.height);
        mFooterLayout.setLayoutParams(rlp);

        // further things to be done by derived classes
        setupFooterView();
    }

    /**
     * This is where we will specialize the view. Must be implemented by derived classes like PhoneAvatarUi, MeemUi etc.
     */
    abstract void setupHeaderView();
    abstract void setupContentView();
    abstract void setContentViewLockState(boolean lockState);
    abstract void setupFooterView();

    public RelativeLayout getView() {
        return mVaultLayout;
    }

    public void animFooterDots(boolean start) {
        if (start) {
            mFooterDots.setVisibility(View.VISIBLE);
            mFooterDots.startAnimation();
        } else {
            mFooterDots.stopAnimation();
            mFooterDots.setVisibility(View.GONE);
        }
    }

    public boolean getLockState() {
        return mLockState;
    }

    public void setLockState(boolean lock) {
        setContentViewLockState(lock);
        mLockState = lock;
    }

    /*
     * GestureDetecterAdapter overrides.
     */
    @Override
    public void onSingleTap(View v, float x, float y) {
        onSingleTapOnHeader();
    }

    @Override
    public void onLongPress(View v, float x, float y) {
        if (mLockState) return;
        onLongPressOnHeader(x, y);
    }

    @Override
    public void onLeftToRightScroll(View v, float x, float y, float distx) {
        if (mLockState) return;
        onDnDDragStart(x, y);
    }

    @Override
    public void onRightToLeftScroll(View v, float x, float y, float distx) {
        if (mLockState) return;
        onDnDDragStart(x, y);
    }

    @Override
    public void onTopToBottomScroll(View v, float x, float y, float disty) {
        if (mLockState) return;
        onDnDDragStart(x, y);
    }

    @Override
    public void onBottomToTopScroll(View v, float x, float y, float disty) {
        if (mLockState) return;
        onDnDDragStart(x, y);
    }

    protected void onLongPressOnHeader(float x, float y) {
        if (mLockState) return;
        onLongPressOnName(x, y);
    }

    /**
     * Must be overridden by derived classes to handle opening/closing of the vault.
     */
    protected void onSingleTapOnHeader() {
    }

    /**
     * Must be overridden by derived classes to handle editing of phone/meem names.
     */
    protected void onLongPressOnName(float x, float y) {
    }

    /**
     * Must be overridden by derived classes to handle drag and drop for backup and restore.
     */
    protected void onScrollOnHeader(float x, float y) {
    }

    /**
     * Must be overridden by derived classes to handle drag and drop for backup and restore.
     */
    protected void onDnDDragStart(float x, float y) {
    }

    /**
     * Must be overridden by derived classes to handle drag and drop for backup and restore.
     */
    protected void onDnDDropEvent(DragEvent event) {
    }
}
