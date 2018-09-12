package com.meem.ui;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTextView;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;

import info.hoang8f.android.segmented.SegmentedGroup;

/**
 * Created by Naveen on 8/26/2016.
 * This class is used to display/edit/delete the user data
 */

public class DetailsFragment extends Fragment implements View.OnClickListener, MainActivity.ToolBarItemClickListener {


    public static final String vaultId = "VAULT_ID";
    final String TAG = "DetailFragment";
    int mSelectedCategory = 1;
    int mNumberOfVault;
    boolean mIsEditMode, mIsSaveMode, mIsDefaultMode = true;
    LinearLayout mDeleteVault;
    LinearLayout mContactsLL, mCalenderLL, mPhotosLL, mVideosLL, mDocumentsLL, mMessagesLL, mMusicLL;
    LinearLayout mContactsLLInfo, mCalenderLLInfo, mPhotosLLInfo, mVideosLLInfo, mDocumentsLLInfo, mMessagesLLInfo, mMusicLLInfo;
    LinearLayout mContactsLLInfoBlue, mCalenderLLInfoBlue, mPhotosLLInfoBlue, mVideosLLInfoBlue, mDocumentsLLInfoBlue, mMessagesLLInfoBlue, mMusicLLInfoBlue;
    LinearLayout mTabLL, mLLParentView;
    DebugTracer mDbg = new DebugTracer(TAG, "DetailsFragment.log");
    AppLocalData appLocalData = AppLocalData.getInstance();


    boolean isBackStack = true;

    View rootView;
    View mExpandedView = null;
    MainActivity mMainActivity;

    RadioButton mRadioOFFContacts, mRadioMirrorContacts, mRadioMirrorPlusContacts, mRadioOFFPhotos, mRadioMirrorPhotos, mRadioMirrorPlusPhotos, mRadioOFFVideos, mRadioMirrorVideos, mRadioMirrorPlusVideos, mRadioOFFMusic, mRadioMirrorMusic, mRadioMirrorPlusMusic, mRadioOFFDocuments, mRadioMirrorDocuments, mRadioMirrorPlusDocuments, mRadioOFFMessages, mRadioMirrorMessages, mRadioMirrorPlusMessages;
    RadioButton mRadioOFFCalender, mRadioMirrorCalender, mRadioMirrorPlusCalender;
    RadioButton mRadioSyncOFFContacts, mRadioSyncOFFCalender, mRadioSyncOFFPhotos, mRadioSyncOFFVideos, mRadioSyncOFFMusic, mRadioSyncOFFDocuments, mRadioSyncOFFMessages;

    MeemTextView tvSize;
    int mSelectedTab = 1;
    MeemTextView mProfileOne, mProfileTwo, mProfileThree;

    MeemTextView mContactsTv, mCalenderTV, mPhotosTV, mVideosTV, mDocumentsTV, mMessagesTV, mMusicTV;
    MeemTextView mContactsMirrorSizeTV, mContactsMirrorPlusSizeTV, mCalenderMirrorSizeTV, mCalenderMirrorPlusSizeTV, mPhotosMirrorSizeTV, mPhotosMirrorPlusSizeTV, mVideosMirrorSizeTV, mVideosMirrorPlusSizeTV, mMessagesMirrorSizeTV, mMessagesMirrorPlusSizeTV, mDocumentsMirrorSizeTV, mDocumentsMirrorPlusSizeTV, mMusicMirrorSizeTV, mMusicMirrorPlusSizeTV;
    SegmentedGroup mSegmentedGroupContacts, mSegmentedGroupCalender, mSegmentedGroupPhotos, mSegmentedGroupVideos, mSegmentedGroupDocuments, mSegmentedGroupMessages, mSegmentedGroupmMusic;
    SegmentedGroup mSegmentedGroupContactsBlue, mSegmentedGroupCalenderBlue, mSegmentedGroupPhotosBlue, mSegmentedGroupVideosBlue, mSegmentedGroupDocumentsBlue, mSegmentedGroupMessagesBlue, mSegmentedGroupmMusicBlue;

    ImageView mContactsArrow, mCalenderArrow, mPhotosArrow, mVideosArrow, mDocumentsArrow, mMessagesArrow, mMusicArrow;


    DetailsFragmentInterface mListener;

    View mArrowView, mMirrorDeleteView, mMirrorPlusDeleteView;
    boolean mIsAndroid = true;


    CableInfo mCableInfo;
    ArrayList<String> mUpids = new ArrayList();
    VaultInfo mVaultInfo;
    boolean mIsMirror = false, mHasMirror = false;
    boolean isFromSetting = false;
    long totalSize = 0;
    private long calM, calMP, conM, conMP, phoM, phoMP, vidM, vidMP, musicM, musicMP, smsM, smsMP, docM, docMP;
    public ProgressDialog mInitProgress;
    private int catCode;

    public DetailsFragment() {

    }

