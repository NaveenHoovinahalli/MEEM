package com.meem.viewmodel;

import com.meem.mmp.messages.MMPConstants;

import java.util.LinkedHashMap;

/**
 * Created by arun on 23/8/16.
 *
 * Always think about V1 and V2 whenever changes are made to ViewModel classes. Else bugs are for sure.
 *
 */
public class DummyCableInfo {
    private static final long ONE_GB = (1024);
    private String mUpid, mPhoneName;

    public CableInfo createDummyInstance(String upid, String phoneName) {
        mUpid = upid;
        mPhoneName = phoneName;

        CableInfo cableInfo = new CableInfo();
        cableInfo.fwVersion = "2.2.12.0";
        cableInfo.mCapacityKB = 32 * ONE_GB;
        cableInfo.mFreeSpaceKB = 31 * ONE_GB;
        cableInfo.mIsVirgin = false;
        cableInfo.mName = "MEEM";
        cableInfo.mNumVaults = 3;

        cableInfo.mVaultInfoMap = createVaultMap();

        return cableInfo;
    }

    private LinkedHashMap<String, VaultInfo> createVaultMap() {
        VaultInfo firstVault = new VaultInfo();
        firstVault.mUpid = mUpid;
        firstVault.mName = mPhoneName;
        firstVault.mIsMirror = true;
        firstVault.mLastBackupTime = 0;
        firstVault.mMirrorSizeKB = 0 * ONE_GB;
        firstVault.mPlusSizeKB = 0 * ONE_GB;
        firstVault.mCategoryInfoMap = createCatMap();

        /*VaultInfo secondVault = new VaultInfo();
        secondVault.mUpid = "456456456456";
        secondVault.mName = "HTC G5";
        secondVault.mIsMirror = false;
        secondVault.mLastBackupTime = 197656789;
        secondVault.mMirrorSizeKB = 2 * ONE_GB; // 4GB
        secondVault.mPlusSizeKB = 4 * ONE_GB; // 4GB
        secondVault.mCategoryInfoMap = createCatMap();

        VaultInfo thirdVault = new VaultInfo();
        thirdVault.mUpid = "123345566778";
        thirdVault.mName = "HTC ";
        thirdVault.mIsMirror = false;
        thirdVault.mLastBackupTime = 197656789;
        thirdVault.mMirrorSizeKB = 2 * ONE_GB; // 4GB
        thirdVault.mPlusSizeKB = 4 * ONE_GB; // 4GB
        thirdVault.mCategoryInfoMap = createCatMap();*/

        LinkedHashMap<String, VaultInfo> vaultMap = new LinkedHashMap<String, VaultInfo>();
        vaultMap.put(mUpid, firstVault);
        /*vaultMap.put("456456456456", secondVault);
        vaultMap.put("123345566778", thirdVault);*/

        return vaultMap;
    }

    private LinkedHashMap<Byte, CategoryInfo> createCatMap() {
        CategoryInfo contacts = new CategoryInfo();
        contacts.mBackupMode = CategoryInfo.BackupMode.MIRROR;
        contacts.mMmpCode = MMPConstants.MMP_CATCODE_CONTACT;
        contacts.mMirrorSizeKB = 0;
        contacts.mPlusSizeKB = 0;

        CategoryInfo messages = new CategoryInfo();
        messages.mBackupMode = CategoryInfo.BackupMode.MIRROR;
        messages.mMmpCode = MMPConstants.MMP_CATCODE_MESSAGE;
        messages.mMirrorSizeKB = 0;
        messages.mPlusSizeKB = 0;

        CategoryInfo calendar = new CategoryInfo();
        calendar.mBackupMode = CategoryInfo.BackupMode.MIRROR;
        calendar.mMmpCode = MMPConstants.MMP_CATCODE_CALENDER;
        calendar.mMirrorSizeKB = 0;
        calendar.mPlusSizeKB = 0;

        CategoryInfo photos = new CategoryInfo();
        photos.mBackupMode = CategoryInfo.BackupMode.MIRROR;
        photos.mMmpCode = MMPConstants.MMP_CATCODE_PHOTO;
        photos.mMirrorSizeKB = 0 * ONE_GB;
        photos.mPlusSizeKB = 0;

        CategoryInfo videos = new CategoryInfo();
        videos.mBackupMode = CategoryInfo.BackupMode.MIRROR;
        videos.mMmpCode = MMPConstants.MMP_CATCODE_VIDEO;
        videos.mMirrorSizeKB = 0 * ONE_GB;
        videos.mPlusSizeKB = 0;

        CategoryInfo music = new CategoryInfo();
        music.mBackupMode = CategoryInfo.BackupMode.PLUS;
        music.mMmpCode = MMPConstants.MMP_CATCODE_MUSIC;
        music.mMirrorSizeKB = 0 * ONE_GB;
        music.mPlusSizeKB = 0;

        CategoryInfo documents = new CategoryInfo();
        documents.mBackupMode = CategoryInfo.BackupMode.DISABLED;
        documents.mMmpCode = MMPConstants.MMP_CATCODE_DOCUMENTS;
        documents.mMirrorSizeKB = 0 * ONE_GB;
        documents.mPlusSizeKB = 0;

        LinkedHashMap<Byte, CategoryInfo> catMap = new LinkedHashMap<Byte, CategoryInfo>();

        catMap.put(MMPConstants.MMP_CATCODE_CONTACT, contacts);
        catMap.put(MMPConstants.MMP_CATCODE_MESSAGE, messages);
        catMap.put(MMPConstants.MMP_CATCODE_CALENDER, calendar);
        catMap.put(MMPConstants.MMP_CATCODE_PHOTO, photos);
        catMap.put(MMPConstants.MMP_CATCODE_VIDEO, videos);
        catMap.put(MMPConstants.MMP_CATCODE_MUSIC, music);
        catMap.put(MMPConstants.MMP_CATCODE_DOCUMENTS, documents);

        return catMap;
    }
}
