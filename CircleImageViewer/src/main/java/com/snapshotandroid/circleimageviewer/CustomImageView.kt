package com.snapshotandroid.circleimageviewer
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import com.snapshotandroid.circleimageviewer.R
import kotlin.math.pow

class CustomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyle) {

    private val SCALE_TYPE = ScaleType.CENTER_CROP

    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private val COLORDRAWABLE_DIMENSION = 2

    private val DEFAULT_BORDER_WIDTH = 0
    private val DEFAULT_BORDER_COLOR = Color.BLACK
    private val DEFAULT_CIRCLE_BACKGROUND_COLOR = Color.TRANSPARENT
    private val DEFAULT_IMAGE_ALPHA = 255
    private val DEFAULT_BORDER_OVERLAY = false

    private val mDrawableRect = RectF()
    private val mBorderRect = RectF()

    private val mShaderMatrix = Matrix()
    private val mBitmapPaint = Paint()
    private val mBorderPaint = Paint()
    private val mCircleBackgroundPaint = Paint()

    private var mBorderColor = DEFAULT_BORDER_COLOR
    private var mBorderWidth = DEFAULT_BORDER_WIDTH
    private var mCircleBackgroundColor = DEFAULT_CIRCLE_BACKGROUND_COLOR
    private var mImageAlpha = DEFAULT_IMAGE_ALPHA

    private var mBitmap: Bitmap? = null
    private var mBitmapCanvas: Canvas? = null

    private var mBorderRadius = 0f
    private var mCornerRadius = 0f

    private var mColorFilter: ColorFilter? = null

    private var mInitialized = false
    private var mRebuildShader = false
    private var mDrawableDirty = false

    private var mBorderOverlay = false
    private var mDisableCircularTransformation = false

