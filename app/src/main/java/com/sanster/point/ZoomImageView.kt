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
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator


/**
 * The code for this class was adapted from the PhotoView project: https://github.com/chrisbanes/PhotoView
 */
class ZoomImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs),
        ScaleGestureDetector.OnScaleGestureListener,
        View.OnTouchListener {

    // scale apply to baseMatrix
    private val MAX_SCALE: Float = 3.0f
    private val MIN_SCALE: Float = 1.0f
    private val INVALID_POINTER_ID: Int = -1

    private val mZoomDuration: Int = 200
    private val mInterpolator = AccelerateDecelerateInterpolator()

    private var mTouchSlop: Int

    private var mActivePointerId: Int = INVALID_POINTER_ID
    private var mActivePointerIndex: Int = 0
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f
    private var mIsDragging: Boolean = false

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
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
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

        onScale(scaleFactor, detector.focusX, detector.focusY)

        Log.d(Constants.TAG, "scale: ${getScale()}")

        return true
    }

    private fun onScale(scaleFactor: Float, focusX: Float, focuxY: Float) {
        val scale = getScale()

        if ((scale < (MAX_SCALE * 1.1) || scaleFactor < 1f)
                && (scale > (MIN_SCALE * 0.9) || scaleFactor > 1f)) {

            mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focuxY)
            checkAndDisplayMatrix()
        }
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        var handled = false

        handled = mScaleGestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = event.getPointerId(0)

                mLastTouchX = getActiveX(event)
                mLastTouchY = getActiveY(event)

                mIsDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val x = getActiveX(event)
                val y = getActiveY(event)
                val dx = x - mLastTouchX
                val dy = y - mLastTouchY

                if (event.pointerCount > 1) {
                    if (!mIsDragging) {
                        mIsDragging = shouldStartDrag(dx, dy)
                    }
                } else {
                    mIsDragging = false
                }

                mLastTouchX = x
                mLastTouchY = y

                if (mIsDragging) {
                    onDrag(dx, dy)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID

                val s = getScale()
                val targetScale = when {
                    s < MIN_SCALE -> MIN_SCALE
                    s > MAX_SCALE -> MAX_SCALE
                    else -> s
                }

                if (targetScale != s) {
                    val rect = getDisplayRect()
                    if (rect != null) {
                        v.post(AnimatedZoomRunnable(getScale(), targetScale, rect.centerX(), rect.centerY()))
                        handled = true
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = getPointerIndex(event.action)
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = when (pointerIndex) {
                        0 -> 1
                        else -> 0
                    }
                    mActivePointerId = event.getPointerId(newPointerIndex)
                    mLastTouchX = event.getX(newPointerIndex)
                    mLastTouchY = event.getY(newPointerIndex)
                }
            }
        }

        mActivePointerIndex = when (mActivePointerId) {
            INVALID_POINTER_ID -> event.findPointerIndex(0)
            else -> event.findPointerIndex(mActivePointerId)
        }
        return handled
    }

    private fun onDrag(dx: Float, dy: Float) {
        mSuppMatrix.postTranslate(dx, dy)
        checkAndDisplayMatrix()
    }

    private fun getPointerIndex(action: Int): Int {
        return action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
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

    public fun getScale(): Float {
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

    public fun getDisplayRect(): RectF? {
        checkMatrixBounds()
        return getDisplayRect(getDrawMatrix())
    }

    private fun updateImageViewMatrix() {
        imageMatrix = getDrawMatrix()
    }

    private fun getDrawMatrix(): Matrix {
        mDrawMatrix.set(mBaseMatrix)
        mDrawMatrix.postConcat(mSuppMatrix)
        return mDrawMatrix
    }

    private fun shouldStartDrag(dx: Float, dy: Float): Boolean {
        return Math.sqrt(((dx * dx) + (dy * dy)).toDouble()) >= mTouchSlop
    }

    private fun getActiveX(event: MotionEvent): Float {
        return try {
            event.getX(mActivePointerId)
        } catch (e: Exception) {
            event.x
        }
    }

    private fun getActiveY(event: MotionEvent): Float {
        return try {
            event.getY(mActivePointerId)
        } catch (e: Exception) {
            event.y
        }
    }


    inner class AnimatedZoomRunnable(
            currentZoom: Float,
            targetZoom: Float,
            focalX: Float,
            focalY: Float
    ) : Runnable {
        private var mFocalX: Float = 0F
        private var mFocalY: Float = 0F
        private var mZoomStart: Float = 0F
        private var mZoomEnd: Float = 0F
        private var mStartTime: Long = 0L

        init {
            mFocalX = focalX
            mFocalY = focalY
            mZoomStart = currentZoom
            mZoomEnd = targetZoom
            mStartTime = System.currentTimeMillis()
        }

        override fun run() {
            val t = interpolate()
            val scale = mZoomStart + t * (mZoomEnd - mZoomStart)
            val deltaScale = scale / getScale()

            onScale(deltaScale, mFocalX, mFocalY)

            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
//                Compat.postOnAnimation(mImageView, this)
                this@ZoomImageView.postDelayed(this@AnimatedZoomRunnable, 1000 / 60)
            }
        }

        private fun interpolate(): Float {
            var t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration
            t = Math.min(1f, t)
            t = mInterpolator.getInterpolation(t)
            return t
        }
    }
}

