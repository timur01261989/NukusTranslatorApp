package com.nukustranslator

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class OcrBlock(
  val text: String,
  val left: Int,
  val top: Int,
  val right: Int,
  val bottom: Int
)

object TextRecognitionModule {
  private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

  fun recognize(bitmap: Bitmap, done: (List<OcrBlock>) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(image)
      .addOnSuccessListener { result: Text ->
        val blocks = mutableListOf<OcrBlock>()
        for (b in result.textBlocks) {
          val bb: Rect? = b.boundingBox
          val t = b.text?.trim() ?: ""
          if (t.isNotEmpty() && bb != null) {
            blocks.add(OcrBlock(t, bb.left, bb.top, bb.right, bb.bottom))
          }
        }
        done(blocks)
      }
      .addOnFailureListener {
        done(emptyList())
      }
  }
}
