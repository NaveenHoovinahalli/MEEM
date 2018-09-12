package com.meem.ui.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class MirroredTextView extends MeemTextView {
    public MirroredTextView(Context context) {
        super(context);
    }

    public MirroredTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    //for mirror text
 /*   @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(getWidth(), 0);
        canvas.scale(-1, 1);
        super.onDraw(canvas);
    }*/


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }


}
