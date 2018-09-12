package com.meem.ui.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by arun on 2/10/16.
 */

public class MeemHeaderTextView extends TextView {
    boolean mIsMirrorMode;

    public MeemHeaderTextView(Context context) {
        super(context);
        setFontProps();
    }

    public MeemHeaderTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFontProps();
    }

    public MeemHeaderTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFontProps();
    }

    private void setFontProps() {
        setTextColor(Color.rgb(77, 77, 77));
    }

    public void setMirrorMode(boolean isMirrorMode) {
        mIsMirrorMode = isMirrorMode;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsMirrorMode) {
//            canvas.translate(getWidth(), 0);
//            canvas.scale(-1, 1);
        }
        super.onDraw(canvas);
    }
}
