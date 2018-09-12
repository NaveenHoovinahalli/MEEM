package com.meem.androidapp;

import com.meem.businesslogic.Backup;
import com.meem.businesslogic.Restore;
import com.meem.businesslogic.Session;
import com.meem.businesslogic.SessionCatFilter;
import com.meem.businesslogic.SessionSmartDataInfo;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPUmid;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataSettings;
import com.meem.phone.Storage;
import com.meem.utils.DebugTracer;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.util.ArrayList;

import static com.meem.androidapp.SessionManager.SessionType.BACKUP;
import static com.meem.androidapp.SessionManager.SessionType.COPY;
import static com.meem.androidapp.SessionManager.SessionType.NONE;
import static com.meem.androidapp.SessionManager.SessionType.RESTORE;

/**
 * Created by arun on 22/8/16.
 * For practical reasons, session manager should be instantiated by MainaAtivity.
 */
public class SessionManager implements Session.SessionStatUpdateHelper {
    private static final String TAG = "SessionManager";

    protected DebugTracer mDbg = new DebugTracer(TAG, "SessionManager.log");
    protected SessionType mSessionType = NONE;

    protected String mPhoneUpid;
    protected CableDriver mDriverInstance;
    protected SessionManagementHelper mHelper;
    protected Backup mBackupLogic;
    protected Restore mRestoreLogic;
    protected Session mSession;
    protected boolean mHasMsgMgmtPermission;
    protected boolean mCableDisconnected;

    public SessionManager(String phoneUpid, SessionManagementHelper helper) {
        mPhoneUpid = phoneUpid;
        mHelper = helper;
    }

    public void setDriverInstance(CableDriver driverInstance) {
        mDbg.trace();

        mDriverInstance = driverInstance;
        mCableDisconnected = false;
        mHasMsgMgmtPermission = false;
    }

    /**
     * IMPORTANT: Must be overriden by SessionManagerV1 and V2.
     */
    protected Session createSession(int sessionType, Session.SessionStatUpdateHelper statHelper) {
        return null;
    }

    protected boolean checkForStorageAfterSessionPreperation() {
        mDbg.trace();

        AppLocalData appLocalData = AppLocalData.getInstance();

        if (null == mSession) {
            // TODO: In GUI, we should ideally prevent the user from doing anything until business logic thread are finished.
            mDbg.trace("Warning: session information is null. The aborted session thread is completing late?");
            return false;
        }

        if (!mDriverInstance.isCableConnected()) {
            mDbg.trace("Cable disconncted during session preperation. Hmm... session thread did not get aborted and thus failed.");
            return false;
        }

        // Arun: 01June2015: These is needed to properly handle the cable disconnect scenario.
        // check other places how these objects are used along with other session related
        // events - most importantly, onCriticalError().
        mSession.setSessionPreparationCompleteWithStatus(true);

        if (mSession.isAbortPending()) {
            mDriverInstance.closeSession(mSession.getHandle(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    String msg = "Abort requested while during session preperation. Close status: " + result;
                    return onCloseSessionResponse(false, msg);
                }
            });
            return false;
        }

        // Check for available space on phone / meem
        long availableMeemStorage = 0, availableInternalStorage = 0, availableExternalStorage = 0;
        boolean enoughSpace = true;

        if (mSession.getType() == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
            long reqStorage = mSession.getTotalSessionDataSize(); // KB
            availableMeemStorage = mSession.getFreeSpaceInCable(); // KB

            // NOTE: To test out of memory stuff, do something like this:
            // availableMeemStorage = 2 * 1024;

            mDbg.trace("Backup size kb: " + reqStorage + ". Free space in cable kb: " + availableMeemStorage);
            if (availableMeemStorage < reqStorage) {
                enoughSpace = false;
            }
        } else {
            String noFreeSpaceMsg = "";

            Storage storage = new Storage();
            availableInternalStorage = (storage.getPrimaryExternalStorageFreeSpace() * 1024) - (ProductSpecs.MIN_FREE_STORAGE_MAINTAINED_MB * 1024); // KB

            // do we need to keep 500MB in removable SD card? it seems nope.
            availableExternalStorage = (storage.getSecondaryExternalStorageFreeSpace() * 1024); // KB

            long reqInternalStorage = mSession.getSessionInternalStorageDataSize(); // kb
            long reqExternalStorage = mSession.getSessionExternalStorageDataSize(); // kb

            mDbg.trace("Restore/Copy size (Internal) kb: " + reqInternalStorage + " (External) kb: " + reqExternalStorage + ". Free space in phone internal memory kb: " + availableInternalStorage + ". Free space in phone external memory kb: " + availableExternalStorage);

            if (availableInternalStorage < reqInternalStorage) {
                enoughSpace = false;
                noFreeSpaceMsg = "Not enough internal storage space available in phone. Please disable or delete some categories and try again.";
            }

            if (availableExternalStorage < reqExternalStorage) {
                /**
                 * 14Dec2015: Arun: Luckily, new product spec regarding SDCARD
                 * data restore/copy did not affect the code path here. Only
                 * some comments an traces changed.
                 */
                // If we are storing SDCARD items into internal memory, make a double check for space now.
                if (appLocalData.getPrimaryStorageUsageOption()) {
                    if (availableInternalStorage < reqExternalStorage) {
                        enoughSpace = false;
                        noFreeSpaceMsg = "Not enough internal storage space available in phone to store SDCARD items into it. As per product spec, we are bailing out.";
                    } else {
                        mDbg.trace("As user opted, SDCARD contents can be fit into available internal storage");
                    }
                } else {
                    enoughSpace = false;
                    noFreeSpaceMsg = "Not enough SDCARD space avaibale to save SDCARD items. As per product spec, we are bailing out.";
                }
            }

            if (!enoughSpace) {
                mDbg.trace(noFreeSpaceMsg);
            }
        }

