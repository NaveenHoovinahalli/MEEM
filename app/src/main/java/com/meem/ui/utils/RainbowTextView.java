package com.meem.ui.utils;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.widget.TextView;

/**
 * https://chiuki.github.io/android-shaders-filters
 */
public class RainbowTextView extends TextView {
    public RainbowTextView(Context context) {
        super(context);
    }

	/* add more ctors */

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int[] rainbow = getRainbowColors();
        Shader shader = new LinearGradient(0, 0, 0, w, rainbow, null, Shader.TileMode.MIRROR);

        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        shader.setLocalMatrix(matrix);

        getPaint().setShader(shader);
    }

    private int[] getRainbowColors() {
        return new int[]{
        /*getResources().getColor(R.color.rainbow_red),
        getResources().getColor(R.color.rainbow_yellow),
		getResources().getColor(R.color.rainbow_green),
		getResources().getColor(R.color.rainbow_blue),
		getResources().getColor(R.color.rainbow_purple)*/};
    }
}

// Must read: http://stackoverflow.com/questions/4381033/multi-gradient-shapes
// Also: http://stackoverflow.com/questions/24070875/android-textview-with-gradient-and-stroke
// Also: http://www.techiecommunity.net/Android/Android-Gradient-Image-Merge-Example
// Must read: https://sriramramani.wordpress.com/2013/09/25/yo-zuck-fix-this/ 

/*
private void FillCustomGradient(View v) {
final View view = v;
Drawable[] layers = new Drawable[1];

ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
    @Override
    public Shader resize(int width, int height) {
        LinearGradient lg = new LinearGradient(
                0,
                0,
                0,
                view.getHeight(),
                new int[] {
                         getResources().getColor(R.color.color1), // please input your color from resource for color-4
                         getResources().getColor(R.color.color2),
                         getResources().getColor(R.color.color3),
                         getResources().getColor(R.color.color4)},
                new float[] { 0, 0.49f, 0.50f, 1 },
                Shader.TileMode.CLAMP);
        return lg;
    }
};
PaintDrawable p = new PaintDrawable();
p.setShape(new RectShape());
p.setShaderFactory(sf);
p.setCornerRadii(new float[] { 5, 5, 5, 5, 0, 0, 0, 0 });
layers[0] = (Drawable) p;

LayerDrawable composite = new LayerDrawable(layers);
view.setBackgroundDrawable(composite);
}
*/