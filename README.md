# MLCamera

A wrapper library for the new CameraX API and Firebase MLKit with built in Material Design barcode and object detection built fully in Kotlin.

## Setup

### Basic Usage

Getting started with MLCamera is very simple, First add these dependencies to your app's `build.gradle`

```
implementation "androidx.camera:camera-camera2:1.0.0-beta01"
implementation "androidx.camera:camera-view:1.0.0-alpha08"
implementation 'com.google.firebase:firebase-ml-vision-barcode-model:16.0.2'
```

Next in your app's `Application` class add the CameraX provider

```
class App:Application(), CameraXConfig.Provider {

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}
```

Create your Activity layout by adding the camera `PreviewView` and the `GraphicOverlay`

```
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <androidx.camera.view.PreviewView
        android:id="@+id/preview_view"
        android:layout_height="match_parent"
        android:layout_width="match_parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" >

    </androidx.camera.view.PreviewView>

    <com.tycz.mlcamera.GraphicOverlay
        android:id="@+id/overlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

In your Activity you setup MLCamera with juts a few lines (Dont forget you need to ask for camera permissions and declare them in your Manifest)

```
if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
    _mlCamera = MLCamera.Builder(this)
        .setLifecycleOwner(this)
        .setImageAnalyzer(analyzer)
        .build()

    _mlCamera.addFutureListener(Runnable {
        _mlCamera.setupCamera(windowManager,preview_view)
    },ContextCompat.getMainExecutor(this))
}
```

# Built-in Image Processors

MLCamera has a few built-in image processors for detecting barcodes and objects 

## Material Barcode Analyzer

Based off of the suggested material design guidelines 

[https://material.io/collections/machine-learning/barcode-scanning.html#](https://material.io/collections/machine-learning/barcode-scanning.html#)

and taken from the firebase example but migrated to use CameraX

[https://github.com/firebase/mlkit-material-android](https://github.com/firebase/mlkit-material-android)

The barcode scanner scans the first barcode it finds in the scan area and returns it

### Setup

```
val analyzer = MaterialBarcodeAnalyzer(overlay).apply {
    barcodeResultListener = this@MainActivity // Optional if you want callbacks from the image analyzer at different steps along the way with information
}
```

then add the analyzer to the MLCamera builder

```
.setImageAnalyzer(analyzer)
```

## Material Object Analyzer

Based off of the suggested material design guidelines 

[https://material.io/collections/machine-learning/object-detection-live-camera.html](https://material.io/collections/machine-learning/object-detection-live-camera.html)

and taken from the firebase example but migrated to use CameraX

[https://github.com/firebase/mlkit-material-android](https://github.com/firebase/mlkit-material-android)

The analyzer displays up to 5 objects on the screen by default with a white dot on the object. When you put the camera reticle over one of the dots it becomes selected.

### Setup

```
val analyzer = MaterialObjectAnalyzer(overlay,true).apply {
    objectDetectionListener = this@MainActivity
}
```

## Basic Barcode Analyzer

This anlyzer detects all barcodes visible on the screen and draws a box around the barcode on the screen. 

### Setup

```
val analyzer = BasicBarcodeAnalyzer(overlay)
```

You can subscribe to one of the callbacks in the `BarcodeListener` interface 

```
onBarcodesDetected(barcodes:List<FirebaseVisionBarcode>)
```

Which gives you the raw barcode data the Firebase detector returned

## Basic Object Analyzer

This analyzer is similar to the Material Object Analyzer in that it can detect either up to 5 objects or a single most prominent object. The only difference is that this just draws a box around the detected object on the screen.

### Setup

```
val analyzer = BasicObjectAnalyzer(overlay,true)
```

You can subscribe to one of the callbacks in the `ObjectDetectionListener` interface

```
fun multipleObjectsDetected(objects:List<FirebaseVisionObject>)
```

Which returns all the raw data returned by the Firebase detector

## Custom Analyzers

MLCamera can also support custom analyzers if needed, you just have to have a class that extends `ImageAnalysis.Analyzer` and pass it to the MLCamera builder

```
.setImageAnalyzer(analyzer)
```

### Example

```
class MyCustomAnalyzer(private val graphicOverlay: GraphicOverlay): ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy){
    
    }
}
```
