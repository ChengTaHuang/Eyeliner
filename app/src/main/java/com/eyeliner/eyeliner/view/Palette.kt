package com.eyeliner.eyeliner.view

import android.content.Context
import android.graphics.*
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.eyeliner.eyeliner.Bezier
import com.eyeliner.eyeliner.R
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.graphics.DashPathEffect




/**
 * Created by zeno on 2018/9/28.
 */
class Palette : View {

    sealed class State {
        object EDIT : State()
        object DELETE : State()
        object SAVE : State()
    }
    private var state : State = State.EDIT

    private lateinit var pointsPaint: Paint
    private lateinit var pointsBiggerPaint : Paint
    private lateinit var linePaint: Paint
    private lateinit var deletePaint : Paint
    private var pointsRadius = 5f
    private var pointsBiggerRadius = 15f

    private @ColorRes var pointColor: Int = R.color.colorPoint
    private @ColorRes var pointBiggerColor : Int = R.color.colorBiggerPoint
    private @ColorRes var pointDeleteColor : Int = R.color.colorDelete

    private val bezierPath = Path()

    private val bezierList = mutableListOf<Bezier>()

    private lateinit var delete : Bitmap
    private lateinit var deleteRed : Bitmap

    private var deletePoints = mutableListOf<Delete>()

    private val NONE_DELETE_INDEX = -1
    private var deleteTouchIndex = NONE_DELETE_INDEX

    private var backgroundBitmap : Bitmap? = null

    fun setBackgroundBitmap(bitmap: Bitmap?){
        this.backgroundBitmap = bitmap
        invalidate()
    }

    constructor(context: Context) : super(context) {
        this.setWillNotDraw(false)
        initPaint()
        createDelete()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.setWillNotDraw(false)
        initPaint()
        createDelete()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        this.setWillNotDraw(false)
        initPaint()
        createDelete()
    }

