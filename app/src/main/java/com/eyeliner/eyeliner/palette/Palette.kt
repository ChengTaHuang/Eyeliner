package com.eyeliner.eyeliner.palette

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.views.interfaces.GestureView
import com.eyeliner.eyeliner.R
import com.eyeliner.eyeliner.palette.model.Anchor
import com.eyeliner.eyeliner.palette.model.Bezier
import com.eyeliner.eyeliner.palette.model.BezierArea
import com.eyeliner.eyeliner.palette.model.BezierCircle


/**
 * Created by zeno on 2018/9/28.
 */
class Palette : View, GestureView {
    private val controller = GestureController(this)
    override fun getController() = controller
    private var _matrix = Matrix()
    private var firstTouch = false
    private val initState by lazy {
        val state = com.alexvasilkov.gestures.State()
        //{x=0.0,y=0.0,zoom=1.0,rotation=0.0}
        state.set(0.0f, 0.0f, 1.0f, 0.0f)
        state
    }

    sealed class State {
        object EDIT : State()
        object DELETE : State()
        object SAVE : State()
        object COLOR : State()
    }

    private var state: State = State.EDIT

    private val bitmapPaint: Paint = Paint()
    private val pointsPaint: Paint = Paint()
    private val pointsBiggerPaint: Paint = Paint()
    private val linePaint: Paint = Paint()
    private val defaultRadius = 5f
    private val defaultBiggerRadius = 15f
    private var pointsRadius = defaultRadius
    private var pointsBiggerRadius = defaultBiggerRadius

    @ColorRes
    var pointColor: Int = R.color.colorPoint
        set(value) {
            pointsPaint.color = ContextCompat.getColor(context, value)
            linePaint.color = ContextCompat.getColor(context, value)
            invalidate()
        }

    @ColorRes
    var pointBiggerColor: Int = R.color.colorBiggerPoint

    @ColorInt
    var currentPaintColor: Int? = null

    var lineStrokeWidth: Float = 5f
        set(value) {
            linePaint.strokeWidth = value
            pointsRadius = if (value > defaultRadius) value else defaultRadius
            pointsBiggerRadius = if (value * 3 > defaultBiggerRadius) value * 3 else defaultBiggerRadius
            invalidate()
        }

    var dashPath = 20f
        set(value) {
            linePaint.pathEffect = DashPathEffect(floatArrayOf(value, value / 2), 0f)
            invalidate()
        }

    private val bezierPath = Path()

    private val bezierList = mutableListOf<Bezier>()

    private lateinit var delete: Bitmap
    private lateinit var colorPalette: Bitmap

    private var bezierAreaList = mutableListOf<BezierArea>()

    private val NONE_TOUCH_AREA = -1
    private var touchIndex = NONE_TOUCH_AREA

    private var backgroundBitmap: Bitmap? = null

    private var bezierCircleList = mutableListOf<BezierCircle>()

    private val bezierCirclePath = Path()

