package com.meem.utils;

/**
 * Abstraction of MEEM's idea of versions.
 *
 * @author Arun T A
 */

public class VersionString {
    private static final int mMaxTokens = 4;
    private String mVersion;
    private int[] mVerNums = new int[mMaxTokens];

    public VersionString(String version) {
        mVersion = version;

        String[] toks = mVersion.split("\\.", mMaxTokens);
        for (int x = 0; x < mMaxTokens; x++) {
            try {
                mVerNums[x] = Integer.parseInt(toks[x]);
            } catch (NumberFormatException e) {
                mVerNums[x] = 0;
            }
        }
    }

    public final String getVersionString() {
        return mVersion;
    }

    public int major() {
        return mVerNums[0];
    }

    public int minor() {
        return mVerNums[1];
    }

    public int build() {
        return mVerNums[2];
    }

    public int trial() {
        return mVerNums[3];
    }

    public boolean isEqual(VersionString that) {
        if ((this.major() == that.major()) && (this.minor() == that.minor()) && (this.build() == that.build()) && (this.trial() == that.trial())) {
            return true;
        }
        return false;
    }

    // 04Sept2015: changing the logic of version string comparison to make it more or less human understandable.

	/*
        public boolean isGreaterThan(VersionString that) {
			boolean result;
			result = (this.major() > that.major()) ? true : false;
			if (!result)
				result = (this.minor() > that.minor()) ? true : false;
			if (!result)
				result = (this.build() > that.build()) ? true : false;
			if (!result)
				result = (this.trial() > that.trial()) ? true : false;

			return result;
		}
		
	*/

    public boolean isGreaterThan(VersionString that) {
        int result = 0;

        /**
         * logic: tri-state comparison for each version component from left to
         * right, continuing till last component if a version component under
         * check is equal. see compare function below.
         */
        result = compare(this.major(), that.major());
        switch (result) {
            case -1:
                return false;
            case 1:
                return true;
        }

        // 'major' component is equal. check next component 'minor'

        result = compare(this.minor(), that.minor());
        switch (result) {
            case -1:
                return false;
            case 1:
                return true;
        }

        // 'minor' component is also equal. check next component 'build'

        result = compare(this.build(), that.build());
        switch (result) {
            case -1:
                return false;
            case 1:
                return true;
        }

        // 'build' component is also equal. check next component 'trial'

        result = compare(this.trial(), that.trial());
        switch (result) {
            case -1:
                return false;
            case 1:
                return true;
        }

        // lol... all components are equal; so version strings are same.
        // caller should have checked for equality first.
        return false;
    }

    /**
     * tri-state comparison of two version components (like strcmp in C: -1 if less, 0 if equal, +1 if greater
     *
     * @param _this
     * @param _that
     *
     * @return
     */
    private int compare(int _this, int _that) {
        if (_this > _that) {
            return 1;
        } else if (_this == _that) {
            return 0;
        } else {
            return -1;
        }
    }

}
