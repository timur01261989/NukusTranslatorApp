package com.nukustranslator

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {

  private const val FILE = "nst_secure"
  private const val KEY_API = "api_key"

  private fun prefs(ctx: Context) =
    EncryptedSharedPreferences.create(
      ctx,
      FILE,
      MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build(),
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

  fun saveApiKey(ctx: Context, key: String) {
    prefs(ctx).edit().putString(KEY_API, key).apply()
  }

  fun getApiKey(ctx: Context): String? {
    return prefs(ctx).getString(KEY_API, null)
  }
}
