package com.nukustranslator

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import java.util.concurrent.atomic.AtomicBoolean

object OverlayRenderer {

  private var ctx: Context? = null
  private var container: FrameLayout? = null
  private var provider: (() -> List<Map<String, Any>>)? = null
  private val needsRender = AtomicBoolean(false)

  fun attach(context: Context, wm: WindowManager, root: FrameLayout) {
    ctx = context
    container = root

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      else
        WindowManager.LayoutParams.TYPE_PHONE,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
      android.graphics.PixelFormat.TRANSLUCENT
    )
    params.gravity = Gravity.TOP or Gravity.START
    wm.addView(root, params)
  }

  fun detach(wm: WindowManager, root: FrameLayout?) {
    if (root != null) {
      try { wm.removeView(root) } catch (_: Exception) {}
    }
    ctx = null
    container = null
    provider = null
  }

  fun setDataProvider(p: () -> List<Map<String, Any>>) {
    provider = p
  }

  fun requestRender() {
    needsRender.set(true)
    renderIfNeeded()
  }

  private fun renderIfNeeded() {
    val root = container ?: return
    if (!needsRender.compareAndSet(true, false)) return

    val data = provider?.invoke() ?: emptyList()
    root.removeAllViews()

    for (b in data) {
      val translated = (b["translated"] as? String)?.trim().orEmpty()
      if (translated.isEmpty()) continue

      val left = (b["left"] as? Int) ?: 0
      val top = (b["top"] as? Int) ?: 0
      val right = (b["right"] as? Int) ?: (left + 200)
      val bottom = (b["bottom"] as? Int) ?: (top + 80)

      val tv = TextView(root.context)
      tv.text = translated
      val ctx = root.context
      tv.setTextColor(OverlayPrefs.getTextColor(ctx))
      tv.setBackgroundColor(OverlayPrefs.getBgColorWithAlpha(ctx))
      tv.textSize = OverlayPrefs.getTextSizeSp(ctx)
      tv.setPadding(6, 4, 6, 4)

      val lp = FrameLayout.LayoutParams(
        (right - left).coerceAtLeast(60),
        FrameLayout.LayoutParams.WRAP_CONTENT
      )
      lp.leftMargin = left
      lp.topMargin = top
      root.addView(tv, lp)
    }
  }
}
