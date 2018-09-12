package com.meem.ui.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Created by arun on 2/10/16.
 */

public class MeemFooterTextView extends TextView {
    public MeemFooterTextView(Context context) {
        super(context);
        setFont();
    }

    public MeemFooterTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFont();
    }

    public MeemFooterTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFont();
    }

    private void setFont() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/GMC-2.ttf");
        setTypeface(tf);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        setTextColor(Color.rgb(77, 77, 77));
    }
}
