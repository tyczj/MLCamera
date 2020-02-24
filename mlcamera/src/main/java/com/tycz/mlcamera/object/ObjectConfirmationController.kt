package com.tycz.mlcamera.`object`

import android.os.CountDownTimer
import com.tycz.mlcamera.GraphicOverlay

/**
 * Controls the progress of object confirmation before performing additional operation on the
 * detected object.
 */
internal class ObjectConfirmationController
/**
 * @param graphicOverlay Used to refresh camera overlay when the confirmation progress updates.
 */
    (graphicOverlay: GraphicOverlay) {

    private val countDownTimer: CountDownTimer

    private var objectId: Int? = null
    /** Returns the confirmation progress described as a float value in the range of [0, 1].  */
    var progress = 0f
        private set

    val isConfirmed: Boolean
        get() = progress.compareTo(1f) == 0

    init {
        val confirmationTimeMs = 300.toLong()
        countDownTimer = object : CountDownTimer(confirmationTimeMs, /* countDownInterval= */ 20) {
            override fun onTick(millisUntilFinished: Long) {
                progress = (confirmationTimeMs - millisUntilFinished).toFloat() / confirmationTimeMs
                graphicOverlay.invalidate()
            }

            override fun onFinish() {
                progress = 1f
            }
        }
    }

    fun confirming(objectId: Int?) {
        if (objectId == this.objectId) {
            // Do nothing if it's already in confirming.
            return
        }

        reset()
        this.objectId = objectId
        countDownTimer.start()
    }

    fun reset() {
        countDownTimer.cancel()
        objectId = null
        progress = 0f
    }
}