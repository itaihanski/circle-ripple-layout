/*
 * Copyright (C) 2014 Itai Hanski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.itaihanski.circleripplelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;

import com.itaihanski.circleripplelayout.animation.BaseAnimationListener;
import com.itaihanski.circleripplelayout.animation.CustomAlphaAnimation;
import com.itaihanski.circleripplelayout.animation.CustomScaleAnimation;
import com.itaihanski.circleripplelayout.widget.CircleLayout;

/**
 * The {@code CircleRippleLayout} provides a wrapping circular layout for backwards compatible
 * Android Lollipop style circular ripple animation and behavior for FABs and circular buttons.
 *
 * @author Itai Hanski
 */
public class CircleRippleLayout extends FrameLayout {

    private static final int DEFAULT_RIPPLE_COLOR = 0x40ffffff;
    private static final float ZERO_SCALE = 0f;
    private static final float EXPANDED_SCALE = 3f;
    private static final float ZERO_ALPHA = 0f;
    private static final float FULL_ALPHA = 1f;
    private static final float CENTER_PIVOT = 0.5f;
    private static final long SLOW_SCALE_DURATION = 700l;
    private static final long SLOW_SCALE_OFFSET = 300l;
    private static final long SLOW_ALPHA_DURATION = 250l;
    private static final long FAST_SCALE_DURATION = 250l;
    private static final long FAST_ALPHA_DURATION = 150l;

    private View mRippleView;
    private View mRippleBgView;
    private ViewGroup mRippleViewContainer;
    private int mRippleSize;
    private int mPadding;
    private boolean mIsSlowScaleAnimating;
    private boolean mIsSlowAlphaAnimating;
    private boolean mHasStopped;
    private CustomScaleAnimation mSlowScaleAnimation;
    private CustomAlphaAnimation mSlowAlphaAnimation;
    private int mRippleColor;
    private int mRippleBgColor;
    private Interpolator mInterpolator;

    public CircleRippleLayout(Context context) {
        this(context, null, 0);
    }

    public CircleRippleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleRippleLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    //
    // Initialization
    //