    private var radius = 100f

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        this.backgroundBitmap = bitmap
        settingController()
        invalidate()
    }

    constructor(context: Context) : super(context) {
        this.setWillNotDraw(false)
        initPaint()
        createDelete()
        createColorPalette()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.setWillNotDraw(false)
        initPaint()
        createDelete()
        createColorPalette()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        this.setWillNotDraw(false)
        initPaint()
        createDelete()
        createColorPalette()
    }

    private fun initPaint() {
        with(pointsPaint) {
            strokeWidth = 1f
            color = ContextCompat.getColor(context, pointColor)
            isAntiAlias = true
        }

        with(pointsBiggerPaint) {
            strokeWidth = 1f
            color = ContextCompat.getColor(context, pointBiggerColor)
            isAntiAlias = true
        }

        with(linePaint) {
            strokeWidth = 5f
            color = ContextCompat.getColor(context, pointColor)
            style = Paint.Style.STROKE
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(dashPath, dashPath / 2), 0f)
        }
    }

    fun addBezier() {
        bezierList.add(createBezier())
        invalidate()
    }

    fun removeBezier(bezier: Bezier) {
        bezierList.remove(bezier)
        changeSate(state)
        invalidate()
    }

    fun addBezierCircle() {
        bezierCircleList.add(createBezierCircle())
        invalidate()
    }

    fun changeSate(state: State) {
        this.state = state
        when (state) {
            is State.EDIT, State.SAVE -> {
                bezierAreaList.clear()
            }

            is State.DELETE, State.COLOR -> {
                checkDelete()
            }
        }

        invalidate()
    }

    fun resetScale() {
        _matrix = Matrix()
    }

    private fun settingController() {
        controller.settings
                .setRotationEnabled(false)
                .setDoubleTapEnabled(false)
                .setFitMethod(Settings.Fit.INSIDE)
                .setBoundsType(Settings.Bounds.INSIDE)
                .setMinZoom(1f)
                .setImage(backgroundBitmap!!.width, backgroundBitmap!!.height)

        controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateReset(oldState: com.alexvasilkov.gestures.State?, newState: com.alexvasilkov.gestures.State?) {
                applyState(newState)
            }

            override fun onStateChanged(state: com.alexvasilkov.gestures.State?) {
                applyState(state)
            }
        })
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        controller.settings.setViewport(
                width - paddingLeft - paddingRight,
                height - paddingTop - paddingBottom)
        controller.updateState()
    }

    private fun applyState(state: com.alexvasilkov.gestures.State?) {
        if (state != null) {
            if (!firstTouch && (initState == state)) {
                firstTouch = true
            }
            if (firstTouch) {
                state[_matrix]
                invalidate()
            }
        }
    }

    private fun createDelete() {
        val d = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!
        delete = drawableToBitmap(d)
    }

    private fun createColorPalette() {
        val p = ContextCompat.getDrawable(context, R.drawable.ic_color_palette)!!
        colorPalette = drawableToBitmap(p)
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

    private fun createBezier(): Bezier {
        val anchor = Anchor(invertPoint(PointF(100f, 100f)), false)
        val anchor4 = Anchor(invertPoint(PointF(200f, 100f)), false)
        val anchor2 = Anchor(invertPoint(PointF(120f, 200f)), false)
        val anchor3 = Anchor(invertPoint(PointF(180f, 200f)), false)
        return Bezier(anchor, anchor2, anchor3, anchor4, ContextCompat.getColor(context, R.color.colorPoint))
    }

    private fun createBezierCircle(): BezierCircle {
        return BezierCircle(Anchor(PointF(200f, 200f), false), radius, ContextCompat.getColor(context, R.color.colorPoint))
    }

    private fun invertPoint(point: PointF): PointF {
        val touchPoint = floatArrayOf(point.x, point.y)
        val newMatrix = Matrix()
        _matrix.invert(newMatrix)
        newMatrix.mapPoints(touchPoint)
        return PointF(touchPoint[0], touchPoint[1])
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(_matrix)
        backgroundBitmap?.run {
            canvas.drawBitmap(backgroundBitmap, 0f, 0f, bitmapPaint)
        }

        bezierList.forEachIndexed { index, bezier ->

            bezierPath.reset()

            bezierPath.moveTo(bezier.start.point.x, bezier.start.point.y)
            bezierPath.cubicTo(
                    bezier.mid1.point.x, bezier.mid1.point.y,
                    bezier.mid2.point.x, bezier.mid2.point.y,
                    bezier.end.point.x, bezier.end.point.y
            )
            linePaint.color = bezier.color
            canvas.drawPath(bezierPath, linePaint)

        }

        bezierCirclePath.reset()

        bezierCircleList.forEach { circle ->
            bezierCirclePath.reset()

            bezierCirclePath.moveTo(circle.topBezier.middle.point.x, circle.topBezier.middle.point.y)
            bezierCirclePath.cubicTo(circle.topBezier.right.point.x, circle.topBezier.right.point.y, circle.rightBezier.top.point.x, circle.rightBezier.top.point.y,
                    circle.rightBezier.middle.point.x, circle.rightBezier.middle.point.y)
            bezierCirclePath.cubicTo(circle.rightBezier.bottom.point.x, circle.rightBezier.bottom.point.y, circle.bottomBezier.right.point.x, circle.bottomBezier.right.point.y,
                    circle.bottomBezier.middle.point.x, circle.bottomBezier.middle.point.y)
            bezierCirclePath.cubicTo(circle.bottomBezier.left.point.x, circle.bottomBezier.left.point.y, circle.leftBezier.bottom.point.x, circle.leftBezier.bottom.point.y,
                    circle.leftBezier.middle.point.x, circle.leftBezier.middle.point.y)
            bezierCirclePath.cubicTo(circle.leftBezier.top.point.x, circle.leftBezier.top.point.y, circle.topBezier.left.point.x, circle.topBezier.left.point.y,
                    circle.topBezier.middle.point.x, circle.topBezier.middle.point.y)

            linePaint.color = circle.color
            canvas.drawPath(bezierCirclePath, linePaint)

        }

        when (state) {
            is State.EDIT -> {
                bezierList.forEach { bezier ->
                    canvas.drawCircle(bezier.start.point.x, bezier.start.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(bezier.mid1.point.x, bezier.mid1.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(bezier.mid2.point.x, bezier.mid2.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(bezier.end.point.x, bezier.end.point.y, pointsBiggerRadius, pointsBiggerPaint)

                    pointsPaint.color = bezier.color
                    canvas.drawCircle(bezier.start.point.x, bezier.start.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(bezier.mid1.point.x, bezier.mid1.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(bezier.mid2.point.x, bezier.mid2.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(bezier.end.point.x, bezier.end.point.y, pointsRadius, pointsPaint)
                }

                bezierCircleList.forEach { circle ->
                    canvas.drawCircle(circle.topBezier.middle.point.x, circle.topBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(circle.bottomBezier.middle.point.x, circle.bottomBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(circle.leftBezier.middle.point.x, circle.leftBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(circle.rightBezier.middle.point.x, circle.rightBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(circle.center.point.x, circle.center.point.y, pointsBiggerRadius, pointsBiggerPaint)
                    canvas.drawCircle(circle.topBezier.middle.point.x, circle.topBezier.middle.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(circle.bottomBezier.middle.point.x, circle.bottomBezier.middle.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(circle.leftBezier.middle.point.x, circle.leftBezier.middle.point.y, pointsRadius, pointsPaint)
                    canvas.drawCircle(circle.rightBezier.middle.point.x, circle.rightBezier.middle.point.y, pointsRadius, pointsPaint)

                    pointsPaint.color = circle.color
                    canvas.drawCircle(circle.center.point.x, circle.center.point.y, pointsRadius, pointsPaint)
                }
            }

            is State.DELETE -> {
                bezierAreaList.forEachIndexed { index, point ->
                    canvas.drawBitmap(delete, point.right, point.top, bitmapPaint)
                }
            }

            is State.COLOR -> {
                bezierAreaList.forEachIndexed { index, point ->
                    canvas.drawBitmap(colorPalette, point.left, point.top, bitmapPaint)
                }
            }
        }

        canvas.restore()
    }

    private fun checkDelete() {
        bezierAreaList.clear()
        bezierList.forEach { bezier ->
            val top = Math.min(bezier.start.point.y, Math.min(bezier.mid1.point.y, Math.min(bezier.mid2.point.y, bezier.end.point.y))) - delete.height
            val right = Math.max(bezier.start.point.x, Math.max(bezier.mid1.point.x, Math.max(bezier.mid2.point.x, bezier.end.point.x)))
            val left = right - delete.width
            val bottom = top + delete.height
            val deletePoint = BezierArea(left, top, right, bottom)
            bezierAreaList.add(deletePoint)
        }
        bezierCircleList.forEach { circle ->
            val top = circle.topMost() - delete.height
            val right = circle.rightMost()
            val left = right - delete.width
            val bottom = top + delete.height
            val deletePoint = BezierArea(left, top, right, bottom)
            bezierAreaList.add(deletePoint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var isMyControl = false

        val nowPoint = invertPoint(PointF(event.getX(0), event.getY(0)))

        when (event.action) {
            MotionEvent.ACTION_UP -> {

                when (state) {
                    is State.EDIT -> {
                        bezierList.forEach { bezier ->
                            bezier.start.draw = false
                            bezier.mid1.draw = false
                            bezier.mid2.draw = false
                            bezier.end.draw = false
                        }

                        bezierCircleList.forEach { circle ->
                            circle.topBezier.left.draw = false
                            circle.topBezier.middle.draw = false
                            circle.topBezier.right.draw = false

                            circle.bottomBezier.left.draw = false
                            circle.bottomBezier.middle.draw = false
                            circle.bottomBezier.right.draw = false

                            circle.leftBezier.top.draw = false
                            circle.leftBezier.middle.draw = false
                            circle.leftBezier.bottom.draw = false

                            circle.rightBezier.top.draw = false
                            circle.rightBezier.middle.draw = false
                            circle.rightBezier.bottom.draw = false

                            circle.center.draw = false
                        }
                    }

                    is State.DELETE -> {
                        touchIndex = NONE_TOUCH_AREA
                    }
                }
            }

            MotionEvent.ACTION_DOWN -> {

                when (state) {
                    is State.EDIT -> {
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
                                    bezier.start.point.x = nowPoint.x
                                    bezier.start.point.y = nowPoint.y
                                }

                                if (bezier.mid1.draw) {
                                    bezier.mid1.point.x = nowPoint.x
                                    bezier.mid1.point.y = nowPoint.y
                                }

                                if (bezier.mid2.draw) {
                                    bezier.mid2.point.x = nowPoint.x
                                    bezier.mid2.point.y = nowPoint.y
                                }

                                if (bezier.end.draw) {
                                    bezier.end.point.x = nowPoint.x
                                    bezier.end.point.y = nowPoint.y
                                }

                                if (min < 50) return@breaking
                            }

                            bezierCircleList.forEach { circle ->

                                val dis1 = getDistance(circle.topBezier.middle.point, nowPoint)

                                val dis2 = getDistance(circle.bottomBezier.middle.point, nowPoint)

                                val dis3 = getDistance(circle.leftBezier.middle.point, nowPoint)

                                val dis4 = getDistance(circle.rightBezier.middle.point, nowPoint)

                                val dis5 = getDistance(circle.center.point, nowPoint)

                                val min = Math.min(dis1 , Math.min(dis2 , Math.min(dis3 , Math.min(dis4 , dis5))))

                                if (min < 50) {
                                    when (min) {
                                        dis1 -> {
                                            circle.topBezier.middle.draw = true
                                        }

                                        dis2 -> {
                                            circle.bottomBezier.middle.draw = true
                                        }

                                        dis3 -> {
                                            circle.leftBezier.middle.draw = true
                                        }

                                        dis4 -> {
                                            circle.rightBezier.middle.draw = true
                                        }

                                        dis5 -> {
                                            circle.center.draw = true
                                        }

                                        else -> {
                                        }
                                    }
                                }

                                if (circle.topBezier.middle.draw) {
                                    circle.moveTop(nowPoint)
                                }

                                if (circle.bottomBezier.middle.draw) {
                                    circle.moveBottom(nowPoint)
                                }

                                if (circle.leftBezier.middle.draw) {
                                    circle.moveLeft(nowPoint)
                                }

                                if (circle.rightBezier.middle.draw) {
                                    circle.moveRight(nowPoint)
                                }

                                if (circle.center.draw) {
                                    circle.moveCenter(nowPoint)
                                }
                            }
                        }
                    }

                    is State.DELETE -> {
                        touchIndex = findTouchedBezierPoint(nowPoint)
                        if (touchIndex != NONE_TOUCH_AREA) {
                            bezierAreaList.removeAt(touchIndex)
                            if (bezierList.size - 1 >= touchIndex) {
                                bezierList.removeAt(touchIndex)
                            } else {
                                val baseIndex = bezierList.size
                                bezierCircleList.removeAt(touchIndex - baseIndex)
                            }
                            isMyControl = true
                            invalidate()
                        }
                    }

                    is State.COLOR -> {
                        touchIndex = findTouchedBezierPoint(nowPoint)
                        if (touchIndex != NONE_TOUCH_AREA && currentPaintColor != null) {
                            if (bezierList.size - 1 >= touchIndex) {
                                bezierList[touchIndex].color = currentPaintColor!!
                            } else {
                                val baseIndex = bezierList.size
                                bezierCircleList[touchIndex - baseIndex].color = currentPaintColor!!
                            }
                            isMyControl = true
                            invalidate()
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (state == State.EDIT) {
                    run breaking@ {
                        bezierList.forEach { bezier ->
                            if (bezier.start.draw) {
                                bezier.start.point.x = nowPoint.x
                                bezier.start.point.y = nowPoint.y
                            }

                            if (bezier.mid1.draw) {
                                bezier.mid1.point.x = nowPoint.x
                                bezier.mid1.point.y = nowPoint.y
                            }

                            if (bezier.mid2.draw) {
                                bezier.mid2.point.x = nowPoint.x
                                bezier.mid2.point.y = nowPoint.y
                            }

                            if (bezier.end.draw) {
                                bezier.end.point.x = nowPoint.x
                                bezier.end.point.y = nowPoint.y
                            }

                            if (bezier.start.draw || bezier.mid1.draw || bezier.mid2.draw || bezier.end.draw) {
                                isMyControl = true
                                return@breaking
                            }
                        }

                        bezierCircleList.forEach { circle ->

                            if (circle.topBezier.middle.draw) {
                                circle.moveTop(nowPoint)
                            }

                            if (circle.bottomBezier.middle.draw) {
                                circle.moveBottom(nowPoint)
                            }

                            if (circle.leftBezier.middle.draw) {
                                circle.moveLeft(nowPoint)
                            }

                            if (circle.rightBezier.middle.draw) {
                                circle.moveRight(nowPoint)
                            }

                            if (circle.center.draw) {
                                circle.moveCenter(nowPoint)
                            }

                            if (circle.topBezier.left.draw || circle.topBezier.middle.draw || circle.topBezier.right.draw ||
                                    circle.bottomBezier.left.draw || circle.bottomBezier.middle.draw || circle.bottomBezier.right.draw ||
                                    circle.leftBezier.top.draw || circle.leftBezier.middle.draw || circle.leftBezier.bottom.draw ||
                                    circle.rightBezier.top.draw || circle.rightBezier.middle.draw || circle.rightBezier.bottom.draw ||
                                    circle.center.draw) {
                                isMyControl = true
                                return@breaking
                            }
                        }
                    }
                }
            }
        }
        if (!isMyControl)
            controller.onTouch(this@Palette, event)

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

                val index = findTouchedBezierPoint(nowPoint)

                if (index != NONE_TOUCH_AREA) {
                    bezierAreaList.removeAt(index)
                    bezierList.removeAt(index)
                    touchIndex = NONE_TOUCH_AREA
                    invalidate()
                }

            }
        })
    }

    private fun findTouchedBezierPoint(nowPoint: PointF): Int {

        var index1 = NONE_TOUCH_AREA
        bezierAreaList.forEachIndexed { index, deletePoint ->
            val dis1 = getDistance(PointF(deletePoint.left, deletePoint.top), nowPoint)
            val dis2 = getDistance(PointF(deletePoint.left, deletePoint.down), nowPoint)
            val dis3 = getDistance(PointF(deletePoint.right, deletePoint.top), nowPoint)
            val dis4 = getDistance(PointF(deletePoint.left, deletePoint.down), nowPoint)
            val min = Math.min(dis1, Math.min(dis2, Math.min(dis3, dis4)))
            if (min < 50) {
                index1 = index
            }
        }
        return index1
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        //onTouchEvent(ev)
        //gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}