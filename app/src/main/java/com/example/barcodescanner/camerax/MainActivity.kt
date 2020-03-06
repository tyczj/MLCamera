package com.example.barcodescanner.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.tycz.mlcamera.MLCamera
import com.tycz.mlcamera.`object`.DetectedObject
import com.tycz.mlcamera.`object`.ObjectDetectionListener
import com.tycz.mlcamera.analyzers.BasicBarcodeAnalyzer
import com.tycz.mlcamera.analyzers.BasicObjectAnalyzer
import com.tycz.mlcamera.analyzers.MaterialObjectAnalyzer
import com.tycz.mlcamera.barcode.BarcodeListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), BarcodeListener, ObjectDetectionListener {

    private lateinit var _mlCamera:MLCamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val analyzer = MaterialBarcodeAnalyzer(overlay).apply {
//            barcodeResultListener = this@MainActivity
//        }

//        val analyzer = MaterialObjectAnalyzer(overlay,true).apply {
//            objectDetectionListener = this@MainActivity
//        }

//        val analyzer = BasicObjectAnalyzer(overlay,true).apply {
//            objectDetectionListener = this@MainActivity
//        }

        val analyzer = BasicBarcodeAnalyzer(overlay)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            _mlCamera = MLCamera.Builder(this)
                .setLifecycleOwner(this)
                .setImageAnalyzer(analyzer)
//                .setImageDimensions(1280,720)
                .build()

            _mlCamera.addFutureListener(Runnable {
                _mlCamera.setupCamera(windowManager,preview_view)
            },ContextCompat.getMainExecutor(this))
        }
    }

    override fun onBarcodeProcessed(barcode: FirebaseVisionBarcode) {
        _mlCamera.stopScanning()

        Handler().postDelayed({
            _mlCamera.startPreview(windowManager,preview_view)
            _mlCamera.startScanning()
        }, 1000)
    }

    override fun onBarcodeProcessing() {
        _mlCamera.stopPreview()
    }

    override fun objectDetected(detectedObject: DetectedObject) {
        Log.d("MainActivity","Detected")
    }

    override fun objectProcessing() {
        Log.d("MainActivity","Processing")
    }

    override fun multipleObjectsDetected(objects: List<FirebaseVisionObject>) {
        Log.d("MainActivity","Multiple")
    }
}
