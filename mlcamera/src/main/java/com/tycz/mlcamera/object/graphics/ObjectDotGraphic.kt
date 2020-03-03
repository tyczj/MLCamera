package com.tycz.mlcamera.`object`.graphics

import android.graphics.*
import android.util.Log
import android.util.Size
import com.tycz.mlcamera.GraphicOverlay
import com.tycz.mlcamera.R
import com.tycz.mlcamera.`object`.DetectedObject
import com.tycz.mlcamera.`object`.ObjectDotAnimator

/** A dot to indicate a detected object used by multiple objects detection mode.  */
internal class ObjectDotGraphic(private val overlay: GraphicOverlay, private val detectedObject: DetectedObject, private val animator: ObjectDotAnimator)
    :GraphicOverlay.Graphic(overlay) {

    private val _paint: Paint
    private val _center: PointF
    private val _dotRadius: Int
    private val _dotAlpha: Int
    private val _scaleX: Float
    private val _scaleY: Float

    init {

        val box = detectedObject.boundingBox

        if(overlay.isPortraitMode()){
            _scaleY = overlay.height.toFloat() / detectedObject.imageWidth.toFloat()
            _scaleX = overlay.width.toFloat() / detectedObject.imageHeight.toFloat()
        }else{
            _scaleY = overlay.height.toFloat() / detectedObject.imageHeight.toFloat()
            _scaleX = overlay.width.toFloat() / detectedObject.imageWidth.toFloat()
        }

        _center = PointF(
            translateX((box.left + box.right) / 2f),
            translateY((box.top + box.bottom) / 2f)
        )

        _paint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        _dotRadius = context.resources.getDimensionPixelOffset(R.dimen.object_dot_radius)
        _dotAlpha = _paint.alpha
    }

    override fun draw(canvas: Canvas) {
        _paint.alpha = (_dotAlpha * animator.alphaScale).toInt()
        canvas.drawCircle(_center.x, _center.y, _dotRadius * animator.radiusScale, _paint)
    }

    fun translateX(x: Float): Float = x * _scaleX
    fun translateY(y: Float): Float = y * _scaleY

    fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )
}