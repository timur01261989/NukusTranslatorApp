package com.nukustranslator

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class BridgeModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val TAG = "BridgeModule"

    override fun getName(): String = "BridgeModule"

    @ReactMethod
    fun configure(apiKey: String, sourceLang: String, targetLang: String, promise: Promise) {
        try {
            AppState.apiKey = apiKey
            AppState.sourceLang = sourceLang
            AppState.targetLang = targetLang
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CONFIG_ERR", e)
        }
    }

    @ReactMethod
    fun ensureOverlayPermission(promise: Promise) {
        try {
            // TUZATILDI: currentActivity ni reactApplicationContext orqali chaqiramiz
            val activity = reactApplicationContext.currentActivity
            val ctx = activity ?: reactApplicationContext
            
            if (Settings.canDrawOverlays(ctx)) {
                promise.resolve(true)
                return
            }
            
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + reactApplicationContext.packageName)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            reactApplicationContext.startActivity(intent)
            
            // Biz kutib tura olmaymiz, React Native o'zi qayta tekshirishi kerak
            promise.resolve(false)
        } catch (e: Exception) {
            promise.reject("OVERLAY_ERR", e)
        }
    }

    @ReactMethod
    fun start(promise: Promise) {
        try {
            val ctx = reactApplicationContext
            val i = Intent(ctx, MainForegroundService::class.java)
            i.action = MainForegroundService.ACTION_START
            ctx.startForegroundService(i)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("START_ERR", e)
        }
    }

    @ReactMethod
    fun stop(promise: Promise) {
        try {
            val ctx = reactApplicationContext
            val i = Intent(ctx, MainForegroundService::class.java)
            i.action = MainForegroundService.ACTION_STOP
            ctx.startService(i)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("STOP_ERR", e)
        }
    }

    @ReactMethod
    fun updateOverlay(blocks: ReadableArray, promise: Promise) {
        try {
            OverlayService.updateBlocks(reactApplicationContext, blocks)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("OVERLAY_UPDATE_ERR", e)
        }
    }

    fun emitOcrBlocks(blocksJson: WritableMap) {
        try {
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("OCR_BLOCKS", blocksJson)
        } catch (e: Exception) {
            Log.e(TAG, "emit failed", e)
        }
    }

    @ReactMethod
    fun saveApiKey(key: String, promise: Promise) {
        try {
            SecurePrefs.saveApiKey(reactApplicationContext, key)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SAVE_KEY_ERR", e)
        }
    }

    @ReactMethod
    fun getApiKey(promise: Promise) {
        try {
            val k = SecurePrefs.getApiKey(reactApplicationContext)
            promise.resolve(k)
        } catch (e: Exception) {
            promise.reject("GET_KEY_ERR", e)
        }
    }

@ReactMethod
fun setOverlayStyle(textSizeSp: Double, textColorHex: String, bgColorHex: String, bgAlpha: Int, promise: Promise) {
    try {
        val ctx = reactApplicationContext
        val textColor = parseColorSafe(textColorHex, android.graphics.Color.WHITE)
        val bgColor = parseColorSafe(bgColorHex, 0x000000)
        OverlayPrefs.setStyle(ctx, textSizeSp.toFloat(), textColor, bgColor, bgAlpha)
        promise.resolve(true)
    } catch (e: Exception) {
        promise.reject("OVERLAY_STYLE_SET_ERR", e)
    }
}

@ReactMethod
fun getOverlayStyle(promise: Promise) {
    try {
        val ctx = reactApplicationContext
        val map = Arguments.createMap()
        map.putDouble("textSizeSp", OverlayPrefs.getTextSizeSp(ctx).toDouble())
        map.putString("textColorHex", String.format("#%06X", 0xFFFFFF and OverlayPrefs.getTextColor(ctx)))
        map.putString("bgColorHex", String.format("#%06X", 0xFFFFFF and OverlayPrefs.getBgColor(ctx)))
        map.putInt("bgAlpha", OverlayPrefs.getBgAlpha(ctx))
        promise.resolve(map)
    } catch (e: Exception) {
        promise.reject("OVERLAY_STYLE_GET_ERR", e)
    }
}

private fun parseColorSafe(hex: String, fallback: Int): Int {
    return try {
        val h = hex.trim()
        if (h.startsWith("#")) android.graphics.Color.parseColor(h) else android.graphics.Color.parseColor("#" + h)
    } catch (_: Exception) { fallback }
}

}
