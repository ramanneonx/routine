package com.neonroutine.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * TimeLapseVideoExporter — encodes a list of photos into an H.264 MP4 using MediaCodec + MediaMuxer.
 *
 * Key design decisions:
 * - Output goes to app internal cache (exports/) — NOT auto-saved to gallery.
 * - Gallery save happens ONLY when the user explicitly chooses to via the share sheet.
 * - [secondsPerPhoto]: each photo is held for this many seconds in the final video.
 *   FPS is fixed at 30 internally; each photo is repeated for (secondsPerPhoto * 30) frames.
 *   This gives the user full control over video duration.
 * - Watermark / date banner is drawn on each frame via Canvas.
 * - Returns the temporary File which MemoryScreen shares or saves as user chooses.
 */
object TimeLapseVideoExporter {

    private const val MIME_TYPE = "video/avc" // H.264
    private const val BIT_RATE = 4_000_000    // 4 Mbps
    private const val OUTPUT_FPS = 30          // Always 30fps output; photo hold is via frame repeat
    private const val I_FRAME_INTERVAL = 1

    data class ExportConfig(
        /** How many seconds each photo is visible in the exported video (0.5 – 5.0). */
        val secondsPerPhoto: Float = 1.5f,
        val width: Int = 720,
        val height: Int = 1280,
        val showDateBanner: Boolean = true,
        val showWatermark: Boolean = true
    )

    data class ExportProgress(
        val currentPhoto: Int,
        val totalPhotos: Int,
        val percentage: Float
    )

    /**
     * Exports selected photos as a time-lapse MP4.
     *
     * @param context        Android context
     * @param photoPaths     Ordered list of absolute file paths
     * @param dateLabels     Optional date string per photo (same length as photoPaths)
     * @param config         [ExportConfig] controlling duration, size, branding
     * @param onProgress     Called on each photo encoded — use to drive a progress bar
     * @return               Output File in cache/exports/, or null on failure
     */
    suspend fun exportTimeLapseVideo(
        context: Context,
        photoPaths: List<String>,
        dateLabels: List<String> = emptyList(),
        config: ExportConfig = ExportConfig(),
        onProgress: (ExportProgress) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        if (photoPaths.isEmpty()) return@withContext null

        val outputDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "timelapse_${System.currentTimeMillis()}.mp4")

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        // Frames each photo occupies: secondsPerPhoto * OUTPUT_FPS (rounded, min 1)
        val framesPerPhoto = (config.secondsPerPhoto * OUTPUT_FPS).toInt().coerceAtLeast(1)
        val frameDurationUs = (1_000_000L / OUTPUT_FPS)

        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, config.width, config.height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1
            var muxerStarted = false
            var globalFrameIndex = 0L

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 34f
                setShadowLayer(5f, 2f, 2f, Color.BLACK)
            }
            val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE.and(0xCCFFFFFF.toInt())
                textSize = 22f
                setShadowLayer(3f, 1f, 1f, Color.BLACK)
            }
            val bannerPaint = Paint().apply { color = Color.argb(160, 0, 0, 0) }

            photoPaths.forEachIndexed { photoIndex, path ->
                val bitmap = PhotoStorageUtil.decodeAndCorrectOrientation(path, targetMaxDim = config.height)
                    ?: return@forEachIndexed

                // Repeat each photo for framesPerPhoto frames
                repeat(framesPerPhoto) { frameWithinPhoto ->
                    val canvas = try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            inputSurface.lockHardwareCanvas()
                        } else {
                            inputSurface.lockCanvas(null)
                        }
                    } catch (e: Exception) {
                        try { inputSurface.lockCanvas(null) } catch (e2: Exception) { return@repeat }
                    }

                    // ── Draw bitmap centered-crop ──────────────────────────────────
                    canvas.drawColor(Color.BLACK)
                    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                    val scale = maxOf(
                        config.width.toFloat() / bitmap.width,
                        config.height.toFloat() / bitmap.height
                    )
                    val scaledW = bitmap.width * scale
                    val scaledH = bitmap.height * scale
                    val left = (config.width - scaledW) / 2f
                    val top = (config.height - scaledH) / 2f
                    canvas.drawBitmap(bitmap, srcRect, RectF(left, top, left + scaledW, top + scaledH), null)

                    // ── Bottom date + watermark banner ──────────────────────────────
                    if (config.showDateBanner || config.showWatermark) {
                        val bannerH = 80f
                        val bannerTop = config.height - bannerH - 20f
                        canvas.drawRect(0f, bannerTop, config.width.toFloat(), config.height - 20f, bannerPaint)
                        if (config.showDateBanner) {
                            val dateStr = dateLabels.getOrNull(photoIndex) ?: "Day ${photoIndex + 1}"
                            canvas.drawText("📅 $dateStr", 24f, config.height - 44f, textPaint)
                        }
                        if (config.showWatermark) {
                            val wm = "NeonRoutine"
                            val wmWidth = smallTextPaint.measureText(wm)
                            canvas.drawText(wm, config.width - wmWidth - 20f, config.height - 28f, smallTextPaint)
                        }
                    }

                    // ── Photo progress indicator (thin top bar) ────────────────────
                    val progressPaint = Paint().apply {
                        color = Color.argb(200, 127, 119, 221) // neon violet
                    }
                    val barW = ((photoIndex + 1).toFloat() / photoPaths.size) * config.width
                    canvas.drawRect(0f, 0f, barW, 6f, progressPaint)

                    inputSurface.unlockCanvasAndPost(canvas)

                    // ── Drain encoder into muxer ───────────────────────────────────
                    bufferInfo.presentationTimeUs = globalFrameIndex * frameDurationUs
                    globalFrameIndex++

                    var outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    while (outIndex >= 0 || outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        } else if (outIndex >= 0) {
                            val encodedData = encoder.getOutputBuffer(outIndex)
                            if (encodedData != null && muxerStarted &&
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                            ) {
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                        }
                        outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0L)
                    }
                }

                bitmap.recycle()

                withContext(Dispatchers.Main) {
                    onProgress(
                        ExportProgress(
                            currentPhoto = photoIndex + 1,
                            totalPhotos = photoPaths.size,
                            percentage = (photoIndex + 1).toFloat() / photoPaths.size
                        )
                    )
                }
            }

            // ── Signal end of stream and drain remaining buffers ───────────────────
            encoder.signalEndOfInputStream()
            var outIndex = encoder.dequeueOutputBuffer(bufferInfo, 50_000L)
            while (outIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outIndex)
                if (encodedData != null && muxerStarted &&
                    (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                ) {
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }
                encoder.releaseOutputBuffer(outIndex, false)
                outIndex = encoder.dequeueOutputBuffer(bufferInfo, 50_000L)
            }

            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            outputFile.delete()
            null
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
                inputSurface?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // Suppress cleanup exceptions
            }
        }
    }
}