    init {
        init()
        // Retrieve attributes from XML
        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomImageView, defStyle, 0)
        mBorderWidth = a.getDimensionPixelSize(R.styleable.CustomImageView_ssciv_border_width, DEFAULT_BORDER_WIDTH)
        mBorderOverlay = a.getBoolean(R.styleable.CustomImageView_ssciv_border_overlay, DEFAULT_BORDER_OVERLAY)
        mCircleBackgroundColor = a.getColor(R.styleable.CustomImageView_ssciv_custom_background_color, DEFAULT_CIRCLE_BACKGROUND_COLOR)
        mBorderColor = a.getColor(R.styleable.CustomImageView_ssciv_border_color, DEFAULT_BORDER_COLOR)
        mCornerRadius = a.getDimension(R.styleable.CustomImageView_ssciv_corner_radius, 0f)
        a.recycle()
    }

    private fun init() {
        mInitialized = true

        super.setScaleType(SCALE_TYPE)

        mBitmapPaint.isAntiAlias = true
        mBitmapPaint.isDither = true
        mBitmapPaint.isFilterBitmap = true
        mBitmapPaint.alpha = mImageAlpha
        mBitmapPaint.colorFilter = mColorFilter

        mBorderPaint.style = Paint.Style.STROKE
        mBorderPaint.isAntiAlias = true
        mBorderPaint.color = mBorderColor
        mBorderPaint.strokeWidth = mBorderWidth.toFloat()

        mCircleBackgroundPaint.style = Paint.Style.FILL
        mCircleBackgroundPaint.isAntiAlias = true
        mCircleBackgroundPaint.color = mCircleBackgroundColor


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = OutlineProvider()
        }
    }

    private fun updateDimensions(width: Int, height: Int) {
        mBorderRect.set(calculateBounds())
        mBorderRadius = Math.min((mBorderRect.height() - mBorderWidth) / 2.0f, (mBorderRect.width() - mBorderWidth) / 2.0f)

        mDrawableRect.set(mBorderRect)
        if (!mBorderOverlay && mBorderWidth > 0) {
            mDrawableRect.inset((mBorderWidth - 1.0f), (mBorderWidth - 1.0f))
        }

        // Update drawable radius
        val drawableRadius = Math.min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f)

        // Ensure corner radius is within bounds
        mCornerRadius = Math.min(mCornerRadius, drawableRadius)

        updateShaderMatrix()
    }

    override fun onDraw(canvas: Canvas) {
        if (mBitmap != null) {
            if (mDrawableDirty && mBitmapCanvas != null) {
                mDrawableDirty = false
                val drawable = drawable
                drawable.setBounds(0, 0, mBitmapCanvas!!.width, mBitmapCanvas!!.height)
                drawable.draw(mBitmapCanvas!!)
            }

            if (mRebuildShader) {
                mRebuildShader = false

                val bitmapShader = BitmapShader(mBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                bitmapShader.setLocalMatrix(mShaderMatrix)

                mBitmapPaint.shader = bitmapShader
            }

            // Draw rounded rectangle
            canvas.drawRoundRect(mDrawableRect, mCornerRadius, mCornerRadius, mBitmapPaint)
        }

        if (mBorderWidth > 0) {
            // Draw border
            canvas.drawRoundRect(mBorderRect, mCornerRadius, mCornerRadius, mBorderPaint)
        }
    }



    override fun invalidateDrawable(dr: Drawable) {
        mDrawableDirty = true
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDimensions(w, h)
        invalidate()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        updateDimensions(width, height)
        invalidate()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        updateDimensions(width, height)
        invalidate()
    }

    @ColorInt
    fun getBorderColor(): Int {
        return mBorderColor
    }

    fun setBorderColor(@ColorInt borderColor: Int) {
        if (borderColor == mBorderColor) {
            return
        }

        mBorderColor = borderColor
        mBorderPaint.color = borderColor
        invalidate()
    }

    @ColorInt
    fun getCircleBackgroundColor(): Int {
        return mCircleBackgroundColor
    }

    fun setCircleBackgroundColor(@ColorInt circleBackgroundColor: Int) {
        if (circleBackgroundColor == mCircleBackgroundColor) {
            return
        }

        mCircleBackgroundColor = circleBackgroundColor
        mCircleBackgroundPaint.color = circleBackgroundColor
        invalidate()
    }

    @Deprecated("Use setCircleBackgroundColor instead", ReplaceWith("setCircleBackgroundColor(circleBackgroundColor)"))
    fun setCircleBackgroundColorResource(@ColorRes circleBackgroundRes: Int) {
        setCircleBackgroundColor(context.resources.getColor(circleBackgroundRes))
    }

    fun getBorderWidth(): Int {
        return mBorderWidth
    }

    fun setBorderWidth(borderWidth: Int) {
        if (borderWidth == mBorderWidth) {
            return
        }

        mBorderWidth = borderWidth
        mBorderPaint.strokeWidth = borderWidth.toFloat()
        updateDimensions(width, height)
        invalidate()
    }

    fun isBorderOverlay(): Boolean {
        return mBorderOverlay
    }

    fun setBorderOverlay(borderOverlay: Boolean) {
        if (borderOverlay == mBorderOverlay) {
            return
        }

        mBorderOverlay = borderOverlay
        updateDimensions(width, height)
        invalidate()
    }

    fun isDisableCircularTransformation(): Boolean {
        return mDisableCircularTransformation
    }

    fun setDisableCircularTransformation(disableCircularTransformation: Boolean) {
        if (disableCircularTransformation == mDisableCircularTransformation) {
            return
        }

        mDisableCircularTransformation = disableCircularTransformation

        if (disableCircularTransformation) {
            mBitmap = null
            mBitmapCanvas = null
            mBitmapPaint.shader = null
        } else {
            initializeBitmap()
        }

        invalidate()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        initializeBitmap()
        invalidate()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
        invalidate()
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
        invalidate()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initializeBitmap()
        invalidate()
    }

    override fun setImageAlpha(alpha: Int) {
        var alpha = alpha
        alpha = alpha and 0xFF

        if (alpha == mImageAlpha) {
            return
        }

        mImageAlpha = alpha

        if (mInitialized) {
            mBitmapPaint.alpha = alpha
            invalidate()
        }
    }

    override fun getImageAlpha(): Int {
        return mImageAlpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        if (cf == mColorFilter) {
            return
        }

        mColorFilter = cf

        if (mInitialized) {
            mBitmapPaint.colorFilter = cf
            invalidate()
        }
    }

    override fun getColorFilter(): ColorFilter? {
        return mColorFilter
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        return try {
            val bitmap: Bitmap

            if (drawable is ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG)
            } else {
                bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, BITMAP_CONFIG)
            }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun initializeBitmap() {
        mBitmap = getBitmapFromDrawable(drawable)

        if (mBitmap != null && mBitmap!!.isMutable) {
            mBitmapCanvas = Canvas(mBitmap!!)
        } else {
            mBitmapCanvas = null
        }

        if (!mInitialized) {
            return
        }

        if (mBitmap != null) {
            updateShaderMatrix()
        } else {
            mBitmapPaint.shader = null
        }
    }

    private fun calculateBounds(): RectF {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = left + availableWidth.toFloat()
        val bottom = top + availableHeight.toFloat()

        return RectF(left, top, right, bottom)
    }

    private fun updateShaderMatrix() {
        if (mBitmap == null) {
            return
        }

        val bitmapHeight = mBitmap!!.height
        val bitmapWidth = mBitmap!!.width

        val scale: Float
        var dx = 0f
        var dy = 0f

        mShaderMatrix.set(null)

        if (bitmapWidth * mDrawableRect.height() > mDrawableRect.width() * bitmapHeight) {
            scale = mDrawableRect.height() / bitmapHeight.toFloat()
            dx = (mDrawableRect.width() - bitmapWidth * scale) * 0.5f
        } else {
            scale = mDrawableRect.width() / bitmapWidth.toFloat()
            dy = (mDrawableRect.height() - bitmapHeight * scale) * 0.5f
        }

        mShaderMatrix.setScale(scale, scale)
        mShaderMatrix.postTranslate((dx + 0.5f) + mDrawableRect.left, (dy + 0.5f) + mDrawableRect.top)

        mRebuildShader = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mDisableCircularTransformation) {
            return super.onTouchEvent(event)
        }

        return inTouchableArea(event.x, event.y) && super.onTouchEvent(event)
    }

    private fun inTouchableArea(x: Float, y: Float): Boolean {
        if (mBorderRect.isEmpty) {
            return true
        }

        val centerX = mBorderRect.centerX()
        val centerY = mBorderRect.centerY()

        return (x - centerX).pow(2) + (y - centerY).pow(2) <= mBorderRadius.pow(2)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private inner class OutlineProvider : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            if (mDisableCircularTransformation) {
                ViewOutlineProvider.BACKGROUND.getOutline(view, outline)
            } else {
                val bounds = Rect()
                mBorderRect.roundOut(bounds)
                outline.setRoundRect(bounds, mCornerRadius)
            }
        }
    }
}
