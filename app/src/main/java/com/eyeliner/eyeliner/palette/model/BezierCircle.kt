package com.eyeliner.eyeliner.palette.model

import android.graphics.Matrix
import android.graphics.PointF
import com.eyeliner.eyeliner.palette.PointsUtility

/**
 * Created by zeno on 2018/10/3.
 */
class BezierCircle(val center: Anchor,
                   private val radius: Float,
                   var color: Int,
                   private val editIconWidth: Float,
                   private val editIconHeight: Float) : BezierShape(editIconWidth, editIconHeight) {

    sealed class AnchorType {
        object LEFT : AnchorType()
        object RIGHT : AnchorType()
        object TOP : AnchorType()
        object BOTTOM : AnchorType()
        object CENTER : AnchorType()
    }

    private val c = 0.551915024494f
    private var minX = 0.0f
    private var minY = 0.0f
    private var maxX = 0.0f
    private var maxY = 0.0f

    var leftBezier: BezierCircleVerticalPoint
    var topBezier: BezierCircleHorizontalPoint
    var rightBezier: BezierCircleVerticalPoint
    var bottomBezier: BezierCircleHorizontalPoint

    init {
        leftBezier = vertical(-radius + center.point.x, center.point.y)
        rightBezier = vertical(radius + center.point.x, center.point.y)
        topBezier = horizontal(center.point.x, -radius + center.point.y)
        bottomBezier = horizontal(center.point.x, radius + center.point.y)
        calcPoints()
    }

    private fun minimumX() = Math.min(topBezier.middle.point.x, Math.min(bottomBezier.middle.point.x, Math.min(leftBezier.middle.point.x, rightBezier.middle.point.x)))
    private fun minimumY() = Math.min(topBezier.middle.point.y, Math.min(bottomBezier.middle.point.y, Math.min(leftBezier.middle.point.y, rightBezier.middle.point.y)))
    private fun maximumX() = Math.max(topBezier.middle.point.x, Math.max(bottomBezier.middle.point.x, Math.max(leftBezier.middle.point.x, rightBezier.middle.point.x)))
    private fun maximumY() = Math.max(topBezier.middle.point.y, Math.max(bottomBezier.middle.point.y, Math.max(leftBezier.middle.point.y, rightBezier.middle.point.y)))

    private fun calcPoints() {
        minX = minimumX()
        maxX = maximumX()
        minY = minimumY()
        maxY = maximumY()

        val squareCenter = squareCenter()
        center.point.x = squareCenter.x
        center.point.y = squareCenter.y
    }

    fun moveLeft(pointF: PointF) {
        val moveX = pointF.x - leftBezier.middle.point.x
        val moveY = pointF.y - leftBezier.middle.point.y

        leftBezier.middle.point.x = pointF.x
        leftBezier.middle.point.y = pointF.y

        leftBezier.top.point.x += moveX
        leftBezier.top.point.y += moveY

        leftBezier.bottom.point.x += moveX
        leftBezier.bottom.point.y += moveY

        calcPoints()
    }

    fun moveRight(pointF: PointF) {
        val moveX = pointF.x - rightBezier.middle.point.x
        val moveY = pointF.y - rightBezier.middle.point.y

        rightBezier.middle.point.x = pointF.x
        rightBezier.middle.point.y = pointF.y

        rightBezier.top.point.x += moveX
        rightBezier.top.point.y += moveY

        rightBezier.bottom.point.x += moveX
        rightBezier.bottom.point.y += moveY

        calcPoints()
    }

    fun moveTop(pointF: PointF) {
        val moveX = pointF.x - topBezier.middle.point.x
        val moveY = pointF.y - topBezier.middle.point.y

        topBezier.middle.point.x = pointF.x
        topBezier.middle.point.y = pointF.y

        topBezier.left.point.x += moveX
        topBezier.left.point.y += moveY

        topBezier.right.point.x += moveX
        topBezier.right.point.y += moveY

        calcPoints()
    }

    fun moveBottom(pointF: PointF) {
        val moveX = pointF.x - bottomBezier.middle.point.x
        val moveY = pointF.y - bottomBezier.middle.point.y

        bottomBezier.middle.point.x = pointF.x
        bottomBezier.middle.point.y = pointF.y

        bottomBezier.left.point.x += moveX
        bottomBezier.left.point.y += moveY

        bottomBezier.right.point.x += moveX
        bottomBezier.right.point.y += moveY

        calcPoints()
    }

    fun closeAnchor(point: PointF): BezierCircle.AnchorType? {
        val dis1 = PointsUtility.getDistance(topBezier.middle.point, point)
        val dis2 = PointsUtility.getDistance(bottomBezier.middle.point, point)
        val dis3 = PointsUtility.getDistance(leftBezier.middle.point, point)
        val dis4 = PointsUtility.getDistance(rightBezier.middle.point, point)
        val dis5 = PointsUtility.getDistance(center.point, point)

        val min = Math.min(dis1, Math.min(dis2, Math.min(dis3, Math.min(dis4 , dis5))))
        return if (min < 50) {
            when (min) {
                dis1 -> {
                    BezierCircle.AnchorType.TOP
                }
                dis2 -> {
                    BezierCircle.AnchorType.BOTTOM
                }
                dis3 -> {
                    BezierCircle.AnchorType.LEFT
                }
                dis4 -> {
                    BezierCircle.AnchorType.RIGHT
                }
                dis5 -> {
                    BezierCircle.AnchorType.CENTER
                }
                else -> {
                    null
                }
            }
        } else null
    }

    private fun squareCenter(): PointF {
        return PointF((leftMost() + rightMost()) / 2, (topMost() + bottomMost()) / 2)
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

    private fun rotate(matrix: Matrix, circlePoint: BezierCircleHorizontalPoint) {
        var touchPoint = floatArrayOf(circlePoint.left.point.x, circlePoint.left.point.y)
        matrix.mapPoints(touchPoint)
        circlePoint.left.point.x = touchPoint[0]
        circlePoint.left.point.y = touchPoint[1]
        touchPoint = floatArrayOf(circlePoint.middle.point.x, circlePoint.middle.point.y)
        matrix.mapPoints(touchPoint)
        circlePoint.middle.point.x = touchPoint[0]
        circlePoint.middle.point.y = touchPoint[1]
        touchPoint = floatArrayOf(circlePoint.right.point.x, circlePoint.right.point.y)
        matrix.mapPoints(touchPoint)
        circlePoint.right.point.x = touchPoint[0]
        circlePoint.right.point.y = touchPoint[1]
    }

    private fun rotate(matrix: Matrix, circlePoint: BezierCircleVerticalPoint) {
        var touchPoint = floatArrayOf(circlePoint.top.point.x, circlePoint.top.point.y)
        matrix.mapPoints(touchPoint)
        circlePoint.top.point.x = touchPoint[0]
        circlePoint.top.point.y = touchPoint[1]
        touchPoint = floatArrayOf(circlePoint.middle.point.x, circlePoint.middle.point.y)
        matrix.mapPoints(touchPoint)
        circlePoint.middle.point.x = touchPoint[0]
        circlePoint.middle.point.y = touchPoint[1]
        touchPoint = floatArrayOf(circlePoint.bottom.point.x, circlePoint.bottom.point.y)
        matrix.mapPoints(touchPoint)
        circlePoint.bottom.point.x = touchPoint[0]
        circlePoint.bottom.point.y = touchPoint[1]
    }

    fun setEnableDraw(anchorType: BezierCircle.AnchorType, draw: Boolean) {
        return when (anchorType) {
            BezierCircle.AnchorType.LEFT ->{
                leftBezier.middle.draw = draw
                leftBezier.top.draw = draw
                leftBezier.bottom.draw = draw
            }
            BezierCircle.AnchorType.RIGHT -> {
                rightBezier.middle.draw = draw
                rightBezier.top.draw = draw
                rightBezier.bottom.draw = draw
            }
            BezierCircle.AnchorType.TOP -> {
                topBezier.middle.draw = draw
                topBezier.right.draw = draw
                topBezier.left.draw = draw
            }
            BezierCircle.AnchorType.BOTTOM -> {
                bottomBezier.middle.draw = draw
                bottomBezier.right.draw = draw
                bottomBezier.left.draw = draw
            }
            BezierCircle.AnchorType.CENTER -> {
                center.draw = draw
            }
        }
    }

    fun getDraw(): BezierCircle.AnchorType? {
        if (leftBezier.middle.draw) return BezierCircle.AnchorType.LEFT
        if (rightBezier.middle.draw) return BezierCircle.AnchorType.RIGHT
        if (topBezier.middle.draw) return BezierCircle.AnchorType.TOP
        if (bottomBezier.middle.draw) return BezierCircle.AnchorType.BOTTOM
        if (center.draw) return BezierCircle.AnchorType.CENTER
        return null
    }

    override fun rightMost() = maxX

    override fun leftMost() = minX

    override fun topMost() = minY

    override fun bottomMost() = maxY

    override fun center(): Anchor = center

    override fun moveCenter(point: PointF) {
        val moveX = point.x - center.point.x
        val moveY = point.y - center.point.y
        center.point.x = point.x
        center.point.y = point.y

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

        calcPoints()
    }

    override fun isTouchEditIcon(point: PointF): Boolean {
        val iconRight = rightMost() + editIconWidth
        val iconBottom = topMost() + editIconHeight
        val iconLeft = rightMost()
        val iconTop = topMost()

        return (point.x in iconLeft..iconRight && point.y in iconTop..iconBottom)
    }

    override fun isTouch(point: PointF) = (point.x in minX..maxX && point.y in minY..maxY)

    override fun isTouchCenter(point: PointF) = (PointsUtility.getDistance(center.point, point) < 50)

    override fun getDrawIconPoint(): PointF = PointF(rightMost(), topMost())

    override fun rotate(matrix: Matrix) {

        rotate(matrix, topBezier)
        rotate(matrix, bottomBezier)
        rotate(matrix, leftBezier)
        rotate(matrix, rightBezier)

        val touchPoint = floatArrayOf(center.point.x, center.point.y)
        matrix.mapPoints(touchPoint)
        center.point.x = touchPoint[0]
        center.point.y = touchPoint[1]

        minX = minimumX()
        maxX = maximumX()
        minY = minimumY()
        maxY = maximumY()
    }
}

class BezierCircleVerticalPoint(val top: Anchor, val middle: Anchor, val bottom: Anchor) {

}

class BezierCircleHorizontalPoint(val left: Anchor, val middle: Anchor, val right: Anchor) {

}
