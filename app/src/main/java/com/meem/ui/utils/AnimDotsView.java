package com.meem.ui.utils;

/**
 * Created by arun on 3/10/16.
 */

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.ui.GuiSpec;

public class AnimDotsView extends LinearLayout {
    private static final String TAG = AnimDotsView.class.getSimpleName();

    private static final int DEFAULT_NEUTRAL_COLOR = Color.parseColor("#000000");
    private static final int DEFAULT_BLINKING_COLOR = Color.parseColor("#87D300");

    private static final int OPT_NEUTRAL_COLOR = Color.parseColor("#535A5F"); //meemGray1
    private static final int OPT_BLINKING_COLOR = Color.parseColor("#87D300");

    private static final int DOT_COUNT = 3;
    private static final int DOT_RADIUS = (int) UiContext.getInstance().specToPix(GuiSpec.TYPE_WIDTH, GuiSpec.ANIM_DOT_WIDTH_AKA_RADIUS);
    private static final int DOT_HMARGIN = 5;

    private static final long DURATION = 500L;
    private static final Interpolator DOT_INTERPOLATOR = new AccelerateInterpolator(2.0f);

    protected CircleView[] dotViews;
    protected AnimatorSet animatorSet;

    protected boolean stop = false;

    protected int blinkingColor = DEFAULT_BLINKING_COLOR;
    protected int neutralColor = DEFAULT_NEUTRAL_COLOR;

    protected int dotCount = DOT_COUNT;
    protected int dotRadius = DOT_RADIUS;

    public AnimDotsView(Context context) {
        this(context, null);
    }

    public AnimDotsView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public AnimDotsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.v_animated_dots, this);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnimDotsView);
            try {
                dotRadius = a.getDimensionPixelSize(R.styleable.AnimDotsView_adv___dotRadius, DOT_RADIUS);
                dotCount = a.getInt(R.styleable.AnimDotsView_adv___dotCount, DOT_COUNT);
                blinkingColor = a.getColor(R.styleable.AnimDotsView_adv___dotBlinkingColor, DEFAULT_BLINKING_COLOR);
                neutralColor = a.getColor(R.styleable.AnimDotsView_adv___dotNeutralColor, DEFAULT_NEUTRAL_COLOR);
            } finally {
                a.recycle();
            }
        } else {
            dotRadius = DOT_RADIUS;
            dotCount = DOT_COUNT;
            blinkingColor = OPT_BLINKING_COLOR;
            neutralColor = OPT_NEUTRAL_COLOR;
        }

        setOrientation(HORIZONTAL);
        if (dotCount < 1 || dotCount > 10) {
            throw new IllegalArgumentException("The number of dot should be between [1, 10]");
        }
        addCircleViews();
    }

    private AnimatorSet prepareAnimators() {
        ObjectAnimator[] animators = new ObjectAnimator[dotCount];
        long d = DURATION;

        for (int i = dotCount - 1; i >= 0; i--) {
            animators[i] = createAnimator(dotViews[i], d);
        }

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(animators);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!stop) {
                    animatorSet.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return animatorSet;
    }

    private void addCircleViews() {
        UiContext uiCtxt = UiContext.getInstance();

        dotViews = new CircleView[dotCount];
        final Context context = getContext();
        for (int i = 0; i < dotCount; ++i) {
            dotViews[i] = new CircleView(context);

            dotViews[i].setRadius(dotRadius);
            dotViews[i].setColor(neutralColor);

            // TODO: Arun: If more spacing is needed between dots, add margin to LayoutParams here.
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int dotHorizMargin = (int) uiCtxt.specToPix(GuiSpec.TYPE_WIDTH, DOT_HMARGIN);
            llp.setMargins(dotHorizMargin, 0, dotHorizMargin, 0);

            addView(dotViews[i], i, llp);
        }
        animatorSet = prepareAnimators();
    }

    public void startAnimation() {
        stop = false;
        animatorSet.start();
    }

    public ObjectAnimator createAnimator(final CircleView v, long duration) {
        final ObjectAnimator animator = ObjectAnimator.ofObject(v, "color", new ArgbEvaluator(), neutralColor, blinkingColor);
        animator.setDuration(duration);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                v.setColor(neutralColor);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return animator;
    }

    public void stopAnimation() {
        stop = true;
        animatorSet.end();
    }

    public int getDotCount() {
        return dotCount;
    }

    public int getDotRadius() {
        return dotRadius;
    }

    public void setDotRadius(int radius) {
        dotRadius = radius;

        for (int i = 0; i < dotCount; ++i) {
            dotViews[i].setRadius(dotRadius);
        }

        invalidate();
    }

    public int getNeutralColor() {
        return neutralColor;
    }

    public boolean isStop() {
        return stop;
    }

    public int getBlinkingColor() {
        return blinkingColor;
    }
}
