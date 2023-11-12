package com.human_factors.nebulight

import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import java.io.IOException

class RecordingService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentAudioPath: String = ""

    override fun onBind(intent: Intent): IBinder? {
        // This service is not designed with binding in mind
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isRecording) {
            startRecording()
            // Stop recording after 2 minutes
            Handler().postDelayed({
                stopRecording()
                stopSelf()
            }, 120000)
        }
        return START_STICKY
    }

    private fun startRecording() {
        currentAudioPath = "${externalCacheDir?.absolutePath}/audio_note_${System.currentTimeMillis()}.3gp"
        mediaRecorder = MediaRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(currentAudioPath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
                isRecording = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            mediaRecorder?.apply {
                stop()
                reset() // You can call reset to drop the audio file or delete the file at 'currentAudioPath'.
                release()
            }
            mediaRecorder = null
            isRecording = false
        }
    }

    override fun onDestroy() {
        if (isRecording) {
            stopRecording()
        }
        super.onDestroy()
    }
}