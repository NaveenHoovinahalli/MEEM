package com.meem.cablemodel;

import com.meem.androidapp.ProductSpecs;
import com.meem.utils.GenUtils;

import java.util.Map;

/**
 * It is very important to keep in mind that almost all functions of this class implementation uses events to communicate back with main
 * activity. This event model is implemented in all MMP handling classes.
 *
 * @author Arun T A
 */
public class MeemCable {
    private static final String tag = "MeemCable";
    private MeemConfig mConfig = new MeemConfig();

    public boolean createMCFG(String path) {
        dbgTrace();
        return mConfig.createMcfg(path);
    }

    public boolean updateStatus(String mstatPath) {
        dbgTrace();
        if (ProductSpecs.DUMMY_CABLE_MODE) {
            mConfig.mFwVersion = ProductSpecs.DUMMY_CABLE_MOED_FW_VERSION;
            mConfig.mName = "MEEM";
            mConfig.mUsage = new MeemUsage();
            return true;
        } else {
            return mConfig.update(mstatPath);
        }
    }

    public int getNumVaults() {
        dbgTrace();
        return mConfig.mVaults.size();
    }

    public MeemVault getVault(String upid) {
        dbgTrace();
        return mConfig.getVault(upid);
    }

    public Map<String, MeemVault> getVaults() {
        dbgTrace();
        return mConfig.mVaults;
    }

    public boolean addVault(MeemVault vault) {
        dbgTrace();

        mConfig.mVaults.put(vault.getUpid(), vault);
        return true;
    }

    public boolean removeVault(String upid) {
        if (mConfig.mVaults.containsKey(upid)) {
            mConfig.mVaults.remove(upid);
            return true;
        }

        return false;
    }

    public boolean updateAllVaultStatus() {
        dbgTrace();

        boolean ret = true;
        for (String upid : mConfig.mVaults.keySet()) {
            MeemVault vault = mConfig.getVault(upid);
            ret &= vault.update();
        }

        return ret;
    }

    public MeemUsage getUsageInfo() {
        // dbgTrace();
        return mConfig.getUsageInfo();
    }

    public String getName() {
        dbgTrace();
        return mConfig.mName;
    }

    public void setName(String name) {
        dbgTrace();
        mConfig.mOldName = mConfig.mName;
        mConfig.mName = name;
    }

    public void revertName() {
        mConfig.mName = mConfig.mOldName;
    }

    public String getFwVersion() {
        return mConfig.getFwVersion();
    }

    /*public boolean notifyAppQuit(NotifyAppQuit notifyAppQuit) {
        dbgTrace();

		// 23Sept2015: See commented out code and added check for mCoreHandler below.
		*//*		if (!mConnected) {
            dbgTrace("Cable not connected!");
			return false;
		}*//*

		if(null == mCoreHandler) {
			return false;
		}

		return mCoreHandler.addHandler(notifyAppQuit);
	}

	public boolean setChargingMode(byte mode) {
		dbgTrace();

		if (!mConnected) {
			dbgTrace("Cable not connected!");
			return false;
		}

		return mCoreHandler.addHandler(new SetChargingMode(mode));
	}

	public boolean getFirmwareLog(String path) {
		dbgTrace();

		if (!mConnected) {
			dbgTrace("Cable not connected!");
			return false;
		}

		return mCoreHandler.addHandler(new GetFirmwareLog(new MMPFPath(path, 0)));
	}

	public boolean toggleRDIS() {
		dbgTrace();

		if (!mConnected) {
			dbgTrace("Cable not connected!");
			return false;
		}

		return mCoreHandler.addHandler(new ToggleRNDIS((byte) 0));
	}

	public boolean getAuthDetails(ResponseCallback callback) {
		dbgTrace();

		if (!mConnected) {
			dbgTrace("Cable not connected!");
			return false;
		}

		return mCoreHandler.addHandler(new GetAuthDetails(callback));
	}

	public boolean doAuth(int method, String credential, ResponseCallback callback) {
		dbgTrace();

		if (!mConnected) {
			dbgTrace("Cable not connected!");
			return false;
		}

		return mCoreHandler.addHandler(new PerformAuthentication(method, credential, callback));
	}
	
	public boolean getPassword_HACK_FOR_FW(ResponseCallback callback) {
		dbgTrace();

		if (!mConnected) {
			dbgTrace("Cable not connected!");
			return false;
		}

		return mCoreHandler.addHandler(new GetPassword(callback));
	}*/

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MeemCable.log", trace);
    }

    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }
}
