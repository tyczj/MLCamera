package com.tycz.mlcamera.analyzers

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import com.tycz.mlcamera.GraphicOverlay
import com.tycz.mlcamera.`object`.ObjectDetectionListener
import com.tycz.mlcamera.basic.BasicBoundingBoxOverlay
import com.tycz.mlcamera.basic.BasicDetectedObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Basic detector class for detecting multiple objects from the camera
 * Displays the detected objects bounding box on the overlay
 *
 * @param graphicOverlay The overlay that displays where objects were detected
 * @param trackMultipleObjects Flag for if the detector should detect multiple objects (up to 5) or just the most prominent object
 * @param firebaseApp Your firebase app configuration
 */
class BasicObjectAnalyzer(private val graphicOverlay: GraphicOverlay, trackMultipleObjects: Boolean, firebaseApp: FirebaseApp): ImageAnalysis.Analyzer {

    private val _isRunning: AtomicBoolean = AtomicBoolean(false)
    private val _detector: FirebaseVisionObjectDetector

    /**
     * Callbacks for the object detector states
     */
    var objectDetectionListener: ObjectDetectionListener? = null

    init {
        val builder = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
            .enableClassification()

        if(trackMultipleObjects){
            builder.enableMultipleObjects()
        }

        val options = builder.build()

        _detector = FirebaseVision.getInstance(firebaseApp).getOnDeviceObjectDetector(options)
    }

    /**
     * Converts the device rotation to a Firebase Image Rotation
     */
    private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {

        if(_isRunning.get()){
            image.close()
            return
        }

        val mediaImage = image.image
        val imageRotation = degreesToFirebaseRotation(image.imageInfo.rotationDegrees)

        mediaImage?.let {
            val firebaseImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
            graphicOverlay.setImageDimens(image.width, image.height)
            _detector.processImage(firebaseImage).addOnSuccessListener {
                _isRunning.set(true)

                graphicOverlay.clear()

                if (it.size > 0){
                    objectDetectionListener?.multipleObjectsDetected(it.toList())
                }

                for (i in it.indices){
                    val detectedObject = BasicDetectedObject(image.width, image.height, it[i].boundingBox, Color.RED)

                    graphicOverlay.add(BasicBoundingBoxOverlay(graphicOverlay,detectedObject))
                }

                graphicOverlay.invalidate()
                _isRunning.set(false)
                image.close()
            }.addOnFailureListener{

            }
        }
    }
}