package com.meem.ui;

/**
 * The values here are based on spec version rev_27, based on XXHDPI 1080 pixels screen width and 1845 pixels screen height. You must also
 * see the UiContext class method specToPix() where necessary percentage calculations are done based upon running device's screen
 * properties.
 * <p/>
 * Obviously, these constants are only to be used in code - never in layouts.
 * <p/>
 * Note: "vault" means either phone avatar view OR meem avatar view OR meem views.
 *
 * @author arun
 */
public class GuiSpec {
    public static final int TYPE_WIDTH = 0;
    public static final int TYPE_HEIGHT = 1;
    public static final int TYPE_FONTSIZE = 2;

    public static final float PHONE_SIDE_WIDTH = 531f;
    public static final float MEEM_SIDE_WIDTH = 531f;
    public static final float MEMORY_TUBE_WIDTH = 18f;

    public static final float VAULT_WIDTH = 501f;
    public static final float VAULT_HEIGHT = 351f;
    public static final float VAULT_HEADER_HEIGHT = 273f;
    public static final float VAULT_FOOTER_HEIGHT = 78f; // = 111 - 33;
    public static final float VAULT_RIGHT_PADDING = 15f;
    public static final float VAULT_LEFT_PADDING = 15f;

    /**
     * vault header icon is the anchor for other items in header
     */
    public static final float VAULT_HEADER_ICON_ALL_MARGIN = 36f;
    public static final float VAULT_HEADER_NAME_TOP_MARGIN = 64f;

    public static final float CATEGORY_LIST_HEIGHT = 150f;
    public static final float CATEGORY_LIST_DIVIDER_HEIGHT = 1f;
    public static final float CATEGORY_LIST_PHONE_SIDE_RIGHT_PADDING = 36f;
    public static final float CATEGORY_LIST_MEEM_SIDE_LEFT_PADDING = 36f;

    public static final long ANIM_TEST_DURATION_FAST = 500;
    public static final long ANIM_TEST_DURATION_SLOW = 1000;

    // Should use TYPE_WIDTH. Note: 12pix is 4dip in XXHDPI (which was found good looking by trials).
    public static final int ANIM_DOT_WIDTH_AKA_RADIUS = 12;

    public static int TIME_DELAY_TIMESTAMP_CHANGE = 300000;

}
