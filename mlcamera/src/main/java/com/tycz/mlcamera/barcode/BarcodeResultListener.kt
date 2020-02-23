package com.tycz.mlcamera.barcode

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode

interface BarcodeResultListener {

    /**
     * Callback for when a barcode is found
     * @param barcode The barcode and its information that was found
     */
    fun onBarcodeFound(barcode:FirebaseVisionBarcode)
}