package com.willblaschko.android.recorderview

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View

/**
 * @author willb_000 on 5/5/2016.
 */
class RecorderView : View {

    private var rmsdbLevel = 0f
    internal val rotation = 0f

    internal lateinit var backgroundPaint: Paint
    internal lateinit var wavePaint: Paint

    internal var width = 0
    internal var height = 0
    internal var min = 0
    internal var imageSize: Int = 0
    internal var waveRotation = 30

    internal var microphone: Drawable? = null

    private val radius: Float
        get() {
            var percent = (rmsdbLevel * Math.log(rmsdbLevel.toDouble())).toFloat() * .01f
            percent = Math.min(Math.max(percent, 0f), 1f)
            percent = .55f + .45f * percent
            return percent * min.toFloat() / 2f
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        width = View.MeasureSpec.getSize(widthMeasureSpec)
        height = View.MeasureSpec.getSize(heightMeasureSpec)
        min = Math.min(width, height)

        imageSize = (min * .45).toInt()
        setRmsdbLevel(1f)
    }

    private fun init() {
        backgroundPaint = Paint()
        backgroundPaint.color = 0x66000000
        backgroundPaint.style = Paint.Style.FILL

        wavePaint = Paint()
        wavePaint.color = COLOR_INDICATOR_DEFAULT
        wavePaint.isAntiAlias = true
        wavePaint.style = Paint.Style.FILL
    }

    fun setRmsdbLevel(level: Float) {
        rmsdbLevel = level
        postInvalidate()
    }

    fun setIndicatorColor(color: Int) {
        wavePaint.color = color
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, wavePaint)

        if (microphone == null) {
            microphone = ContextCompat.getDrawable(context, R.drawable.microphone)
            microphone!!.isFilterBitmap = true
            microphone!!.setBounds((width - imageSize) / 2, (height - imageSize) / 2, width - (width - imageSize) / 2, height - (height - imageSize) / 2)
        }

        microphone!!.draw(canvas)

        //
        //        rotation+=ROTATION_SPEED;
        //        postInvalidateDelayed(100);
    }

    companion object {

        private val TAG = "RecorderView"

        private val ROTATION_SPEED = 1

        val COLOR_INDICATOR_DEFAULT = -0xc0ae4b
        val COLOR_INDICATOR_GONE = 0x00000000
    }
    //
    //    Path wavePath;
    //    private Path getPath(){
    //
    //        wavePath = new Path();
    //
    //        wavePath.moveTo();
    //
    //        for(int i = 0; i < 360 / waveRotation; i++){
    //
    //        }
    //
    //        return wavePath;
    //    }
    //
    //    private void getArcPoint(int degree, float value){
    //
    //    }
}
