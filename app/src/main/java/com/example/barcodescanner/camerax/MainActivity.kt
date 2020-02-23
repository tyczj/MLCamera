package com.example.barcodescanner.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import androidx.lifecycle.LifecycleOwner

class MainActivity : AppCompatActivity(),
    BarcodeResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlay.setCameraInfo(preview_view)

        val analyzer = com.tycz.mlcamera.BarcodeAnalyzer(overlay).apply {
            this.barcodeResultListener = this@MainActivity
        }

        val mlCamera: com.tycz.mlcamera.MLCamera = com.tycz.mlcamera.MLCamera.Builder(this)
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
