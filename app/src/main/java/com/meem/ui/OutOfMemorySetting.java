package com.meem.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.MeemTextView;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Created by SCS on 2/9/2017.
 */

public class OutOfMemorySetting extends Fragment implements MainActivity.ToolBarItemClickListener, CompoundButton.OnCheckedChangeListener {


    View mRootView;
    MainActivity mMainActivity;
    MeemTextView mRequiredTV, mAvailableTV;
    MeemTextView tvContacts, tvCalenders, tvMessages, tvPhotos, tvVideos, tvMusic, tvDocuments;
    Switch swContacts, swCalenders, swMessages, swPhotos, swVideos, swMusic, swDocuments;
    HashMap<Byte, Long> catSize = new HashMap<>();
    byte cateCalender = MMPConstants.MMP_CATCODE_CALENDER, catContacts = MMPConstants.MMP_CATCODE_CONTACT, catMessages = MMPConstants.MMP_CATCODE_MESSAGE, catPhotos = MMPConstants.MMP_CATCODE_PHOTO, catVideos = MMPConstants.MMP_CATCODE_VIDEO, catMusic = MMPConstants.MMP_CATCODE_MUSIC, catDocuments = MMPConstants.MMP_CATCODE_DOCUMENTS;

    Long requiredSize, availableSize;
    OutOfMemoryInterface mListner;

    VaultInfo mVaultInfo;

