/*
 * Copyright (C) 2017 BullyBoo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.bullyboo.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * by BullyBoo on 01.12.2017.
 */

public class CircleSeekBar extends FrameLayout {

    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_VALUE = 0;

    private float backgroundCircleLineWidth;
    private float progressCircleLineWidth;

    private int backgroundCircleLineColor;
    private int progressCircleLineColor;

    private float dotRadius;
    private int dotColor;
    private int dotImage;

    private int maxValue;
    private int minValue;
    private int value;

    private boolean showCounter;
    private boolean isClockwise;

    @StyleRes
    private int textAppearance;

    private TextView textView;

    private float width;
    private float height;

    private float centerX;
    private float centerY;

    private float baseCircleRadius;

    private float previousAngel;
    private float totalAngel;

    private Paint backgroundCirclePaint;
    private Paint progressCirclePaint;
    private Paint dotPaint;

    private RectF progressArc;

    // Line
    private float verticalLineWidth;

    public interface Callback{
        void onStartScrolling(int startValue);

        void onEndScrolling(int endValue);
    }

    public interface OnValueChangedListener{
        void onValueChanged(int value);
    }

    private Callback callback;

    private OnValueChangedListener onValueChangedListener;

    public CircleSeekBar(Context context) {
        this(context, null);
    }

    public CircleSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.circleProgressViewStyle);
    }

    public CircleSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.CircleProgressViewStyle);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CircleSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CircleSeekBar,
                defStyleRes,
                R.style.CircleProgressViewStyle);

        float dp = context.getResources().getDisplayMetrics().density;

        backgroundCircleLineWidth = array.getDimension(
                R.styleable.CircleSeekBar_backgroundCircleLineWidth, 2 * dp);
        progressCircleLineWidth = array.getDimension(
                R.styleable.CircleSeekBar_progressCircleLineWidth, 2 * dp);

        backgroundCircleLineColor = array.getColor(
                R.styleable.CircleSeekBar_backgroundCircleLineColor, backgroundCircleLineColor);
        progressCircleLineColor = array.getColor(
                R.styleable.CircleSeekBar_progressCircleLineColor, progressCircleLineColor);

        dotRadius = array.getDimension(
                R.styleable.CircleSeekBar_dotRadius, 20 * dp);
        dotColor = array.getColor(
                R.styleable.CircleSeekBar_dotColor, progressCircleLineColor);
        dotImage = array.getResourceId(
                R.styleable.CircleSeekBar_dotImage, dotImage);

        //Line
        verticalLineWidth = array.getDimension(
                R.styleable.CircleSeekBar_verticalLineWidth, 2 * dp);

        maxValue = array.getInt(
                R.styleable.CircleSeekBar_maxValue, DEFAULT_MAX_VALUE);
        minValue = array.getInt(
                R.styleable.CircleSeekBar_minValue, DEFAULT_MIN_VALUE);

        value = array.getInt(
                R.styleable.CircleSeekBar_value, DEFAULT_VALUE);

        textAppearance = array.getResourceId(R.styleable.CircleSeekBar_textAppearance,
                R.style.CircleProgressViewStyle_TextAppearance);

        showCounter = array.getBoolean(R.styleable.CircleSeekBar_showCounter, true);
        isClockwise = array.getBoolean(R.styleable.CircleSeekBar_isClockwise, true);

        array.recycle();

        if(minValue > maxValue){
            minValue = maxValue;
        }
        if(value > maxValue || value < minValue){
            value = minValue;
        }

        backgroundCirclePaint = new Paint();
        backgroundCirclePaint.setColor(backgroundCircleLineColor);
        backgroundCirclePaint.setStrokeWidth(backgroundCircleLineWidth);
        backgroundCirclePaint.setAntiAlias(true);
        backgroundCirclePaint.setStyle(Paint.Style.STROKE);

        progressCirclePaint = new Paint();
        progressCirclePaint.setColor(progressCircleLineColor);
        progressCirclePaint.setStrokeWidth(progressCircleLineWidth);
        progressCirclePaint.setAntiAlias(true);
        progressCirclePaint.setStyle(Paint.Style.STROKE);

        dotPaint = new Paint();
        dotPaint.setColor(dotColor);
        dotPaint.setAntiAlias(true);
        dotPaint.setStyle(Paint.Style.FILL);

        previousAngel = degrees(value);
        totalAngel = previousAngel;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(!showCounter){
            return;
        }

        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();

        final int childRight = getMeasuredWidth() - getPaddingRight();
        final int childBottom = getMeasuredHeight() - getPaddingBottom();

        final int childWidth = childRight - childLeft;
        final int childHeight = childBottom - childTop;

        if(textView == null){
            textView = new TextView(getContext());
            textView.setTextAppearance(getContext(), textAppearance);
            textView.setText(String.valueOf(value));
            textView.setTextColor(0xff000000);

            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;

            addView(textView, layoutParams);
        }

        textView.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST));

        int curWidth = textView.getMeasuredWidth();
        int curHeight = textView.getMeasuredHeight();

        int centerX = childWidth / 2;
        int centerY = childHeight / 2;

        textView.layout(centerX - curWidth / 2,
                centerY - curHeight / 2,
                centerX + curWidth / 2,
                centerY + curHeight / 2);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        width = w;
        height = h;

//        calculating radius
        baseCircleRadius = height > width ? width / 2 : height / 2;
        baseCircleRadius -= backgroundCircleLineWidth > progressCircleLineColor ?
                backgroundCircleLineWidth : progressCircleLineColor;
        baseCircleRadius -= dotRadius;

