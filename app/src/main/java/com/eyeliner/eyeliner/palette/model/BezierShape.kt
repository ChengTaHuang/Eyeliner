package com.eyeliner.eyeliner.palette.model

import android.graphics.Matrix
import android.graphics.PointF

/**
 * Created by zeno on 2018/10/16.
 */
abstract class BezierShape(editIconWidth : Float , editIconHeight : Float) {
    var rotate = false

    abstract fun leftMost() : Float

    abstract fun rightMost() : Float

    abstract fun topMost() : Float

    abstract fun bottomMost() : Float

    abstract fun center() : Anchor

    abstract fun moveCenter(point: PointF)

    abstract fun isTouchEditIcon(point: PointF) : Boolean

    abstract fun isTouch(point: PointF) : Boolean

    abstract fun isTouchCenter(point: PointF) : Boolean

    abstract fun getDrawIconPoint() : PointF

    abstract fun rotate(matrix: Matrix)
}