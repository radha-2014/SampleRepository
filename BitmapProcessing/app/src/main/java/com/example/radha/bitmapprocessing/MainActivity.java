package com.example.radha.bitmapprocessing;

import android.app.Activity;
import android.os.Bundle;


public class MainActivity extends Activity {

    FrameView mView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new FrameView(this);
        setContentView(mView);
    }



}