        if (!enoughSpace) {
            mDbg.trace("Not enough storage to execute the session. Bailing out");

            if (mSession.getType() == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
                // TODO: Remove dummy values for smart data (it is quite harmless anyway - as it doesn't add to any major size calculations)
                mHelper.notifyInsufficientCableStorage(1234, 1234, 1234,
                        mSession.getTotalPhoneImageSize(), mSession.getTotalPhoneVideoSize(), mSession.getTotalPhoneMusicSize(), mSession.getTotalPhoneDocsSize(),
                        mSession.getFreeSpaceInCable());
            } else {
                mHelper.notifyInsufficientPhoneStorage();
            }

            return mDriverInstance.closeSession(mSession.getHandle(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    String msg = "Session closed because of insufficient storage. close session result: " + result;
                    return onCloseSessionResponse(false, msg);
                }
            });
        }

        mDbg.trace("Storage requirements are met for executing session. Good.");

        return true;
    }

    protected boolean onSessionPreperationSucceeded() {
        mDbg.trace();

        if (!checkForStorageAfterSessionPreperation()) {
            return false;
        }

        mDbg.trace("We have enough storage to finish session.");

        AppLocalData appLocalData = AppLocalData.getInstance();

        // go for SET_SESD and EXECUTE_SESSION
        String sesdPath = appLocalData.getSesdPath();

        File sesdFile = new File(sesdPath);
        MMPFPath sesdFPath = new MMPFPath(sesdPath, sesdFile.length());

        // Important to get the db full path as we are doing file read/write.
        String thumbDbPath = appLocalData.getGenDataThumbnailDbFullPath();
        File thumbDbFile = new File(thumbDbPath);
        MMPFPath thumbDbFPath = new MMPFPath(thumbDbPath, thumbDbFile.length());

        // TODO: remove this trace
        mDbg.trace("Thumbnail db path: " + thumbDbPath + " Size: " + thumbDbFile.length());

        // TODO: Minor workaround for COPY implementation. This interface needs cleanup.
        boolean isCopySession = (mSession.getType() == MMPConstants.MMP_CODE_CREATE_CPY_SESSION) ? true : false;
        return mDriverInstance.executeSession(mSession.getHandle(), sesdFPath, thumbDbFPath, isCopySession, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                onSessionEnd(result, "Execute session response received");
                return false;
            }
        });
    }

    // --------------------------------------------------------------------------
    // ------------- Start: business logic thread result handling related stuff -
    // --------------------------------------------------------------------------

    public void onSessionPreperationFailed() {
        mDbg.trace();

        if (!mCableDisconnected && null != mSession) {
            // Arun: 01June2015: These is needed to properly handle the cable disconnect scenario.
            // check other places how these objects are used along with other session related
            // events - most importantly, onCriticalError().
            mSession.setSessionPreparationCompleteWithStatus(false);

            if (mSession.isAbortPending()) {
                mDbg.trace("Session aborted during preperation stage");

                if (true == mSession.getDataXfrStatus()) {
                    // TODO: This condition is impossible!
                    mDbg.trace("XFR has started. Simply aborting");
                    mDriverInstance.abortSession();
                } else {
                    mDbg.trace("XFR is not started yet. Closing session");
                    mDriverInstance.closeSession(mSession.getHandle(), new ResponseCallback() {
                        @Override
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            String msg = "Session closed on abort request during session preperation. Close session status: " + result;
                            return onCloseSessionResponse(false, msg);
                        }
                    });
                }
            } else {
                mDbg.trace("Error: Session preperation failed!");
                mDriverInstance.closeSession(mSession.getHandle(), new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        String msg = "Session closed because of errors encountered during session preperation. Close session status: " + result;
                        return onCloseSessionResponse(false, msg);
                    }
                });
            }
        } else {
            mDbg.trace("Session is null OR Cable disconnected");
        }
    }

    public void onFileReceivedFromMeem(String filePath) {
        mDbg.trace();

        if (filePath == null) {
            mDbg.trace("WTF: null file path for received file. Possible backend bug.");
            return;
        }

        if (null == mSession) {
            mDbg.trace("No live sessions. Ignoring: " + filePath);
            return;
        }

        File thisFile = new File(filePath);
        mDbg.trace("Received file: " + filePath + ": has size: " + thisFile.length() + " bytes.");

        // NOTE: If this affects XFR performance, we can always filter what we
        // can scanning.
        // make use of getMimeType above.
        if (mSession.getType() != MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
            // Update last modification time
            if (null != mRestoreLogic) {
                mRestoreLogic.updateModTime(filePath);
            }

            if (null != mHelper) {
                mHelper.updateMediaLibrary(filePath);
            }
        }

        // update completed data size in session
        long dataSize = mSession.getCompletedSessionDataSize();
        dataSize += (thisFile.length()) / 1024; // KB
        // Arun: 15June2017, this is now more accurately done internally in session.
        /*mSession.setCompletedSessionDataSize(dataSize);*/
    }

    // --------------------------------------------------------------------------
    // ------------- End: business logic thread result handling related stuff ---
    // --------------------------------------------------------------------------


    // --------------------------------------------------------------------------
    // ------------- Start: File transfer tracking and handling -----------------
    // --------------------------------------------------------------------------

    // To update session data size info during backup
    // Modified: 04 July 2015: To update the contact tracker service database upon successful backup of contacts
    public void onFileSentToMeem(String filePath) {
        mDbg.trace();

        File thisFile = new File(filePath);

        mDbg.trace("Sent file: " + filePath + ": has size: " + thisFile.length() + " bytes.");

        if (null == mSession || filePath == null) {
            mDbg.trace("Sent file is not part of a session (null sessionInfo)");
            return;
        }

        // update completed data size in session
        long dataSize = mSession.getCompletedSessionDataSize();
        dataSize += (thisFile.length()) / 1024; // KB

        // Arun: 15June2017, this is now more accurately done internally in session.
        /*mSession.setCompletedSessionDataSize(dataSize);*/

        // if this is contact mirror file and if this is a backup session, update contact tracker
        // It is bit hackish to use file name comparison here. But it is most logical.
        if (filePath.contains("contact-mirror-out.json")) {
            /**
             * The commented out code is the reverse order to get the mirror
             * file path from session info. This is kept here for future
             * reference if somebody ever comes this way :)
             */
            /*SessionSmartDataInfo smartInfo = mSession.getSmartDataInfo();
            if (smartInfo != null) {
				HashMap<Byte, ArrayList<MMPFPath>> pathMap = smartInfo.getCatsFilesMap();
				if(pathMap != null) {
					ArrayList<MMPFPath> contactFPaths = pathMap.get(Byte.valueOf(MMPConstants.MMP_CATCODE_CONTACT));
					if(contactFPaths != null) {
						MMPFPath contactMirrorFPath = contactFPaths.get(0);
						if(contactMirrorFPath != null) {
							if(filePath.equals(contactMirrorFPath.getPath()) {
								// verified! this is our contact mirror file
							}
						}
					}
				}
			}*/

            if (mSession.getType() == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
                ContactTrackerWrapper ctw = mSession.getContactTrackingWrapper();
                if (ctw != null) {
                    mDbg.trace("Updating contact tracker on successful contact mirror file transfer");
                    ctw.onContactBackupCompleted();
                } else {
                    mDbg.trace("BUG: Contact tracking wrapper is null!");
                }
            }
        }
    }

    private boolean checkPhoneStorageAvailabilityForSession(final VaultInfo srcVaultInfo, final SessionCatFilter filter) {
        mDbg.trace();

        final Storage storage = new Storage();

        int srcCatMask = srcVaultInfo.getGenericDataCategoryMask();
        mDbg.trace("Source vault generic category mask: " + srcCatMask);

		/*
         * Change of requirements understanding: 1. Restore/copy of vaults using
		 * the avatar header in UI must restore/copy of all categories that have
		 * data.
		 *
		 * Note that if avatar header copy is performed, the category filter
		 * object passed to this function will be null. We are going to use this fact
		 * to implement this requirement.
		 */
        if (null == filter) {
            MMLGenericDataSettings genDataSettings = new MMLGenericDataSettings(true);
            // This will get the default category mask, which is all items
            // enabled.
            // see the rest of the logic below
            srcCatMask = genDataSettings.getCategoryMask();

            mDbg.trace("Full copy/restore - Source vault generic category mask updated: " + srcCatMask);
        }

        // apply filter for individual category copy
        if (null != filter) {
            ArrayList<Byte> catMaskArray = MMLCategory.getGenericCatCodesArray(srcCatMask);

            filter.applyGenCatFilter(catMaskArray);
            srcCatMask = 0;
            for (Byte cat : catMaskArray) {
                srcCatMask = MMLCategory.updateGenCatMask(srcCatMask, cat, true);
            }

            mDbg.trace("Category copy/restore - Source vault generic category mask updated: " + srcCatMask);
        }

        int genCatMask = srcCatMask;

        // get size of internal storage items
        final boolean hasPhotosOnIntMem = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO, genCatMask);
        final boolean hasMusicOnIntMem = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_MUSIC, genCatMask);
        final boolean hasVideosOnIntMem = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO, genCatMask);

        // get size of external storage items
        final boolean hasPhotosOnSD = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO_CAM, genCatMask);
        final boolean hasAudioOnSD = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_FILE, genCatMask);
        final boolean hasVideosOnSD = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO_CAM, genCatMask);

        // TODO: Assuming a restore operation do not restore archive/plus items.
        long intMemSpaceNeeded = 0;
        if (filter == null || hasPhotosOnIntMem || hasMusicOnIntMem || hasVideosOnIntMem) {
            if (filter == null || hasPhotosOnIntMem) {
                intMemSpaceNeeded += srcVaultInfo.getGenDataMirrorOnlyUsage(MMPConstants.MMP_CATCODE_PHOTO);
            }

            if (filter == null || hasMusicOnIntMem) {
                intMemSpaceNeeded += srcVaultInfo.getGenDataMirrorOnlyUsage(MMPConstants.MMP_CATCODE_MUSIC);
            }

            if (filter == null || hasMusicOnIntMem) {
                intMemSpaceNeeded += srcVaultInfo.getGenDataMirrorOnlyUsage(MMPConstants.MMP_CATCODE_VIDEO);
            }

            intMemSpaceNeeded = intMemSpaceNeeded / 1024; // its in KB
        }

        long extMemSpaceNeeded = 0;
        if (filter == null || hasPhotosOnSD || hasAudioOnSD || hasVideosOnSD) {
            if (filter == null || hasPhotosOnSD) {
                extMemSpaceNeeded += srcVaultInfo.getGenDataMirrorOnlyUsage(MMPConstants.MMP_CATCODE_PHOTO_CAM);
            }

            if (filter == null || hasAudioOnSD) {
                extMemSpaceNeeded += srcVaultInfo.getGenDataMirrorOnlyUsage(MMPConstants.MMP_CATCODE_FILE);
            }

            if (filter == null || hasVideosOnSD) {
                extMemSpaceNeeded += srcVaultInfo.getGenDataMirrorOnlyUsage(MMPConstants.MMP_CATCODE_VIDEO_CAM);
            }

            extMemSpaceNeeded = extMemSpaceNeeded / 1024; // its in KB
        }

        long totSpaceNeeded = intMemSpaceNeeded + extMemSpaceNeeded + ProductSpecs.MIN_FREE_STORAGE_MAINTAINED_MB;

        mDbg.trace("Phone storage requirements (MB) for session: Internal: " + intMemSpaceNeeded + " External: " + extMemSpaceNeeded + " Total: " + totSpaceNeeded);

        if (extMemSpaceNeeded == 0) {
            mDbg.trace("Source vault does not have SDCARD items. Check for internal storage availability after preparing SESD");
            mSession.prepareGenericDataInfoForRestoreOrCopy(genCatMask, 0);
            return startRestoreOrCopySessionAfterStorageOptions(srcVaultInfo.getUpid());
        }

        // Else, we must look out for SDCARD related storage issues now.
        AppLocalData appLocalData = AppLocalData.getInstance();
        boolean phoneHasSDCARD = (null == appLocalData.getSecondaryExternalStorageRootPath()) ? false : true;

        mDbg.trace("Removable SD card presence: " + phoneHasSDCARD);

        // If source vault have SDCARD items and if this phone does not have SDCARD
        if ((extMemSpaceNeeded > 0) && !phoneHasSDCARD) {
            mDbg.trace("Source vault have SDCARD items. This phone does NOT have secondary external SDCARD");

            long internalSpaceAvailable = storage.getPrimaryExternalStorageFreeSpace();
            mDbg.trace("Phone internal storage available (MB) for session: " + internalSpaceAvailable);

            if (totSpaceNeeded < internalSpaceAvailable) {
                // we have enough space in internal storage. so proceed with user option on SDCARD items
                mDbg.trace("We have enough space in internal storage. Proceeding with product spec of saving all data including SDCARD data in internal storage.");

                /**
                 * 14Dec2015: Arun: New product spec reg SDCARd items in effect
                 * - save all data, including SDCARD data into phone's internal
                 * storage.
                 */
                /*final MeemDecisionDialog dlg = new MeemDecisionDialog(this);
                dlg.setMessage(getResources().getString(R.string.sd_card_content1));

				dlg.setOnYes(new Runnable() {
					@Override
					public void run() {
						// we need to keep it in global singleton as this info
						// is needed from meem core later.
						mAppData.optUsingPrimaryStorage(true);
						int catMask = srcVault.getGenericDataCategoryMask();

						mSession.prepareGenericDataInfoForRestoreOrCopy(catMask, 0);
						mMainActivity.startRestoreOrCopySessionAfterStorageOptions(srcVault.getUpid());

						dlg.cancel();
					}
				});

				dlg.setOnNo(new Runnable() {
					@Override
					public void run() {
						// remove SDCARD items from session
						mDbg.trace("Removing SDCARD items from session");

						int catMask = srcVault.getGenericDataCategoryMask();

						catMask = MMLCategory.updateGenCatMask(catMask, MMPConstants.MMP_CATCODE_PHOTO_CAM, false);
						catMask = MMLCategory.updateGenCatMask(catMask, MMPConstants.MMP_CATCODE_VIDEO_CAM, false);
						catMask = MMLCategory.updateGenCatMask(catMask, MMPConstants.MMP_CATCODE_FILE, false);

						mSession.prepareGenericDataInfoForRestoreOrCopy(catMask, 0);
						mMainActivity.startRestoreOrCopySessionAfterStorageOptions(srcVault.getUpid());

						dlg.cancel();
					}
				});
				dlg.show();*/

                // we need to keep it in global singleton as this info
                // is needed from meem core later.
                appLocalData.optUsingPrimaryStorage(true);

                // Arun: 27June2017: Bugfix: If we do this, the filter is gone! Why the heck are we missing these bugs for 3 years!
                /*int catMask = srcVaultInfo.getGenericDataCategoryMask();
                mSession.prepareGenericDataInfoForRestoreOrCopy(catMask, 0);*/
                mSession.prepareGenericDataInfoForRestoreOrCopy(genCatMask, 0);

                return startRestoreOrCopySessionAfterStorageOptions(srcVaultInfo.getUpid());
            } else {
                mDbg.trace("We do NOT have enough space on internal memory to save all data including SDCARD items");

                /**
                 * 14Dec2015: Arun: New product spec reg SDCARd items in effect
                 * - abort the session now itself
                 */

				/*dlg = new MeemDecisionDialog(this);
                dlg.setMessage(getResources().getString(R.string.sd_card_content2));
				dlg.setNoButtonLabel(getResources().getString(R.string.cancel));

				dlg.setOnYes(new Runnable() {
					@Override
					public void run() {
						mAppData.optUsingPrimaryStorage(true);

						// remove SDCARD items from session
						mDbg.trace("Removing SDCARD items from session on user decision");

						int catMask = srcVault.getGenericDataCategoryMask();
						catMask = MMLCategory.updateGenCatMask(catMask, MMPConstants.MMP_CATCODE_PHOTO_CAM, false);
						catMask = MMLCategory.updateGenCatMask(catMask, MMPConstants.MMP_CATCODE_VIDEO_CAM, false);
						catMask = MMLCategory.updateGenCatMask(catMask, MMPConstants.MMP_CATCODE_FILE, false);

						mSession.prepareGenericDataInfoForRestoreOrCopy(catMask, 0);
						mMainActivity.startRestoreOrCopySessionAfterStorageOptions(srcVault.getUpid());
						dlg.cancel();
					}
				});

				dlg.setOnNo(new Runnable() {
					@Override
					public void run() {
						// abort session on lack of space (setting this flag
						// will make sure of it)
						mDbg.trace("Will abort this session as per user decision");
						mSession.mDropSessionOnUserOpt = true;

						mSession.prepareGenericDataInfoForRestoreOrCopy(0, 0);
						startRestoreOrCopySessionAfterStorageOptions(srcVault.getUpid());

						dlg.cancel();
					}
				});
				dlg.show();*/
                mDbg.trace("Will abort this session as per new product specification.");
                // TODO: Arun: 27June2017: Show a message for the poor user!
                mSession.mDropSessionOnUserOpt = true;

                mSession.prepareGenericDataInfoForRestoreOrCopy(0, 0);
                return startRestoreOrCopySessionAfterStorageOptions(srcVaultInfo.getUpid());
            }
        } else if ((extMemSpaceNeeded > 0) && phoneHasSDCARD) {
            /**
             * 14Dec2015: Arun: change in product spec for SDCARD support.
             * Anyway, in this method, right now we can not do anything until
             * SESD is prepared.
             */
            mDbg.trace("Source vault have SDCARD items. Phone also has SDCARD. Check for internal aand SDCARD storage availability after preparing SESD");
            mSession.prepareGenericDataInfoForRestoreOrCopy(genCatMask, 0);
            return startRestoreOrCopySessionAfterStorageOptions(srcVaultInfo.getUpid());
        } else {
            mDbg.trace("Unknown scenario while scheking phone storage. BUG ALERT!");
            return false;
        }
    }

    // --------------------------------------------------------------------------
    // ------------- End: File transfer tracking and handling -------------------
    // --------------------------------------------------------------------------

    // --------------------------------------------------------------------------
    // ------------- Start: session processing related stuff --------------------
    // --------------------------------------------------------------------------

    /**
     * To be overridden by derived classes
     *
     * @param upid
     *
     * @return
     */
    protected boolean startRestoreOrCopySessionAfterStorageOptions(String upid) {
        mDbg.trace();

        if (mSession.mDropSessionOnUserOpt) {
            String msg = "Dropping session on user choice";
            mDbg.trace(msg);
            onSessionEnd(false, msg);

            return false;
        }

        MMPUpid mmpUpid = new MMPUpid(upid);
        MMPUmid mmpUmid = new MMPUmid(null);

        mSession.startStats();

        return mDriverInstance.getDATD(mmpUpid, mmpUmid, mSession.getType(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return onDATDReceived(result, (Byte) info);
            }
        });
    }

    private boolean onDATDReceived(boolean result, Byte handle) {
        mDbg.trace();

        if (mSession.isAbortPending()) {
            onSessionEnd(false, "Aborting on request");
            return false;
        }

        if (!result) {
            onSessionEnd(false, "Getting DATD Failed. This is a serious error!");
            return false;
        }

        mSession.setHandle(handle);
        mSession.prepareSmartDataInfo();

        SessionSmartDataInfo sesSmartDataInfo = mSession.getSmartDataInfo();

        if (0 == sesSmartDataInfo.size()) {
            mDbg.trace("No smart data to get");
            return onSmartDataReceived(true);
        }

        // to cleanup smart data files from previous sessions.
        mSession.cleanupPreviousSmartDataFiles();

        if (mSession.getType() == MMPConstants.MMP_CODE_CREATE_CPY_SESSION) {
            mDriverInstance.getCopySmartData(handle, sesSmartDataInfo, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    return onSmartDataReceived(result);
                }
            });
        } else {
            mDriverInstance.getSmartData(handle, sesSmartDataInfo, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    return onSmartDataReceived(result);
                }
            });
        }

        return true;
    }

    private boolean onSmartDataReceived(boolean status) {
        mDbg.trace();

        if (mSession == null) {
            mDbg.trace("WARNING: Session is null while SmartData received!");
            return false;
        }

        if (null != mSession && mSession.isAbortPending()) {
            mDriverInstance.closeSession(mSession.getHandle(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    String msg = "Abort requested while waiting for smart data. Close status: " + result;
                    return onCloseSessionResponse(false, msg);
                }
            });
            return false;
        }

        if (!status) {
            mDbg.trace("Failed to get smart data!");
            mDriverInstance.closeSession(mSession.getHandle(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    String msg = "Session failed because of failed smart data retrieval. Close status: " + result;
                    return onCloseSessionResponse(false, msg);
                }
            });
        }

        if (mSession.getType() == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
            /**
             * filter generic data categories - if there is a filter and get the
             * generic data category masks for this session.
             *
             * IMPORTANT LOGIC TWIST: Note that for other type of sessions, this
             * calculation is done and generic data info is prepared beforehand
             * during phone storage availability checking.
             */
            mSession.prepareGenericDataInfoForBackup();
            mBackupLogic = new Backup(mSession);
            mBackupLogic.start();
        } else {
            if (mSession.getSmartDataInfo().getCatCodes().contains(Byte.valueOf(MMPConstants.MMP_CATCODE_MESSAGE))) {
                mHelper.requestSmsManagementPermission();
            } else {
                accessForSMSManagement(true);
            }
        }

        return true;
    }

    public boolean startBackup(CableInfo cableInfo, ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        mSession = createSession(MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION, this);

        SessionCatFilter catFilter = null;
        if (null != catInfos) {
            catFilter = prepareCatFilter(catInfos);
        }

        mSession.setSessionParams(cableInfo.mFreeSpaceKB, cableInfo.getVaultInfo(mPhoneUpid), catFilter);
        mSession.startStats();

        mSessionType = BACKUP;

        if (!mSession.isFirstBackupPossible()) {
            String msg = "Not enough space in cable for first backup";
            mDbg.trace(msg);

            // using dummy values for smart data.
            mHelper.notifyInsufficientCableStorage(1234, 1234, 1234,
                    mSession.getTotalPhoneImageSize(), mSession.getTotalPhoneVideoSize(), mSession.getTotalPhoneMusicSize(), mSession.getTotalPhoneDocsSize(),
                    mSession.getFreeSpaceInCable());

            onSessionEnd(false, msg);
            return false; // Arun: bug fix on 03Feb2017
        }

        return startBackupSessionAfterInitialCableSpaceCheck();
    }

    /**
     * To be overridden by derived classes
     *
     * @return
     */
    protected boolean startBackupSessionAfterInitialCableSpaceCheck() {
        mDbg.trace();

        MMPUpid upid = new MMPUpid(mPhoneUpid);
        MMPUmid umid = new MMPUmid(null);

        return mDriverInstance.getDATD(upid, umid, MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return onDATDReceived(result, (Byte) info);
            }
        });
    }

    // --------------------------------------------------------------------------
    // ------------- End: session processing related stuff ----------------------
    // --------------------------------------------------------------------------

    public boolean startRestore(CableInfo cableInfo, ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        mSession = createSession(MMPConstants.MMP_CODE_CREATE_RESTORE_SESSION, this);

        SessionCatFilter catFilter = null;
        if (null != catInfos) {
            catFilter = prepareCatFilter(catInfos);
        }

        VaultInfo vaultInfo = cableInfo.getVaultInfo(mPhoneUpid);
        mSession.setSessionParams(cableInfo.mFreeSpaceKB, vaultInfo, catFilter);

        mSessionType = RESTORE;

        return checkPhoneStorageAvailabilityForSession(vaultInfo, catFilter);
        // we will proceed from startRestoreOrCopySessionAfterStorageOptions.
    }

    // This is almost similar to startRestore.
    public boolean startCopy(CableInfo cableInfo, VaultInfo srcVaultInfo, ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        mSession = createSession(MMPConstants.MMP_CODE_CREATE_CPY_SESSION, this);

        SessionCatFilter catFilter = null;
        if (null != catInfos) {
            catFilter = prepareCatFilter(catInfos);
        }

        mSession.setSessionParams(cableInfo.mFreeSpaceKB, srcVaultInfo, catFilter);

        mSessionType = COPY;

        return checkPhoneStorageAvailabilityForSession(srcVaultInfo, catFilter);
        // we will proceed from startRestoreOrCopySessionAfterStorageOptions.
    }

    protected boolean abort() {
        mDbg.trace();

        boolean result = false;

        if (null == mSession) {
            mDbg.trace("Session is null during abort!");
            return result;
        }

        if (mSession.isAbortPending()) {
            mDbg.trace("Session is already marked for aborting.");
            return result;
        }

        mSession.setAbortPending(true);

        if (mSession.getDataXfrStatus()) {
            mDbg.trace("Session data xfr ongoing, sending abort to meem");
            mDriverInstance.abortSession();
        } else {
            mDbg.trace("Session data xfr has not started, aborting scanning");
            if (mSession.getType() == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
                if (mBackupLogic != null) result = mBackupLogic.abort();
            } else {
                if (mRestoreLogic != null) result = mRestoreLogic.abort();
            }
        }

        return result;
    }

    public void accessForSMSManagement(boolean granted) {
        mDbg.trace();

        mHasMsgMgmtPermission = granted;

        if (mCableDisconnected) {
            mDbg.trace("Cable disconnected while waiting for default sms app permission");
            return;
        }

        if (null != mSession && mSession.isAbortPending()) {
            mDriverInstance.closeSession(mSession.getHandle(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    String msg = "Abort requested while waiting for SMS management permission request. Close status: " + result;
                    return onCloseSessionResponse(false, msg);
                }
            });
            return;
        }

        if (!granted) {
            // we should remove SMS category from session...
            mDbg.trace("SMS permission is denied! Removing sms processing from session");
            mSession.getSmartDataInfo().getCatCodes().remove(Byte.valueOf(MMPConstants.MMP_CATCODE_MESSAGE));
            mSession.getSmartDataInfo().getCatsFilesMap().remove(MMPConstants.MMP_CATCODE_MESSAGE);
        }

        // copy is a restore (from another phone).
        mRestoreLogic = new Restore(mSession);
        mRestoreLogic.start();
    }

    public boolean isSessionLive() {
        return (mSession != null);
    }

    public boolean onCableDisconnect() {
        mDbg.trace();

        mCableDisconnected = true;

        // simply call abort internally.
        return abort();
    }

    public double getEstimatedTime() {
        if (mSession == null) {
            return 0;
        }

        return mSession.getEstimatedTime();
    }

    /**
     * This is equivalent to onCableDisconnect. Duplicated for future purposes.
     *
     * @return result.
     */
    public boolean failOnCriticalError() {
        mDbg.trace();

        // Critical errors are treated as bad as this.
        mCableDisconnected = true;

        // simply call abort internally.
        return abort();
    }

    public void onSessionXfrStarted() {
        mDbg.trace();
        if (mSession != null) mSession.setDataXfrStatus(true);
    }

    public SessionType getSessionType() {
        return mSessionType;
    }

    public boolean isAbortPending() {
        if (mSession == null) {
            return true;
        }

        return mSession.isAbortPending();
    }

    private boolean onCloseSessionResponse(boolean sessionEndResult, String msg) {
        mDbg.trace();

        onSessionEnd(sessionEndResult, msg);
        return sessionEndResult;
    }

    // --------------------------------------------------------------------------
    // ----------------- Private functions --------------------------------------
    // --------------------------------------------------------------------------

    protected void onSessionEnd(boolean result, String message) {
        mDbg.trace();

        mDbg.trace(message);
        mHelper.notifySessionResult(result, message);

        if (mHasMsgMgmtPermission) {
            mHelper.dropSmsManagementPermission();
        }

        if (null == mSession) {
            mSessionType = NONE;
            return;
        }

        // This guy is a sync call.. hmm.. no big wait is expected anyway
        mSession.stopStats();

        mSession.setDataXfrStatus(false);
        mSession.setGlobalStatus(Session.Status.SESSION_ENDED);

        // Arun: AbortFix: 21May2015 & 01June2015 & 30July2015
        // This is needed mainly during cable disconnect when preparation is going on.
        // TODO: This is not fool proof. Shall we switch over to AsyncTask for this?
        if (false == mSession.getSessionPreperationCompletionStatus()) {
            if (mBackupLogic != null) {
                mDbg.trace("Session ended with errors. Force abort on backup logic");
                mBackupLogic.abort();
            }

            if (mRestoreLogic != null) {
                mDbg.trace("Session ended with errors. Force abort on restore logic");
                mRestoreLogic.abort();
            }
        }

        /*
         * Just being paranoid... huh? No. The session itself is progressing on
		 * more than one thread at times in parallel and at times in a
		 * sequential manner. Such threads may cause completion/error/status
		 * callback functions by posting events to UI thread So, unless we set
		 * this session as null here, there is no way we know the session is
		 * cancelled. Also note that all events are handled by single UI thread.
		 * So there is no question of race conditions with session information
		 * in main activity.
		 */
        mSession = null;
        mBackupLogic = null;
        mRestoreLogic = null;

        AppLocalData appLocalData = AppLocalData.getInstance();
        appLocalData.optUsingPrimaryStorage(false);

        mSessionType = NONE;
    }

    private SessionCatFilter prepareCatFilter(ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        SessionCatFilter catFilter = new SessionCatFilter();

        for (CategoryInfo catInfo : catInfos) {
            byte cat = catInfo.getmMmpCode();

            if (catInfo.getmBackupMode() == CategoryInfo.BackupMode.MIRROR) {
                if (MMLCategory.isGenericCategoryCode(cat)) {
                    catFilter.addGenMirrFilter(cat);
                } else {
                    catFilter.addSmartMirrFilter(cat);
                }
            } else if (catInfo.getmBackupMode() == CategoryInfo.BackupMode.PLUS) {
                if (MMLCategory.isGenericCategoryCode(cat)) {
                    catFilter.addGenPlusFilter(cat);
                    catFilter.addGenMirrFilter(cat);
                } else {
                    catFilter.addSmartPlusFilter(cat);
                    catFilter.addSmartMirrFilter(cat);
                }
            }
        }

        return catFilter;
    }

    // --------------------------------------------------------------------------
    // ------------- Start: Session statistics update helper interface ----------
    // --------------------------------------------------------------------------
    @Override
    public void resetStats() {
        mDbg.trace();
        mDriverInstance.resetXfrStats();
    }

    @Override
    public double getStats() {
        return mDriverInstance.getXfrStats();
    }

    public enum SessionType {NONE, BACKUP, RESTORE, COPY}

    // --------------------------------------------------------------------------
    // ------------- End: Session statistics update helper interface ----------
    // --------------------------------------------------------------------------

    /**
     * Must be implemented by an Activity (preferably MainActivity) as this has to deal with so many system conf and permissions.
     */
    public interface SessionManagementHelper {
        void requestSmsManagementPermission();
        void dropSmsManagementPermission();
        void notifyInsufficientCableStorage(long conSize, long msgSize, long calSize, long phoSize, long vidSize, long musSize, long docSize, long availableSize);
        void notifyInsufficientPhoneStorage();
        void updateSessionDataXfrProgress(long total, long sofar);
        void notifySessionResult(boolean result, String message);
        void updateMediaLibrary(String filePath);
    }
}

