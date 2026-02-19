package com.nukustranslator

import android.content.Context
import android.graphics.Color

object OverlayPrefs {
  private const val FILE = "nst_overlay_prefs"
  private const val KEY_TEXT_SIZE = "text_size_sp"
  private const val KEY_TEXT_COLOR = "text_color"
  private const val KEY_BG_COLOR = "bg_color"
  private const val KEY_BG_ALPHA = "bg_alpha" // 0..255

  private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

  fun setStyle(ctx: Context, textSizeSp: Float, textColor: Int, bgColor: Int, bgAlpha: Int) {
    prefs(ctx).edit()
      .putFloat(KEY_TEXT_SIZE, textSizeSp)
      .putInt(KEY_TEXT_COLOR, textColor)
      .putInt(KEY_BG_COLOR, bgColor)
      .putInt(KEY_BG_ALPHA, bgAlpha.coerceIn(0,255))
      .apply()
  }

  fun getTextSizeSp(ctx: Context): Float = prefs(ctx).getFloat(KEY_TEXT_SIZE, 14f)
  fun getTextColor(ctx: Context): Int = prefs(ctx).getInt(KEY_TEXT_COLOR, Color.WHITE)
  fun getBgColor(ctx: Context): Int = prefs(ctx).getInt(KEY_BG_COLOR, 0x000000)
  fun getBgAlpha(ctx: Context): Int = prefs(ctx).getInt(KEY_BG_ALPHA, 0x99)

  fun getBgColorWithAlpha(ctx: Context): Int {
    val c = getBgColor(ctx)
    val a = getBgAlpha(ctx)
    return (a shl 24) or (c and 0x00FFFFFF)
  }
}
