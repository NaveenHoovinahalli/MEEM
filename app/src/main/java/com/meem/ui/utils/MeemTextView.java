package com.meem.ui.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by SCS on 7/22/2016.
 */
public class MeemTextView extends TextView {
    public MeemTextView(Context context) {
        super(context);
        setFont();
    }

    public MeemTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFont();
    }

    public MeemTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFont();
    }

    private void setFont() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/DM.ttf");
        setTypeface(tf);
    }
}
