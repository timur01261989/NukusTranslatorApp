package com.nukustranslator

/**
 * Small shared state store between service and RN bridge.
 * In production, store in encrypted prefs instead.
 */
object AppState {
  @Volatile var apiKey: String = ""
  @Volatile var sourceLang: String = "English"
  @Volatile var targetLang: String = "Uzbek"
}
