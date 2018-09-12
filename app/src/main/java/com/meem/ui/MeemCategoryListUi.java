package com.meem.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.mml.MMLCategory;
import com.meem.ui.utils.AnimDotsView;
import com.meem.ui.utils.MirroredTextView;
import com.meem.viewmodel.CategoryInfo;

import java.util.HashMap;

/**
 * This class implements the category list gesture handling - specialized for phone side.
 *
 * @author arun
 */

public class MeemCategoryListUi extends GenericVaultContentUi {

    protected MeemCategoryListEventListener mListener;
    FrameLayout mViewRoot;
    int mHeight;

    HashMap<Byte, CategoryInfo> mCatInfoMap;

    public MeemCategoryListUi(Context context, FrameLayout viewRoot) {
        super(context, viewRoot);
        mViewRoot = viewRoot;
    }

    public void setEventListener(MeemCategoryListEventListener listener) {
        mListener = listener;
    }

    public void create(HashMap<Byte, CategoryInfo> catInfoMap) {
        RelativeLayout lvi;
        String catName;
        CategoryInfo.BackupMode mode;

        mCatInfoMap = catInfoMap;

        for (Byte catCode : mCatInfoMap.keySet()) {
            if (catCode == MMPConstants.MMP_CATCODE_PHOTO_CAM ||
                    catCode == MMPConstants.MMP_CATCODE_VIDEO_CAM ||
                    catCode == MMPConstants.MMP_CATCODE_FILE ||
                    catCode == MMPConstants.MMP_CATCODE_DOCUMENTS_SD) {
                // we wont show sdcard items in view.
                // in v1, they wont be there in view model. but in v2, they will be (because there is no real cable model concept in v2)
                continue;
            }

            if (MMLCategory.isGenericCategoryCode(catCode)) {
                catName = MMLCategory.toGenericCatPrettyString(catCode);
            } else {
                catName = MMLCategory.toSmartCatPrettyString(catCode);
            }

            mode = catInfoMap.get(catCode).getmBackupMode();
            boolean isDummy = catInfoMap.get(catCode).ismDummy();

            lvi = createListViewItem(catName, mode, isDummy);
            lvi.setTag(catCode);

            addView(lvi, isDummy);
        }
    }

    private RelativeLayout createListViewItem(String label, CategoryInfo.BackupMode mode, boolean isDummy) {
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

        MirroredTextView tv = new MirroredTextView(mContext);

        int width = RelativeLayout.LayoutParams.WRAP_CONTENT;
        int height = (int) ctx.specToPix(GuiSpec.TYPE_HEIGHT, GuiSpec.CATEGORY_LIST_HEIGHT);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(width, height);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        tv.setLayoutParams(rlp);

        // Remember: This is MirroredTextview - left and right is interchanged in onDraw()
        int rightPadding = (int) ctx.specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.CATEGORY_LIST_MEEM_SIDE_LEFT_PADDING);
        tv.setPadding(rightPadding, 0, 0, 0);
        tv.setGravity(Gravity.CENTER_VERTICAL);

        tv.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), R.color.meemWhite));
        tv.setText(label);

        switch (mode) {
            case DISABLED:
                tv.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), R.color.meemGray4));
                break;
            case PLUS:
                tv.setText(Html.fromHtml(label + "<sup>+</sup>"));
                break;
            default:
                break;
        }

        if(isDummy) {
            tv.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), R.color.meemGray4));
        }

        rl.addView(tv, 0);

        // add the dots to the right
        AnimDotsView adv = new AnimDotsView(mContext);
        rlp = new RelativeLayout.LayoutParams(width, height);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        adv.setLayoutParams(rlp);
        adv.setPadding(0, 0, rightPadding, 0);
        adv.setGravity(Gravity.CENTER_VERTICAL);

        rl.addView(adv, 1);
        adv.setVisibility(View.GONE);

        mHeight += height;

        return rl;
    }

    private void updateCatListItem(RelativeLayout listItemRelativeLayout) {
        byte catCode = (Byte) listItemRelativeLayout.getTag();
        TextView tv = (TextView) listItemRelativeLayout.getChildAt(0);

        String catName;
        if (MMLCategory.isGenericCategoryCode(catCode)) {
            catName = MMLCategory.toGenericCatPrettyString(catCode);
        } else {
            catName = MMLCategory.toSmartCatPrettyString(catCode);
        }

        CategoryInfo.BackupMode mode = mCatInfoMap.get(catCode).getmBackupMode();

        switch (mode) {
            case MIRROR:
                mode = CategoryInfo.BackupMode.PLUS;
                tv.setText(Html.fromHtml(catName + "<sup>+</sup>"));
                break;
            case PLUS:
                mode = CategoryInfo.BackupMode.DISABLED;
                tv.setText(catName);
                tv.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), R.color.meemGray4));
                break;
            case DISABLED:
                mode = CategoryInfo.BackupMode.MIRROR;
                tv.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), R.color.meemWhite));
                break;
        }

        // update view model
        mCatInfoMap.get(catCode).setmBackupMode(mode);

        mListener.onListItemSingleTap(catCode); // unused.
    }

    /**
     * Listener notifications (here, listener is Meem view)
     */
    @Override
    public void onSingleTap(View v, float x, float y) {
        if (!mIsMirror) {
            // taps ignored for non-mirror vaults
            return;
        }

        updateCatListItem((RelativeLayout) v);
    }

    @Override
    public void onRightToLeftSwipe(View v) {
        byte catCode = (Byte) v.getTag();
        mListener.onListItemLeftSwipe(catCode);
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

    /**
     * To handle taps on phone side.
     *
     * @param catCode
     */
    public void onVirtualSingleTap(byte catCode) {
        RelativeLayout catListItemRelLayout = (RelativeLayout) mViewMap.get(new Integer(catCode));
        updateCatListItem(catListItemRelLayout);
    }

    public interface MeemCategoryListEventListener {
        void onListItemSingleTap(byte cat);

        void onListItemRightSwipe(byte cat);

        void onListItemLeftSwipe(byte cat);
    }
}
