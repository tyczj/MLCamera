package com.tycz.mlcamera

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.View
import androidx.camera.view.PreviewView
import kotlin.math.ceil

class GraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val lock = Any()
    private val graphics = ArrayList<Graphic>()
    private var _imageWidth: Int = 0
    private var _imageHeight: Int = 0
    private var widthScaleFactor: Float = 0.0f
    private var heightScaleFactor: Float = 0.0f

    abstract class Graphic protected constructor(private val overlay: GraphicOverlay) {
        protected val context: Context = overlay.context

        /** Draws the graphic on the supplied canvas.  */
        abstract fun draw(canvas: Canvas)
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /**
     * Sets the dimensions for the processed image. This in turn sets the scale to display the overlay in the correct position
     */
    fun setImageDimens(imageWidth: Int, imageHeight: Int){
        _imageWidth = imageWidth
        _imageHeight = imageHeight

        setScale()
    }

    /**
     * Calculates the image scale and sets the height and width scale for the overlay
     */
    private fun setScale(){
        if(isPortraitMode()){
            heightScaleFactor = height.toFloat() / _imageWidth.toFloat()
            widthScaleFactor = width.toFloat() / _imageHeight.toFloat()
        }else{
            heightScaleFactor = height.toFloat() / _imageHeight.toFloat()
            widthScaleFactor = width.toFloat() / _imageWidth.toFloat()
        }
    }

    /**
     * Adjusts the `rect`'s coordinate from the preview's coordinate system to the view
     * coordinate system.
     */
    fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )

    /**
     * Translates the given x value to the correct scale of the overlay size
     */
    fun translateX(x: Float): Float = x * widthScaleFactor
    /**
     * Translates the given y value to the correct scale of the overlay size
     */
    fun translateY(y: Float): Float = y * heightScaleFactor

    /** Removes all graphics from the overlay.  */
    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    /** Adds a graphic to the overlay.  */
    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    /** Draws the overlay with its associated graphic objects.  */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            graphics.forEach { it.draw(canvas) }
        }
    }

    fun isPortraitMode(): Boolean = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}