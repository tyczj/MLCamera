package com.example.barcodescanner.camerax

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.tycz.mlcamera.MLCamera
import com.tycz.mlcamera.analyzers.MaterialBarcodeAnalyzer
import com.tycz.mlcamera.barcode.BarcodeListener

class MainActivity : AppCompatActivity(), BarcodeListener {

    private lateinit var _mlCamera:MLCamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlay.setCameraInfo(preview_view)

        val analyzer = MaterialBarcodeAnalyzer(overlay).apply {
            this.barcodeResultListener = this@MainActivity
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            _mlCamera = MLCamera.Builder(this)
                .setLifecycleOwner(this)
                .setImageAnalyzer(analyzer)
                .build()

            _mlCamera.addFutureListener(Runnable {
                _mlCamera.setupCamera(windowManager,preview_view)
            },ContextCompat.getMainExecutor(this))
        }
    }

    override fun onBarcodeProcessed(barcode: FirebaseVisionBarcode) {
//        _mlCamera.stopScanning()
//
//        Handler().postDelayed({
//            _mlCamera.startPreview()
//            _mlCamera.startScanning()
//        }, 1000)
    }

    override fun onBarcodeProcessing() {
//        _mlCamera.stopPreview()
    }
}