    public OutOfMemorySetting() {
        // Mandatory default constructor
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


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.outofmemory_setting, null);
        init();
        return mRootView;
    }

    public void setValues(VaultInfo vaultInfo, long conSize, long msgSize, long calSize, long phoSize, long vidSize, long musSize, long docSize, long availableSize) {
        catSize.clear();

        requiredSize = 0L;
        mVaultInfo = vaultInfo;
        createHashMap(vaultInfo, catContacts, conSize);
        createHashMap(vaultInfo, catMessages, msgSize);
        createHashMap(vaultInfo, cateCalender, calSize);
        createHashMap(vaultInfo, catPhotos, phoSize);
        createHashMap(vaultInfo, catVideos, vidSize);
        createHashMap(vaultInfo, catMusic, musSize);
        createHashMap(vaultInfo, catDocuments, docSize);

        this.availableSize = availableSize;
    }

    private void createHashMap(VaultInfo vaultInfo, byte catCode, long size) {
        if (vaultInfo.getmCategoryInfoMap().get(catCode).getmBackupMode() != CategoryInfo.BackupMode.DISABLED) {
            catSize.put(catCode, size);
            requiredSize = requiredSize + size;
        }
    }

    private void init() {

        findViewById();
        setValues();
        setSwitchClickListner();

        setRequiredTV(requiredSize);
        if (availableSize < 100000) {
            mAvailableTV.setText("Available size: " + convertKBtoMB(availableSize) + " MB");

        } else {
            mAvailableTV.setText("Available size: " + convertKBtoGB(availableSize) + " GB");
        }
    }

    private void setSwitchClickListner() {

    }

    private void setValues() {
        requiredSize = 0L;

        setCatValues(cateCalender, tvCalenders, swCalenders);
        setCatValues(catContacts, tvContacts, swContacts);
        setCatValues(catMessages, tvMessages, swMessages);
        setCatValues(catPhotos, tvPhotos, swPhotos);
        setCatValues(catVideos, tvVideos, swVideos);
        setCatValues(catMusic, tvMusic, swMusic);
        setCatValues(catDocuments, tvDocuments, swDocuments);
    }

    private void setCatValues(Byte cateCode, MeemTextView textView, Switch mSwitch) {
        if (catSize.get(cateCode) != null) {
            if (catSize.get(cateCode) < 100000) {
                textView.setText(convertKBtoMB(catSize.get(cateCode)) + " MB");

            } else {
                textView.setText(convertKBtoGB(catSize.get(cateCode)) + " GB");
            }
            mSwitch.setOnCheckedChangeListener(this);
            requiredSize = requiredSize + catSize.get(cateCode);
        } else {
            textView.setText("");
            mSwitch.setClickable(false);
            mSwitch.setChecked(false);
        }
    }

    private void findViewById() {
        mRequiredTV = (MeemTextView) mRootView.findViewById(R.id.requiredSize);
        mAvailableTV = (MeemTextView) mRootView.findViewById(R.id.availableSize);

        tvCalenders = (MeemTextView) mRootView.findViewById(R.id.calenderSize);
        tvContacts = (MeemTextView) mRootView.findViewById(R.id.contactsSize);
        tvMessages = (MeemTextView) mRootView.findViewById(R.id.messagesSize);
        tvPhotos = (MeemTextView) mRootView.findViewById(R.id.photosSize);
        tvVideos = (MeemTextView) mRootView.findViewById(R.id.videosSize);
        tvMusic = (MeemTextView) mRootView.findViewById(R.id.musicSize);
        tvDocuments = (MeemTextView) mRootView.findViewById(R.id.documentsSize);

        swCalenders = (Switch) mRootView.findViewById(R.id.calenderSwitch);
        swContacts = (Switch) mRootView.findViewById(R.id.contactsSwitch);
        swMessages = (Switch) mRootView.findViewById(R.id.messagesSwitch);
        swPhotos = (Switch) mRootView.findViewById(R.id.photosSwitch);
        swVideos = (Switch) mRootView.findViewById(R.id.videosSwitch);
        swMusic = (Switch) mRootView.findViewById(R.id.musicSwitch);
        swDocuments = (Switch) mRootView.findViewById(R.id.documentsSwitch);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        mMainActivity.setAppTitle("Out of memory");
        mMainActivity.setBackPressDisable(true);
        setEventListner(mMainActivity.getCablePresenter());
    }

    private void setEventListner(CablePresenter cablePresenter) {

        mListner = cablePresenter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMainActivity.hideSaveandCancelButton();
        mMainActivity.setBackPressDisable(false);
    }

    @Override
    public void onEditOrCancelClickListner(String text) {
        mMainActivity.hideSaveandCancelButton();
        mMainActivity.setBackPressDisable(false);
        mMainActivity.onBackPressed();

    }

    @Override
    public void onSaveClickListner() {
        if (requiredSize > availableSize) {
            mMainActivity.setToolBarforOutOfMemory();
            return;
        }

        if (!swCalenders.isChecked()) {
            mVaultInfo.getmCategoryInfoMap().get(cateCalender).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
        }
        if (!swContacts.isChecked()) {
            mVaultInfo.getmCategoryInfoMap().get(catContacts).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
        }
        if (!swMessages.isChecked()) {
            mVaultInfo.getmCategoryInfoMap().get(catMessages).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
        }
        if (!swPhotos.isChecked()) {
            mVaultInfo.getmCategoryInfoMap().get(catPhotos).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
        }
        if (!swVideos.isChecked()) {
            mVaultInfo.getmCategoryInfoMap().get(catVideos).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
        }
        if (!swMusic.isChecked()) {
            mVaultInfo.getmCategoryInfoMap().get(catMusic).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
        }
        if (!swDocuments.isChecked()) {
            mVaultInfo.getmCategoryInfoMap().get(catDocuments).setmBackupMode(CategoryInfo.BackupMode.DISABLED);
        }

        mListner.onCatModeChanged(mVaultInfo, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                mMainActivity.hideToolbarIcons();
                mMainActivity.hideSaveandCancelButton();
                mMainActivity.setBackPressDisable(false);
                mMainActivity.onBackPressed();

                mMainActivity.setAppTitle(getString(R.string.home));

                return false;
            }
        });
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
    }

    @Override
    public void onDeleteIconClickListener() {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (buttonView == swCalenders) {
            updateSize(isChecked, cateCalender);
        } else if (buttonView == swContacts) {
            updateSize(isChecked, catContacts);
        } else if (buttonView == swMessages) {
            updateSize(isChecked, catMessages);
        } else if (buttonView == swPhotos) {
            updateSize(isChecked, catPhotos);
        } else if (buttonView == swVideos) {
            updateSize(isChecked, catVideos);
        } else if (buttonView == swMusic) {
            updateSize(isChecked, catMusic);
        } else if (buttonView == swDocuments) {
            updateSize(isChecked, catDocuments);
        }

    }

    void updateSize(boolean isChecked, byte catID) {
        if (isChecked) {
            requiredSize = requiredSize + catSize.get(catID);
            setRequiredTV(requiredSize);
            if (requiredSize <= availableSize)
                mMainActivity.setSaveButton(true);
            else
                mMainActivity.setSaveButton(false);


        } else {
            requiredSize = requiredSize - catSize.get(catID);
            setRequiredTV(requiredSize);
            if (requiredSize <= availableSize)
                mMainActivity.setSaveButton(true);
            else
                mMainActivity.setSaveButton(false);
        }
    }

    private void setRequiredTV(Long requiredSize) {
        if (requiredSize < 100000) {
            mRequiredTV.setText("Required size: " + convertKBtoMB(requiredSize) + " MB");
        } else {
            mRequiredTV.setText("Required size: " + convertKBtoGB(requiredSize) + " GB");
        }
    }

    public interface OutOfMemoryInterface {
        public void onCatModeChanged(VaultInfo vaultInfo, ResponseCallback responseCallback);
    }
}
