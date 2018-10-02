package com.eyeliner.eyeliner.color.picker;

import android.content.Context;
import android.widget.TextView;

import com.eyeliner.eyeliner.R;
import com.skydoves.colorpickerview.AlphaTileView;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.flag.FlagView;

public class CustomFlag extends FlagView {

    private AlphaTileView alphaTileView;

    public CustomFlag(Context context, int layout) {
        super(context, layout);
        alphaTileView = findViewById(R.id.flag_color_layout);
    }

    @Override
    public void onRefresh(ColorEnvelope colorEnvelope) {
        alphaTileView.setPaintColor(colorEnvelope.getColor());
    }
}