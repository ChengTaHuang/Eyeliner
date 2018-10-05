package com.eyeliner.eyeliner.palette.model

import android.graphics.PointF

/**
 * Created by zeno on 2018/10/3.
 */
class BezierCircle(val center: Anchor, private val radius: Float, var color: Int) {
    private val c = 0.551915024494f

    var leftBezier: BezierCircleVerticalPoint
    var topBezier: BezierCircleHorizontalPoint
    var rightBezier: BezierCircleVerticalPoint
    var bottomBezier: BezierCircleHorizontalPoint

    init {
        leftBezier = vertical(-radius + center.point.x, center.point.y)
        rightBezier = vertical(radius + center.point.x, center.point.y)
        topBezier = horizontal(center.point.x, -radius + center.point.y)
        bottomBezier = horizontal(center.point.x, radius + center.point.y)
    }

    fun moveCenter(pointF: PointF) {
        val moveX = pointF.x - center.point.x
        val moveY = pointF.y - center.point.y
        center.point.x = pointF.x
        center.point.y = pointF.y

        leftBezier.top.point.x += moveX
        leftBezier.top.point.y += moveY

        leftBezier.middle.point.x += moveX
        leftBezier.middle.point.y += moveY

        leftBezier.bottom.point.x += moveX
        leftBezier.bottom.point.y += moveY

        rightBezier.top.point.x += moveX
        rightBezier.top.point.y += moveY

        rightBezier.middle.point.x += moveX
        rightBezier.middle.point.y += moveY

        rightBezier.bottom.point.x += moveX
        rightBezier.bottom.point.y += moveY

        topBezier.left.point.x += moveX
        topBezier.left.point.y += moveY

        topBezier.middle.point.x += moveX
        topBezier.middle.point.y += moveY

        topBezier.right.point.x += moveX
        topBezier.right.point.y += moveY

        bottomBezier.left.point.x += moveX
        bottomBezier.left.point.y += moveY

        bottomBezier.middle.point.x += moveX
        bottomBezier.middle.point.y += moveY

        bottomBezier.right.point.x += moveX
        bottomBezier.right.point.y += moveY
    }

    fun moveLeft(pointF: PointF) {
        leftBezier.middle.point.x = pointF.x
        leftBezier.middle.point.y = pointF.y

        leftBezier.top.point.x = pointF.x
        leftBezier.top.point.y = pointF.y - (c * radius)

        leftBezier.bottom.point.x = pointF.x
        leftBezier.bottom.point.y = pointF.y + (c * radius)

        val squareCenter = squareCenter()
        center.point.x = squareCenter.x
        center.point.y = squareCenter.y
    }

    fun moveRight(pointF: PointF) {
        rightBezier.middle.point.x = pointF.x
        rightBezier.middle.point.y = pointF.y

        rightBezier.top.point.x = pointF.x
        rightBezier.top.point.y = pointF.y - (c * radius)

        rightBezier.bottom.point.x = pointF.x
        rightBezier.bottom.point.y = pointF.y + (c * radius)

        val squareCenter = squareCenter()
        center.point.x = squareCenter.x
        center.point.y = squareCenter.y
    }

    fun moveTop(pointF: PointF) {

        topBezier.middle.point.x = pointF.x
        topBezier.middle.point.y = pointF.y

        topBezier.left.point.x = pointF.x - (c * radius)
        topBezier.left.point.y = pointF.y

        topBezier.right.point.x = pointF.x + (c * radius)
        topBezier.right.point.y = pointF.y

        val squareCenter = squareCenter()
        center.point.x = squareCenter.x
        center.point.y = squareCenter.y
    }

    fun moveBottom(pointF: PointF) {
        bottomBezier.middle.point.x = pointF.x
        bottomBezier.middle.point.y = pointF.y

        bottomBezier.left.point.x = pointF.x - (c * radius)
        bottomBezier.left.point.y = pointF.y

        bottomBezier.right.point.x = pointF.x + (c * radius)
        bottomBezier.right.point.y = pointF.y

        val squareCenter = squareCenter()
        center.point.x = squareCenter.x
        center.point.y = squareCenter.y
    }

    private fun squareCenter(): PointF {
        return PointF((leftMost() + rightMost()) / 2, (topMost() + bottomMost()) / 2)
    }

    fun rightMost() : Float{
        return Math.max(topBezier.middle.point.x, Math.max(bottomBezier.middle.point.x, Math.max(leftBezier.middle.point.x, rightBezier.middle.point.x)))
    }

    fun leftMost() : Float{
        return Math.min(topBezier.middle.point.x, Math.min(bottomBezier.middle.point.x, Math.min(leftBezier.middle.point.x, rightBezier.middle.point.x)))
    }

    fun topMost() : Float{
        return Math.min(topBezier.middle.point.y, Math.min(bottomBezier.middle.point.y, Math.min(leftBezier.middle.point.y, rightBezier.middle.point.y)))
    }

    fun bottomMost() : Float{
        return Math.max(topBezier.middle.point.y, Math.max(bottomBezier.middle.point.y, Math.max(leftBezier.middle.point.y, rightBezier.middle.point.y)))
    }

    private fun vertical(x: Float, y: Float): BezierCircleVerticalPoint {

        val topPoint = Anchor(PointF(x, y - (c * radius)), false)
        val middlePoint = Anchor(PointF(x, y), false)
        val bottomPoint = Anchor(PointF(x, y + (c * radius)), false)

        return BezierCircleVerticalPoint(topPoint, middlePoint, bottomPoint)
    }

    private fun horizontal(x: Float, y: Float): BezierCircleHorizontalPoint {
        val leftPoint = Anchor(PointF(x - (c * radius), y), false)
        val middlePoint = Anchor(PointF(x, y), false)
        val rightPoint = Anchor(PointF(x + (c * radius), y), false)

        return BezierCircleHorizontalPoint(leftPoint, middlePoint, rightPoint)
    }
}

class BezierCircleVerticalPoint(val top: Anchor, val middle: Anchor, val bottom: Anchor) {

}

class BezierCircleHorizontalPoint(val left: Anchor, val middle: Anchor, val right: Anchor) {

    fun setY(y: Float) {
        left.point.y = y
        middle.point.y = y
        right.point.y = y
    }
}