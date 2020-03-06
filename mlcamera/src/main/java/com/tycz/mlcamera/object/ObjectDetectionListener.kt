package com.tycz.mlcamera.`object`

import com.google.firebase.ml.vision.objects.FirebaseVisionObject

interface ObjectDetectionListener {

    /**
     * Callback for when an object has been detected and processed
     * Not called when using BasicObjectAnalyzer
     * @param detectedObject Object data that was processed
     */
    fun objectDetected(detectedObject: DetectedObject)

    /**
     * Callback for when an object is found and called before any additional processing of the object happens
     * Not called when using BasicObjectAnalyzer
     */
    fun objectProcessing()

    /**
     * Callback for when the detector has finished processing the latest image and returned results
     * Use this callback for when you just want all the results the detector found pre-processing
     */
    fun multipleObjectsDetected(objects:List<FirebaseVisionObject>)
}