    private void init(Context context, AttributeSet attrs) {
        mInterpolator = new LinearInterpolator();
        mRippleColor = mRippleBgColor = DEFAULT_RIPPLE_COLOR;

        // get attribute information
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleRippleLayout);
            mRippleColor = a.getColor(R.styleable.CircleRippleLayout_crl_rippleColor, mRippleColor);
            mRippleBgColor = a.getColor(R.styleable.CircleRippleLayout_crl_rippleBgColor, mRippleBgColor);
            mPadding = a.getDimensionPixelSize(R.styleable.CircleRippleLayout_crl_padding, 0);
            a.recycle();
        }

        // set a layout listener to get view size
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getWidth() != 0) {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    mRippleSize = getWidth();
                }
            }
        });

        initAnimations();
    }

    private void initAnimations() {
        // init the slow scale animation
        mSlowScaleAnimation = new CustomScaleAnimation(
                ZERO_SCALE, EXPANDED_SCALE,
                ZERO_SCALE, EXPANDED_SCALE,
                Animation.RELATIVE_TO_SELF, CENTER_PIVOT,
                Animation.RELATIVE_TO_SELF, CENTER_PIVOT);
        mSlowScaleAnimation.setInterpolator(mInterpolator);
        mSlowScaleAnimation.setDuration(SLOW_SCALE_DURATION);
        mSlowScaleAnimation.setStartOffset(SLOW_SCALE_OFFSET);
        mSlowScaleAnimation.setAnimationListener(new BaseAnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mRippleView.setVisibility(VISIBLE);
                mIsSlowScaleAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsSlowScaleAnimating = false;

                // reset the ripple view only if animation finished
                if (!mSlowScaleAnimation.isCanceled()) {
                    LayoutParams lp = (LayoutParams) mRippleView.getLayoutParams();
                    lp.leftMargin = 0;
                    lp.topMargin = 0;
                    mRippleView.requestLayout();
                }
            }
        });

        // init the slow alpha animation
        mSlowAlphaAnimation = new CustomAlphaAnimation(ZERO_ALPHA, FULL_ALPHA);
        mSlowAlphaAnimation.setInterpolator(mInterpolator);
        mSlowAlphaAnimation.setDuration(SLOW_ALPHA_DURATION);
        mSlowAlphaAnimation.setAnimationListener(new BaseAnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mRippleBgView.setVisibility(VISIBLE);
                mIsSlowAlphaAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsSlowAlphaAnimating = false;
            }
        });
    }

    //
    // Override Touch Events
    //

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        // ignore touch events when disabled or not clickable
        if (isEnabled() & isClickable()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mHasStopped = false;
                    addRippleViews();

                    // set layout according to touch event
                    LayoutParams layoutParams = (LayoutParams) mRippleView.getLayoutParams();
                    layoutParams.width = layoutParams.height = mRippleSize;

                    // set the margin for half the touch location (radius)
                    layoutParams.leftMargin = (int) (event.getX() - (layoutParams.width / 2));
                    layoutParams.topMargin = (int) (event.getY() - (layoutParams.height / 2));

                    // request layout after changes
                    mRippleView.requestLayout();

                    // start the animation
                    mRippleView.startAnimation(mSlowScaleAnimation);
                    mRippleBgView.startAnimation(mSlowAlphaAnimation);
                    break;

                case MotionEvent.ACTION_MOVE:
                    // ignore if already stopped
                    if (mHasStopped) {
                        break;
                    }

                    // cancel animation if touch moved outside view
                    if (event.getX() < 0 || event.getY() < 0
                            || event.getX() > getWidth()
                            || event.getY() > getHeight()) {
                        stopAnimations();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_CANCEL:
                    // ignore if already stopped
                    if (mHasStopped) {
                        break;
                    }

                    stopAnimations();
            }
        }
        return super.onTouchEvent(event);
    }

    //
    // Animations Stop
    //

    private void stopAnimations() {
        mHasStopped = true;
        // keep references to the current views since they will be replaced
        final View rippleView = mRippleView;
        final View rippleViewContainer = mRippleViewContainer;
        final View rippleBgView = mRippleBgView;

        // setup ripple fade out animation
        final Animation rippleFadeOut = createFastAlphaAnimation();
        rippleFadeOut.setAnimationListener(new BaseAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                removeView(rippleViewContainer);
            }
        });

        // setup ripple bg fade out animation
        final Animation rippleBgFadeOut = createFastAlphaAnimation();
        rippleBgFadeOut.setAnimationListener(new BaseAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                removeView(rippleBgView);
            }
        });

        // cancel slow scaling if still animating
        if (mIsSlowScaleAnimating) {
            mSlowScaleAnimation.cancel();
            mSlowScaleAnimation.reset();

            // start a fast scale animation from the point we left off
            float startingSize = mSlowScaleAnimation.getLastInterpolatedTime() * EXPANDED_SCALE;
            ScaleAnimation fastScale = new ScaleAnimation(
                    startingSize, EXPANDED_SCALE,
                    startingSize, EXPANDED_SCALE,
                    Animation.RELATIVE_TO_SELF, CENTER_PIVOT,
                    Animation.RELATIVE_TO_SELF, CENTER_PIVOT);
            fastScale.setInterpolator(mInterpolator);
            fastScale.setDuration(FAST_SCALE_DURATION);
            fastScale.setAnimationListener(new BaseAnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    LayoutParams lp = (LayoutParams) rippleView.getLayoutParams();
                    lp.leftMargin = 0;
                    lp.topMargin = 0;
                    rippleView.requestLayout();
                    rippleView.startAnimation(rippleFadeOut);
                }
            });
            rippleView.startAnimation(fastScale);

            // override the bg fade out to be in sync with the scale animation
            rippleBgFadeOut.setDuration(FAST_SCALE_DURATION);

            // cancel the slow alpha animation if needed
            if (mIsSlowAlphaAnimating) {
                mSlowAlphaAnimation.cancel();
                mSlowAlphaAnimation.reset();

                // start a fast alpha animation from the point we left off
                AlphaAnimation fastAlpha = new AlphaAnimation(mSlowAlphaAnimation.getLastInterpolatedTime(), FULL_ALPHA);
                fastAlpha.setInterpolator(mInterpolator);
                fastAlpha.setDuration(FAST_ALPHA_DURATION);
                fastAlpha.setAnimationListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        rippleBgView.startAnimation(rippleBgFadeOut);
                    }
                });
                rippleBgView.startAnimation(fastAlpha);
            } else {
                rippleBgView.startAnimation(rippleBgFadeOut);
            }
        } else {
            rippleView.startAnimation(rippleFadeOut);
            rippleBgView.startAnimation(rippleBgFadeOut);
        }
    }

    private Animation createFastAlphaAnimation() {
        AlphaAnimation animation = new AlphaAnimation(FULL_ALPHA, ZERO_ALPHA);
        animation.setInterpolator(mInterpolator);
        animation.setDuration(FAST_ALPHA_DURATION);
        return animation;
    }

    //
    // Utility Methods
    //

    private void addRippleViews() {
        // create new ripple views
        mRippleView = initRippleView(mRippleColor);
        mRippleBgView = initRippleView(mRippleBgColor);

        // wrap the ripple view in a circle layout to correctly clip the circle when expanding
        mRippleViewContainer = new CircleLayout(getContext());
        mRippleViewContainer.addView(mRippleView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        // add ripple and ripple bg views
        addRippleView(mRippleViewContainer);
        addRippleView(mRippleBgView);
    }

    private View initRippleView(int color) {
        View view = new View(getContext());
        view.setBackgroundResource(R.drawable.circle);
        ((GradientDrawable) view.getBackground()).setColor(color);
        view.setVisibility(INVISIBLE);
        return view;
    }

    private void addRippleView(View view) {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(mPadding, mPadding, mPadding, mPadding);
        addView(view, 0, layoutParams);
    }
}
