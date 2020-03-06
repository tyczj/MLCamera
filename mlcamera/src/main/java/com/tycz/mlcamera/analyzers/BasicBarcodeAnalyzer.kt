package com.tycz.mlcamera.analyzers

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.tycz.mlcamera.GraphicOverlay
import com.tycz.mlcamera.barcode.BarcodeListener
import com.tycz.mlcamera.basic.BasicBoundingBoxOverlay
import com.tycz.mlcamera.basic.BasicDetectedObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Basic detector class for detecting barcodes from the camera.
 * Displays the detected barcodes bounding box on the overlay
 *
 * @param graphicOverlay The overlay that displays where objects were detected
 */
class BasicBarcodeAnalyzer(private val graphicOverlay: GraphicOverlay): ImageAnalysis.Analyzer  {

    private val TAG = "BsicBarcodeAnayzer"

    private val _isRunning: AtomicBoolean = AtomicBoolean(false)
    private val _detector: FirebaseVisionBarcodeDetector

    /**
     * Callback for when a barcode is found, the full FirebaseVisionBarcode object is returned
     */
    var barcodeResultListener: BarcodeListener? = null

    init {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
            .build()

        _detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
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

            _detector.detectInImage(firebaseImage).addOnSuccessListener { barcodes ->
                _isRunning.set(true)
                graphicOverlay.clear()

                barcodeResultListener?.onBarcodesDetected(barcodes)

                for (i in barcodes.indices){
                    barcodes[i].boundingBox?.let {
                        val detectedObject = BasicDetectedObject(image.width, image.height, it, Color.RED)

                        graphicOverlay.add(BasicBoundingBoxOverlay(graphicOverlay,detectedObject))
                    }
                }
                graphicOverlay.invalidate()
                _isRunning.set(false)
                image.close()
            }
        }
    }
}