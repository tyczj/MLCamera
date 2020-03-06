package com.tycz.mlcamera.basic

import android.graphics.Rect

/**
 * Base object for holding basic information about the object
 *
 * @param width Processed image width
 * @param height Processed image height
 * @param bBox Bounding box of the detected object
 * @param boxColor Color of the bounding box overlay
 */
open class BasicDetectedObject(width:Int, height:Int, bBox: Rect, boxColor: Int) {

    val boundingBox: Rect = bBox
    val imageWidth:Int = width
    val imageHeight:Int = height
    val color: Int = boxColor
}