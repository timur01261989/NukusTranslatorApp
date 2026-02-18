package com.nukustranslator

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * MediaProjection capture with throttling.
 * DO NOT do "every millisecond". Start with 1 fps.
 */
class ScreenCaptureManager(
  private val ctx: Context,
  private val resultCode: Int,
  private val data: Intent,
  private val onFrame: (Bitmap) -> Unit
) {
  private var projection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null

  private val thread = HandlerThread("capture-thread")
  private lateinit var handler: Handler

  private var running = false
  private var lastTs = 0L
  private val fps = 1 // <- tune this, 1–2 is realistic

  fun start() {
    if (running) return
    running = true
    thread.start()
    handler = Handler(thread.looper)

    val mpm = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    projection = mpm.getMediaProjection(resultCode, data)

    val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    wm.defaultDisplay.getRealMetrics(metrics)

    val width = max(1, metrics.widthPixels)
    val height = max(1, metrics.heightPixels)
    val density = metrics.densityDpi

    imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    virtualDisplay = projection?.createVirtualDisplay(
      "NST-VirtualDisplay",
      width, height, density,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
      imageReader?.surface,
      null,
      handler
    )

    imageReader?.setOnImageAvailableListener({ reader ->
      val now = System.currentTimeMillis()
      if (now - lastTs < (1000L / fps)) {
        reader.acquireLatestImage()?.close()
        return@setOnImageAvailableListener
      }
      lastTs = now
      val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
      try {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width

        val bmp = Bitmap.createBitmap(
          width + rowPadding / pixelStride,
          height,
          Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
        bmp.recycle()

        onFrame(cropped)
      } finally {
        image.close()
      }
    }, handler)
  }

  fun stop() {
    running = false
    try { imageReader?.setOnImageAvailableListener(null, null) } catch (_: Exception) {}
    try { virtualDisplay?.release() } catch (_: Exception) {}
    try { projection?.stop() } catch (_: Exception) {}
    virtualDisplay = null
    projection = null
    imageReader = null
    try { thread.quitSafely() } catch (_: Exception) {}
  }
}
