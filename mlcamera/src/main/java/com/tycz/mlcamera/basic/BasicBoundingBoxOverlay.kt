package com.tycz.mlcamera.basic

import android.graphics.*
import com.tycz.mlcamera.GraphicOverlay

/**
 * Displays the bounding box of the detected object
 */
class BasicBoundingBoxOverlay(overlay: GraphicOverlay, detectedObject: BasicDetectedObject): GraphicOverlay.Graphic(overlay) {

    private val _paint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = detectedObject.color
        strokeWidth = 5f
    }

    private val _scaleX: Float
    private val _scaleY: Float
    private val _scaledBoundingBox: RectF

    init {
        if(overlay.isPortraitMode()){
            _scaleY = overlay.height.toFloat() / detectedObject.imageWidth.toFloat()
            _scaleX = overlay.width.toFloat() / detectedObject.imageHeight.toFloat()
        }else{
            _scaleY = overlay.height.toFloat() / detectedObject.imageHeight.toFloat()
            _scaleX = overlay.width.toFloat() / detectedObject.imageWidth.toFloat()
        }

        _scaledBoundingBox = translateRect(detectedObject.boundingBox)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(_scaledBoundingBox, _paint)
    }

    private fun translateX(x: Float): Float = x * _scaleX
    private fun translateY(y: Float): Float = y * _scaleY

    private fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )
}