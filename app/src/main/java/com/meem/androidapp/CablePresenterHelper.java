package com.meem.androidapp;

import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.util.ArrayList;

/**
 * Created by arun on 7/11/16.
 * <p>
 * Must be implemented by MainActivity - since it has so many user interactions and activity related permissions, context usages and the
 * like. Else this would have been done entirely by CableDriver.
 */

public interface CablePresenterHelper {
    void onDisconnectedStateUiFinished();

    void onAutoBackupCountDownEnd();
    void onAutoBackupCountDownUiFinish();

    void onVirginCablePinSetupComplete();
    void onUnregisteredPhoneAuthComplete();

    void startBackup(VaultInfo vaultInfo, ArrayList<CategoryInfo> cats);
    void startRestore(VaultInfo vaultInfo, ArrayList<CategoryInfo> cats);
    void startCopy(VaultInfo vaultInfo, ArrayList<CategoryInfo> cats);
    void abortSession();

    // This is for category backup modes changed by fragments like out of memory management fragment
    void onCatModeChanged(VaultInfo vaultInfo);

    void onSoundUpdate(boolean isOn);
    void onCriticalError(String msg);

    // Arun: 23April2018: To refresh the vault names when it is changed in settings
    void onVaultNameChanged(VaultInfo vaultInfo);
}
