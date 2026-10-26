package com.plabin.librespeedo.trips

import android.content.Context
import android.net.Uri
import android.database.sqlite.SQLiteException
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes a standalone, consistent SQLite snapshot to a user-selected document. */
object TripDatabaseExporter {
    suspend fun export(context: Context, destination: Uri) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val snapshot = File.createTempFile("librespeedo-trips-", ".db", appContext.cacheDir)
        try {
            check(snapshot.delete()) { "Could not prepare a temporary database snapshot" }
            val database = TripDatabase.get(appContext)
            val sqlite = database.openHelper.writableDatabase
            val quotedPath = snapshot.absolutePath.replace("'", "''")
            try {
                // VACUUM INTO produces a self-contained snapshot including committed WAL data.
                sqlite.execSQL("VACUUM INTO '$quotedPath'")
            } catch (error: SQLiteException) {
                if (!error.message.orEmpty().contains("syntax error", ignoreCase = true)) throw error

                // Older Android SQLite versions lack VACUUM INTO. The caller serializes
                // trip writes while we run a WAL checkpoint and copy the main database file.
                sqlite.query("PRAGMA wal_checkpoint(FULL)").use { result ->
                    if (result.moveToFirst() && result.getInt(0) != 0) {
                        throw IOException("The trip database is busy; try exporting again")
                    }
                }
                val databaseFile = appContext.getDatabasePath("trips.db")
                if (!databaseFile.isFile) throw IOException("Trip database file was not found")
                databaseFile.inputStream().use { input ->
                    snapshot.outputStream().use { output -> input.copyTo(output) }
                }
            }

            val output = appContext.contentResolver.openOutputStream(destination, "wt")
                ?: throw IOException("Could not open the selected export location")
            output.use { destinationStream ->
                snapshot.inputStream().use { input -> input.copyTo(destinationStream) }
            }
        } finally {
            snapshot.delete()
        }
    }
}
