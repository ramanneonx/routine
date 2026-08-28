package com.neonroutine.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * PhotoStorageUtil — manages all habit memory photo storage.
 *
 * Storage location: context.filesDir/neon_memories/
 * - INTERNAL storage — never scanned by MediaStore / gallery apps.
 * - Survives "Clear Cache" in Android Settings.
 * - Only deleted if user taps "Clear Storage" (full app reset).
 * - Photos are NOT auto-deleted; they persist indefinitely until the user
 *   explicitly deletes them within the app.
 */
object PhotoStorageUtil {

    private const val MEMORIES_DIR = "neon_memories"

    /**
     * Returns (creating if needed) the permanent internal directory for memory photos.
     * Internal filesDir is NOT visible to gallery apps and survives cache clears.
     */
    fun getMemoriesDirectory(context: Context): File {
        // Use internal filesDir — invisible to gallery, permanent across cache clears
        val dir = File(context.filesDir, MEMORIES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Creates a deterministic file path for a camera selfie.
     * Pattern: selfie_{taskId}_{date}_{timestamp}.jpg
     */
    fun createMemoryPhotoFile(context: Context, taskId: String, date: LocalDate): File {
        val dir = getMemoriesDirectory(context)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val fileName = "selfie_${taskId}_${dateStr}_${System.currentTimeMillis()}.jpg"
        return File(dir, fileName)
    }

    /**
     * Imports a photo from a Gallery Uri into the permanent memories directory,
     * correcting any EXIF orientation issue.
     * Returns the absolute path of the saved file, or null on failure.
     */
    fun importFromGallery(context: Context, uri: Uri, taskId: String, date: LocalDate): String? {
        return try {
            val destFile = createMemoryPhotoFile(context, taskId, date)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { out -> inputStream.copyTo(out) }
            inputStream.close()

            // Correct EXIF orientation before saving permanently
            val bitmap = decodeAndCorrectOrientation(tempFile.absolutePath)
            if (bitmap != null) {
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                bitmap.recycle()
            } else {
                tempFile.copyTo(destFile, overwrite = true)
            }
            tempFile.delete()

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a bitmap from [photoPath] and rotates it to be upright per EXIF data.
     * Downsamples so the largest dimension ≤ [targetMaxDim] to bound memory usage.
     */
    fun decodeAndCorrectOrientation(photoPath: String, targetMaxDim: Int = 1280): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photoPath, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > targetMaxDim ||
                options.outHeight / sampleSize > targetMaxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val original = BitmapFactory.decodeFile(photoPath, decodeOptions) ?: return null

            val exif = ExifInterface(photoPath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else                                 -> 0f
            }

            if (rotationAngle != 0f) {
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                if (rotated != original) original.recycle()
                rotated
            } else {
                original
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a specific memory photo by its absolute path.
     * Returns true if successfully deleted.
     */
    fun deletePhoto(absolutePath: String): Boolean {
        return try { File(absolutePath).delete() } catch (e: Exception) { false }
    }

    /**
     * Returns total number of photos stored in the memories directory.
     */
    fun getPhotoCount(context: Context): Int =
        getMemoriesDirectory(context).listFiles()?.count { it.extension == "jpg" } ?: 0
}
