package com.meem.cablemodel;

import android.annotation.SuppressLint;

import com.meem.utils.GenUtils;

import java.util.HashMap;

/**
 * @author Arun T A
 *         <p/>
 *         MSTAT gives data size in MB, VSTAT GenericData in KB and SmartData in bytes! To make life of UI developer easier, all size getter
 *         functions of back-end classes will return values in KiloBytes.
 */

public class VaultUsage {
    private static final String tag = "VaultUsage";
    HashMap<Byte, UsageInfo> mSmartCatUsage;
    HashMap<Byte, UsageInfo> mGenCatUsage;

    @SuppressLint("UseSparseArrays")
    public VaultUsage() {
        mSmartCatUsage = new HashMap<Byte, UsageInfo>();
        mGenCatUsage = new HashMap<Byte, UsageInfo>();
    }

    // NOTE: Firmware gives smart data size in bytes... but generic data usage
    // in kb - oh well :/
    public boolean setSmartCatUsage(byte cat, long mirrSize, long plusSize) {
        Byte key = Byte.valueOf(cat);
        UsageInfo use;

        if (mSmartCatUsage.containsKey(key)) {
            use = mSmartCatUsage.get(key);
        } else {
            use = new UsageInfo();
        }

        // If we keep smart data in KB, when there is very little smart data,
        // the business logic will fail as division by 1024 will make it 0. So,
        // some care is taken to manage that scenario. Note that there will be minor
        // issue with GUI usage screen - as VaultUsage class is always assumed to
        // return data size in KB. But its impact is negligible.
        if (mirrSize >= 1024) {
            mirrSize = mirrSize / 1024;
        } else {
            mirrSize = (mirrSize == 0) ? 0 : 1;
        }

        if (plusSize >= 1024) {
            plusSize = plusSize / 1024;
        } else {
            plusSize = (plusSize == 0) ? 0 : 1;
        }

        use.mMirror = mirrSize;
        use.mPlus = plusSize;

        mSmartCatUsage.put(key, use);

        return true;
    }

    public UsageInfo getSmartUsageInfo(byte cat) {
        UsageInfo use = null;
        Byte key = Byte.valueOf(cat);

        if (mSmartCatUsage.containsKey(key)) {
            use = mSmartCatUsage.get(key);
        }

        if (use == null) {
            use = new UsageInfo();
        }

        return use;
    }

    public boolean setGenCatUsage(byte cat, long mirrSize, long plusSize) {
        Byte key = Byte.valueOf(cat);
        UsageInfo use;

        if (mGenCatUsage.containsKey(key)) {
            use = mGenCatUsage.get(key);
        } else {
            use = new UsageInfo();
        }

        use.mMirror = mirrSize;
        use.mPlus = plusSize;

        mGenCatUsage.put(key, use);

        return true;
    }

    public UsageInfo getGenUsageInfo(byte cat) {
        UsageInfo use = null;
        Byte key = Byte.valueOf(cat);

        if (mGenCatUsage.containsKey(key)) {
            use = mGenCatUsage.get(key);
        }

        if (use == null) {
            use = new UsageInfo();
        }

        return use;
    }

    public long getTotalMirrorDataUsage() {
        long total = 0;
        long part_1 = 0;
        long part_2 = 0;

        for (UsageInfo info : mSmartCatUsage.values()) {
            total += info.mMirror;
            part_1 += info.mMirror;
        }

        dbgTrace("Total Smart data: " + part_1);

        for (UsageInfo info : mGenCatUsage.values()) {
            total += info.mMirror;
            part_2 += info.mMirror;
        }

        dbgTrace("Total Gen data: " + part_2);

        return total;
    }

    public long getTotalPlusDataUsage() {
        long total = 0;
        long part_1 = 0;
        long part_2 = 0;

        for (UsageInfo info : mSmartCatUsage.values()) {
            total += info.mPlus;
            part_1 += info.mPlus;
        }

        dbgTrace("Total SmartPlus data: " + part_1);

        for (UsageInfo info : mGenCatUsage.values()) {
            total += info.mPlus;
            part_2 += info.mPlus;
        }

        dbgTrace("Total GenPlus data: " + part_2);

        return total;
    }

    /**
     * Mirror + mirror plus size
     *
     * @return
     */
    public long getTotalSmartCatDataUsage(byte cat) {
        UsageInfo uinfo = getSmartUsageInfo(cat);
        long size = uinfo.getMirrorSize();
        size += uinfo.getPlusSize();
        return size;
    }

    /**
     * Mirror + mirror plus size
     *
     * @return
     */
    public long getTotalGenCatDataUsage(byte cat) {
        UsageInfo uinfo = getGenUsageInfo(cat);
        long size = uinfo.getMirrorSize();
        size += uinfo.getPlusSize();
        return size;
    }

    public void reset() {
        mGenCatUsage.clear();
        mSmartCatUsage.clear();
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile(tag + ".log", trace);
    }

    // debug support
    @SuppressWarnings("unused")
    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }

    public class UsageInfo {
        long mMirror;
        long mPlus;

        public UsageInfo() {
            mMirror = 0;
            mPlus = 0;
        }

        public long getMirrorSize() {
            return mMirror;
        }

        public long getPlusSize() {
            return mPlus;
        }
    }
}
