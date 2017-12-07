package com.sanster.point

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.sanster.point.utils.Constants


class ZoomImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs), ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {
    private val MAX_SCALE_FACTOR = 3.0F
    private var mActiveX: Float = 0F
    private var mActiveY: Float = 0F
    private var mMoveX: Float = 0F
    private var mMoveY: Float = 0F
    private var mInitScale: Float = 1.0F
    private var mScaleCenterX: Float = 0F
    private var mScaleCenterY: Float = 0F

    private val mScaleMatrix: Matrix = Matrix()
    private val mTranMatrix: Matrix = Matrix()
    private var mDrawMatrix: Matrix = Matrix()
    private val mMatrixValues = FloatArray(9)

    private var mCanvasClipsBounds: Rect = Rect()
    private val mScaleGestureDetector: ScaleGestureDetector

    init {
        super.setScaleType(ScaleType.MATRIX)
        setOnTouchListener(this)
        mScaleGestureDetector = ScaleGestureDetector(context, this)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val scale = getScale()
        var scaleFactor = detector.scaleFactor

        if (drawable == null) return true

        if (scale in mInitScale..MAX_SCALE_FACTOR) {
            val s = scaleFactor * scale

            scaleFactor = when {
                s < mInitScale -> mInitScale / scale
                s > MAX_SCALE_FACTOR -> MAX_SCALE_FACTOR / scale
                else -> scaleFactor
            }

            mScaleMatrix.postScale(scaleFactor, scaleFactor, width / 2.0f, height / 2.0f)
            updateImageMatrix()
        }

        Log.d(Constants.TAG, "scale: $scale scaleCenterX: $mScaleCenterX scaleCenterY: $mScaleCenterY")

        return true
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        var handled = false

        handled = mScaleGestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mActiveX = event.x
                mActiveY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                mMoveX = event.x
                mMoveY = event.y
            }

        }

        return handled
    }

    //    override fun onDraw(canvas: Canvas) {
//        canvas.save()
//        canvas.getClipBounds(mCanvasClipsBounds)
//        canvas.scale(mInitScale, mInitScale, mScaleCenterX + mCanvasClipsBounds.left, mScaleCenterY + mCanvasClipsBounds.top)
//        super.onDraw(canvas)
//        canvas.restore()
//    }

    override fun setImageBitmap(bm: Bitmap) {
        // 拿到图片的宽和高
        val dw = bm.width
        val dh = bm.height

        // 如果图片的宽/高大于屏幕，则缩放至屏幕的宽/高
        mInitScale = when {
            dw > width && dh <= height -> width * 1.0f / dw
            dh > height && dw <= width -> height * 1.0f / dh
            dw > width && dh > height -> Math.min(width * 1.0f / dw, height * 1.0f / dh)
            else -> mInitScale
        }

        // 图片移动至屏幕中心
        mTranMatrix.postTranslate((width - dw) / 2.0f, (height - dh) / 2.0f)
        mScaleMatrix.postScale(mInitScale, mInitScale, width / 2.0f, height / 2.0f)

        updateImageMatrix()

        super.setImageBitmap(bm)
    }

    fun getScale(): Float {
        mDrawMatrix.getValues(mMatrixValues)
        return mMatrixValues[Matrix.MSCALE_X]
    }

    private fun updateImageMatrix() {
        mDrawMatrix.reset()
        mDrawMatrix.postConcat(mTranMatrix)
        mDrawMatrix.postConcat(mScaleMatrix)
        imageMatrix = mDrawMatrix
    }
}

