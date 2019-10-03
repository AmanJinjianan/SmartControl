package com.example.smartcontrol.View;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

import com.example.smartcontrol.R;

public class VerticalSeekBar extends AppCompatSeekBar {
    public VerticalSeekBar(Context context) {
        super(context);
        this.setRotation(90);
        this.setBackgroundResource(R.drawable.icon_ball_r);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public VerticalSeekBar GetVerticalSeekBar(){


        return this;
    }
}
