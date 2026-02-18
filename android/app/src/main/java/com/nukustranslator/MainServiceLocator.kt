package com.nukustranslator

/**
 * Small locator so ProjectionPermissionActivity can deliver the MediaProjection result to the running service.
 * Not pretty, but simple for a starter.
 */
object MainServiceLocator {
  @Volatile var currentService: MainForegroundService? = null
}
