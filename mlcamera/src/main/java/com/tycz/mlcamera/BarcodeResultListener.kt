package com.tycz.mlcamera

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode

interface BarcodeResultListener {
    fun onBarcodeFound(barcode:FirebaseVisionBarcode)
}