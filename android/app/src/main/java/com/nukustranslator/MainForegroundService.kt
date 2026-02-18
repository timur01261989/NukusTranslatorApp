package com.nukustranslator

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MainForegroundService : Service() {

  companion object {
    const val ACTION_START = "com.nukustranslator.action.START"
    const val ACTION_STOP = "com.nukustranslator.action.STOP"
    private const val CHANNEL_ID = "NST_CHANNEL"
    private const val NOTIF_ID = 1337
  }

  private val TAG = "MainForegroundService"
  private var captureManager: ScreenCaptureManager? = null

  override fun onCreate() {
    super.onCreate()
    MainServiceLocator.currentService = this
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> startWork()
      ACTION_STOP -> stopWork()
      else -> { /* ignore */ }
    }
    return START_STICKY
  }

  private fun startWork() {
    Log.d(TAG, "startWork")
    startForeground(NOTIF_ID, buildNotification("Translating… Tap to open app"))
    OverlayService.ensureRunning(this)

    // IMPORTANT: MediaProjection requires user consent via an Activity.
    // We launch a tiny activity to request it.
    val i = Intent(this, ProjectionPermissionActivity::class.java)
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(i)
  }

  fun onProjectionReady(resultCode: Int, data: Intent) {
    captureManager?.stop()
    captureManager = ScreenCaptureManager(this, resultCode, data) { bitmap ->
      // OCR on each frame (throttle inside manager)
      TextRecognitionModule.recognize(bitmap) { blocks ->
        // Send blocks to RN
        OcrEventBus.emit(this, blocks)
      }
    }
    captureManager?.start()
  }

  private fun stopWork() {
    Log.d(TAG, "stopWork")
    captureManager?.stop()
    captureManager = null
    OverlayService.stop(this)
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onDestroy() {
    MainServiceLocator.currentService = null
    super.onDestroy()
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val ch = NotificationChannel(CHANNEL_ID, "Live Screen Translator", NotificationManager.IMPORTANCE_LOW)
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      nm.createNotificationChannel(ch)
    }
  }

  private fun buildNotification(text: String): Notification {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    val pending = PendingIntent.getActivity(
      this, 0, launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Live Screen Translator")
      .setContentText(text)
      .setSmallIcon(android.R.drawable.ic_menu_view)
      .setContentIntent(pending)
      .setOngoing(true)
      .build()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