    private fun initPaint() {
        pointsPaint = Paint().apply {
            strokeWidth = 1f
            color = ContextCompat.getColor(context , pointColor)
            isAntiAlias = true
        }

        pointsBiggerPaint = Paint().apply {
            strokeWidth = 1f
            color = ContextCompat.getColor(context , pointBiggerColor)
            isAntiAlias = true
        }

        linePaint = Paint().apply {
            strokeWidth = 5f
            color = ContextCompat.getColor(context , pointColor)
            style = Paint.Style.STROKE
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }

        deletePaint = Paint().apply {
            strokeWidth = 5f
            color = ContextCompat.getColor(context , pointDeleteColor)
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
    }

    fun addBezier(){
        bezierList.add(createBezier())
        invalidate()
    }

    fun removeBezier(bezier: Bezier){
        bezierList.remove(bezier)
        changeSate(state)
        invalidate()
    }

    fun changeSate(state: State){
        this.state = state
        when(state){
            is State.EDIT -> {
                deletePoints.clear()
            }

            is State.DELETE ->{
                checkDelete()
            }

            is State.SAVE ->{
                deletePoints.clear()
            }
        }

        invalidate()
    }

    private fun createDelete(){
        val d = ContextCompat.getDrawable(context , R.drawable.ic_delete)!!
        delete = drawableToBitmap(d)

        val dRed = ContextCompat.getDrawable(context , R.drawable.ic_delete_r)!!
        deleteRed = drawableToBitmap(dRed)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun createBezier() : Bezier{
        val anchor = Anchor(PointF(100f, 100f), false)
        val anchor4 = Anchor(PointF(200f, 100f), false)
        val anchor2 = Anchor(PointF(120f, 200f), false)
        val anchor3 = Anchor(PointF(180f, 200f), false)
        return Bezier(anchor , anchor2 , anchor3 , anchor4)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        backgroundBitmap?.run {
            canvas.drawBitmap(backgroundBitmap , 0f ,0f , pointsPaint)
        }

        when(state){
            is State.EDIT ->{
                bezierList.forEach { bezier ->
                    canvas.drawCircle(bezier.start.point.x, bezier.start.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(bezier.mid1.point.x, bezier.mid1.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(bezier.mid2.point.x, bezier.mid2.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(bezier.end.point.x, bezier.end.point.y, pointsBiggerRadius, pointsBiggerPaint)

                    canvas.drawCircle(bezier.start.point.x, bezier.start.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(bezier.mid1.point.x, bezier.mid1.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(bezier.mid2.point.x, bezier.mid2.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(bezier.end.point.x, bezier.end.point.y, pointsRadius, pointsPaint)
                }
            }
        }


        bezierList.forEachIndexed { index, bezier ->

            bezierPath.reset()

            bezierPath.moveTo(bezier.start.point.x, bezier.start.point.y)
            bezierPath.cubicTo(
                    bezier.mid1.point.x, bezier.mid1.point.y,
                    bezier.mid2.point.x, bezier.mid2.point.y,
                    bezier.end.point.x, bezier.end.point.y
            )

            if(index == deleteTouchIndex){
                canvas.drawPath(bezierPath, deletePaint)
            }else{
                canvas.drawPath(bezierPath, linePaint)
            }
        }

        deletePoints.forEachIndexed { index, point ->
            if(index == deleteTouchIndex){
                canvas.drawBitmap(deleteRed, point.left , point.top, pointsPaint)
            }else{
                canvas.drawBitmap(delete, point.left , point.top, pointsPaint)
            }
        }

    }

    private fun checkDelete(){
        deletePoints.clear()
        bezierList.forEach{ bezier ->
            val top = Math.min(bezier.start.point.y , Math.min(bezier.mid1.point.y , Math.min(bezier.mid2.point.y , bezier.end.point.y)))
            val left = Math.max(bezier.start.point.x , Math.max(bezier.mid1.point.x , Math.max(bezier.mid2.point.x , bezier.end.point.x)))
            val right = delete.width + left
            val bottom = delete.height + top
            val deletePoint = Delete(left , top , right , bottom)
            deletePoints.add(deletePoint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_UP -> {

                when(state){
                    is State.EDIT ->{
                        bezierList.forEach { bezier ->
                            bezier.start.draw = false
                            bezier.mid1.draw = false
                            bezier.mid2.draw = false
                            bezier.end.draw = false
                        }
                    }

                    is State.DELETE ->{
                        deleteTouchIndex = NONE_DELETE_INDEX
                    }
                }
            }

            MotionEvent.ACTION_DOWN -> {
                val nowPoint = PointF(event.getX(0), event.getY(0))

                when(state){
                    is State.EDIT ->{
                        run breaking@ {
                            bezierList.forEach { bezier ->
                                val dis1 = getDistance(bezier.start.point, nowPoint)
                                val dis2 = getDistance(bezier.mid1.point, nowPoint)
                                val dis3 = getDistance(bezier.mid2.point, nowPoint)
                                val dis4 = getDistance(bezier.end.point, nowPoint)
                                val min = Math.min(dis1, Math.min(dis2, Math.min(dis3, dis4)))

                                if (min < 50) {
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

                                if (min < 50) return@breaking
                            }
                        }
                    }

                    is State.DELETE -> {
                        deleteTouchIndex = findTouchedDeletePoint(nowPoint)
                    }
                }


            }

            MotionEvent.ACTION_MOVE -> {
                if(state == State.EDIT) {
                    run breaking@ {
                        bezierList.forEach { bezier ->
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

                            if (bezier.start.draw || bezier.mid1.draw || bezier.mid2.draw || bezier.end.draw) return@breaking
                        }
                    }
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

    private val gestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(event: MotionEvent) {
                val nowPoint = PointF(event.getX(0), event.getY(0))

                val index = findTouchedDeletePoint(nowPoint)

                if(index != NONE_DELETE_INDEX) {
                    deletePoints.removeAt(index)
                    bezierList.removeAt(index)
                    deleteTouchIndex = NONE_DELETE_INDEX
                    invalidate()
                }

            }
        })
    }

    private fun findTouchedDeletePoint(nowPoint : PointF) : Int {

        var index1 = NONE_DELETE_INDEX
        deletePoints.forEachIndexed { index , deletePoint ->
            val dis1 = getDistance(PointF(deletePoint.left , deletePoint.top), nowPoint)
            val dis2 = getDistance(PointF(deletePoint.left , deletePoint.down), nowPoint)
            val dis3 = getDistance(PointF(deletePoint.right , deletePoint.top), nowPoint)
            val dis4 = getDistance(PointF(deletePoint.left , deletePoint.down), nowPoint)
            val min = Math.min(dis1, Math.min(dis2, Math.min(dis3, dis4)))
            if(min < 50) {
                index1 = index
            }
        }
        return index1
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onTouchEvent(ev)
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}