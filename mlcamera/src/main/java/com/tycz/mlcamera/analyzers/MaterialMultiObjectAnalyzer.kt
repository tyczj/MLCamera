package com.tycz.mlcamera.analyzers

import android.annotation.SuppressLint
import android.graphics.PointF
import android.util.SparseArray
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.util.forEach
import androidx.core.util.set
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import com.tycz.mlcamera.CameraReticleAnimator
import com.tycz.mlcamera.GraphicOverlay
import com.tycz.mlcamera.R
import com.tycz.mlcamera.`object`.*
import com.tycz.mlcamera.`object`.ObjectConfirmationController
import com.tycz.mlcamera.`object`.ObjectDotAnimator
import com.tycz.mlcamera.`object`.graphics.ObjectGraphicInMultiMode
import com.tycz.mlcamera.`object`.graphics.ObjectConfirmationGraphic
import com.tycz.mlcamera.`object`.graphics.ObjectDotGraphic
import com.tycz.mlcamera.`object`.graphics.ObjectReticleGraphic
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot

class MaterialMultiObjectAnalyzer(private val graphicOverlay: GraphicOverlay): ImageAnalysis.Analyzer {

    private val TAG:String = "MaterialObjectAnalyzer"

    private val _cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(graphicOverlay)
    private val _confirmationController: ObjectConfirmationController = ObjectConfirmationController(graphicOverlay)
    private val _isRunning: AtomicBoolean = AtomicBoolean(false)
    private val _objectSelectionDistanceThreshold: Int = graphicOverlay.resources.getDimensionPixelOffset(R.dimen.object_selection_distance_threshold)
    private val _objectDotAnimatorArray = SparseArray<ObjectDotAnimator>()

    var objectDetectionListener: ObjectDetectionListener? = null

    private lateinit var _detector: FirebaseVisionObjectDetector

    init {
        setupObjectScanning()
    }

    private fun setupObjectScanning(){
        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()

        _detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)
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

            _detector.processImage(firebaseImage).addOnSuccessListener {
                _isRunning.set(true)

                removeAnimatorsFromUntrackedObjects(it)

                graphicOverlay.clear()

                var selectedObject: DetectedObject? = null

                for (i in it.indices){

                    val detectedObject = it[i]

                    if(selectedObject == null && shouldSelectObject(graphicOverlay, detectedObject)){
                        selectedObject = DetectedObject(detectedObject, i, firebaseImage)
                        objectDetectionListener?.objectProcessing()
                        _confirmationController.confirming(detectedObject.trackingId)
                        graphicOverlay.add(ObjectConfirmationGraphic(graphicOverlay,_confirmationController, true))
                        graphicOverlay.add(ObjectGraphicInMultiMode(graphicOverlay,selectedObject,_confirmationController))
                    }else{
                        if (_confirmationController.isConfirmed) {
                            // Don't render other objects when an object is in confirmed state.
                            continue
                        }

                        val trackingId = detectedObject.trackingId ?: return@addOnSuccessListener
                        val objectDotAnimator = _objectDotAnimatorArray.get(trackingId) ?: let {
                            ObjectDotAnimator(graphicOverlay).apply {
                                start()
                                _objectDotAnimatorArray[trackingId] = this
                            }
                        }
                        graphicOverlay.add(ObjectDotGraphic(graphicOverlay,DetectedObject(detectedObject, i, firebaseImage),objectDotAnimator))
                    }
                }

                if (selectedObject == null) {
                    _confirmationController.reset()
                    graphicOverlay.add(ObjectReticleGraphic(graphicOverlay,_cameraReticleAnimator))
                    _cameraReticleAnimator.start()
                } else {
                    objectDetectionListener?.objectDetected(selectedObject)
                    _cameraReticleAnimator.cancel()
                }

                graphicOverlay.invalidate()

            }.addOnCompleteListener {
                image.close()
                _isRunning.set(false)
            }
        }
    }

    private fun shouldSelectObject(graphicOverlay: GraphicOverlay, visionObject: FirebaseVisionObject): Boolean {
        // Considers an object as selected when the camera reticle touches the object dot.
        val box = graphicOverlay.translateRect(visionObject.boundingBox)
        val objectCenter = PointF((box.left + box.right) / 2f, (box.top + box.bottom) / 2f)
        val reticleCenter = PointF(graphicOverlay.width / 2f, graphicOverlay.height / 2f)
        val distance =
            hypot((objectCenter.x - reticleCenter.x).toDouble(), (objectCenter.y - reticleCenter.y).toDouble())
        return distance < _objectSelectionDistanceThreshold
    }

    private fun removeAnimatorsFromUntrackedObjects(detectedObjects: List<FirebaseVisionObject>) {
        val trackingIds = detectedObjects.mapNotNull { it.trackingId }
        // Stop and remove animators from the objects that have lost tracking.
        val removedTrackingIds = ArrayList<Int>()

        _objectDotAnimatorArray.forEach { key, value ->
            if (!trackingIds.contains(key)) {
                value.cancel()
                removedTrackingIds.add(key)
            }
        }
        removedTrackingIds.forEach {
            _objectDotAnimatorArray.remove(it)
        }
    }
}