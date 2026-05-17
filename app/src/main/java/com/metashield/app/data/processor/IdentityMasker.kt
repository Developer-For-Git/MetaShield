package com.metashield.app.data.processor

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

/**
 * IdentityMasker - Implementation of the "Identity Masking Protocol".
 * Detects faces and sensitive areas using ML Kit and applies tactical anonymization.
 */
class IdentityMasker(private val context: Context) {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()
    )

    suspend fun anonymize(uri: Uri): Result<File> {
        return try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return Result.failure(Exception("Failed to decode image"))
            
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            
            if (faces.isEmpty()) {
                return Result.failure(Exception("No identities detected to mask"))
            }

            // Create mutable copy for masking
            val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)

            faces.forEach { face ->
                val bounds = face.boundingBox
                applyPrivacyMatrix(canvas, resultBitmap, bounds)
            }

            // Save sanitized file to cache
            val outputFile = File(context.cacheDir, "sanitized_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a 'Privacy Matrix' (pixelation) effect to the given bounds.
     */
    private fun applyPrivacyMatrix(canvas: Canvas, bitmap: Bitmap, bounds: Rect) {
        val pixelSize = (bounds.width() / 15).coerceAtLeast(10) // Tactical pixel density
        
        // Ensure bounds are within bitmap
        val safeBounds = Rect(
            bounds.left.coerceAtLeast(0),
            bounds.top.coerceAtLeast(0),
            bounds.right.coerceAtMost(bitmap.width),
            bounds.bottom.coerceAtMost(bitmap.height)
        )

        for (x in safeBounds.left until safeBounds.right step pixelSize) {
            for (y in safeBounds.top until safeBounds.bottom step pixelSize) {
                val color = bitmap.getPixel(
                    (x + pixelSize / 2).coerceAtMost(bitmap.width - 1),
                    (y + pixelSize / 2).coerceAtMost(bitmap.height - 1)
                )
                
                val paint = Paint().apply {
                    this.color = color
                    style = Paint.Style.FILL
                }
                
                val rect = Rect(
                    x, y, 
                    (x + pixelSize).coerceAtMost(safeBounds.right), 
                    (y + pixelSize).coerceAtMost(safeBounds.bottom)
                )
                canvas.drawRect(rect, paint)
            }
        }
        
        // Add a subtle tech border around the masked area
        val borderPaint = Paint().apply {
            color = Color.CYAN
            alpha = 80
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(safeBounds, borderPaint)
    }
}
