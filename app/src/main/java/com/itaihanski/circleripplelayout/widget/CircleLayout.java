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
package com.itaihanski.circleripplelayout.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A simple layout that clips its contents to a circle shape.
 *
 * @author Itai Hanski
 */
public class CircleLayout extends FrameLayout {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private RectF mBounds;

    public CircleLayout(Context context) {
        super(context);
    }

    public CircleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw && h != oldh) {
            mBounds = new RectF(0, 0, w, h);
            mBitmap = Bitmap.createBitmap((int) mBounds.width(), (int) mBounds.height(), Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        // clear the canvas and save draw a new one one the bitmap
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        super.dispatchDraw(mCanvas);

        BitmapShader shader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        canvas.drawCircle(mBounds.centerX(), mBounds.centerY(), mBounds.width() / 2, paint);
    }
}
