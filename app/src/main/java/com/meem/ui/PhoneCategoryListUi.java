package com.meem.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.AnimDotsView;
import com.meem.ui.utils.MeemTextView;

public class PhoneCategoryListUi extends GenericVaultContentUi {
    @SuppressWarnings("unused")
    private static final String TAG = "PhoneCategoryListUi";
    protected PhoneCategoryListEventListener mListener;
    int mHeight;

    public PhoneCategoryListUi(Context context, FrameLayout viewRoot) {
        super(context, viewRoot);
    }

    public void setEventListener(PhoneCategoryListEventListener listener) {
        mListener = listener;
    }

    public void create() {
        RelativeLayout lvi;

        lvi = createListViewItem(UiContext.getInstance().getAppContext().getResources().getString(R.string.contacts));
        lvi.setTag(MMPConstants.MMP_CATCODE_CONTACT);
        addView(lvi, false);

        lvi = createListViewItem(UiContext.getInstance().getAppContext().getResources().getString(R.string.messages));
        lvi.setTag(MMPConstants.MMP_CATCODE_MESSAGE);
        addView(lvi, false);

        lvi = createListViewItem(UiContext.getInstance().getAppContext().getResources().getString(R.string.calendar));
        lvi.setTag(MMPConstants.MMP_CATCODE_CALENDER);
        addView(lvi, false);

        lvi = createListViewItem(UiContext.getInstance().getAppContext().getResources().getString(R.string.photos));
        lvi.setTag(MMPConstants.MMP_CATCODE_PHOTO);
        addView(lvi, false);

        lvi = createListViewItem(UiContext.getInstance().getAppContext().getResources().getString(R.string.videos));
        lvi.setTag(MMPConstants.MMP_CATCODE_VIDEO);
        addView(lvi, false);

        lvi = createListViewItem(UiContext.getInstance().getAppContext().getResources().getString(R.string.music));
        lvi.setTag(MMPConstants.MMP_CATCODE_MUSIC);
        addView(lvi, false);

        lvi = createListViewItem(UiContext.getInstance().getAppContext().getResources().getString(R.string.documents));
        lvi.setTag(MMPConstants.MMP_CATCODE_DOCUMENTS);
        addView(lvi, false);
    }

    private RelativeLayout createListViewItem(String label) {
        UiContext ctx = UiContext.getInstance();
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) ctx.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.CATEGORY_LIST_HEIGHT));
        RelativeLayout rl = new RelativeLayout(mContext);
        rl.setLayoutParams(flp);

        // using a GradientDrawable for divider (boarder)
        GradientDrawable border = new GradientDrawable();
        int boarderWidth = (int) ctx.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.CATEGORY_LIST_DIVIDER_HEIGHT);
        border.setStroke(boarderWidth, ContextCompat.getColor(mContext, R.color.meemGray3));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            rl.setBackgroundDrawable(border);
        } else {
            rl.setBackground(border);
        }

        MeemTextView tv = new MeemTextView(mContext);

        int width = RelativeLayout.LayoutParams.WRAP_CONTENT;
        int height = (int) ctx.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.CATEGORY_LIST_HEIGHT);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(width, height);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        tv.setLayoutParams(rlp);

        int rightPadding = (int) ctx.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.CATEGORY_LIST_PHONE_SIDE_RIGHT_PADDING);
        tv.setPadding(0, 0, rightPadding, 0);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setText(label);
        tv.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), R.color.meemWhite));

        rl.addView(tv, 0);

        // add the dots to the left
        AnimDotsView adv = new AnimDotsView(mContext);
        rlp = new RelativeLayout.LayoutParams(width, height);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        adv.setLayoutParams(rlp);
        adv.setPadding(rightPadding, 0, 0, 0);
        adv.setGravity(Gravity.CENTER_VERTICAL);

        rl.addView(adv, 1);
        adv.setVisibility(View.GONE);

        mHeight += height;

        return rl;
    }

    /**
     * Listener notifications (here, listener is PhoneAvatarUi)
     */
    @Override
    public void onSingleTap(View v, float x, float y) {
        byte catCode = (Byte) v.getTag();
        mListener.onListItemSingleTap(catCode);
    }

    @Override
    public void onLeftToRightSwipe(View v) {
        byte catCode = (Byte) v.getTag();
        mListener.onListItemRightSwipe(catCode);
    }

    /**
     * ********************************************************
     * <p/>
     * Public interfaces
     * <p/>
     * ********************************************************
     */

    public int getHeight() {
        return mHeight;
    }

    public interface PhoneCategoryListEventListener {
        void onListItemSingleTap(byte cat);

        void onListItemRightSwipe(byte cat);

        void onListItemLeftSwipe(byte cat);
    }
}
