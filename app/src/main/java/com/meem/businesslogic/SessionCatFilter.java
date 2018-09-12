package com.meem.businesslogic;

import java.util.ArrayList;

/**
 * If user initiate individual category operations by ListView gestures, an instance of this object is created with the creation of a new
 * session and corresponding category codes will be added to this object.
 * <p/>
 * Once this happens, whatever be the real vault category status (mirror, plus or disabled), the final SESD will contain only those
 * categories that are added to this filter.
 *
 * @author Arun T A
 */

public class SessionCatFilter {
    ArrayList<Byte> mGenMirrCatCodes, mSmartMirrCatCodes, mGenPlusCatCodes, mSmartPlusCatCodes;

    public SessionCatFilter() {
        mGenMirrCatCodes = new ArrayList<Byte>();
        mGenPlusCatCodes = new ArrayList<Byte>();

        mSmartMirrCatCodes = new ArrayList<Byte>();
        mSmartPlusCatCodes = new ArrayList<Byte>();
    }

    public void addGenMirrFilter(Byte cat) {
        if (!mGenMirrCatCodes.contains(cat)) {
            mGenMirrCatCodes.add(cat);
        }
    }

    public void addGenPlusFilter(Byte cat) {
        if (!mGenPlusCatCodes.contains(cat)) {
            mGenPlusCatCodes.add(cat);
        }
    }

    public void addSmartMirrFilter(Byte cat) {
        if (!mSmartMirrCatCodes.contains(cat)) {
            mSmartMirrCatCodes.add(cat);
        }
    }

    public void addSmartPlusFilter(Byte cat) {
        if (!mSmartPlusCatCodes.contains(cat)) {
            mSmartPlusCatCodes.add(cat);
        }
    }

    public boolean containsGenMirrCat(byte cat) {
        return mGenMirrCatCodes.contains(Byte.valueOf(cat));
    }

    public boolean containsGenPlusCat(byte cat) {
        return mGenPlusCatCodes.contains(Byte.valueOf(cat));
    }

    public boolean containsSmartMirrCat(byte cat) {
        return mSmartMirrCatCodes.contains(Byte.valueOf(cat));
    }

    public boolean containsSmartPlusCat(byte cat) {
        return mSmartPlusCatCodes.contains(Byte.valueOf(cat));
    }

    public void applySmartCatFilter(ArrayList<Byte> smartCats) {
        filter(mSmartMirrCatCodes, smartCats);
    }

    public void applySmartPlusCatFilter(ArrayList<Byte> smartPlusCats) {
        filter(mSmartPlusCatCodes, smartPlusCats);
    }

    public void applyGenCatFilter(ArrayList<Byte> genCats) {
        filter(mGenMirrCatCodes, genCats);
    }

    public void applyGenPlusCatFilter(ArrayList<Byte> genPlusCats) {
        filter(mGenPlusCatCodes, genPlusCats);
    }

    private void filter(ArrayList<Byte> filter, ArrayList<Byte> target) {
        // here, filter action is just copying the filter categories to target
        // (that is, whatever be the enabled categories in the vault, ignore all
        // of it and make the categories in filter as the
        target.clear();
        for (Byte cat : filter) {
            target.add(cat);
        }
    }

    public ArrayList<Byte> getAllCatsInfilter() {
        ArrayList<Byte> cats = new ArrayList<Byte>();

        for (Byte cat : mGenMirrCatCodes) {
            cats.add(cat);
        }

        for (Byte cat : mSmartMirrCatCodes) {
            cats.add(cat);
        }

        return cats;
    }
}
