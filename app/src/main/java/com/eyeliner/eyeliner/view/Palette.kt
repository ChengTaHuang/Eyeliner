package com.eyeliner.eyeliner.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.eyeliner.eyeliner.Bezier
import com.eyeliner.eyeliner.MyPointF
import com.eyeliner.eyeliner.PathEvaluator
import com.eyeliner.eyeliner.R


/**
 * Created by zeno on 2018/9/28.
 */
class Palette : View {

    private lateinit var pointsPaint: Paint
    private lateinit var pointsBiggerPaint : Paint
    private lateinit var linePaint: Paint
    private var pointsRadius = 5f
    private var pointsBiggerRadius = 15f

    private @ColorRes var pointColor: Int = R.color.colorPoint
    private @ColorRes var pointBiggerColor : Int = R.color.colorBiggerPoint

    private val bezierPath = Path()

    private lateinit var bezier: Bezier

    constructor(context: Context) : super(context) {
        this.setWillNotDraw(false)
        initPaint()
        setAnchor()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.setWillNotDraw(false)
        initPaint()
        setAnchor()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        this.setWillNotDraw(false)
        initPaint()
        setAnchor()
    }

    private fun initPaint() {
        pointsPaint = Paint().apply {
            strokeWidth = 1f
            color = ContextCompat.getColor(context , pointColor)
        }

        pointsBiggerPaint = Paint().apply {
            strokeWidth = 1f
            color = ContextCompat.getColor(context , pointBiggerColor)
        }

        linePaint = Paint().apply {
            strokeWidth = 5f
            color = ContextCompat.getColor(context , pointColor)
            style = Paint.Style.STROKE
        }
    }

    private fun setAnchor() {
        val anchor = Anchor(PointF(100f, 100f), false)
        val anchor2 = Anchor(PointF(200f, 100f), false)
        val anchor3 = Anchor(PointF(120f, 200f), false)
        val anchor4 = Anchor(PointF(180f, 200f), false)
        bezier = Bezier(anchor , anchor3 , anchor4 , anchor2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(bezier.start.point.x , bezier.start.point.y , pointsBiggerRadius, pointsBiggerPaint)
        canvas.drawCircle(bezier.mid1.point.x , bezier.mid1.point.y , pointsBiggerRadius , pointsBiggerPaint)
        canvas.drawCircle(bezier.mid2.point.x , bezier.mid2.point.y , pointsBiggerRadius ,pointsBiggerPaint)
        canvas.drawCircle(bezier.end.point.x , bezier.end.point.y , pointsBiggerRadius, pointsBiggerPaint)

        canvas.drawCircle(bezier.start.point.x , bezier.start.point.y , pointsRadius, pointsPaint)
        canvas.drawCircle(bezier.mid1.point.x , bezier.mid1.point.y , pointsRadius , pointsPaint)
        canvas.drawCircle(bezier.mid2.point.x , bezier.mid2.point.y , pointsRadius ,pointsPaint)
        canvas.drawCircle(bezier.end.point.x , bezier.end.point.y , pointsRadius, pointsPaint)


        bezierPath.reset()

        bezierPath.moveTo(bezier.start.point.x, bezier.start.point.y)
        bezierPath.cubicTo(
                bezier.mid1.point.x, bezier.mid1.point.y,
                bezier.mid2.point.x, bezier.mid2.point.y,
                bezier.end.point.x, bezier.end.point.y
        )

        canvas.drawPath(bezierPath, linePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_UP -> {
                bezier.start.draw = false
                bezier.mid1.draw = false
                bezier.mid2.draw = false
                bezier.end.draw = false
            }

            MotionEvent.ACTION_DOWN -> {

                val nowPoint = PointF(event.getX(0), event.getY(0))

                val dis1 = getDistance(bezier.start.point , nowPoint)
                val dis2 = getDistance(bezier.mid1.point , nowPoint)
                val dis3 = getDistance(bezier.mid2.point , nowPoint)
                val dis4 = getDistance(bezier.end.point , nowPoint)
                val min = Math.min(dis1,Math.min(dis2 , Math.min(dis3 , dis4)))

                if(min < 50) {
                    when (min) {
                        dis1 -> {
                            bezier.start.draw = true
                        }

                        dis2 -> {
                            bezier.mid1.draw = true
                        }

                        dis3 -> {
                            bezier.mid2.draw = true
                        }

                        dis4 -> {
                            bezier.end.draw = true
                        }

                        else -> {
                        }
                    }
                }

                if (bezier.start.draw) {
                    bezier.start.point.x = event.getX(0)
                    bezier.start.point.y = event.getY(0)
                }

                if (bezier.mid1.draw) {
                    bezier.mid1.point.x = event.getX(0)
                    bezier.mid1.point.y = event.getY(0)
                }

                if (bezier.mid2.draw) {
                    bezier.mid2.point.x = event.getX(0)
                    bezier.mid2.point.y = event.getY(0)
                }

                if (bezier.end.draw) {
                    bezier.end.point.x = event.getX(0)
                    bezier.end.point.y = event.getY(0)
                }

            }

            MotionEvent.ACTION_MOVE -> {

                if (bezier.start.draw) {
                    bezier.start.point.x = event.getX(0)
                    bezier.start.point.y = event.getY(0)
                }

                if (bezier.mid1.draw) {
                    bezier.mid1.point.x = event.getX(0)
                    bezier.mid1.point.y = event.getY(0)
                }

                if (bezier.mid2.draw) {
                    bezier.mid2.point.x = event.getX(0)
                    bezier.mid2.point.y = event.getY(0)
                }

                if (bezier.end.draw) {
                    bezier.end.point.x = event.getX(0)
                    bezier.end.point.y = event.getY(0)
                }
            }
        }

        invalidate()
        return true
    }

    private fun getDistance(last: PointF, now: PointF): Double {
        return Math.sqrt(
                Math.pow((last.x - now.x).toDouble(), 2.0) +
                        Math.pow((last.y - now.y).toDouble(), 2.0)
        )
    }

//    private val gestureDetector by lazy {
//        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
//            override fun onLongPress(event: MotionEvent) {
//                val nowPoint = PointF(event.getX(0), event.getY(0))
//                if (!anchor.draw &&
//                        getDistance(anchor.point, nowPoint) < 100) {
//                    anchor.draw = true
//                    return
//                }
//
//                if (!anchor2.draw &&
//                        getDistance(anchor2.point, nowPoint) < 100) {
//                    anchor2.draw = true
//                }
//            }
//        })
//    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onTouchEvent(ev)
        //gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}