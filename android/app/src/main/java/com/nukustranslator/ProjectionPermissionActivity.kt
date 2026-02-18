package com.nukustranslator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

/**
 * Transparent Activity to request screen capture permission.
 */
class ProjectionPermissionActivity : Activity() {

  private val REQ = 9001

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    startActivityForResult(mpm.createScreenCaptureIntent(), REQ)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQ && resultCode == RESULT_OK && data != null) {
      val svc = MainServiceLocator.currentService
      svc?.onProjectionReady(resultCode, data)
    }
    finish()
  }
}
