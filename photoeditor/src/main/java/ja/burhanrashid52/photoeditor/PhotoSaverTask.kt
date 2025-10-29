package ja.burhanrashid52.photoeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import ja.burhanrashid52.photoeditor.BitmapUtil.removeTransparency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Burhanuddin Rashid on 18/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
internal class PhotoSaverTask(
    private val photoEditorView: PhotoEditorView,
    private val boxHelper: BoxHelper,
    private var saveSettings: SaveSettings
) {

    private val drawingView: DrawingView = photoEditorView.drawingView

    private fun onBeforeSaveImage() {
        boxHelper.clearHelperBox()
        drawingView.destroyDrawingCache()
    }

    fun saveImageAsBitmap(): Bitmap {
        onBeforeSaveImage()
        val bitmap = buildBitmap()
        if (saveSettings.isClearViewsEnabled) {
            boxHelper.clearAllViews(drawingView)
        }
        return bitmap
    }

    suspend fun saveImageAsFile(imagePath: String): SaveFileResult {
        onBeforeSaveImage()
        val capturedBitmap = buildBitmap()

        val result = withContext(Dispatchers.IO) {
            val file = File(imagePath)
            try {
                FileOutputStream(file, false).use { outputStream ->
                    capturedBitmap.compress(
                        saveSettings.compressFormat,
                        saveSettings.compressQuality,
                        outputStream
                    )
                    outputStream.flush()
                }

                SaveFileResult.Success
            } catch (e: IOException) {
                SaveFileResult.Failure(e)
            }
        }

        if (result is SaveFileResult.Success) {
            // Clear all views if it's enabled in save settings
            if (saveSettings.isClearViewsEnabled) {
                boxHelper.clearAllViews(drawingView)
            }
        }

        return result
    }

    private fun buildBitmap(): Bitmap {
        return if (saveSettings.isTransparencyEnabled) {
            removeTransparency(captureView(photoEditorView))
        } else {
            captureView(photoEditorView)
        }
    }

    private fun captureView(view: PhotoEditorView): Bitmap {
        val drawable = view.source.drawable
        val fallbackWidth = view.width.takeIf { it > 0 }
        val fallbackHeight = view.height.takeIf { it > 0 }

        val sourceWidth = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.width
            else -> drawable?.intrinsicWidth ?: fallbackWidth
        } ?: fallbackWidth ?: 1

        val sourceHeight = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.height
            else -> drawable?.intrinsicHeight ?: fallbackHeight
        } ?: fallbackHeight ?: 1

        val bitmap = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val displayedWidth = fallbackWidth ?: sourceWidth
        val displayedHeight = fallbackHeight ?: sourceHeight

        val scaleX = if (displayedWidth == 0) 1f else sourceWidth.toFloat() / displayedWidth
        val scaleY = if (displayedHeight == 0) 1f else sourceHeight.toFloat() / displayedHeight

        canvas.save()
        canvas.scale(scaleX, scaleY)
        view.draw(canvas)
        canvas.restore()
        return bitmap
    }

}
