package com.example.radha.bitmapprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by radha on 28/8/15.
 */
public class FrameView extends View {

    private Bitmap mBitmap;
    private int mScreenHeight;
    private int mScreenWidth;

    public FrameView(Context context) {
       super(context);
        init(context);

    }

    public FrameView(Context context,AttributeSet attrs) {
        super(context,attrs);
        init(context);

    }

    public void init(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        mScreenHeight = display.getHeight();
        mScreenWidth = display.getWidth();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.image1);
        mBitmap = BitmapUtil.fitToViewByScale(bitmap,mScreenWidth,mScreenHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap,0,0,null);
    }
}