    public static void enableDisableView(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int idx = 0; idx < group.getChildCount(); idx++) {
                enableDisableView(group.getChildAt(idx), enabled);
            }
        }
    }

    public static String convertKBtoGB(long size) {


        if (size <= 0) return "--";
        double temp = ((double) size / 1024) / 1024;
        if (temp < .01) return "0.01";

        return new DecimalFormat("#0.00").format(temp).replace(',', '.');
    }


    public static String convertKBtoMB(long size) {
        if (size <= 0) return "--";
        double temp = ((double) size / 1024);
        if (temp < .01) return "0.01";

        return new DecimalFormat("#0.00").format(temp).replace(',', '.');
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDbg.trace();
        isFromSetting = getArguments().getBoolean("FROMSETTING", false);
        rootView = inflater.inflate(R.layout.detailsfrag, container, false);
        init();
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(new Bundle());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;

        // Arun: 18Sept2017: Ideally, onAttach is the place to call methods of MainActivity to init the fragment.
        if (mMainActivity != null && mMainActivity.getCablePresenter() != null && mMainActivity.isDetailsFragmentRefreshPending()) {
            try {
                CableInfo cableInfo = mMainActivity.getCablePresenter().getCableInfo();
                if (cableInfo != null) onNewObject(cableInfo);
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void init() {
        mDbg.trace();
        mInitProgress = CustomDialogProgress.ctor(getContext());
        fetchViewId();
        setSegmentButtons();
        setCategoryNames();
        setClickListener();

        try {
            CableInfo cableInfo = mMainActivity.getCablePresenter().getCableInfo();
            if (cableInfo != null) onNewObject(cableInfo);
        } catch (Exception e) {
            // Ignored
        }
    }

    public void onNewObject(CableInfo cableInfo) {
        mDbg.trace();
        if (cableInfo.getmNumVaults() < mSelectedTab)
            mSelectedTab = 1;//If the cable is reset then this value has to update
        mCableInfo = cableInfo;
        updateNewObject();
        setNoOfTabs();
        updateSelectedTabBackground(mSelectedTab);
        setTabsName();

        setValues(mSelectedTab);

    }

    private void setCatEnableOrDisable() {
        mDbg.trace();

        if (mIsAndroid) {
            mMessagesLL.setClickable(true);
            mMusicLL.setClickable(true);
        } else {
            mMessagesLL.setClickable(false);
            mMusicLL.setClickable(false);
        }
    }

    private void updateNewObject() {
        mDbg.trace();
        fetchVaultIds();
        fetchVaultInfo(mSelectedTab - 1);
    }

    private void fetchVaultInfo(int i) {
        mDbg.trace();
        mVaultInfo = mCableInfo.getmVaultInfoMap().get(mUpids.get(i));
        mIsMirror = mVaultInfo.mIsMirror;
    }

    public void fetchVaultIds() {
        mDbg.trace();

        mUpids.clear();
        String mMirrorUpid = null;
        mHasMirror = false;
        for (String upid : this.mCableInfo.getmVaultInfoMap().keySet()) {
            if (this.mCableInfo.getmVaultInfoMap().get(upid).ismIsMirror()) {
                mUpids.add(upid);
                mMirrorUpid = upid;
                mHasMirror = true;
            }
        }

        for (String upid : this.mCableInfo.getmVaultInfoMap().keySet()) {
            if (!upid.equals(mMirrorUpid)) {
                mUpids.add(upid);
            }
        }
    }

    public void setEventListner(CablePresenter listener) {
        mDbg.trace();

        mListener = listener;
    }

    private void setNoOfTabs() {
        mDbg.trace();

        mNumberOfVault = mCableInfo.getmNumVaults();

        mDbg.trace("No of vaults ::"+mNumberOfVault);



        if (mNumberOfVault == 3) {
            mProfileOne.setVisibility(View.VISIBLE);
            mProfileTwo.setVisibility(View.VISIBLE);
            mProfileThree.setVisibility(View.VISIBLE);
        } else if (mNumberOfVault == 2) {
            mProfileOne.setVisibility(View.VISIBLE);
            mProfileTwo.setVisibility(View.VISIBLE);
            mProfileThree.setVisibility(View.GONE);
        } else if (mNumberOfVault == 1) {
            mProfileOne.setVisibility(View.VISIBLE);
            mProfileTwo.setVisibility(View.GONE);
            mProfileThree.setVisibility(View.GONE);

        }

    }

    private void setCategoryNames() {
        mDbg.trace();
        mContactsTv.setText(getString(R.string.contacts));
        mCalenderTV.setText(getString(R.string.calendar));
        mPhotosTV.setText(getString(R.string.photos));
        mVideosTV.setText(getString(R.string.videos));
        mDocumentsTV.setText(getString(R.string.documents));
        mMessagesTV.setText(getString(R.string.messages));
        mMusicTV.setText(getString(R.string.music));
    }

    private void setRadioButton(VaultInfo vaultInfo) {
        mDbg.trace();


        if (mIsMirror) {
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_CONTACT, mRadioOFFContacts, mRadioMirrorContacts, mRadioMirrorPlusContacts, mContactsTv);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_CALENDER, mRadioOFFCalender, mRadioMirrorCalender, mRadioMirrorPlusCalender, mCalenderTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_PHOTO, mRadioOFFPhotos, mRadioMirrorPhotos, mRadioMirrorPlusPhotos, mPhotosTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_VIDEO, mRadioOFFVideos, mRadioMirrorVideos, mRadioMirrorPlusVideos, mVideosTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_MUSIC, mRadioOFFMusic, mRadioMirrorMusic, mRadioMirrorPlusMusic, mMusicTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_DOCUMENTS, mRadioOFFDocuments, mRadioMirrorDocuments, mRadioMirrorPlusDocuments, mDocumentsTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_MESSAGE, mRadioOFFMessages, mRadioMirrorMessages, mRadioMirrorPlusMessages, mMessagesTV);
        } else {
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_CONTACT, mRadioOFFContacts, mRadioMirrorContacts, mRadioMirrorPlusContacts, mContactsTv);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_CALENDER, mRadioOFFCalender, mRadioMirrorCalender, mRadioMirrorPlusCalender, mCalenderTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_PHOTO, mRadioOFFPhotos, mRadioMirrorPhotos, mRadioMirrorPlusPhotos, mPhotosTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_VIDEO, mRadioOFFVideos, mRadioMirrorVideos, mRadioMirrorPlusVideos, mVideosTV);
            setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_DOCUMENTS, mRadioOFFDocuments, mRadioMirrorDocuments, mRadioMirrorPlusDocuments, mDocumentsTV);

            if (mIsAndroid) {
                setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_MUSIC, mRadioOFFMusic, mRadioMirrorMusic, mRadioMirrorPlusMusic, mMusicTV);
                setIndividualRadioButton(vaultInfo, MMPConstants.MMP_CATCODE_MESSAGE, mRadioOFFMessages, mRadioMirrorMessages, mRadioMirrorPlusMessages, mMessagesTV);
            } else {
                mMusicTV.setTextColor(getResources().getColor(R.color.meemBlack50));
                mMessagesTV.setTextColor(getResources().getColor(R.color.meemBlack50));

            }
        }

    }

    private void setIndividualRadioButton(VaultInfo vaultInfo, byte mmpCatcode, RadioButton mRadioOFF, RadioButton mRadioMirror, RadioButton mRadioMirrorPlus, MeemTextView mCatNameTV) {
        mDbg.trace();
        CategoryInfo.BackupMode mode = vaultInfo.getmCategoryInfoMap().get(mmpCatcode).getmBackupMode();
        setMirrorPlusText(false, mmpCatcode);
        mCatNameTV.setTextColor(getResources().getColor(R.color.meemWhite));

        if (mode == CategoryInfo.BackupMode.PLUS) {
            mRadioMirrorPlus.setChecked(true);
            setMirrorPlusText(true, mmpCatcode);
        } else if (mode == CategoryInfo.BackupMode.MIRROR) {
            mRadioMirror.setChecked(true);
        } else {
            mRadioOFF.setChecked(true);
            mCatNameTV.setTextColor(getResources().getColor(R.color.meemBlack50));
        }
    }

    private void setMirrorPlusText(boolean b, byte catCode) {
        mDbg.trace();

        switch (catCode) {
            case MMPConstants.MMP_CATCODE_CONTACT:
                if (b)
                    mContactsTv.setText(Html.fromHtml(getString(R.string.contacts) + "<sup>+</sup>"));
                else mContactsTv.setText(R.string.contacts);
                break;
            case MMPConstants.MMP_CATCODE_CALENDER:
                if (b)
                    mCalenderTV.setText(Html.fromHtml(getString(R.string.calendar) + "<sup>+</sup>"));
                else mCalenderTV.setText(R.string.calendar);
                break;
            case MMPConstants.MMP_CATCODE_PHOTO:
                if (b)
                    mPhotosTV.setText(Html.fromHtml(getString(R.string.photos) + "<sup>+</sup>"));
                else mPhotosTV.setText(R.string.photos);
                break;
            case MMPConstants.MMP_CATCODE_VIDEO:
                if (b)
                    mVideosTV.setText(Html.fromHtml(getString(R.string.videos) + "<sup>+</sup>"));
                else mVideosTV.setText(R.string.videos);
                break;
            case MMPConstants.MMP_CATCODE_MUSIC:
                if (b) mMusicTV.setText(Html.fromHtml(getString(R.string.music) + "<sup>+</sup>"));
                else mMusicTV.setText(R.string.music);
                break;
            case MMPConstants.MMP_CATCODE_DOCUMENTS:
                if (b)
                    mDocumentsTV.setText(Html.fromHtml(getString(R.string.documents) + "<sup>+</sup>"));
                else mDocumentsTV.setText(R.string.documents);
                break;
            case MMPConstants.MMP_CATCODE_MESSAGE:
                if (b)
                    mMessagesTV.setText(Html.fromHtml(getString(R.string.messages) + "<sup>+</sup>"));
                else mMessagesTV.setText(R.string.messages);
                break;
        }


    }

    private void setValues(int position) {
        mDbg.trace();

        fetchVaultInfo(position - 1);


        mIsAndroid = mVaultInfo.mPlatform.equals("Android");
        setCatEnableOrDisable();


        totalSize = mVaultInfo.getmMirrorSizeKB() + mVaultInfo.getmPlusSizeKB();
        if (totalSize < 100000) {
            tvSize.setText(getString(R.string.total_size) + " " + convertKBtoMB(totalSize) + "MB");
        } else {
            tvSize.setText(getString(R.string.total_size) + " " + convertKBtoGB(totalSize) + "GB");
        }
        setMirrorAndMirrorPlusSize(mVaultInfo);
        setRadioButton(mVaultInfo);
    }

    private void setMirrorAndMirrorPlusSize(VaultInfo vaultInfo) {
        mDbg.trace();


        if (mMainActivity.getCableVersion() == 1) {
            calM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CALENDER).getmMirrorSizeMB();
            calMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CALENDER).getmPlusSizeMB();
            conM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CONTACT).getmMirrorSizeMB();
            conMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CONTACT).getmPlusSizeMB();
            if (mIsAndroid) {
                smsM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MESSAGE).getmMirrorSizeMB();
                smsMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MESSAGE).getmPlusSizeMB();
            } else {
                smsM = 0;
                smsMP = 0;
            }
        } else {

            if (checkIsDBHasRows(appLocalData.getCalendarV2MirrorDbFullPath(mVaultInfo.getmUpid()), "calendar_event_table ")) {
                calM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CALENDER).getmMirrorSizeMB();
            } else {
                calM = 0;
            }


            if (checkIsDBHasRows(appLocalData.getCalendarV2PlusDbFullPath(mVaultInfo.getmUpid()), "calendar_event_table ")) {
                calMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CALENDER).getmPlusSizeMB();
            } else {
                calMP = 0;
            }


            if (checkIsDBHasRows(appLocalData.getContactsV2MirrorDbFullPath(mVaultInfo.getmUpid()), "contacts_table")) {
                conM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CONTACT).getmMirrorSizeMB();
            } else {
                conM = 0;
            }


            if (checkIsDBHasRows(appLocalData.getContactsV2PlusDbFullPath(mVaultInfo.getmUpid()), "contacts_table")) {
                conMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CONTACT).getmPlusSizeMB();
            } else {
                conMP = 0;
            }


            if (mIsAndroid) {

                if (checkIsDBHasRows(appLocalData.getMessageV2MirrorDbFullPath(mVaultInfo.getmUpid()), "sms_msg_table")) {
                    smsM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MESSAGE).getmMirrorSizeMB();
                } else {
                    smsM = 0;
                }

                if (checkIsDBHasRows(appLocalData.getMessageV2PlusDbFullPath(mVaultInfo.getmUpid()), "sms_msg_table")) {
                    smsMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MESSAGE).getmPlusSizeMB();
                } else {
                    smsMP = 0;
                }
            } else {
                smsM = 0;
                smsMP = 0;
            }

        }


        phoM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO).getmMirrorSizeMB();
        if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO_CAM) != null)
            phoM += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO_CAM).getmMirrorSizeMB();
        phoMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO).getmPlusSizeMB();
        if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO_CAM) != null)
            phoMP += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO_CAM).getmPlusSizeMB();

        vidM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO).getmMirrorSizeMB();
        if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO_CAM) != null)
            vidM += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO_CAM).getmMirrorSizeMB();
        vidMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO).getmPlusSizeMB();
        if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO_CAM) != null)
            vidMP += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO_CAM).getmPlusSizeMB();


        if (mIsAndroid) {

            musicM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MUSIC).getmMirrorSizeMB();
            if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_FILE) != null)
                musicM += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_FILE).getmMirrorSizeMB();
            musicMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MUSIC).getmPlusSizeMB();
            if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_FILE) != null)
                musicMP += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_FILE).getmPlusSizeMB();
        } else {
            musicM = 0;
            musicMP = 0;
        }


        docM = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS).getmMirrorSizeMB();
        if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS_SD) != null)
            docM += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS_SD).getmMirrorSizeMB();
        docMP = vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS).getmPlusSizeMB();
