package com.sanster.point

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.sanster.point.utils.Constants
import android.graphics.RectF


/**
 * The code for this class was adapted from the PhotoView project: https://github.com/chrisbanes/PhotoView
 */
class ZoomImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs), ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {
    private val mMaxScale: Float = 3.0F

    private var mActiveX: Float = 0F
    private var mActiveY: Float = 0F
    private var mMoveX: Float = 0F
    private var mMoveY: Float = 0F
    private var mInitScale: Float = 1.0F
    private var mScaleCenterX: Float = 0F
    private var mScaleCenterY: Float = 0F

    private var mBaseMatrix: Matrix = Matrix()
    private val mSuppMatrix: Matrix = Matrix()
    private var mDrawMatrix: Matrix = Matrix()

    private val mMatrixValues = FloatArray(9)
    private val mDisplayRect = RectF()

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
        val scaleFactor = detector.scaleFactor
        if (scaleFactor.isNaN() || scaleFactor.isInfinite()) return false

        if (drawable == null) return true

        val scale = getScale()

        if ((scale < mMaxScale || scaleFactor < 1f)
                && (scale > mInitScale || scaleFactor > 1f)) {
            mSuppMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            checkAndDisplayMatrix()
        }

        Log.d(Constants.TAG, "scale: ${getScale()} scaleCenterX: $mScaleCenterX scaleCenterY: $mScaleCenterY")

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


    override fun setImageBitmap(bm: Bitmap) {
        mBaseMatrix.reset()

        // 拿到图片的宽和高
        val dw = bm.width
        val dh = bm.height

//        // 如果图片的宽/高大于屏幕，则缩放至屏幕的宽/高
//        mInitScale = when {
//            dw > width && dh <= height -> width * 1.0f / dw
//            dh > height && dw <= width -> height * 1.0f / dh
//            dw > width && dh > height -> Math.min(width * 1.0f / dw, height * 1.0f / dh)
//            else -> mInitScale
//        }
//
//        // 图片移动至屏幕中心
//        mBaseMatrix.postTranslate((width - dw) / 2.0f, (height - dh) / 2.0f)
//        mBaseMatrix.postScale(mInitScale, mInitScale, width / 2.0f, height / 2.0f)

        val mTempSrc = RectF(0f, 0f, dw.toFloat(), dh.toFloat())
        val mTempDst = RectF(0f, 0f, width.toFloat(), height.toFloat())
        mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.CENTER)

        updateImageViewMatrix()

        super.setImageBitmap(bm)
    }

    fun getScale(): Float {
        mSuppMatrix.getValues(mMatrixValues)
        return mMatrixValues[Matrix.MSCALE_X]
    }

    /**
     * 在缩放时，进行图片显示范围的控制，防止图片的周围出现白边
     */
    private fun checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            updateImageViewMatrix()
        }
    }

    private fun checkMatrixBounds(): Boolean {
        val rect = getDisplayRect(getDrawMatrix()) ?: return false

        var deltaX: Float
        var deltaY: Float

        Log.d(Constants.TAG, rect.toString())
        Log.d(Constants.TAG, "rect.width: ${rect.width()}, rect.height: ${rect.height()}")
        Log.d(Constants.TAG, "width: $width, height: $height")

        deltaX = when {
            rect.width() <= width -> (width - rect.width()) / 2.0f - rect.left
            rect.left > 0 -> -rect.left
            rect.right < width -> width - rect.right
            else -> 0f
        }

        deltaY = when {
            rect.height() <= height -> (height - rect.height()) / 2.0f - rect.top
            rect.top > 0 -> -rect.top
            rect.bottom < height -> height - rect.bottom
            else -> 0f
        }

        Log.d(Constants.TAG, "deltaX: $deltaX, deltaY: $deltaY")

        mSuppMatrix.postTranslate(deltaX, deltaY)
        updateImageViewMatrix()

        return true
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private fun getDisplayRect(matrix: Matrix): RectF? {
        if (null != drawable) {
            mDisplayRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            matrix.mapRect(mDisplayRect)
            return mDisplayRect
        }
        return null
    }

    private fun updateImageViewMatrix() {
        imageMatrix = getDrawMatrix()
    }

    private fun getDrawMatrix(): Matrix {
        mDrawMatrix.set(mBaseMatrix)
        mDrawMatrix.postConcat(mSuppMatrix)
        return mDrawMatrix
    }
}

