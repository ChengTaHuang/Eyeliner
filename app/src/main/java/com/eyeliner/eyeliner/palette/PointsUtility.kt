package com.eyeliner.eyeliner.palette

import android.graphics.PointF

/**
 * Created by zeno on 2018/10/15.
 */
object PointsUtility {
    fun getDistance(last: PointF, now: PointF): Double {

        return Math.sqrt(
                Math.pow((last.x - now.x).toDouble(), 2.0) +
                        Math.pow((last.y - now.y).toDouble(), 2.0)
        )
    }
}