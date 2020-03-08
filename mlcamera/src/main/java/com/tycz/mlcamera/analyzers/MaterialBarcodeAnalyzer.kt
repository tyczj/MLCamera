package com.tycz.mlcamera.analyzers

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.tycz.mlcamera.*
import com.tycz.mlcamera.barcode.BarcodeListener
import com.tycz.mlcamera.barcode.graphics.BarcodeLoadingGraphic
import com.tycz.mlcamera.barcode.graphics.BarcodeReticleGraphic
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Image analyzer for finding a single barcode with material design styling
 * It detects all barcode formats
 * @see <a href="https://material.io/collections/machine-learning/barcode-scanning.html">link</a>
 *
 * @param graphicOverlay The overlay that displays where objects were detected
 * @param firebaseApp Your firebase app configuration
 */
class MaterialBarcodeAnalyzer(private val graphicOverlay: GraphicOverlay, firebaseApp:FirebaseApp):ImageAnalysis.Analyzer {

    private val TAG:String = "MaterialBarcodeAnalyzer"

    private val _cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(graphicOverlay)
    private val _detector: FirebaseVisionBarcodeDetector
    private val _isRunning:AtomicBoolean = AtomicBoolean(false)
    private lateinit var _barcodeReticile: BarcodeReticleGraphic

    /**
     * Callback for when a barcode is found, the full FirebaseVisionBarcode object is returned
     */
    var barcodeResultListener: BarcodeListener? = null

    /**
     * Flag for showing an animation when a barcode is found that simulates loading/scanning.
     * Default value is true
     */
    var shouldShowLoadingAnimation: Boolean = true

    init {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
            .build()

        _detector = FirebaseVision.getInstance(firebaseApp).getVisionBarcodeDetector(options)
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

    // When the BackpressureStrategy is set to STRATEGY_KEEP_ONLY_LATEST analyze only gets called again after we close the image
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

            _detector.detectInImage(firebaseImage).addOnSuccessListener {barcodes ->

                graphicOverlay.clear()

                if(barcodes.size == 0){
                    _cameraReticleAnimator.start()
                    _barcodeReticile = BarcodeReticleGraphic(graphicOverlay, _cameraReticleAnimator)
                    graphicOverlay.add(_barcodeReticile)
                }else{
                    barcodeResultListener?.onBarcodesDetected(barcodes)
                    _isRunning.set(true)
                    _cameraReticleAnimator.cancel()

                    barcodeResultListener?.onBarcodeProcessing()

                    if(shouldShowLoadingAnimation){
                        val loadingAnimator = createLoadingAnimator(graphicOverlay, barcodes.first())
                        loadingAnimator.start()
                        graphicOverlay.add(BarcodeLoadingGraphic(graphicOverlay, loadingAnimator))
                    }else{
                        barcodeResultListener?.onBarcodeProcessed(barcodes.first())
                        _barcodeReticile = BarcodeReticleGraphic(graphicOverlay, _cameraReticleAnimator)
                        graphicOverlay.add(_barcodeReticile)
                        _isRunning.set(false)
                    }
                }
            }.addOnFailureListener{exception ->
                Log.e(TAG, "Barcode detection failure", exception)
            }.addOnCompleteListener{
                image.close()
            }
        }

        graphicOverlay.invalidate()
    }

    /**
     * Creates a loading/processing like animation around the reticle when a barcode is found
     */
    private fun createLoadingAnimator(graphicOverlay: GraphicOverlay, barcode: FirebaseVisionBarcode): ValueAnimator {
        val endProgress = 1.1f
        return ValueAnimator.ofFloat(0f, endProgress).apply {
            duration = 2000
            addUpdateListener {
                if ((animatedValue as Float).compareTo(endProgress) >= 0) {
                    graphicOverlay.clear()
                    barcodeResultListener?.onBarcodeProcessed(barcode)
                    _isRunning.set(false)
                } else {
                    graphicOverlay.invalidate()
                }
            }
        }
    }
}