//        BugId:258 , Had problem in assigning the values,
        if (vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS_SD) != null)
            docMP += vaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS_SD).getmPlusSizeMB();

        if (totalSize < 100000) {
            mCalenderMirrorSizeTV.setText("" + (convertKBtoMB(calM)));
            mCalenderMirrorPlusSizeTV.setText("" + (convertKBtoMB(calMP)));
            mContactsMirrorSizeTV.setText("" + (convertKBtoMB(conM)));
            mContactsMirrorPlusSizeTV.setText("" + (convertKBtoMB(conMP)));
            mPhotosMirrorSizeTV.setText("" + (convertKBtoMB(phoM)));
            mPhotosMirrorPlusSizeTV.setText("" + (convertKBtoMB(phoMP)));
            mVideosMirrorSizeTV.setText("" + (convertKBtoMB(vidM)));
            mVideosMirrorPlusSizeTV.setText("" + (convertKBtoMB(vidMP)));
            mMusicMirrorSizeTV.setText("" + (convertKBtoMB(musicM)));
            mMusicMirrorPlusSizeTV.setText("" + (convertKBtoMB(musicMP)));
            mDocumentsMirrorSizeTV.setText("" + (convertKBtoMB(docM)));
            mDocumentsMirrorPlusSizeTV.setText("" + (convertKBtoMB(docMP)));
            mMessagesMirrorSizeTV.setText("" + (convertKBtoMB(smsM)));
            mMessagesMirrorPlusSizeTV.setText("" + (convertKBtoMB(smsMP)));
        } else {

            mCalenderMirrorSizeTV.setText("" + (convertKBtoGB(calM)));
            mCalenderMirrorPlusSizeTV.setText("" + (convertKBtoGB(calMP)));
            mContactsMirrorSizeTV.setText("" + (convertKBtoGB(conM)));
            mContactsMirrorPlusSizeTV.setText("" + (convertKBtoGB(conMP)));
            mPhotosMirrorSizeTV.setText("" + (convertKBtoGB(phoM)));
            mPhotosMirrorPlusSizeTV.setText("" + (convertKBtoGB(phoMP)));
            mVideosMirrorSizeTV.setText("" + (convertKBtoGB(vidM)));
            mVideosMirrorPlusSizeTV.setText("" + (convertKBtoGB(vidMP)));
            mMusicMirrorSizeTV.setText("" + (convertKBtoGB(musicM)));
            mMusicMirrorPlusSizeTV.setText("" + (convertKBtoGB(musicMP)));
            mDocumentsMirrorSizeTV.setText("" + (convertKBtoGB(docM)));
            mDocumentsMirrorPlusSizeTV.setText("" + (convertKBtoGB(docMP)));
            mMessagesMirrorSizeTV.setText("" + (convertKBtoGB(smsM)));
            mMessagesMirrorPlusSizeTV.setText("" + (convertKBtoGB(smsMP)));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbg.trace();


    }

    @Override
    public void onResume() {
        super.onResume();
        mDbg.trace();
        mMainActivity.setEventListner(this);
        mMainActivity.setAppTitle(getString(R.string.summary));
        mMainActivity.setOptionMenuContent(false, false, false, false);
        setEventListner(mMainActivity.getCablePresenter());
        if (isFromSetting) {
            mDeleteVault.setVisibility(View.VISIBLE);
            mMainActivity.setTextViewVisibility(false);

        } else {
            setArrowVisibility(true);

            mDeleteVault.setVisibility(View.GONE);
            if (mHasMirror && mSelectedTab == 1) mMainActivity.setTextViewVisibility(true);
            else mMainActivity.setTextViewVisibility(false);

        }

    }

    private void setArrowVisibility(boolean b) {
        mDbg.trace();

        mCalenderArrow.setVisibility(View.VISIBLE);
        mContactsArrow.setVisibility(View.VISIBLE);
        mPhotosArrow.setVisibility(View.VISIBLE);
        mVideosArrow.setVisibility(View.VISIBLE);
        mMessagesArrow.setVisibility(View.VISIBLE);
        mDocumentsArrow.setVisibility(View.VISIBLE);
        mMusicArrow.setVisibility(View.VISIBLE);


    }

    @Override
    public void onPause() {
        super.onPause();
        mDbg.trace();
        mMainActivity.setTextViewVisibility(false);


    }

    private void setClickListener() {
        mDbg.trace();


        mProfileOne.setOnClickListener(this);
        mProfileTwo.setOnClickListener(this);
        mProfileThree.setOnClickListener(this);

        mDeleteVault.setOnClickListener(this);

        mContactsLL.setOnClickListener(this);
        mCalenderLL.setOnClickListener(this);
        mPhotosLL.setOnClickListener(this);
        mVideosLL.setOnClickListener(this);
        mDocumentsLL.setOnClickListener(this);
        mMessagesLL.setOnClickListener(this);
        mMusicLL.setOnClickListener(this);


        mRadioOFFContacts.setOnClickListener(this);
        mRadioMirrorContacts.setOnClickListener(this);
        mRadioMirrorPlusContacts.setOnClickListener(this);

        mRadioOFFCalender.setOnClickListener(this);
        mRadioMirrorCalender.setOnClickListener(this);
        mRadioMirrorPlusCalender.setOnClickListener(this);

        mRadioOFFPhotos.setOnClickListener(this);
        mRadioMirrorPhotos.setOnClickListener(this);
        mRadioMirrorPlusPhotos.setOnClickListener(this);

        mRadioOFFVideos.setOnClickListener(this);
        mRadioMirrorVideos.setOnClickListener(this);
        mRadioMirrorPlusVideos.setOnClickListener(this);

        mRadioOFFMusic.setOnClickListener(this);
        mRadioMirrorMusic.setOnClickListener(this);
        mRadioMirrorPlusMusic.setOnClickListener(this);

        mRadioOFFDocuments.setOnClickListener(this);
        mRadioMirrorDocuments.setOnClickListener(this);
        mRadioMirrorPlusDocuments.setOnClickListener(this);

        mRadioOFFMessages.setOnClickListener(this);
        mRadioMirrorMessages.setOnClickListener(this);
        mRadioMirrorPlusMessages.setOnClickListener(this);


    }

    private void closeSettingLayout() {
        mDbg.trace();

        if (mExpandedView != null) expandOrCollapseWithAnimation(null, mExpandedView);
    }

    private void setTabsName() {
        mDbg.trace();

        mMainActivity.setTextFont(mProfileOne);
        mMainActivity.setTextFont(mProfileTwo);
        mMainActivity.setTextFont(mProfileThree);


        mProfileOne.setText(mCableInfo.getmVaultInfoMap().get(mUpids.get(0)).mName);
        if (mCableInfo.getmNumVaults() > 1) {
            mProfileTwo.setText(mCableInfo.getmVaultInfoMap().get(mUpids.get(1)).mName);
            if (mCableInfo.getmNumVaults() > 2)
                mProfileThree.setText(mCableInfo.getmVaultInfoMap().get(mUpids.get(2)).mName);
        }
    }

    private void onSaveSettings() {
        mDbg.trace();

        sendVaultObject();


    }

    private void sendVaultObject() {
        mDbg.trace();
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onUpdateVault(mVaultInfo.getmUpid(), mVaultInfo, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                mInitProgress.dismiss();
                if (result) {
//                    BugId:47, Change the string
                    Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
                    updateAfterSave();
                    onNewObject((CableInfo) info);
                    mMainActivity.updateHomeFragmentOnCableUpdate();
                } else {
                    mMainActivity.setToolbarText(getResources().getString(R.string.cancel));
                    onNewObject((CableInfo) info);
                    Toast.makeText(getActivity(),R.string.failed, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }


    @Override
    public void onClick(View v) {
        mDbg.trace();
        findClickedView(v);
    }

    private void findClickedView(View view) {
        mDbg.trace();
        View footerView = null;

        if (isFromSetting) {

            switch (view.getId()) {
                case R.id.profileOneNew:
                    mSelectedTab = 1;
                    setValues(mSelectedTab);
                    updateSelectedTabBackground(mSelectedTab);
                    break;
                case R.id.profileTwoNew:
                    mSelectedTab = 2;
                    setValues(mSelectedTab);
                    updateSelectedTabBackground(mSelectedTab);
                    break;
                case R.id.profileThreeNew:
                    mSelectedTab = 3;
                    setValues(mSelectedTab);
                    updateSelectedTabBackground(mSelectedTab);
                    break;
                case R.id.deleteVault:
                    showDialogBox(view, 0, false);
                    break;
            }

        } else {


            switch (view.getId()) {
                case R.id.llContacts:
                    catCode = MMPConstants.MMP_CATCODE_CONTACT;
                    mSelectedCategory = 1;
                    if (!mIsDefaultMode) {

                        if (mIsMirror) {
                            footerView = mContactsLLInfo;
                            mArrowView = mContactsArrow;
                            expandOrCollapseView(view, footerView);
                            setIndividualRadioButton(mVaultInfo, MMPConstants.MMP_CATCODE_CONTACT, mRadioOFFContacts, mRadioMirrorContacts, mRadioMirrorPlusContacts, mContactsTv);
                        } else {

                            showDeleteIcons();
                        }


                    } else onRequestDBDownload(1);
                    break;

                case R.id.llMessages:
                    mSelectedCategory = 2;
                    catCode = MMPConstants.MMP_CATCODE_MESSAGE;

                    if (!mIsDefaultMode) {


                        if (mIsMirror) {
                            footerView = mMessagesLLInfo;
                            mArrowView = mMessagesArrow;
                            expandOrCollapseView(view, footerView);
                            setIndividualRadioButton(mVaultInfo, MMPConstants.MMP_CATCODE_MESSAGE, mRadioOFFMessages, mRadioMirrorMessages, mRadioMirrorPlusMessages, mMessagesTV);

                        } else showDeleteIcons();

                    } else onRequestDBDownload(2);
                    break;

                case R.id.llCalender:
                    catCode = MMPConstants.MMP_CATCODE_CALENDER;
                    mSelectedCategory = 3;
                    if (!mIsDefaultMode) {

                        if (mIsMirror) {
                            footerView = mCalenderLLInfo;

                            mArrowView = mCalenderArrow;
                            expandOrCollapseView(view, footerView);
                            setIndividualRadioButton(mVaultInfo, MMPConstants.MMP_CATCODE_CALENDER, mRadioOFFCalender, mRadioMirrorCalender, mRadioMirrorPlusCalender, mCalenderTV);

                        } else showDeleteIcons();
                    } else onRequestDBDownload(3);
                    break;

                case R.id.llPhotos:
                    catCode = MMPConstants.MMP_CATCODE_PHOTO;
                    mSelectedCategory = 4;
                    if (!mIsDefaultMode) {


                        if (mIsMirror) {
                            footerView = mPhotosLLInfo;
                            mArrowView = mPhotosArrow;

                            expandOrCollapseView(view, footerView);
                            setIndividualRadioButton(mVaultInfo, MMPConstants.MMP_CATCODE_PHOTO, mRadioOFFPhotos, mRadioMirrorPhotos, mRadioMirrorPlusPhotos, mPhotosTV);

                        } else showDeleteIcons();

                    } else onRequestDBDownload(4);
                    break;

                case R.id.llVideos:
                    catCode = MMPConstants.MMP_CATCODE_VIDEO;
                    mSelectedCategory = 5;
                    if (!mIsDefaultMode) {

                        if (mIsMirror) {
                            footerView = mVideosLLInfo;
                            mArrowView = mVideosArrow;
                            expandOrCollapseView(view, footerView);
                            setIndividualRadioButton(mVaultInfo, MMPConstants.MMP_CATCODE_VIDEO, mRadioOFFVideos, mRadioMirrorVideos, mRadioMirrorPlusVideos, mVideosTV);

                        } else showDeleteIcons();
                    } else onRequestDBDownload(5);
                    break;
                case R.id.llMusic:
                    mSelectedCategory = 6;
                    catCode = MMPConstants.MMP_CATCODE_MUSIC;

                    if (!mIsDefaultMode) {


                        if (mIsMirror) {
                            footerView = mMusicLLInfo;
                            mArrowView = mMusicArrow;
                            expandOrCollapseView(view, footerView);
                            setIndividualRadioButton(mVaultInfo, MMPConstants.MMP_CATCODE_MUSIC, mRadioOFFMusic, mRadioMirrorMusic, mRadioMirrorPlusMusic, mMusicTV);

                        } else showDeleteIcons();

                    } else onRequestDBDownload(6);
                    break;

                case R.id.llDocuments:
                    catCode = MMPConstants.MMP_CATCODE_DOCUMENTS;
                    mSelectedCategory = 7;
                    if (!mIsDefaultMode) {

                        if (mIsMirror) {
                            footerView = mDocumentsLLInfo;
                            mArrowView = mDocumentsArrow;
                            expandOrCollapseView(view, footerView);
                            setIndividualRadioButton(mVaultInfo, MMPConstants.MMP_CATCODE_DOCUMENTS, mRadioOFFDocuments, mRadioMirrorDocuments, mRadioMirrorPlusDocuments, mDocumentsTV);

                        } else showDeleteIcons();

                    } else onRequestDBDownload(7);
                    break;


                case R.id.profileOneNew:
                    mSelectedTab = 1;
                    setValues(mSelectedTab);
                    updateSelectedTabBackground(mSelectedTab);
                    break;
                case R.id.profileTwoNew:
                    mSelectedTab = 2;
                    setValues(mSelectedTab);
                    updateSelectedTabBackground(mSelectedTab);
                    break;
                case R.id.profileThreeNew:
                    mSelectedTab = 3;
                    setValues(mSelectedTab);
                    updateSelectedTabBackground(mSelectedTab);
                    break;
                case R.id.deleteVault:
                    showDialogBox(view, 0, false);
                    break;

            }

            if (view == mRadioOFFCalender) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_CALENDER);
                mCalenderTV.setTextColor(getResources().getColor(R.color.meemBlack50));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CALENDER).setmBackupMode(CategoryInfo.BackupMode.DISABLED);

            } else if (view == mRadioMirrorCalender) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_CALENDER);
                mCalenderTV.setTextColor(getResources().getColor(R.color.meemWhite));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CALENDER).setmBackupMode(CategoryInfo.BackupMode.MIRROR);

            } else if (view == mRadioMirrorPlusCalender) {
                mCalenderTV.setTextColor(getResources().getColor(R.color.meemWhite));
                setMirrorPlusText(true, MMPConstants.MMP_CATCODE_CALENDER);
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CALENDER).setmBackupMode(CategoryInfo.BackupMode.PLUS);

            } else if (view == mRadioOFFContacts) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_CONTACT);
                mContactsTv.setTextColor(getResources().getColor(R.color.meemBlack50));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CONTACT).setmBackupMode(CategoryInfo.BackupMode.DISABLED);

            } else if (view == mRadioMirrorContacts) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_CONTACT);
                mContactsTv.setTextColor(getResources().getColor(R.color.meemWhite));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CONTACT).setmBackupMode(CategoryInfo.BackupMode.MIRROR);

            } else if (view == mRadioMirrorPlusContacts) {
                mContactsTv.setTextColor(getResources().getColor(R.color.meemWhite));
                setMirrorPlusText(true, MMPConstants.MMP_CATCODE_CONTACT);
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_CONTACT).setmBackupMode(CategoryInfo.BackupMode.PLUS);
            } else if (view == mRadioOFFMessages) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_MESSAGE);
                mMessagesTV.setTextColor(getResources().getColor(R.color.meemBlack50));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MESSAGE).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
            } else if (view == mRadioMirrorMessages) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_MESSAGE);
                mMessagesTV.setTextColor(getResources().getColor(R.color.meemWhite));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MESSAGE).setmBackupMode(CategoryInfo.BackupMode.MIRROR);
            } else if (view == mRadioMirrorPlusMessages) {
                mMessagesTV.setTextColor(getResources().getColor(R.color.meemWhite));
                setMirrorPlusText(true, MMPConstants.MMP_CATCODE_MESSAGE);
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MESSAGE).setmBackupMode(CategoryInfo.BackupMode.PLUS);
            } else if (view == mRadioOFFMusic) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_MUSIC);
                mMusicTV.setTextColor(getResources().getColor(R.color.meemBlack50));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MUSIC).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
            } else if (view == mRadioMirrorMusic) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_MUSIC);
                mMusicTV.setTextColor(getResources().getColor(R.color.meemWhite));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MUSIC).setmBackupMode(CategoryInfo.BackupMode.MIRROR);
            } else if (view == mRadioMirrorPlusMusic) {
                mMusicTV.setTextColor(getResources().getColor(R.color.meemWhite));
                setMirrorPlusText(true, MMPConstants.MMP_CATCODE_MUSIC);
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_MUSIC).setmBackupMode(CategoryInfo.BackupMode.PLUS);
            } else if (view == mRadioOFFVideos) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_VIDEO);
                mVideosTV.setTextColor(getResources().getColor(R.color.meemBlack50));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
            } else if (view == mRadioMirrorVideos) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_VIDEO);
                mVideosTV.setTextColor(getResources().getColor(R.color.meemWhite));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO).setmBackupMode(CategoryInfo.BackupMode.MIRROR);
            } else if (view == mRadioMirrorPlusVideos) {
                mVideosTV.setTextColor(getResources().getColor(R.color.meemWhite));
                setMirrorPlusText(true, MMPConstants.MMP_CATCODE_VIDEO);
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_VIDEO).setmBackupMode(CategoryInfo.BackupMode.PLUS);
            } else if (view == mRadioOFFPhotos) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_PHOTO);
                mPhotosTV.setTextColor(getResources().getColor(R.color.meemBlack50));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
            } else if (view == mRadioMirrorPhotos) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_PHOTO);
                mPhotosTV.setTextColor(getResources().getColor(R.color.meemWhite));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO).setmBackupMode(CategoryInfo.BackupMode.MIRROR);
            } else if (view == mRadioMirrorPlusPhotos) {
                mPhotosTV.setTextColor(getResources().getColor(R.color.meemWhite));
                setMirrorPlusText(true, MMPConstants.MMP_CATCODE_PHOTO);
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_PHOTO).setmBackupMode(CategoryInfo.BackupMode.PLUS);
            } else if (view == mRadioOFFDocuments) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_DOCUMENTS);
                mDocumentsTV.setTextColor(getResources().getColor(R.color.meemBlack50));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
            } else if (view == mRadioMirrorDocuments) {
                setMirrorPlusText(false, MMPConstants.MMP_CATCODE_DOCUMENTS);
                mDocumentsTV.setTextColor(getResources().getColor(R.color.meemWhite));
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS).setmBackupMode(CategoryInfo.BackupMode.MIRROR);
            } else if (view == mRadioMirrorPlusDocuments) {
                mDocumentsTV.setTextColor(getResources().getColor(R.color.meemWhite));
                setMirrorPlusText(true, MMPConstants.MMP_CATCODE_DOCUMENTS);
                mVaultInfo.getmCategoryInfoMap().get(MMPConstants.MMP_CATCODE_DOCUMENTS).setmBackupMode(CategoryInfo.BackupMode.PLUS);
            }

        }

    }

    private void showDialogBox(final View view, final int catcode, final boolean b) {
        mDbg.trace();


        final AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder(getActivity());
        alertdialogbuilder.setMessage(getString(R.string.delete_vault_warning));
        alertdialogbuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (view.getId() == R.id.deleteVault) {
                    deleteVault();

                } else {
                    //Category delete removed
                }

            }
        });

        alertdialogbuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


            }
        });
        AlertDialog alertDialog = alertdialogbuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private void setSegmentButtons() {
        mDbg.trace();

        mSegmentedGroupContacts.setTintColor(Color.parseColor("#ff99cc00"), Color.parseColor("#000000"));
        mSegmentedGroupCalender.setTintColor(Color.parseColor("#ff99cc00"), Color.parseColor("#000000"));
        mSegmentedGroupPhotos.setTintColor(Color.parseColor("#ff99cc00"), Color.parseColor("#000000"));
        mSegmentedGroupVideos.setTintColor(Color.parseColor("#ff99cc00"), Color.parseColor("#000000"));
        mSegmentedGroupDocuments.setTintColor(Color.parseColor("#ff99cc00"), Color.parseColor("#000000"));
        mSegmentedGroupMessages.setTintColor(Color.parseColor("#ff99cc00"), Color.parseColor("#000000"));
        mSegmentedGroupmMusic.setTintColor(Color.parseColor("#ff99cc00"), Color.parseColor("#000000"));


    }

    private void updateSelectedTabBackground(int profileNo) {
        mDbg.trace();

        mSelectedTab = profileNo;

        if (isFromSetting) {
            mMainActivity.setTextViewVisibility(false);
        } else {
            if (mHasMirror && profileNo == 1) mMainActivity.setTextViewVisibility(true);
            else mMainActivity.setTextViewVisibility(false);
        }

        if (profileNo == 1) {
            if (mHasMirror) {
                mProfileOne.setBackgroundResource(R.drawable.rounded_rect_green_new);
            } else {
                mProfileOne.setBackgroundResource(R.drawable.rounded_rect_blue);
            }
            mProfileTwo.setBackgroundResource(R.drawable.blueborder);
            mProfileThree.setBackgroundResource(R.drawable.blueborder);

            mProfileOne.setTextColor(getResources().getColor(R.color.meemBlack));
            mProfileTwo.setTextColor(getResources().getColor(R.color.meemWhite));
            mProfileThree.setTextColor(getResources().getColor(R.color.meemWhite));


        } else if (profileNo == 2) {
            if (mHasMirror) {
                mProfileOne.setBackgroundResource(R.drawable.greenborder);
            } else {
                mProfileOne.setBackgroundResource(R.drawable.blueborder);
            }

            mProfileTwo.setBackgroundResource(R.drawable.rounded_rect_blue);
            mProfileThree.setBackgroundResource(R.drawable.blueborder);

            mProfileOne.setTextColor(getResources().getColor(R.color.meemWhite));
            mProfileTwo.setTextColor(getResources().getColor(R.color.meemBlack));
            mProfileThree.setTextColor(getResources().getColor(R.color.meemWhite));
        } else {
            if (mHasMirror) {
                mProfileOne.setBackgroundResource(R.drawable.greenborder);
            } else {
                mProfileOne.setBackgroundResource(R.drawable.blueborder);
            }
            mProfileTwo.setBackgroundResource(R.drawable.blueborder);
            mProfileThree.setBackgroundResource(R.drawable.rounded_rect_blue);

            mProfileOne.setTextColor(getResources().getColor(R.color.meemWhite));
            mProfileTwo.setTextColor(getResources().getColor(R.color.meemWhite));
            mProfileThree.setTextColor(getResources().getColor(R.color.meemBlack));
        }
    }

    private void expandOrCollapseView(View headerView, final View viewInfo) {
        mDbg.trace();

        if (mExpandedView != null) expandOrCollapseWithAnimation(headerView, mExpandedView);

        expandOrCollapseWithAnimation(headerView, viewInfo);


    }

    public void expandOrCollapseWithAnimation(View headerView, final View viewInfo) {
        mDbg.trace();


        if (viewInfo.getHeight() <= 0) {
            Log.d("Expand/Collapse", "Expand");
            viewInfo.setVisibility(View.VISIBLE);
            mExpandedView = viewInfo;
            showDeleteIconAndRotateArrow();
            ValueAnimator animator = ValueAnimator.ofInt(viewInfo.getMeasuredHeightAndState(), mContactsLL.getMeasuredHeight());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int val = (int) animation.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = viewInfo.getLayoutParams();
                    layoutParams.height = val;
                    viewInfo.setLayoutParams(layoutParams);
                }
            });
            animator.start();
        } else {
            mExpandedView = null;
            Log.d("Expand/Collapse", "Collapse");
            hideDeleteIcons(true);
            rotateArrowImage(90);

            ValueAnimator animator = ValueAnimator.ofInt(viewInfo.getMeasuredHeightAndState(), 0);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int val = (int) animation.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = viewInfo.getLayoutParams();
                    layoutParams.height = val;
                    viewInfo.setLayoutParams(layoutParams);
                }
            });
            animator.start();
        }

    }

    private void showDeleteIconAndRotateArrow() {
        mDbg.trace();

        mArrowView.setRotation(-90);
        mArrowView = null;
        showDeleteIcons();

    }

    public void showDeleteIcons() {
        mDbg.trace();


        //Disabling the category delete option
        if (true) return;

        hideDeleteIcons(true);

        boolean mMirrorDelete = false, mMirrorPlusDelete = false;
        switch (mSelectedCategory) {
            case 1:
                if (conM > 0) mMirrorDelete = true;
                if (conMP > 0) mMirrorPlusDelete = true;
                break;
            case 2:
                if (calM > 0) mMirrorDelete = true;
                if (calMP > 0) mMirrorPlusDelete = true;
                break;
            case 3:
                if (phoM > 0) mMirrorDelete = true;
                if (phoMP > 0) mMirrorPlusDelete = true;
                break;
            case 4:
                if (vidM > 0) mMirrorDelete = true;
                if (vidMP > 0) mMirrorPlusDelete = true;
                break;
            case 5:
                if (musicM > 0) mMirrorDelete = true;
                if (musicMP > 0) mMirrorPlusDelete = true;
                break;
            case 6:
                if (docM > 0) mMirrorDelete = true;
                if (docMP > 0) mMirrorPlusDelete = true;
                break;
            case 7:
                if (smsM > 0) mMirrorDelete = true;
                if (smsMP > 0) mMirrorPlusDelete = true;
                break;
        }

        if (mMirrorDelete) mMirrorDeleteView.setVisibility(View.VISIBLE);
        mMirrorDeleteView = null;
        if (mMirrorPlusDelete) mMirrorPlusDeleteView.setVisibility(View.VISIBLE);
        mMirrorPlusDeleteView = null;

    }

    private void hideOtherTabs() {
        mDbg.trace();


        if (mSelectedTab == 3) {
            mProfileOne.setVisibility(View.GONE);
            mProfileTwo.setVisibility(View.GONE);
        } else if (mSelectedTab == 2) {
            mProfileOne.setVisibility(View.GONE);
            mProfileThree.setVisibility(View.GONE);
        } else {
            mProfileTwo.setVisibility(View.GONE);
            mProfileThree.setVisibility(View.GONE);

        }
    }

    private void deleteVault() {
        mDbg.trace();
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onDeleteVault(mVaultInfo.getmUpid(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                mDbg.trace("Vault delete sucess");
                mInitProgress.dismiss();
                if (result) {

                    if (mMainActivity.getCableVersion() == 1) {
                        mSelectedTab = 1;
                        onNewObject((CableInfo) info);
                        mMainActivity.updateHomeFragmentOnCableUpdate();
                    }
                } else {
//                    mMainActivity.showToast(getString(R.string.failed));
                }
                return false;
            }
        });
    }

    private void showDeleteVaultButton(boolean b) {
        mDbg.trace();

        if (b) {
            mDeleteVault.setVisibility(View.VISIBLE);
        } else mDeleteVault.setVisibility(View.GONE);
    }

    private void hideDeleteIcons(boolean b) {
        mDbg.trace();

    }


    private void ShowCatDetailPage(int i) {
        mDbg.trace();


        Bundle bundle = new Bundle();
        bundle.putString(vaultId, mVaultInfo.getmUpid());

        AppLocalData appData = AppLocalData.getInstance();

        if (i == 1) {
            CategoryDetailInfoContacts categoryDetailInfoContacts = new CategoryDetailInfoContacts();

            if (mMainActivity.getCableVersion() == 1) {
                bundle.putString("DBPATH", appData.getContactDbFullPath());
            } else {
                bundle.putString("DBPATH", appData.getDatabaseFolderPath());
            }
            categoryDetailInfoContacts.setArguments(bundle);
            mMainActivity.showFragment(categoryDetailInfoContacts, isBackStack, false);
        } else if (i == 2) {
            CategoryDetailInfoMessages categoryDetailInfoMessages = new CategoryDetailInfoMessages();

            if (mMainActivity.getCableVersion() == 1) {
                bundle.putString("DBPATH", appData.getMessageDbFullPath());
            } else {
                bundle.putString("DBPATH", appData.getDatabaseFolderPath());
            }
            categoryDetailInfoMessages.setArguments(bundle);
            mMainActivity.showFragment(categoryDetailInfoMessages, isBackStack, false);
        } else if (i == 3) {
            CategoryDetailInfoCalender categoryDetailInfoCalender = new CategoryDetailInfoCalender();

            if (mMainActivity.getCableVersion() == 1) {
                bundle.putString("DBPATH", appData.getCalendarDbFullPath());
            } else {
                bundle.putString("DBPATH", appData.getDatabaseFolderPath());
            }
            categoryDetailInfoCalender.setArguments(bundle);
            mMainActivity.showFragment(categoryDetailInfoCalender, isBackStack, false);
        } else if (i == 4) {
            CategoryDetailInfoPhotos categoryDetailInfoPhotos = new CategoryDetailInfoPhotos();

            if (mMainActivity.getCableVersion() == 1) {
                bundle.putString("DBPATH", appData.getGenDataThumbnailDbFullPath());
            } else {
                bundle.putString("DBPATH", appData.getSecureDbFullPath());
            }
            categoryDetailInfoPhotos.setArguments(bundle);
            mMainActivity.showFragment(categoryDetailInfoPhotos, isBackStack, false);
        } else if (i == 5) {
            CategoryDetailInfoVideos categoryDetailInfoVideos = new CategoryDetailInfoVideos();
            if (mMainActivity.getCableVersion() == 1) {
                bundle.putString("DBPATH", appData.getGenDataThumbnailDbFullPath());
            } else {
                bundle.putString("DBPATH", appData.getSecureDbFullPath());
            }
            categoryDetailInfoVideos.setArguments(bundle);
            mMainActivity.showFragment(categoryDetailInfoVideos, isBackStack, false);
        } else if (i == 6) {
            CategoryDetailInfoMusic categoryDetailInfoMusic = new CategoryDetailInfoMusic();
            if (mMainActivity.getCableVersion() == 1) {
                bundle.putString("DBPATH", appData.getGenDataThumbnailDbFullPath());
            } else {
                bundle.putString("DBPATH", appData.getSecureDbFullPath());
            }
            categoryDetailInfoMusic.setArguments(bundle);
            mMainActivity.showFragment(categoryDetailInfoMusic, isBackStack, false);
        } else if (i == 7) {
            CategoryDetailInfoDocuments categoryDetailInfoDocuments = new CategoryDetailInfoDocuments();
            if (mMainActivity.getCableVersion() == 1) {
                bundle.putString("DBPATH", appData.getGenDataThumbnailDbFullPath());
            } else {
                bundle.putString("DBPATH", appData.getSecureDbFullPath());
            }
            categoryDetailInfoDocuments.setArguments(bundle);
            mMainActivity.showFragment(categoryDetailInfoDocuments, isBackStack, false);
        }
    }

    private void onRequestDBDownload(final int listItem) {
        mDbg.trace();


        if (listItem <= 3) {

            mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
            mListener.onRequestSmartDB(vaultId, catCode, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mInitProgress.dismiss();
                    if (result) {
                        ShowCatDetailPage(listItem);
                    }
                    return false;
                }
            });
        } else {
            mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
            mListener.onRequestGenDB(vaultId, catCode, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mInitProgress.dismiss();
                    if (result) {
                        ShowCatDetailPage(listItem);
                    }
                    return false;
                }
            });
        }

    }

    @Override
    public void onEditOrCancelClickListner(String text) {
        mDbg.trace();


        SetSettingMode(text);
    }

    @Override
    public void onSaveClickListner() {
        mDbg.trace();

        SetSettingMode(getResources().getString(R.string.done));
    }

    @Override
    public void onShareIconClickListener() {

    }

    @Override
    public void onRestoreIconListener() {

    }

    @Override
    public void onSelectIconClickListener() {

    }

    @Override
    public void onBackButtonPressed() {
        mDbg.trace();

        mMainActivity.setToolbarText(getResources().getString(R.string.edit));
        SetSettingMode(getResources().getString(R.string.cancel));
    }

    @Override
    public void onDeleteIconClickListener() {

    }

    public void SetSettingMode(String mode) {
        mDbg.trace();


        if (mode.equals(getResources().getString(R.string.edit))) {
            mMainActivity.hideMenuIcon();
            mMainActivity.setBackPressDisable(true);
            mIsEditMode = true;
            mIsSaveMode = false;
            mIsDefaultMode = false;
            rotateArrowImage(90);
            onClick(mContactsLL);
            showDeleteVaultButton(isFromSetting);
            hideOtherTabs();
        } else if (mode.equals(getResources().getString(R.string.cancel))) {
            mMainActivity.showMenuIcon();
            mMainActivity.setBackPressDisable(false);
            closeSettingLayout();
            mIsEditMode = false;
            mIsSaveMode = false;
            mIsDefaultMode = true;
            rotateArrowImage(0);
            hideDeleteIcons(true);
            showDeleteVaultButton(false);
            setNoOfTabs();
            onNewObject(mMainActivity.getCablePresenter().getCableViewModel());
        } else {
            onSaveSettings();
        }
    }

    public void updateAfterSave() {
        mDbg.trace();

        mMainActivity.showMenuIcon();
        mMainActivity.setBackPressDisable(false);
        closeSettingLayout();
        mIsEditMode = false;
        mIsSaveMode = true;
        mIsDefaultMode = true;
        rotateArrowImage(0);
        showDeleteVaultButton(false);
    }

    private void rotateArrowImage(float angle) {
        mDbg.trace();

        if (!mIsMirror) {
            hideDeleteIcons(true);
            return;

        }

        mCalenderArrow.setRotation(angle);
        mContactsArrow.setRotation(angle);
        mPhotosArrow.setRotation(angle);
        mVideosArrow.setRotation(angle);
        mMessagesArrow.setRotation(angle);
        mDocumentsArrow.setRotation(angle);
        mMusicArrow.setRotation(angle);

    }

    private void fetchViewId() {
        mDbg.trace();


        mLLParentView = (LinearLayout) rootView.findViewById(R.id.llMain);

        mTabLL = (LinearLayout) rootView.findViewById(R.id.customTab);
        mProfileOne = (MeemTextView) mTabLL.findViewById(R.id.profileOneNew);
        mProfileTwo = (MeemTextView) mTabLL.findViewById(R.id.profileTwoNew);
        mProfileThree = (MeemTextView) mTabLL.findViewById(R.id.profileThreeNew);


        tvSize = (MeemTextView) rootView.findViewById(R.id.tvSize);
        mDeleteVault = (LinearLayout) rootView.findViewById(R.id.deleteVault);


        mContactsLL = (LinearLayout) rootView.findViewById(R.id.llContacts);
        mContactsLLInfo = (LinearLayout) rootView.findViewById(R.id.llcontactssetting);
        mCalenderLL = (LinearLayout) rootView.findViewById(R.id.llCalender);
        mCalenderLLInfo = (LinearLayout) rootView.findViewById(R.id.llcalendersetting);
        mPhotosLL = (LinearLayout) rootView.findViewById(R.id.llPhotos);
        mPhotosLLInfo = (LinearLayout) rootView.findViewById(R.id.llphotossetting);
        mVideosLL = (LinearLayout) rootView.findViewById(R.id.llVideos);
        mVideosLLInfo = (LinearLayout) rootView.findViewById(R.id.llvideossetting);
        mDocumentsLL = (LinearLayout) rootView.findViewById(R.id.llDocuments);
        mDocumentsLLInfo = (LinearLayout) rootView.findViewById(R.id.lldocumentssetting);
        mMessagesLL = (LinearLayout) rootView.findViewById(R.id.llMessages);
        mMessagesLLInfo = (LinearLayout) rootView.findViewById(R.id.llmessagessetting);
        mMusicLL = (LinearLayout) rootView.findViewById(R.id.llMusic);
        mMusicLLInfo = (LinearLayout) rootView.findViewById(R.id.llmusicsetting);

        mContactsMirrorSizeTV = (MeemTextView) mContactsLL.findViewById(R.id.tvMirrorSize);
        mContactsMirrorPlusSizeTV = (MeemTextView) mContactsLL.findViewById(R.id.tvMirrorPlusSize);
        mCalenderMirrorSizeTV = (MeemTextView) mCalenderLL.findViewById(R.id.tvMirrorSize);
        mCalenderMirrorPlusSizeTV = (MeemTextView) mCalenderLL.findViewById(R.id.tvMirrorPlusSize);
        mPhotosMirrorSizeTV = (MeemTextView) mPhotosLL.findViewById(R.id.tvMirrorSize);
        mPhotosMirrorPlusSizeTV = (MeemTextView) mPhotosLL.findViewById(R.id.tvMirrorPlusSize);
        mVideosMirrorSizeTV = (MeemTextView) mVideosLL.findViewById(R.id.tvMirrorSize);
        mVideosMirrorPlusSizeTV = (MeemTextView) mVideosLL.findViewById(R.id.tvMirrorPlusSize);
        mMessagesMirrorSizeTV = (MeemTextView) mMessagesLL.findViewById(R.id.tvMirrorSize);
        mMessagesMirrorPlusSizeTV = (MeemTextView) mMessagesLL.findViewById(R.id.tvMirrorPlusSize);
        mDocumentsMirrorSizeTV = (MeemTextView) mDocumentsLL.findViewById(R.id.tvMirrorSize);
        mDocumentsMirrorPlusSizeTV = (MeemTextView) mDocumentsLL.findViewById(R.id.tvMirrorPlusSize);
        mMusicMirrorSizeTV = (MeemTextView) mMusicLL.findViewById(R.id.tvMirrorSize);
        mMusicMirrorPlusSizeTV = (MeemTextView) mMusicLL.findViewById(R.id.tvMirrorPlusSize);


        mContactsArrow = (ImageView) mContactsLL.findViewById(R.id.ivArrow);
        mCalenderArrow = (ImageView) mCalenderLL.findViewById(R.id.ivArrow);
        mPhotosArrow = (ImageView) mPhotosLL.findViewById(R.id.ivArrow);
        mVideosArrow = (ImageView) mVideosLL.findViewById(R.id.ivArrow);
        mDocumentsArrow = (ImageView) mDocumentsLL.findViewById(R.id.ivArrow);
        mMessagesArrow = (ImageView) mMessagesLL.findViewById(R.id.ivArrow);
        mMusicArrow = (ImageView) mMusicLL.findViewById(R.id.ivArrow);


        mContactsTv = (MeemTextView) mContactsLL.findViewById(R.id.tvCategoryName);
        mCalenderTV = (MeemTextView) mCalenderLL.findViewById(R.id.tvCategoryName);
        mPhotosTV = (MeemTextView) mPhotosLL.findViewById(R.id.tvCategoryName);
        mVideosTV = (MeemTextView) mVideosLL.findViewById(R.id.tvCategoryName);
        mDocumentsTV = (MeemTextView) mDocumentsLL.findViewById(R.id.tvCategoryName);
        mMessagesTV = (MeemTextView) mMessagesLL.findViewById(R.id.tvCategoryName);
        mMusicTV = (MeemTextView) mMusicLL.findViewById(R.id.tvCategoryName);


        //Green Meem
        mSegmentedGroupContacts = (SegmentedGroup) mContactsLLInfo.findViewById(R.id.radioGroup);
        mSegmentedGroupCalender = (SegmentedGroup) mCalenderLLInfo.findViewById(R.id.radioGroup);
        mSegmentedGroupPhotos = (SegmentedGroup) mPhotosLLInfo.findViewById(R.id.radioGroup);
        mSegmentedGroupVideos = (SegmentedGroup) mVideosLLInfo.findViewById(R.id.radioGroup);
        mSegmentedGroupDocuments = (SegmentedGroup) mDocumentsLLInfo.findViewById(R.id.radioGroup);
        mSegmentedGroupMessages = (SegmentedGroup) mMessagesLLInfo.findViewById(R.id.radioGroup);
        mSegmentedGroupmMusic = (SegmentedGroup) mMusicLLInfo.findViewById(R.id.radioGroup);


        mRadioOFFContacts = (RadioButton) mContactsLLInfo.findViewById(R.id.radioOff);
        mRadioMirrorContacts = (RadioButton) mContactsLLInfo.findViewById(R.id.radioMirror);
        mRadioMirrorPlusContacts = (RadioButton) mContactsLLInfo.findViewById(R.id.radioMirrorPlus);

        mRadioOFFCalender = (RadioButton) mCalenderLLInfo.findViewById(R.id.radioOff);
        mRadioMirrorCalender = (RadioButton) mCalenderLLInfo.findViewById(R.id.radioMirror);
        mRadioMirrorPlusCalender = (RadioButton) mCalenderLLInfo.findViewById(R.id.radioMirrorPlus);

        mRadioOFFPhotos = (RadioButton) mPhotosLLInfo.findViewById(R.id.radioOff);
        mRadioMirrorPhotos = (RadioButton) mPhotosLLInfo.findViewById(R.id.radioMirror);
        mRadioMirrorPlusPhotos = (RadioButton) mPhotosLLInfo.findViewById(R.id.radioMirrorPlus);

        mRadioOFFVideos = (RadioButton) mVideosLLInfo.findViewById(R.id.radioOff);
        mRadioMirrorVideos = (RadioButton) mVideosLLInfo.findViewById(R.id.radioMirror);
        mRadioMirrorPlusVideos = (RadioButton) mVideosLLInfo.findViewById(R.id.radioMirrorPlus);

        mRadioOFFMusic = (RadioButton) mMusicLLInfo.findViewById(R.id.radioOff);
        mRadioMirrorMusic = (RadioButton) mMusicLLInfo.findViewById(R.id.radioMirror);
        mRadioMirrorPlusMusic = (RadioButton) mMusicLLInfo.findViewById(R.id.radioMirrorPlus);

        mRadioOFFDocuments = (RadioButton) mDocumentsLLInfo.findViewById(R.id.radioOff);
        mRadioMirrorDocuments = (RadioButton) mDocumentsLLInfo.findViewById(R.id.radioMirror);
        mRadioMirrorPlusDocuments = (RadioButton) mDocumentsLLInfo.findViewById(R.id.radioMirrorPlus);

        mRadioOFFMessages = (RadioButton) mMessagesLLInfo.findViewById(R.id.radioOff);
        mRadioMirrorMessages = (RadioButton) mMessagesLLInfo.findViewById(R.id.radioMirror);
        mRadioMirrorPlusMessages = (RadioButton) mMessagesLLInfo.findViewById(R.id.radioMirrorPlus);


        //Blue Meem
        mContactsLLInfoBlue = (LinearLayout) rootView.findViewById(R.id.llcontactssettingblue);
        mCalenderLLInfoBlue = (LinearLayout) rootView.findViewById(R.id.llcalendersettingblue);
        mPhotosLLInfoBlue = (LinearLayout) rootView.findViewById(R.id.llphotossettingblue);
        mVideosLLInfoBlue = (LinearLayout) rootView.findViewById(R.id.llvideossettingblue);
        mDocumentsLLInfoBlue = (LinearLayout) rootView.findViewById(R.id.lldocumentssettingblue);
        mMessagesLLInfoBlue = (LinearLayout) rootView.findViewById(R.id.llmessagessettingblue);
        mMusicLLInfoBlue = (LinearLayout) rootView.findViewById(R.id.llmusicsettingblue);


        mSegmentedGroupContactsBlue = (SegmentedGroup) mContactsLLInfoBlue.findViewById(R.id.radioGroup);
        mSegmentedGroupCalenderBlue = (SegmentedGroup) mCalenderLLInfoBlue.findViewById(R.id.radioGroup);
        mSegmentedGroupPhotosBlue = (SegmentedGroup) mPhotosLLInfoBlue.findViewById(R.id.radioGroup);
        mSegmentedGroupVideosBlue = (SegmentedGroup) mVideosLLInfoBlue.findViewById(R.id.radioGroup);
        mSegmentedGroupDocumentsBlue = (SegmentedGroup) mDocumentsLLInfoBlue.findViewById(R.id.radioGroup);
        mSegmentedGroupMessagesBlue = (SegmentedGroup) mMessagesLLInfoBlue.findViewById(R.id.radioGroup);
        mSegmentedGroupmMusicBlue = (SegmentedGroup) mMusicLLInfoBlue.findViewById(R.id.radioGroup);


        mRadioSyncOFFContacts = (RadioButton) mContactsLLInfoBlue.findViewById(R.id.radioOff);
        mRadioSyncOFFCalender = (RadioButton) mCalenderLLInfoBlue.findViewById(R.id.radioOff);
        mRadioSyncOFFPhotos = (RadioButton) mPhotosLLInfoBlue.findViewById(R.id.radioOff);
        mRadioSyncOFFVideos = (RadioButton) mVideosLLInfoBlue.findViewById(R.id.radioOff);
        mRadioSyncOFFMusic = (RadioButton) mMusicLLInfoBlue.findViewById(R.id.radioOff);
        mRadioSyncOFFDocuments = (RadioButton) mDocumentsLLInfoBlue.findViewById(R.id.radioOff);
        mRadioSyncOFFMessages = (RadioButton) mMessagesLLInfoBlue.findViewById(R.id.radioOff);


    }


    private void dbgTrace(String trace) {
        GenUtils.logCat(TAG, trace);
        GenUtils.logMessageToFile("DetailFragment", trace);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public boolean checkIsDBHasRows(String DBPATH, String TABLE_NAME) {
        mDbg.trace();
        mDbg.trace(DBPATH + "---" + TABLE_NAME + "");

        SQLiteDatabase db = SQLiteDatabase.openDatabase(DBPATH, null, SQLiteDatabase.OPEN_READONLY);
        long numRows = DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        db.close();
        return numRows > 0;


    }


    public interface DetailsFragmentInterface {

        void onDeleteCategory(String VaultId, int CategoryId, boolean isMirror, ResponseCallback responseCallBack);

        void onDeleteVault(String VaultId, ResponseCallback responseCallBack);

        void onUpdateVault(String vaultId, Object Vault, ResponseCallback responseCallBack);

        void onRequestGenDB(String vaultId, int CategoryId, ResponseCallback responseCallback);

        void onRequestSmartDB(String vaultId, int CategoryId, ResponseCallback responseCallback);

    }
}
