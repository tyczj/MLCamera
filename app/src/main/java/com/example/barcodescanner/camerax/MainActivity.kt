package com.example.barcodescanner.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import androidx.lifecycle.LifecycleOwner
import com.tycz.mlcamera.BarcodeResultListener
import com.tycz.mlcamera.MLCamera
import com.tycz.mlcamera.analyzers.MaterialBarcodeAnalyzer
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import androidx.camera.view.PreviewView

class MainActivity : AppCompatActivity(),
    BarcodeResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val previewView: PreviewView? = null

        overlay.setCameraInfo(preview_view)

        val analyzer = MaterialBarcodeAnalyzer(overlay).apply {
            this.barcodeResultListener = this@MainActivity
        }

        val mlCamera: MLCamera = MLCamera.Builder(this)
            .setLifecycleOwner(this as LifecycleOwner)
            .setImageAnalyzer(analyzer)
            .build()

        mlCamera.addFutureListener(Runnable {
            mlCamera.setupCamera(windowManager,preview_view)
        },ContextCompat.getMainExecutor(this))
    }

    override fun onBarcodeFound(barcode: FirebaseVisionBarcode) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
