package com.human_factors.nebulight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var recordButton: Button
    private lateinit var notesList: ListView
    private lateinit var noteAdapter: NoteAdapter
    private val notes = mutableListOf<Note>()
    private var isRecording = false
    private var currentAudioPath: String = ""
    private lateinit var titleInput: EditText
    private lateinit var contentInput: EditText
    private lateinit var addButton: Button
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var saveTextNoteButton: Button
    private lateinit var stopRecordingButton: Button

    private val RECORD_AUDIO_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleInput = findViewById(R.id.titleInput)
        contentInput = findViewById(R.id.contentInput)
        addButton = findViewById(R.id.addButton)
        buttonsLayout = findViewById(R.id.buttonsLayout)
        recordButton = findViewById(R.id.recordButton)
        notesList = findViewById(R.id.notesList)
        saveTextNoteButton = findViewById(R.id.saveTextNoteButton)
        stopRecordingButton = findViewById(R.id.stopRecordingButton)

        noteAdapter = NoteAdapter(this, notes)
        notesList.adapter = noteAdapter

        notes.addAll(NoteStorageUtil.loadNotesFromFile(this))
        noteAdapter.notifyDataSetChanged()

        setupButtonListeners()

        // Start the recording service
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startService(Intent(this, RecordingService::class.java))
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        }
    }

    private fun setupButtonListeners() {
        addButton.setOnClickListener {
            val title = titleInput.text.toString()
            if (title.isNotEmpty()) {
                titleInput.isEnabled = false
                addButton.visibility = View.GONE
                buttonsLayout.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Please enter a title for the note.", Toast.LENGTH_SHORT).show()
            }
        }

        val textNoteButton: Button = findViewById(R.id.textNoteButton)
        textNoteButton.setOnClickListener {
            buttonsLayout.visibility = View.GONE
            contentInput.visibility = View.VISIBLE
            saveTextNoteButton.visibility = View.VISIBLE
        }

        recordButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
            } else {
                buttonsLayout.visibility = View.GONE
                startRecording()
            }
        }

        saveTextNoteButton.setOnClickListener {
            saveTextNote()
        }

        stopRecordingButton.setOnClickListener {
            stopRecording()
        }
    }

    private fun startRecording() {
        isRecording = true
        currentAudioPath = "${getExternalFilesDir(null)?.absolutePath}/audio_note_${System.currentTimeMillis()}.3gp"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(currentAudioPath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
                stopRecordingButton.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e("MainActivity", "Recording failed", e)
                Toast.makeText(this@MainActivity, "Recording failed, please try again", Toast.LENGTH_SHORT).show()
                isRecording = false
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Log.e("MainActivity", "Failed to stop recording", e)
                }
                release()
            }
            mediaRecorder = null
            isRecording = false
            saveNote(titleInput.text.toString(), null, currentAudioPath)
            resetRecordingUI()
        }
    }

    private fun saveTextNote() {
        val textContent = contentInput.text.toString()
        if (textContent.isNotEmpty()) {
            saveNote(titleInput.text.toString(), textContent, null)
            resetTextNoteUI()
        } else {
            Toast.makeText(this, "Please enter some text for the note.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNote(title: String, textContent: String?, audioPath: String?) {
        if (title.isNotEmpty()) {
            val audioFile = audioPath?.let { File(it) }
            if (audioFile?.exists() == true || textContent != null) {
                val note = Note(title, textContent ?: "", audioPath)
                notes.add(note)
                noteAdapter.notifyDataSetChanged()
                NoteStorageUtil.saveNotesToFile(this, notes)
                Toast.makeText(this, "Note saved successfully.", Toast.LENGTH_SHORT).show()
            } else if (audioPath != null) {
                Toast.makeText(this, "Recording failed to save. Please try again.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Title cannot be empty.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetRecordingUI() {
        stopRecordingButton.visibility = View.GONE
        titleInput.isEnabled = true
        addButton.visibility = View.VISIBLE
        titleInput.text.clear()
    }

    private fun resetTextNoteUI() {
        contentInput.visibility = View.GONE
        saveTextNoteButton.visibility = View.GONE
        titleInput.text.clear()
        contentInput.text.clear()
        titleInput.isEnabled = true
        addButton.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startService(Intent(this, RecordingService::class.java))
            startRecording()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }
}
