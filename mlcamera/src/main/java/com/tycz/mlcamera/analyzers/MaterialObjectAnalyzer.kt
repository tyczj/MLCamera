package com.tycz.mlcamera.analyzers

import android.annotation.SuppressLint
import android.graphics.PointF
import android.util.Log
import android.util.Size
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

/**
 * Detector class for detecting multiple objects from the camera
 * Displays a dot on detected objects with material design styling
 * @see <a href="https://material.io/collections/machine-learning/object-detection-live-camera.html">link</a>
 *
 * @param graphicOverlay The overlay that displays where objects were detected
 * @param trackMultipleObjects Flag for if the detector should detect multiple objects (up to 5) or just the most prominent object
 */
class MaterialObjectAnalyzer(private val graphicOverlay: GraphicOverlay, private val trackMultipleObjects: Boolean): ImageAnalysis.Analyzer {

    private companion object{
        private const val TAG:String = "MaterialObjectAnalyzer"
    }

    private val _cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(graphicOverlay)
    private val _confirmationController: ObjectConfirmationController = ObjectConfirmationController(graphicOverlay)
    private val _isRunning: AtomicBoolean = AtomicBoolean(false)
    private val _objectSelectionDistanceThreshold: Int = graphicOverlay.resources.getDimensionPixelOffset(R.dimen.object_selection_distance_threshold)
    private val _objectDotAnimatorArray = SparseArray<ObjectDotAnimator>()

    /**
     * Callbacks for the object detector states
     */
    var objectDetectionListener: ObjectDetectionListener? = null

    private val _detector: FirebaseVisionObjectDetector

    init {
        val builder = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
            .enableClassification()

        if(trackMultipleObjects){
            builder.enableMultipleObjects()
        }

        val options = builder.build()

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
            graphicOverlay.setImageDimens(image.width, image.height)
            _detector.processImage(firebaseImage).addOnSuccessListener {
                _isRunning.set(true)

                Log.d(TAG, "${it.size} Items Found")

                if(it.isNotEmpty()){
                    objectDetectionListener?.multipleObjectsDetected(it.toList())
                }

                removeAnimatorsFromUntrackedObjects(it)

                graphicOverlay.clear()

                var selectedObject: DetectedObject? = null

                for (i in it.indices){

                    val detectedObject = it[i]

                    val detObject = DetectedObject(detectedObject, i, firebaseImage, Size(image.width,image.height))

                    if(selectedObject == null && shouldSelectObject(graphicOverlay, detectedObject)){
                        selectedObject = detObject
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
                        graphicOverlay.add(ObjectDotGraphic(graphicOverlay,detObject,objectDotAnimator))
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

            }.addOnFailureListener {
                Log.e(TAG,"Unable to detect objects")
            }.addOnCompleteListener {
                image.close()
                _isRunning.set(false)
            }
        }
    }

    /**
     * Checks to see if the camera reticle is on an object dot. If on the dot the object is considered selected
     */
    private fun shouldSelectObject(graphicOverlay: GraphicOverlay, visionObject: FirebaseVisionObject): Boolean {
        val box = graphicOverlay.translateRect(visionObject.boundingBox)
        val objectCenter = PointF((box.left + box.right) / 2f, (box.top + box.bottom) / 2f)
        val reticleCenter = PointF(graphicOverlay.width / 2f, graphicOverlay.height / 2f)
        val distance =
            hypot((objectCenter.x - reticleCenter.x).toDouble(), (objectCenter.y - reticleCenter.y).toDouble())
        return distance < _objectSelectionDistanceThreshold
    }

    /**
     * Removes dots that have no longer been tracked, ie. moving out of frame
     */
    private fun removeAnimatorsFromUntrackedObjects(detectedObjects: List<FirebaseVisionObject>) {
        val trackingIds = detectedObjects.mapNotNull { it.trackingId }
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