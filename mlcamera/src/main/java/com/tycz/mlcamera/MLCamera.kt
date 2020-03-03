package com.tycz.mlcamera

import android.Manifest
import android.content.Context
import android.util.Size
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.lang.IllegalArgumentException
import java.util.concurrent.Executor

/**
 * Class that wraps the CameraX API and provides an interface for prebuilt or custom machine learning image analyzers
 */
class MLCamera private constructor(private val _context:Context,
                                   private val _imageHeight:Int,
                                   private val _imageWidth:Int,
                                   private val _requiredCameraLens:Int,
                                   private val _imageBackpressureStrategy:Int,
                                   private val _analyzer:ImageAnalysis.Analyzer,
                                   private val _imageExecutor:Executor,
                                   private val _lifecycleOwner:LifecycleOwner){

    private val _cameraProviderFuture : ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(_context)
    private lateinit var _imageAnalyzer: ImageAnalysis
    private lateinit var _cameraProvider: ProcessCameraProvider
    private lateinit var _preview: Preview
    private lateinit var _cameraSelector: CameraSelector

    /**
     * Creates the camera preview and the image analyzer
     *
     * @param windowManager The Activities window manager
     * @param previewView The camera preview view
     */
    fun setupCamera(windowManager: WindowManager, previewView: PreviewView){

        _cameraProvider = _cameraProviderFuture.get()

        _cameraSelector = CameraSelector.Builder()
            .requireLensFacing(_requiredCameraLens)
            .build()

        startPreview(windowManager, previewView)

        startScanning()
    }

    /**
     * Starts the image analyzer if it has been stopped. Analyzer starts automatically when calling setupCamera
     */
    fun startScanning(){
        _imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(_imageWidth, _imageHeight))
            .setBackpressureStrategy(_imageBackpressureStrategy)
            .build()

        _imageAnalyzer.setAnalyzer(_imageExecutor,_analyzer)

        _cameraProvider.bindToLifecycle(_lifecycleOwner, _cameraSelector, _imageAnalyzer)
    }

    /**
     * Stops the image analyzer
     */
    fun stopScanning(){
        _cameraProvider.unbind(_imageAnalyzer)
    }

    /**
     * Stops the camera preview
     */
    fun stopPreview(){
        _cameraProvider.unbind(_preview)
    }

    /**
     * Starts the camera preview after it has been stopped
     */
    fun startPreview(windowManager: WindowManager, previewView: PreviewView){
        _preview = Preview.Builder()
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .build()

        _preview.setSurfaceProvider(previewView.previewSurfaceProvider)

        _cameraProvider.bindToLifecycle(_lifecycleOwner, _cameraSelector, _preview)
    }

    /**
     * Registers a listener to be run on the given executor.
     * The listener will run when the Future's computation is
     * complete or, if the computation is already complete, immediately.
     * @see ListenableFuture.addListener
     */
    fun addFutureListener(runnable: Runnable, executor: Executor){
        _cameraProviderFuture.addListener(runnable,executor)
    }

    /**
     * Builder class for setting up the camera and image analyzer
     */
    data class Builder(private val context: Context){

        private val DEFAULT_IMAGE_HEIGHT:Int = 640
        private val DEFAULT_IMAGE_WIDTH:Int = 480

        private var _imageHeight:Int = DEFAULT_IMAGE_HEIGHT
        private var _imageWidth:Int = DEFAULT_IMAGE_WIDTH
        private var _requiredCameraLens:Int = CameraSelector.LENS_FACING_BACK
        private var _imageBackpressureStrategy:Int = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST

        private lateinit var _analyzer:ImageAnalysis.Analyzer
        private lateinit var _imageExecutor:Executor
        private lateinit var _lifecycleOwner:LifecycleOwner

        /**
         * Optionally set the image capture dimensions of the analyzer.
         * This is the image that gets processed by the detector so the smaller the image the faster it will process
         * The image Default is 640x480 for fast processing
         *
         * @param height Requested image height
         * @param width Requested image width
         */
        fun setImageDimensions(height:Int, width:Int): Builder {
            _imageHeight = height
            _imageWidth = width
            return this
        }

        /**
         * Optionally sets the required camera lens. Default is CameraSelector.LENS_FACING_BACK
         * @see CameraSelector.LENS_FACING_BACK
         */
        fun setRequiredCameraLens(lens:Int): Builder {
            _requiredCameraLens = lens
            return this
        }

        /**
         * Sets the image analyzer that is used for
         *
         * @param analyzer Analyzer for processing frames from the camera
         */
        fun setImageAnalyzer(analyzer:ImageAnalysis.Analyzer, executor:Executor = ContextCompat.getMainExecutor(context)): Builder {
            _analyzer = analyzer
            _imageExecutor = executor
            return this
        }

        /**
         * Sets the lifecycle owner of the camera provider
         */
        fun setLifecycleOwner(lifecycleOwner:LifecycleOwner): Builder {
            _lifecycleOwner = lifecycleOwner
            return this
        }

        /**
         * Optionally sets the image back pressure strategy, default value is ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST. This means that in the image analyzer
         * you must call close() on the image for the analyze method to be called again
         * @see ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
         */
        fun setImageBackpressureStrategy(backpressureStategy:Int): Builder {
            _imageBackpressureStrategy = backpressureStategy
            return this
        }

        /**
         * Creates the the MLCamera
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        fun build(): MLCamera {

            if(!::_analyzer.isInitialized){
                throw IllegalArgumentException("Must provide an analyzer")
            }

            if(!::_lifecycleOwner.isInitialized){
                throw IllegalArgumentException("Must provide a lifecycle owner")
            }

            return MLCamera(
                context,
                _imageHeight,
                _imageWidth,
                _requiredCameraLens,
                _imageBackpressureStrategy,
                _analyzer,
                _imageExecutor,
                _lifecycleOwner
            )
        }
    }

}