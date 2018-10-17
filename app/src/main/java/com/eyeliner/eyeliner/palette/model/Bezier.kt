package com.eyeliner.eyeliner.palette.model

import android.graphics.Matrix
import android.graphics.PointF
import com.eyeliner.eyeliner.palette.PointsUtility.getDistance

/**
 * Created by zeno on 2018/9/28.
 */
class Bezier(val start: Anchor,
             val mid1: Anchor,
             val mid2: Anchor,
             val end: Anchor,
             var color: Int,
             private val editIconWidth: Float,
             private val editIconHeight: Float) : BezierShape(editIconWidth, editIconHeight) {

    sealed class AnchorType {
        object START : AnchorType()
        object MID1 : AnchorType()
        object MID2 : AnchorType()
        object END : AnchorType()
        object CENTER : AnchorType()
    }

    private var center = Anchor(PointF(0f,0f) , false)
    private var minX = 0.0f
    private var minY = 0.0f
    private var maxX = 0.0f
    private var maxY = 0.0f
    private var matrix = Matrix()

    init {
        calcPoints()
    }

    private fun minimumX() = Math.min(start.point.x, Math.min(mid1.point.x, Math.min(mid2.point.x, end.point.x)))
    private fun minimumY() = Math.min(start.point.y, Math.min(mid1.point.y, Math.min(mid2.point.y, end.point.y)))
    private fun maximumX() = Math.max(start.point.x, Math.max(mid1.point.x, Math.max(mid2.point.x, end.point.x)))
    private fun maximumY() = Math.max(start.point.y, Math.max(mid1.point.y, Math.max(mid2.point.y, end.point.y)))
    private fun centerX() = (minimumX() + maximumX()) / 2
    private fun centerY() = (minimumY() + maximumY()) / 2
    private fun calcPoints() {
        minX = minimumX()
        maxX = maximumX()
        minY = minimumY()
        maxY = maximumY()

        center.point.x = centerX()
        center.point.y = centerY()
    }

    fun moveStart(point: PointF) {
        start.point = point
        calcPoints()
    }

    fun moveMid1(point: PointF) {
        mid1.point = point
        calcPoints()
    }

    fun moveMid2(point: PointF) {
        mid2.point = point
        calcPoints()
    }

    fun moveEnd(point: PointF) {
        end.point = point
        calcPoints()
    }

    fun closeAnchor(point: PointF): AnchorType? {
        val dis1 = getDistance(start.point, point)
        val dis2 = getDistance(mid1.point, point)
        val dis3 = getDistance(mid2.point, point)
        val dis4 = getDistance(end.point, point)
        val dis5 = getDistance(center.point, point)

        val min = Math.min(dis1, Math.min(dis2, Math.min(dis3, Math.min(dis4 , dis5))))
        return if (min < 50) {
            when (min) {
                dis1 -> {
                    AnchorType.START
                }
                dis2 -> {
                    AnchorType.MID1
                }
                dis3 -> {
                    AnchorType.MID2
                }
                dis4 -> {
                    AnchorType.END
                }
                dis5 -> {
                    AnchorType.CENTER
                }
                else -> {
                    null
                }
            }
        } else null
    }

    fun setEnableDraw(anchorType: AnchorType, draw: Boolean) {
        return when (anchorType) {

            Bezier.AnchorType.START -> {
                start.draw = draw
            }
            Bezier.AnchorType.MID1 -> {
                mid1.draw = draw
            }
            Bezier.AnchorType.MID2 -> {
                mid2.draw = draw
            }
            Bezier.AnchorType.END -> {
                end.draw = draw
            }
            Bezier.AnchorType.CENTER -> {
                center.draw = draw
            }
        }
    }

    fun getDraw(): Bezier.AnchorType? {
        if (start.draw) return Bezier.AnchorType.START
        if (mid1.draw) return Bezier.AnchorType.MID1
        if (mid2.draw) return Bezier.AnchorType.MID2
        if (end.draw) return Bezier.AnchorType.END
        if (center.draw) return Bezier.AnchorType.CENTER
        return null
    }

    override fun isTouch(point: PointF): Boolean = (point.x in minX..maxX && point.y in minY..maxY)

    override fun leftMost(): Float = minX

    override fun rightMost(): Float = maxX

    override fun topMost(): Float = minY

    override fun bottomMost(): Float = maxY

    override fun center(): Anchor = center

    override fun moveCenter(point: PointF) {
        val moveX = point.x - center.point.x
        val moveY = point.y - center.point.y
        center.point.x = point.x
        center.point.y = point.y

        start.point.x += moveX
        start.point.y += moveY
        mid1.point.x += moveX
        mid1.point.y += moveY
        mid2.point.x += moveX
        mid2.point.y += moveY
        end.point.x += moveX
        end.point.y += moveY

        calcPoints()
    }

    override fun isTouchEditIcon(point: PointF): Boolean {
        val iconRight = rightMost() + editIconWidth
        val iconBottom = topMost() + editIconHeight
        val iconLeft = rightMost()
        val iconTop = topMost()

        return (point.x in iconLeft..iconRight && point.y in iconTop..iconBottom)
    }

    override fun isTouchCenter(point: PointF): Boolean = (getDistance(center.point, point) < 50)

    override fun getDrawIconPoint(): PointF = PointF(rightMost(), topMost())

    override fun rotate(matrix: Matrix) {
        this.matrix = matrix;

        var touchPoint = floatArrayOf(start.point.x, start.point.y)
        matrix.mapPoints(touchPoint)
        start.point.x = touchPoint[0]
        start.point.y = touchPoint[1]

        touchPoint = floatArrayOf(mid1.point.x, mid1.point.y)
        matrix.mapPoints(touchPoint)
        mid1.point.x = touchPoint[0]
        mid1.point.y = touchPoint[1]


        touchPoint = floatArrayOf(mid2.point.x, mid2.point.y)
        matrix.mapPoints(touchPoint)
        mid2.point.x = touchPoint[0]
        mid2.point.y = touchPoint[1]

        touchPoint = floatArrayOf(end.point.x, end.point.y)
        matrix.mapPoints(touchPoint)
        end.point.x = touchPoint[0]
        end.point.y = touchPoint[1]

        minX = minimumX()
        maxX = maximumX()
        minY = minimumY()
        maxY = maximumY()

    }
}