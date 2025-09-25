package com.example.bits_helper.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val DB_NAME = "bits_helper.db"

private fun databaseFiles(context: Context): List<File> {
    val base = context.getDatabasePath(DB_NAME)
    val wal = File(base.parentFile, "$DB_NAME-wal")
    val shm = File(base.parentFile, "$DB_NAME-shm")
    return listOf(base, wal, shm)
}

private suspend fun checkpointWal(context: Context) = withContext(Dispatchers.IO) {
    try {
        val db = AppDatabase.get(context)
        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
    } catch (_: Throwable) {
        // ignore
    }
}

private fun closeDb(context: Context) {
    try {
        AppDatabase.get(context).close()
    } catch (_: Throwable) {
        // ignore
    }
}

/**
 * Export the main DB file content into the given [destination] Uri (e.g. SAF CreateDocument result).
 * WAL is checkpointed to ensure all changes are flushed into the main .db file.
 */
suspend fun exportDatabase(context: Context, destination: Uri) = withContext(Dispatchers.IO) {
    checkpointWal(context)
    val dbFile = context.getDatabasePath(DB_NAME)
    context.contentResolver.openOutputStream(destination)?.use { out ->
        dbFile.inputStream().use { input ->
            input.copyTo(out)
        }
    } ?: throw IOException("Unable to open output stream for URI: $destination")
}

/**
 * Import database content from [source] Uri and replace the local DB. Closes the DB before replacing.
 * After import, WAL/SHM are deleted so Room will recreate fresh ones.
 */
suspend fun importDatabase(context: Context, source: Uri) = withContext(Dispatchers.IO) {
    closeDb(context)
    val base = context.getDatabasePath(DB_NAME)
    // Ensure dir exists
    base.parentFile?.mkdirs()

    // Replace main DB file
    context.contentResolver.openInputStream(source)?.use { input ->
        base.outputStream().use { out ->
            input.copyTo(out)
        }
    } ?: throw IOException("Unable to open input stream for URI: $source")

    // Remove auxiliary files so they don't conflict
    for (f in databaseFiles(context)) {
        if (f.name.endsWith("-wal") || f.name.endsWith("-shm")) {
            runCatching { if (f.exists()) f.delete() }
        }
    }
}


