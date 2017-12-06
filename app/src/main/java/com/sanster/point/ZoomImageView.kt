package com.sanster.point

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.sanster.point.utils.Constants

class ZoomImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    private val MIN_SCALE_FACTOR = 1.0F
    private val MAX_SCALE_FACTOR = 3.0F
    private var mActiveX: Float = 0F
    private var mActiveY: Float = 0F
    private var mMoveX: Float = 0F
    private var mMoveY: Float = 0F
    private var mScale: Float = 1.0F
    private var mScaleCenterX: Float = 0F
    private var mScaleCenterY: Float = 0F

    private var mCanvasClipsBounds: Rect = Rect()
    private val mScaleGestureDetector: ScaleGestureDetector

    init {
        mScaleGestureDetector = ScaleGestureDetector(this.context, ScaleListener())
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScale *= detector.scaleFactor

            mScale = Math.max(MIN_SCALE_FACTOR, Math.min(mScale, MAX_SCALE_FACTOR))

            // 缩放的焦点处于屏幕坐标系中
            mScaleCenterX = detector.focusX
            mScaleCenterY = detector.focusY

            invalidate()

            Log.d(Constants.TAG, "scale: $mScale scaleCenterX: $mScaleCenterX scaleCenterY: $mScaleCenterY")
            return true
        }
    }

    fun load(uriString: String) {
        Glide.with(this)
                .asBitmap()
                .load(uriString)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        setImageBitmap(resource)
                    }
                })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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

    override fun onDraw(canvas: Canvas) {
        Log.d(Constants.TAG, "onDraw")
        canvas.save()
        canvas.getClipBounds(mCanvasClipsBounds)
        canvas.scale(mScale, mScale, mScaleCenterX + mCanvasClipsBounds.left, mScaleCenterY + mCanvasClipsBounds.top)
        super.onDraw(canvas)
        canvas.restore()
    }
}

