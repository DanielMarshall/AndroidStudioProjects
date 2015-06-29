/*
 * Copyright (C) 2014 The Android Open Source Project
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

package au.gov.moneysmart.wearable.demo.moneysmartnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class MoneySmart extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
//     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        Calendar mCalendar;
//        Bitmap mBackgroundBitmap;
//        Bitmap mBackgroundScaledBitmap;
        Paint mBlackPaint;
        Paint mWhitePaint;
        Paint mRedPaint;
        Paint mBlackBorderPaint;
        Paint mBackgroundPaint;
        Paint mBorderPaint;
        Paint mTickPaint;
        int numTicks = 8;
        GradientDrawable mBackgroundGradient;
//        Paint mHighlightPaint;
        Paint mTextPaint;

        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone( TimeZone.getDefault() );
                invalidate();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            /* rest is to imitialise custom watch face */

            Resources resources = MoneySmart.this.getResources();

//            Drawable backgroundDrawable = resources.getDrawable( R.drawable.bg, null );
//            mBackgroundBitmap = ( ( BitmapDrawable ) backgroundDrawable ).getBitmap();

            mBlackPaint = new Paint();
            mBlackPaint.setColor(resources.getColor(R.color.moneysmart_black));
            mBlackPaint.setStrokeWidth(5.0f);
            mBlackPaint.setAntiAlias(true);
            mBlackPaint.setStrokeCap(Paint.Cap.ROUND);

            mWhitePaint = new Paint();
            mWhitePaint.setColor(resources.getColor(R.color.moneysmart_white));
            mWhitePaint.setStrokeWidth(5.0f);
            mWhitePaint.setAntiAlias(true);
            mWhitePaint.setStrokeCap(Paint.Cap.ROUND);

            mRedPaint = new Paint();
            mRedPaint.setColor(resources.getColor(R.color.moneysmart_red));
            mRedPaint.setStrokeWidth(5.0f);
            mRedPaint.setAntiAlias(true);
            mRedPaint.setStrokeCap(Paint.Cap.ROUND);

            mBlackBorderPaint = new Paint();
            mBlackBorderPaint.setColor(resources.getColor(R.color.moneysmart_black));
            mBlackBorderPaint.setStrokeWidth(resources.getDimension(R.dimen.compass_border_width));
            mBlackBorderPaint.setStyle(Paint.Style.STROKE);
            mBlackBorderPaint.setAntiAlias(true);
            mBlackBorderPaint.setStrokeCap(Paint.Cap.ROUND);

            mBorderPaint = new Paint();
            mBorderPaint.setColor(resources.getColor(R.color.border_colour));
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setStrokeWidth(resources.getDimension(R.dimen.compass_border_width));
            mBorderPaint.setAntiAlias(true);

            mTickPaint = new Paint();
            mTickPaint.setColor(resources.getColor(R.color.tick_colour));
            mTickPaint.setStyle(Paint.Style.STROKE);
            mTickPaint.setStrokeWidth(resources.getDimension(R.dimen.compass_tick_width));
            mTickPaint.setAntiAlias(true);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.moneysmart_green));

            mTextPaint = new Paint();
            mTextPaint.setColor(resources.getColor(R.color.moneysmart_darkgreen));
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(resources.getDimension(R.dimen.text_size));
            Typeface tf = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(tf);

            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            /* the time changed */
            super.onTimeTick();
            /* NOTE: Invalidate is similar to View.invalidate() and causes the watchface to be redrawn. Can only be used in main UI thread, from another thread, use postInvalidate() */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if ( mLowBitAmbient ) {
                mBlackPaint.setAntiAlias(!inAmbientMode);
                mWhitePaint.setAntiAlias(!inAmbientMode);
                mBlackBorderPaint.setAntiAlias(!inAmbientMode);
                mRedPaint.setAntiAlias(!inAmbientMode);
                mBackgroundPaint.setAntiAlias(!inAmbientMode);
                mTextPaint.setAntiAlias(!inAmbientMode);
                mTickPaint.setAntiAlias(!inAmbientMode);
            }

            invalidate();

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Update the time
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            // Constant to help calculate clock hand rotations
            final float TWO_PI = (float) Math.PI * 2f;

            float mPadding = getResources().getDimension(R.dimen.watch_padding);
            float mFontSize = getResources().getDimension(R.dimen.text_size);
            float width = (float)bounds.width() - 2*mPadding;
            float height = (float)bounds.height() - 2*mPadding;

            // Find the center.
            float centerX = width/2f + mPadding;
            float centerY = height/2f + mPadding;

            /*
            mBackgroundGradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{0xFFFF0000,0xFF00FF,0xF0000FF});
//            mBackgroundGradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{getResources().getColor(R.color.moneysmart_light), getResources().getColor(R.color.moneysmart_darkgreen), getResources().getColor(R.color.moneysmart_green) });
            mBackgroundGradient.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            mBackgroundGradient.setGradientRadius(centerX);
            mBackgroundGradient.setGradientCenter(centerX, centerY);
            mBackgroundGradient.setShape(GradientDrawable.OVAL);
//            mBackgroundGradient.setStroke(R.dimen.compass_border_width,R.color.compass_border);
            mBackgroundGradient.setDither(true);
            mBackgroundGradient.setBounds(bounds);

            mBackgroundGradient.draw( canvas );
            */

            // Draw in radial gradient circle as watchface background
            RadialGradient gradient = new RadialGradient( centerX * 3/4, centerY * 3/4, centerX, new int[]{getResources().getColor(R.color.moneysmart_light), getResources().getColor(R.color.moneysmart_green), getResources().getColor(R.color.moneysmart_green) }, new float[] { 0.01f,0.9f,0.99f}, Shader.TileMode.CLAMP);
            mBackgroundPaint.setShader(gradient);
            canvas.drawCircle(centerX, centerY, width / 2, mBackgroundPaint);

            // Add ticks
            float innerTickRadius = centerX - mPadding - getResources().getDimension( R.dimen.compass_tick_depth );
            float outerTickRadius = centerX - mPadding;

            for (int tickIndex = 0; tickIndex < numTicks; tickIndex++) {
                float tickRot = tickIndex * TWO_PI / numTicks;
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, mTickPaint);
            }

            // add border
            canvas.drawCircle(centerX, centerY, width / 2, mBorderPaint);

            // draw letters

            Path path = new Path();
            path.moveTo(mPadding, mPadding);
            path.lineTo(width, mPadding);

            mTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawTextOnPath("N", path, 0, mFontSize, mTextPaint);

            canvas.drawText("E", mPadding + width - mFontSize/2f, mPadding + ( height + mFontSize ) / 2f, mTextPaint);

            path.reset();
            path.moveTo(mPadding, mPadding + height -mFontSize/2f);
            path.lineTo(mPadding + width, mPadding + height -mFontSize/2f);
            canvas.drawTextOnPath("S", path, 0, 0, mTextPaint);

            canvas.drawText("W", mPadding +mFontSize/2f, mPadding + ( height + mFontSize ) / 2f, mTextPaint);

            // Compute rotations and lengths for the clock hands.
            float seconds = mCalendar.get(Calendar.SECOND) +
                    mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float secRot = seconds / 60f * TWO_PI;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float minRot = minutes / 60f * TWO_PI;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            float hrRot = hours / 12f * TWO_PI;

            float secLength = centerX * 5/6f;
            float minLength = centerX * 4/6f;
            float hrLength = centerX * 3/6f;
            // override
            hrLength = minLength;
            float baseLength = centerX * 1/6f;

            // Only draw the second hand in interactive mode.
            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mRedPaint);
            }

            // Draw the minute and hour hands.
            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            float innerMinX = (float) Math.sin(minRot) * baseLength;
            float innerMinY = (float) -Math.cos(minRot) * baseLength;
            // rotate innerMinX/Y both 90 degrees clockwise and counter clockwise to get other 2 triangle corner points
            float newX1 = centerX - innerMinY;
            float newY1 = centerY + innerMinX;
            float newX2 = centerX + innerMinY;
            float newY2 = centerY - innerMinX;

            path.reset();
            path.moveTo(centerX + minX, centerY + minY);
            path.lineTo(newX1, newY1);
            path.lineTo(newX2, newY2);
            path.lineTo(centerX + minX, centerY + minY);

            canvas.drawPath(path, mWhitePaint);
            canvas.drawPath(path, mBlackBorderPaint);

//            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mWhitePaint);
            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            float innerHrX = (float) Math.sin(hrRot) * baseLength;
            float innerHrY = (float) -Math.cos(hrRot) * baseLength;
            float newX1h = centerX - innerHrY;
            float newY1h = centerY + innerHrX;
            float newX2h = centerX + innerHrY;
            float newY2h = centerY - innerHrX;

            path.reset();
            path.moveTo(centerX + hrX, centerY + hrY);
            path.lineTo(newX1h, newY1h);
            path.lineTo(newX2h, newY2h);
            path.lineTo(centerX + hrX, centerY + hrY);

            canvas.drawPath(path, mBlackPaint);

//            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mBlackPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone( TimeZone.getDefault() );
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MoneySmart.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MoneySmart.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}
