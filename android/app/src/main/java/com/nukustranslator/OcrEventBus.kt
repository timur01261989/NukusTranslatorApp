package com.nukustranslator

import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.ReactContext
import com.facebook.react.ReactApplication
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule

object OcrEventBus {

  private fun toWritable(blocks: List<OcrBlock>): WritableMap {
    val map = Arguments.createMap()
    val arr: WritableArray = Arguments.createArray()
    for (b in blocks) {
      val m = Arguments.createMap()
      m.putString("text", b.text)
      m.putInt("left", b.left)
      m.putInt("top", b.top)
      m.putInt("right", b.right)
      m.putInt("bottom", b.bottom)
      arr.pushMap(m)
    }
    map.putArray("blocks", arr)
    return map
  }

  fun emit(ctx: Context, blocks: List<OcrBlock>) {
    val app = ctx.applicationContext as? ReactApplication ?: return
    val reactContext = app.reactNativeHost.reactInstanceManager.currentReactContext ?: return
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("OCR_BLOCKS", toWritable(blocks))
  }
}
