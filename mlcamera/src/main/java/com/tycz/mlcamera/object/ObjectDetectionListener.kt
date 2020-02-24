package com.tycz.mlcamera.`object`

interface ObjectDetectionListener {

    /**
     * Callback for when an object has been detected and processed
     * @param detectedObject Object data that was processed
     */
    fun objectDetected(detectedObject: DetectedObject)

    /**
     * Callback for when an object is found and called before any additional processing of the object happens
     */
    fun objectProcessing()
}