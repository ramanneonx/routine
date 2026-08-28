package com.neonroutine.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.neonroutine.data.model.Entry
import com.neonroutine.data.model.Task
import com.neonroutine.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * BackupRestoreUtil — creates and restores complete full-archive backups
 * containing both database data (tasks, habits, sleep, history) and photographic memory files.
 */
object BackupRestoreUtil {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Packages all database records (tasks + entries) and all photos into a single compressed .neonbak archive.
     * Returns the generated backup [File] ready for sharing/saving.
     */
    suspend fun createFullBackupArchive(context: Context, viewModel: TaskViewModel): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFileName = "neonroutine_full_backup_$timestamp.neonbak"
        val cacheDir = context.cacheDir
        val zipFile = File(cacheDir, backupFileName)

        val allDataJson = viewModel.exportAllData()
        val memoriesDir = PhotoStorageUtil.getMemoriesDirectory(context)

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Write backup.json
            val jsonEntry = ZipEntry("backup.json")
            zos.putNextEntry(jsonEntry)
            zos.write(allDataJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Write all memory photos
            if (memoriesDir.exists() && memoriesDir.isDirectory) {
                val photoFiles = memoriesDir.listFiles() ?: emptyArray()
                for (file in photoFiles) {
                    if (file.isFile) {
                        val photoEntry = ZipEntry("photos/${file.name}")
                        zos.putNextEntry(photoEntry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }

        zipFile
    }

    /**
     * Dispatches the Android share sheet for a generated backup file.
     */
    fun shareBackupFile(context: Context, backupFile: File) {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "NeonRoutine Complete Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Save or Share Complete Backup").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Restores a full backup archive from a chosen Uri.
     * Unpacks photos into context.filesDir/neon_memories/ and imports tasks & entries into Room DB.
     *
     * @return Result with summary of restored items.
     */
    suspend fun restoreFullBackupArchive(
        context: Context,
        backupUri: Uri,
        viewModel: TaskViewModel
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(backupUri)
                ?: return@withContext Result.failure(Exception("Unable to open selected backup file"))

            val memoriesDir = PhotoStorageUtil.getMemoriesDirectory(context)
            if (!memoriesDir.exists()) memoriesDir.mkdirs()

            var backupJsonContent: String? = null
            var restoredPhotosCount = 0

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName == "backup.json") {
                        backupJsonContent = zis.bufferedReader(Charsets.UTF_8).readText()
                    } else if (entryName.startsWith("photos/") && !entry.isDirectory) {
                        val fileName = File(entryName).name
                        val targetFile = File(memoriesDir, fileName)
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        restoredPhotosCount++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (backupJsonContent.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Invalid backup: backup.json missing from archive"))
            }

            // Parse and remap photo paths to current device internal directory
            val obj = json.decodeFromString<JsonObject>(backupJsonContent!!)
            val tasksArray = obj["tasks"]?.toString() ?: "[]"
            val entriesArray = obj["entries"]?.toString() ?: "[]"

            val rawEntries = json.decodeFromString<List<Entry>>(entriesArray)
            val remappedEntries = rawEntries.map { e ->
                if (!e.photoPath.isNullOrBlank()) {
                    val fileName = File(e.photoPath).name
                    val localFile = File(memoriesDir, fileName)
                    if (localFile.exists()) {
                        e.copy(photoPath = localFile.absolutePath)
                    } else {
                        e
                    }
                } else {
                    e
                }
            }

            val remappedEntriesJson = json.encodeToString(ListSerializer(Entry.serializer()), remappedEntries)
            val finalJson = """{"tasks":$tasksArray,"entries":$remappedEntriesJson}"""

            viewModel.importData(finalJson)

            Result.success("Restored habits & history + $restoredPhotosCount memory photos!")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
