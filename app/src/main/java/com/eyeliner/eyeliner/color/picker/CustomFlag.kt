package com.eyeliner.eyeliner.color.picker

import android.content.Context
import com.eyeliner.eyeliner.R
import com.skydoves.colorpickerview.AlphaTileView
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.flag.FlagView

class CustomFlag(context: Context, layout: Int) : FlagView(context, layout) {

    private val alphaTileView: AlphaTileView = findViewById(R.id.flag_color_layout)

    override fun onRefresh(colorEnvelope: ColorEnvelope) {
        alphaTileView.setPaintColor(colorEnvelope.color)
    }
}