//        calculating centerX and centerY
        int paddingLeft;
        int paddingTop = getPaddingTop();
        int paddingRight;
        int paddingBottom = getPaddingBottom();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            paddingLeft = getPaddingLeft() != 0 ? getPaddingLeft() : getPaddingStart();
            paddingRight = getPaddingRight() != 0 ? getPaddingRight() : getPaddingEnd();
        } else {
            paddingLeft = getPaddingLeft();
            paddingRight = getPaddingRight();
        }

        float innerWidth = width - paddingLeft - paddingRight;
        centerX = getPaddingLeft()  + innerWidth / 2;
        float innerHeight = height - paddingTop - paddingBottom;
        centerY = getPaddingTop() + innerHeight / 2;

//        creating new rectF for drawing progress arc
        progressArc = new RectF();
        progressArc.set(centerX - baseCircleRadius,
                centerY - baseCircleRadius,
                centerX + baseCircleRadius,
                centerY + baseCircleRadius);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        drawBackgroundCircle(canvas);
        drawProgressArc(canvas);
        drawLine(canvas);
        drawDot(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }

    private void drawBackgroundCircle(Canvas canvas){
        canvas.drawCircle(centerX, centerY ,
                baseCircleRadius, backgroundCirclePaint);
    }

    private void drawProgressArc(Canvas canvas){
        if(isClockwise){
            canvas.drawArc(progressArc, 270, degrees(value),
                    false, progressCirclePaint);
        } else {
            canvas.drawArc(progressArc, 270, -(degrees(value)),
                    false, progressCirclePaint);
        }

    }

    private void drawDot(Canvas canvas){
        float degrees;

        if(isClockwise){
            degrees = degrees(value) - 95;
        } else {
            degrees = - degrees(value) - 95;
        }

        float x = (float) (Math.cos(Math.toRadians(degrees)) * baseCircleRadius + centerX);
        float y = (float) (Math.sin(Math.toRadians(degrees)) * baseCircleRadius + centerY);

        try {
            Drawable drawable = getResources().getDrawable(dotImage, null);
            VectorDrawable vectorDrawable = (VectorDrawable) drawable;
            vectorDrawable.setBounds(0, 0, 36, 36);
            canvas.translate(x - 15, y - 18);
            vectorDrawable.draw(canvas);
        } catch (Resources.NotFoundException ex) {
            Log.e("Resources","" + ex);
        } catch (Exception e) {
            Log.e("Exception","" + e);
        }
    }

    //Draw vertical line at 0 degree position
    private void drawLine(Canvas canvas){
        float degrees;
        degrees = (float) 262.2;
        float x = (float) (Math.cos(Math.toRadians(degrees)) * baseCircleRadius + centerX);
        float y = (float) (Math.sin(Math.toRadians(degrees)) * baseCircleRadius + centerY);
        Paint p = new Paint();
        p.setColor(progressCircleLineColor);
        p.setStrokeWidth(verticalLineWidth);
        canvas.drawLine(x+20, y+8, x+20, y-10, p);
    }

    private float degrees(float value){
        float oneDegree = 360 / (float) (maxValue - minValue);

        return (value - minValue) * oneDegree;
    }

    private int value(float degree){
        float oneDegree = 360 / (float) (maxValue - minValue);

        float tmp = degree / oneDegree;

        int result;

        if(tmp % 1 >= 0.5f){
            result = (int) (tmp + 1);
        } else {
            result = (int) tmp;
        }

        value = minValue + result;

        return value;
    }

    private float computeCos(float x, float y) {
        float width = x - this.width / 2;
        float height = y - this.height / 2;
        float slope = (float) Math.sqrt(width * width + height * height);
        return height / slope;
    }

    private boolean isOnArc(MotionEvent event){
        final float distance = distance(event.getX(), event.getY(),
                centerX, centerY);

        final float radius = progressArc.width() / 2f;

        final float halfStrokeWidth = dotRadius;

        return Math.abs(distance - radius) <= halfStrokeWidth;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public float getBackgroundCircleLineWidth() {
        return backgroundCircleLineWidth;
    }

    public void setBackgroundCircleLineWidth(float backgroundCircleLineWidth) {
        this.backgroundCircleLineWidth = backgroundCircleLineWidth;
        invalidate();
    }

    public float getProgressCircleLineWidth() {
        return progressCircleLineWidth;
    }

    public void setProgressCircleLineWidth(float progressCircleLineWidth) {
        this.progressCircleLineWidth = progressCircleLineWidth;
        invalidate();
    }

    public int getBackgroundCircleLineColor() {
        return backgroundCircleLineColor;
    }

    public void setBackgroundCircleLineColor(int backgroundCircleLineColor) {
        this.backgroundCircleLineColor = backgroundCircleLineColor;
        invalidate();
    }

    public int getProgressCircleLineColor() {
        return progressCircleLineColor;
    }

    public void setProgressCircleLineColor(int progressCircleLineColor) {
        this.progressCircleLineColor = progressCircleLineColor;
        invalidate();
    }

    public float getDotRadius() {
        return dotRadius;
    }

    public void setDotRadius(float dotRadius) {
        this.dotRadius = dotRadius;
        invalidate();
    }

    public int getDotColor() {
        return dotColor;
    }

    public void setDotColor(int dotColor) {
        this.dotColor = dotColor;
        invalidate();
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        invalidate();
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        invalidate();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        invalidate();

        if(textView != null){
            textView.setText(String.valueOf(value));
        }

        if(onValueChangedListener != null){
            onValueChangedListener.onValueChanged(value);
        }
    }

    public boolean isClockwise() {
        return isClockwise;
    }

    public void setClockwise(boolean clockwise) {
        isClockwise = clockwise;
    }

    public void setTextAppearance(int textAppearance) {
        this.textAppearance = textAppearance;

        if(textView != null){
            textView.setTextAppearance(getContext(), textAppearance);
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }
}