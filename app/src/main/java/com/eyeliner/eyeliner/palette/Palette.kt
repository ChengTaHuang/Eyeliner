package com.eyeliner.eyeliner.palette

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.views.interfaces.GestureView
import com.eyeliner.eyeliner.R
import com.eyeliner.eyeliner.palette.model.Anchor
import com.eyeliner.eyeliner.palette.model.Bezier
import com.eyeliner.eyeliner.palette.model.BezierCircle
import com.eyeliner.eyeliner.palette.model.BezierShape
import kotlinx.android.synthetic.main.activity_main.*


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
        object ROTATE : State()

    }

    private var state: State = State.EDIT

    private val bitmapPaint: Paint = Paint()
    private val pointsPaint: Paint = Paint()
    private val pointsBiggerPaint: Paint = Paint()
    private val linePaint: Paint = Paint()
    private val centerPaint: Paint = Paint()
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

    private lateinit var delete: Bitmap
    private lateinit var colorPalette: Bitmap
    private lateinit var whirling: Bitmap

    private var backgroundBitmap: Bitmap? = null
    private val bezierCirclePath = Path()

    private var radius = 100f
    private val touchPointsRecord = mutableListOf<PointF>()
    private var lastAngle = 0.0

    sealed class BezierViewType{
        data class Line(val bezier: Bezier) : BezierViewType()
        data class Circle(val bezierCircle: BezierCircle) : BezierViewType()
    }

    private val bezierViewTypeList = mutableListOf<BezierViewType>()

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        this.backgroundBitmap = bitmap
        settingController()
        invalidate()
    }

    fun getBackgroundBitmap() = this.backgroundBitmap

    constructor(context: Context) : super(context) {
        this.setWillNotDraw(false)
        initPaint()
        createAllBitmap()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.setWillNotDraw(false)
        initPaint()
        createAllBitmap()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        this.setWillNotDraw(false)
        initPaint()
        createAllBitmap()
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

        with(centerPaint){
            strokeWidth = 5f
            color = ContextCompat.getColor(context, pointColor)
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
    }

    fun addBezier() {
        bezierViewTypeList.add(BezierViewType.Line(createBezier()))
        invalidate()
    }

    fun addBezierCircle() {
        bezierViewTypeList.add(BezierViewType.Circle(createBezierCircle()))
        invalidate()
    }

    fun changeSate(state: State) {
        this.state = state
        invalidate()
    }

    fun resetScale() {
        _matrix = Matrix()
        controller.resetState()
        invalidate()
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

    private fun createAllBitmap(){
        createDelete()
        createColorPalette()
        createWhirling()
    }

    private fun createDelete() {
        val d = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!
        delete = drawableToBitmap(d)
    }

    private fun createColorPalette() {
        val p = ContextCompat.getDrawable(context, R.drawable.ic_color_palette)!!
        colorPalette = drawableToBitmap(p)
    }

    private fun createWhirling() {
        val r = ContextCompat.getDrawable(context, R.drawable.ic_rotate)!!
        whirling = drawableToBitmap(r)
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
        return Bezier(anchor, anchor2, anchor3, anchor4,
                ContextCompat.getColor(context, R.color.colorPoint),
                delete.width.toFloat() , delete.height.toFloat())
    }

    private fun createBezierCircle(): BezierCircle {
        return BezierCircle(
                Anchor(PointF(200f, 200f), false),
                radius,
                ContextCompat.getColor(context, R.color.colorPoint),
                delete.width.toFloat() , delete.height.toFloat())
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

        drawBackground(canvas)
        drawBezier(canvas)


        when (state) {
            is State.EDIT -> {

                bezierViewTypeList.forEach { viewType ->
                    when(viewType){
                        is Palette.BezierViewType.Line -> {
                            canvas.drawCircle(viewType.bezier.start.point.x, viewType.bezier.start.point.y, pointsBiggerRadius, pointsBiggerPaint)
                            canvas.drawCircle(viewType.bezier.mid1.point.x, viewType.bezier.mid1.point.y, pointsBiggerRadius, pointsBiggerPaint)
                            canvas.drawCircle(viewType.bezier.mid2.point.x, viewType.bezier.mid2.point.y, pointsBiggerRadius, pointsBiggerPaint)
                            canvas.drawCircle(viewType.bezier.end.point.x, viewType.bezier.end.point.y, pointsBiggerRadius, pointsBiggerPaint)

                            pointsPaint.color = viewType.bezier.color
                            canvas.drawCircle(viewType.bezier.start.point.x, viewType.bezier.start.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezier.mid1.point.x, viewType.bezier.mid1.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezier.mid2.point.x, viewType.bezier.mid2.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezier.end.point.x, viewType.bezier.end.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezier.center().point.x, viewType.bezier.center().point.y, pointsRadius, centerPaint)
                        }
                        is Palette.BezierViewType.Circle -> {
                            canvas.drawCircle(viewType.bezierCircle.topBezier.middle.point.x, viewType.bezierCircle.topBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                            canvas.drawCircle(viewType.bezierCircle.bottomBezier.middle.point.x, viewType.bezierCircle.bottomBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                            canvas.drawCircle(viewType.bezierCircle.leftBezier.middle.point.x, viewType.bezierCircle.leftBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                            canvas.drawCircle(viewType.bezierCircle.rightBezier.middle.point.x, viewType.bezierCircle.rightBezier.middle.point.y, pointsBiggerRadius, pointsBiggerPaint)
                            canvas.drawCircle(viewType.bezierCircle.center.point.x, viewType.bezierCircle.center.point.y, pointsBiggerRadius, pointsBiggerPaint)

                            pointsPaint.color = viewType.bezierCircle.color
                            canvas.drawCircle(viewType.bezierCircle.topBezier.middle.point.x, viewType.bezierCircle.topBezier.middle.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezierCircle.bottomBezier.middle.point.x, viewType.bezierCircle.bottomBezier.middle.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezierCircle.leftBezier.middle.point.x, viewType.bezierCircle.leftBezier.middle.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezierCircle.rightBezier.middle.point.x, viewType.bezierCircle.rightBezier.middle.point.y, pointsRadius, pointsPaint)
                            canvas.drawCircle(viewType.bezierCircle.center.point.x, viewType.bezierCircle.center.point.y, pointsRadius, centerPaint)
                        }
                    }
                }
            }

            is State.DELETE -> {
                drawSquareAndIcon(canvas , delete)
            }

            is State.COLOR -> {
                drawSquareAndIcon(canvas , colorPalette)
            }

            is State.ROTATE -> {
                drawSquareAndIcon(canvas , whirling)
            }
        }

        canvas.restore()
    }

    /*
        畫上背景圖、線條、圓形
     */
    private fun drawBackground(canvas : Canvas){
        backgroundBitmap?.run {
            canvas.drawBitmap(backgroundBitmap, 0f, 0f, bitmapPaint)
        }
    }

    private fun drawBezier(canvas : Canvas){
        bezierViewTypeList.forEach { viewType ->
            when(viewType){
                is Palette.BezierViewType.Line -> {
                    bezierPath.reset()
                    bezierPath.moveTo(viewType.bezier.start.point.x, viewType.bezier.start.point.y)
                    bezierPath.cubicTo(
                            viewType.bezier.mid1.point.x, viewType.bezier.mid1.point.y,
                            viewType.bezier.mid2.point.x, viewType.bezier.mid2.point.y,
                            viewType.bezier.end.point.x, viewType.bezier.end.point.y
                    )
                    linePaint.color = viewType.bezier.color
                    canvas.drawPath(bezierPath, linePaint)
                }
                is Palette.BezierViewType.Circle -> {
                    bezierCirclePath.reset()

                    bezierCirclePath.reset()

                    bezierCirclePath.moveTo(viewType.bezierCircle.topBezier.middle.point.x, viewType.bezierCircle.topBezier.middle.point.y)
                    bezierCirclePath.cubicTo(
                            viewType.bezierCircle.topBezier.right.point.x, viewType.bezierCircle.topBezier.right.point.y,
                            viewType.bezierCircle.rightBezier.top.point.x, viewType.bezierCircle.rightBezier.top.point.y,
                            viewType.bezierCircle.rightBezier.middle.point.x, viewType.bezierCircle.rightBezier.middle.point.y)

                    bezierCirclePath.cubicTo(
                            viewType.bezierCircle.rightBezier.bottom.point.x, viewType.bezierCircle.rightBezier.bottom.point.y,
                            viewType.bezierCircle.bottomBezier.right.point.x, viewType.bezierCircle.bottomBezier.right.point.y,
                            viewType.bezierCircle.bottomBezier.middle.point.x, viewType.bezierCircle.bottomBezier.middle.point.y)

                    bezierCirclePath.cubicTo(
                            viewType.bezierCircle.bottomBezier.left.point.x, viewType.bezierCircle.bottomBezier.left.point.y,
                            viewType.bezierCircle.leftBezier.bottom.point.x, viewType.bezierCircle.leftBezier.bottom.point.y,
                            viewType.bezierCircle.leftBezier.middle.point.x, viewType.bezierCircle.leftBezier.middle.point.y)

                    bezierCirclePath.cubicTo(
                            viewType.bezierCircle.leftBezier.top.point.x, viewType.bezierCircle.leftBezier.top.point.y,
                            viewType.bezierCircle.topBezier.left.point.x, viewType.bezierCircle.topBezier.left.point.y,
                            viewType.bezierCircle.topBezier.middle.point.x, viewType.bezierCircle.topBezier.middle.point.y)

                    linePaint.color = viewType.bezierCircle.color
                    canvas.drawPath(bezierCirclePath, linePaint)
                }
            }
        }
    }

    /*
        畫個圖形的邊框以及對應操作圖示
     */
    private fun drawSquareAndIcon(canvas : Canvas , bitmap: Bitmap){
        bezierViewTypeList.forEach { viewType ->
            when(viewType){
                is Palette.BezierViewType.Line -> {
                    if(!viewType.bezier.rotate)
                        canvas.drawBitmap(bitmap, viewType.bezier.getDrawIconPoint().x, viewType.bezier.getDrawIconPoint().y, bitmapPaint)
                }
                is Palette.BezierViewType.Circle -> {
                    if(!viewType.bezierCircle.rotate)
                        canvas.drawBitmap(bitmap, viewType.bezierCircle.getDrawIconPoint().x, viewType.bezierCircle.getDrawIconPoint().y, bitmapPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var isMyControl = false

        val nowPoint = invertPoint(PointF(event.getX(0), event.getY(0)))

        when (event.action) {
            MotionEvent.ACTION_UP -> {
                touchPointsRecord.clear()
                lastAngle = 0.0

                bezierViewTypeList.forEach { viewType ->
                    when (viewType) {
                        is Palette.BezierViewType.Line -> {
                            viewType.bezier.setEnableDraw(Bezier.AnchorType.START , false)
                            viewType.bezier.setEnableDraw(Bezier.AnchorType.MID1 , false)
                            viewType.bezier.setEnableDraw(Bezier.AnchorType.MID2 , false)
                            viewType.bezier.setEnableDraw(Bezier.AnchorType.END , false)
                            viewType.bezier.setEnableDraw(Bezier.AnchorType.CENTER , false)
                            viewType.bezier.rotate = false
                        }
                        is Palette.BezierViewType.Circle -> {
                            viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.TOP , false)
                            viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.LEFT , false)
                            viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.RIGHT , false)
                            viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.BOTTOM , false)
                            viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.CENTER , false)
                            viewType.bezierCircle.rotate = false
                        }
                    }
                }
            }

            MotionEvent.ACTION_DOWN -> {

                when (state) {
                    is State.EDIT -> {
                        run breaking@ {
                            bezierViewTypeList.forEach { viewType ->
                                when (viewType) {
                                    is Palette.BezierViewType.Line -> {
                                        when (viewType.bezier.closeAnchor(nowPoint)) {
                                            Bezier.AnchorType.START -> viewType.bezier.setEnableDraw(Bezier.AnchorType.START, true)
                                            Bezier.AnchorType.MID1 -> viewType.bezier.setEnableDraw(Bezier.AnchorType.MID1, true)
                                            Bezier.AnchorType.MID2 -> viewType.bezier.setEnableDraw(Bezier.AnchorType.MID2, true)
                                            Bezier.AnchorType.END -> viewType.bezier.setEnableDraw(Bezier.AnchorType.END, true)
                                            Bezier.AnchorType.CENTER -> viewType.bezier.setEnableDraw(Bezier.AnchorType.CENTER, true)
                                            else -> {
                                            }
                                        }
                                        when (viewType.bezier.getDraw()) {
                                            Bezier.AnchorType.START -> viewType.bezier.moveStart(nowPoint)
                                            Bezier.AnchorType.MID1 -> viewType.bezier.moveMid1(nowPoint)
                                            Bezier.AnchorType.MID2 -> viewType.bezier.moveMid2(nowPoint)
                                            Bezier.AnchorType.END -> viewType.bezier.moveEnd(nowPoint)
                                            Bezier.AnchorType.CENTER -> viewType.bezier.moveCenter(nowPoint)
                                            else -> {
                                            }
                                        }

                                        when (viewType.bezier.closeAnchor(nowPoint)) {
                                            Bezier.AnchorType.START, Bezier.AnchorType.MID1, Bezier.AnchorType.MID2, Bezier.AnchorType.END, Bezier.AnchorType.CENTER -> {
                                                return@breaking
                                            }
                                        }
                                    }
                                    is Palette.BezierViewType.Circle -> {
                                        when (viewType.bezierCircle.closeAnchor(nowPoint)) {
                                            BezierCircle.AnchorType.LEFT -> viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.LEFT , true)
                                            BezierCircle.AnchorType.RIGHT -> viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.RIGHT , true)
                                            BezierCircle.AnchorType.TOP -> viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.TOP , true)
                                            BezierCircle.AnchorType.BOTTOM -> viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.BOTTOM , true)
                                            BezierCircle.AnchorType.CENTER -> viewType.bezierCircle.setEnableDraw(BezierCircle.AnchorType.CENTER , true)
                                            else -> {}
                                        }

                                        when (viewType.bezierCircle.getDraw()) {
                                            BezierCircle.AnchorType.LEFT -> viewType.bezierCircle.moveLeft(nowPoint)
                                            BezierCircle.AnchorType.RIGHT -> viewType.bezierCircle.moveRight(nowPoint)
                                            BezierCircle.AnchorType.TOP -> viewType.bezierCircle.moveTop(nowPoint)
                                            BezierCircle.AnchorType.BOTTOM -> viewType.bezierCircle.moveBottom(nowPoint)
                                            BezierCircle.AnchorType.CENTER -> viewType.bezierCircle.moveCenter(nowPoint)
                                            else -> {}
                                        }

                                        when (viewType.bezierCircle.closeAnchor(nowPoint)) {
                                            BezierCircle.AnchorType.LEFT , BezierCircle.AnchorType.RIGHT , BezierCircle.AnchorType.TOP , BezierCircle.AnchorType.BOTTOM , BezierCircle.AnchorType.CENTER -> {
                                                return@breaking
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is State.DELETE -> {
                        run breaking@ {
                            bezierViewTypeList.forEach { viewType ->
                                when (viewType) {
                                    is Palette.BezierViewType.Line -> {
                                        if (viewType.bezier.isTouchEditIcon(nowPoint)) {
                                            bezierViewTypeList.remove(viewType)
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                    is Palette.BezierViewType.Circle -> {
                                        if (viewType.bezierCircle.isTouchEditIcon(nowPoint)) {
                                            bezierViewTypeList.remove(viewType)
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is State.COLOR -> {
                        run breaking@ {
                            bezierViewTypeList.forEach { viewType ->
                                when (viewType) {
                                    is Palette.BezierViewType.Line -> {
                                        if (viewType.bezier.isTouchEditIcon(nowPoint)) {
                                            viewType.bezier.color = currentPaintColor!!
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                    is Palette.BezierViewType.Circle -> {
                                        if (viewType.bezierCircle.isTouchEditIcon(nowPoint)) {
                                            viewType.bezierCircle.color = currentPaintColor!!
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                }
                            }
                        }

                    }

                    is State.ROTATE -> {
                        run breaking@ {
                            bezierViewTypeList.forEach { viewType ->
                                when (viewType) {
                                    is Palette.BezierViewType.Line -> {
                                        if (viewType.bezier.isTouchEditIcon(nowPoint)) {
                                            touchPointsRecord.add(nowPoint)
                                            viewType.bezier.rotate = true
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                    is Palette.BezierViewType.Circle -> {
                                        if (viewType.bezierCircle.isTouchEditIcon(nowPoint)) {
                                            touchPointsRecord.add(nowPoint)
                                            viewType.bezierCircle.rotate = true
                                            isMyControl = true
                                            return@breaking
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (state) {
                    is State.EDIT -> {
                        run breaking@ {
                            bezierViewTypeList.forEach { viewType ->
                                when (viewType) {
                                    is Palette.BezierViewType.Line -> {
                                        when (viewType.bezier.getDraw()) {
                                            Bezier.AnchorType.START -> viewType.bezier.moveStart(nowPoint)
                                            Bezier.AnchorType.MID1 -> viewType.bezier.moveMid1(nowPoint)
                                            Bezier.AnchorType.MID2 -> viewType.bezier.moveMid2(nowPoint)
                                            Bezier.AnchorType.END -> viewType.bezier.moveEnd(nowPoint)
                                            Bezier.AnchorType.CENTER -> viewType.bezier.moveCenter(nowPoint)
                                            else -> {
                                            }
                                        }

                                        viewType.bezier.getDraw()?.let {
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                    is Palette.BezierViewType.Circle -> {
                                        when (viewType.bezierCircle.getDraw()) {
                                            BezierCircle.AnchorType.TOP -> viewType.bezierCircle.moveTop(nowPoint)
                                            BezierCircle.AnchorType.BOTTOM -> viewType.bezierCircle.moveBottom(nowPoint)
                                            BezierCircle.AnchorType.LEFT -> viewType.bezierCircle.moveLeft(nowPoint)
                                            BezierCircle.AnchorType.RIGHT -> viewType.bezierCircle.moveRight(nowPoint)
                                            BezierCircle.AnchorType.CENTER -> viewType.bezierCircle.moveCenter(nowPoint)
                                            else -> {
                                            }
                                        }

                                        viewType.bezierCircle.getDraw()?.let {
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is State.ROTATE -> {
                        run breaking@ {
                            bezierViewTypeList.forEach { viewType ->
                                when (viewType) {
                                    is Palette.BezierViewType.Line -> {
                                        if (viewType.bezier.rotate) {
                                            touchPointsRecord.add(nowPoint)
                                            val matrix = calcRotate(viewType.bezier)
                                            viewType.bezier.rotate(matrix)
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                    is Palette.BezierViewType.Circle -> {
                                        if (viewType.bezierCircle.rotate) {
                                            touchPointsRecord.add(nowPoint)
                                            val matrix = calcRotate(viewType.bezierCircle)
                                            viewType.bezierCircle.rotate(matrix)
                                            isMyControl = true
                                            return@breaking
                                        }
                                    }
                                }
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

    private fun calcRotate(shape: BezierShape) : Matrix {
        val now = touchPointsRecord[touchPointsRecord.size -1]
        val angle = (Math.toDegrees(Math.atan2((shape.center().point.y - now.y).toDouble(), (shape.center().point.x - now.x).toDouble())))
        if(lastAngle == 0.0) lastAngle = angle
        val matrixAngle = Matrix()
        matrixAngle.postRotate((angle.toFloat() - lastAngle).toFloat(), shape.center().point.x, shape.center().point.y)
        lastAngle = angle
        return matrixAngle
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.dispatchTouchEvent(ev)
        } catch (e: Exception) {
            false
        }

    }
}