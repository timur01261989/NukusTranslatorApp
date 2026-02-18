package com.nukustranslator

import android.app.Activity
import android.os.Bundle
import android.content.Intent

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ilova ochilishi bilan ruxsat so'rash oynasiga o'tadi
        val intent = Intent(this, ProjectionPermissionActivity::class.java)
        startActivity(intent)
        finish()
    }
}
