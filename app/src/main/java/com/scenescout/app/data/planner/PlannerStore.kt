package com.scenescout.app.data.planner

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists shoot projects as JSON in the app's private files dir.
 * All I/O on Dispatchers.IO.
 */
class PlannerStore(context: Context) {

    private val file: File = File(context.filesDir, "planner.json")

    suspend fun load(): List<ShootProject> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext emptyList()
            ShootProject.listFromJson(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun save(projects: List<ShootProject>) = withContext(Dispatchers.IO) {
        try {
            file.writeText(ShootProject.listToJson(projects))
        } catch (e: Exception) {
            // Best effort — planning data shouldn't crash the app.
        }
    }
}
