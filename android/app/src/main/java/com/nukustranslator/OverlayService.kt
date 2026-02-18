package com.nukustranslator

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Simple overlay drawing translated blocks as TextViews.
 * Real product: custom canvas rendering + background blur + line wrapping.
 */
class OverlayService : Service() {

  companion object {
    private val latestBlocks = AtomicReference<List<Map<String, Any>>>(emptyList())

    fun ensureRunning(ctx: Context) {
      val i = Intent(ctx, OverlayService::class.java)
      ctx.startService(i)
    }

    fun stop(ctx: Context) {
      val i = Intent(ctx, OverlayService::class.java)
      ctx.stopService(i)
    }

    fun updateBlocks(ctx: Context, blocks: ReadableArray) {
      val list = mutableListOf<Map<String, Any>>()
      for (i in 0 until blocks.size()) {
        val m: ReadableMap = blocks.getMap(i) ?: continue
        val item = mapOf(
          "text" to (m.getString("text") ?: ""),
          "translated" to (m.getString("translated") ?: ""),
          "left" to m.getInt("left"),
          "top" to m.getInt("top"),
          "right" to m.getInt("right"),
          "bottom" to m.getInt("bottom")
        )
        list.add(item)
      }
      latestBlocks.set(list)
      OverlayRenderer.requestRender()
    }
  }

  private lateinit var wm: WindowManager
  private var root: FrameLayout? = null

  override fun onCreate() {
    super.onCreate()
    wm = getSystemService(WINDOW_SERVICE) as WindowManager
    root = FrameLayout(this)
    OverlayRenderer.attach(this, wm, root!!)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    OverlayRenderer.setDataProvider { latestBlocks.get() }
    return START_STICKY
  }

  override fun onDestroy() {
    OverlayRenderer.detach(wm, root)
    root = null
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
