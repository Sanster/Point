package com.sanster.point

import android.content.Context
import android.graphics.Bitmap
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

class ZoomImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    private val MIN_SCALE_FACTOR = 1.0F
    private val MAX_SCALE_FACTOR = 3.0F
    private var mActiveX: Float = 0F
    private var mActiveY: Float = 0F
    private var mMoveX: Float = 0F
    private var mMoveY: Float = 0F
    private var mScaleFactor: Float = 1.0F

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor

            mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(mScaleFactor, MAX_SCALE_FACTOR))

            // 缩放的焦点处于屏幕坐标系中
            mActiveX = detector.focusX
            mActiveY = detector.focusY
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
        return super.onTouchEvent(event)
    }

}

