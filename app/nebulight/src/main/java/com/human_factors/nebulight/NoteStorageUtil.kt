package com.human_factors.nebulight

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*

object NoteStorageUtil {

    private const val NOTES_FILE = "notes.json"

    fun saveNotesToFile(context: Context, notes: List<Note>) {
        val gson = Gson()
        val jsonString = gson.toJson(notes)
        context.openFileOutput(NOTES_FILE, Context.MODE_PRIVATE).use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        }
    }

    fun loadNotesFromFile(context: Context): MutableList<Note> {
        try {
            context.openFileInput(NOTES_FILE).use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                val jsonString = String(buffer)
                val type = object : TypeToken<MutableList<Note>>() {}.type
                return Gson().fromJson(jsonString, type)
            }
        } catch (e: FileNotFoundException) {
            // It's OK if the file doesn't exist yet
        } catch (e: IOException) {
            // Handle other I/O errors
        }
        return mutableListOf()
    }

    fun deleteAudioFile(audioPath: String) {
        val file = File(audioPath)
        if (file.exists()) {
            if (!file.delete()) {
                // Log an error or handle the failure to delete the file
            }
        }
    }
}
