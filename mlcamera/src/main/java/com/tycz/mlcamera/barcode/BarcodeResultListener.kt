package com.tycz.mlcamera.barcode

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode

interface BarcodeListener {

    /**
     * Callback for when a barcode is found
     * @param barcode The barcode and its information that was found
     */
    fun onBarcodeProcessed(barcode:FirebaseVisionBarcode)

    /**
     * Callback for when a barcode is found and called before any additional processing of the barcode happens
     */
    fun onBarcodeProcessing()

    /**
     * Callback which gives a list of all detected barcodes the detector found in the image
     */
    fun onBarcodesDetected(barcodes:List<FirebaseVisionBarcode>)
}