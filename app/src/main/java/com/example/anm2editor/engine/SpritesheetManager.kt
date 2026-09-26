package com.example.anm2editor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.InputStream

class SpritesheetManager(private val context: Context) {

    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private val uriMap = mutableMapOf<Int, Uri>()

    // Default procedural Isaac spritesheet bitmap (256x256)
    private val defaultIsaacSheet: Bitmap by lazy {
        loadDefaultIsaacBitmap()
    }

    private fun loadDefaultIsaacBitmap(): Bitmap {
        // Try loading from drawable resource isaac_sheet if available
        try {
            val resId = context.resources.getIdentifier("isaac_sheet", "drawable", context.packageName)
            if (resId != 0) {
                val opt = BitmapFactory.Options().apply { inScaled = false }
                val bmp = BitmapFactory.decodeResource(context.resources, resId, opt)
                if (bmp != null) return bmp
            }
        } catch (_: Exception) {}

        return generateProceduralIsaacSheet()
    }

    fun getOrLoadBitmap(sheetId: Int, path: String?): Bitmap {
        bitmapCache[sheetId]?.let { return it }

        // If user set a custom URI for this sheetId
        uriMap[sheetId]?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opt = BitmapFactory.Options().apply { inScaled = false }
                    val bmp = BitmapFactory.decodeStream(stream, null, opt)
                    if (bmp != null) {
                        bitmapCache[sheetId] = bmp
                        return bmp
                    }
                }
            } catch (_: Exception) {}
        }

        // Return default Isaac spritesheet for slot 0 or fallback
        if (sheetId == 0 || bitmapCache.isEmpty()) {
            bitmapCache[sheetId] = defaultIsaacSheet
            return defaultIsaacSheet
        }

        // Generate procedural labelled sheet for missing secondary spritesheets
        val generated = generatePlaceholderSheet(sheetId, path ?: "Sheet $sheetId")
        bitmapCache[sheetId] = generated
        return generated
    }

    fun getImageBitmap(sheetId: Int, path: String?): ImageBitmap {
        return getOrLoadBitmap(sheetId, path).asImageBitmap()
    }

    fun setSpritesheetUri(sheetId: Int, uri: Uri): Boolean {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val opt = BitmapFactory.Options().apply { inScaled = false }
                val bmp = BitmapFactory.decodeStream(stream, null, opt)
                if (bmp != null) {
                    uriMap[sheetId] = uri
                    bitmapCache[sheetId] = bmp
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    fun registerBitmap(sheetId: Int, bitmap: Bitmap) {
        bitmapCache[sheetId] = bitmap
    }

    fun clearCache() {
        bitmapCache.clear()
        uriMap.clear()
    }

    private fun generateProceduralIsaacSheet(): Bitmap {
        val width = 256
        val height = 256
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw Isaac Head front (0, 0, 32, 32)
        drawIsaacHead(c, 0, 0, false, false)
        // Isaac Head shoot (32, 0, 32, 32)
        drawIsaacHead(c, 32, 0, isShooting = true, isHurt = false)
        // Isaac Head hurt (64, 0, 32, 32)
        drawIsaacHead(c, 64, 0, isShooting = false, isHurt = true)
        // Isaac Head side (96, 0, 32, 32)
        drawIsaacHead(c, 96, 0, isShooting = false, isHurt = false, isSide = true)

        // Isaac Body front stand (0, 32, 32, 32)
        drawIsaacBody(c, 0, 32, walkStep = 0)
        // Isaac Body walk step 1 (32, 32, 32, 32)
        drawIsaacBody(c, 32, 32, walkStep = 1)
        // Isaac Body walk step 2 (64, 32, 32, 32)
        drawIsaacBody(c, 64, 32, walkStep = 2)
        // Isaac Body walk step 3 (96, 32, 32, 32)
        drawIsaacBody(c, 96, 32, walkStep = 3)

        // Tear projectile (0, 64, 32, 32)
        drawTear(c, 0, 64, size = 10)
        // Tear splash (32, 64, 32, 32)
        drawTearSplash(c, 32, 64)

        // Item pedestal (0, 96, 32, 32)
        drawPedestal(c, 0, 96)
        // Collectible Sad Onion / Brimstone icon (32, 96, 32, 32)
        drawSadOnion(c, 32, 96)

        return bmp
    }

    private fun drawIsaacHead(c: Canvas, ox: Int, oy: Int, isShooting: Boolean, isHurt: Boolean, isSide: Boolean = false) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Head shadow
        paint.color = Color.argb(160, 40, 25, 20)
        c.drawOval(ox + 3f, oy + 4f, ox + 29f, oy + 28f, paint)

        // Head skin (#F5C4A0 peach Isaac skin)
        paint.color = if (isHurt) Color.rgb(255, 140, 140) else Color.rgb(245, 196, 160)
        c.drawOval(ox + 4f, oy + 5f, ox + 28f, oy + 27f, paint)

        // Head outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = Color.rgb(65, 38, 28)
        c.drawOval(ox + 4f, oy + 5f, ox + 28f, oy + 27f, paint)

        // Eyes
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(30, 20, 20)
        if (isSide) {
            c.drawOval(ox + 20f, oy + 13f, ox + 25f, oy + 20f, paint)
        } else {
            c.drawOval(ox + 8f, oy + 13f, ox + 13f, oy + 20f, paint)
            c.drawOval(ox + 19f, oy + 13f, ox + 24f, oy + 20f, paint)
        }

        // Tears streaming from eyes (classic Isaac)
        paint.color = Color.rgb(100, 180, 255)
        if (!isSide) {
            c.drawRect(ox + 9f, oy + 19f, ox + 12f, oy + 26f, paint)
            c.drawRect(ox + 20f, oy + 19f, ox + 23f, oy + 26f, paint)
        }

        // Mouth
        paint.color = Color.rgb(70, 35, 25)
        if (isShooting) {
            c.drawOval(ox + 13f, oy + 21f, ox + 19f, oy + 25f, paint)
        } else if (isHurt) {
            c.drawLine(ox + 13f, oy + 23f, ox + 19f, oy + 23f, paint)
        } else {
            c.drawArc(ox + 13f, oy + 20f, ox + 19f, oy + 24f, 0f, 180f, false, paint)
        }
    }

    private fun drawIsaacBody(c: Canvas, ox: Int, oy: Int, walkStep: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Torso (#F5C4A0)
        paint.color = Color.rgb(240, 190, 155)
        c.drawRect(ox + 11f, oy + 6f, ox + 21f, oy + 17f, paint)

        // Torso outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.rgb(65, 38, 28)
        c.drawRect(ox + 11f, oy + 6f, ox + 21f, oy + 17f, paint)

        // Legs / feet
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(230, 180, 145)
        val legLeftY = when (walkStep) {
            1 -> 23f
            3 -> 17f
            else -> 20f
        }
        val legRightY = when (walkStep) {
            1 -> 17f
            3 -> 23f
            else -> 20f
        }
        c.drawRect(ox + 11f, oy + 16f, ox + 14f, oy + legLeftY, paint)
        c.drawRect(ox + 18f, oy + 16f, ox + 21f, oy + legRightY, paint)
    }

    private fun drawTear(c: Canvas, ox: Int, oy: Int, size: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(100, 200, 255)
        val cx = ox + 16f
        val cy = oy + 16f
        c.drawCircle(cx, cy, size / 2f, paint)

        paint.color = Color.argb(180, 255, 255, 255)
        c.drawCircle(cx - 2f, cy - 2f, size / 4f, paint)
    }

    private fun drawTearSplash(c: Canvas, ox: Int, oy: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(90, 190, 250)
        val cx = ox + 16f
        val cy = oy + 16f
        c.drawCircle(cx - 6f, cy - 4f, 3f, paint)
        c.drawCircle(cx + 6f, cy - 5f, 3f, paint)
        c.drawCircle(cx - 4f, cy + 5f, 2.5f, paint)
        c.drawCircle(cx + 5f, cy + 4f, 2.5f, paint)
        c.drawCircle(cx, cy, 4f, paint)
    }

    private fun drawPedestal(c: Canvas, ox: Int, oy: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(120, 115, 125)
        c.drawRect(ox + 4f, oy + 16f, ox + 28f, oy + 28f, paint)
        paint.color = Color.rgb(90, 85, 95)
        c.drawRect(ox + 8f, oy + 8f, ox + 24f, oy + 16f, paint)
    }

    private fun drawSadOnion(c: Canvas, ox: Int, oy: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(180, 220, 140)
        c.drawCircle(ox + 16f, oy + 16f, 8f, paint)
        paint.color = Color.rgb(50, 80, 30)
        c.drawPoint(ox + 14f, oy + 14f, paint)
        c.drawPoint(ox + 18f, oy + 14f, paint)
    }

    private fun generatePlaceholderSheet(id: Int, title: String): Bitmap {
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Checkerboard
        val cellSize = 32
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                paint.color = if ((x + y) % 2 == 0) Color.rgb(40, 40, 48) else Color.rgb(52, 52, 62)
                c.drawRect(x * cellSize.toFloat(), y * cellSize.toFloat(), (x + 1) * cellSize.toFloat(), (y + 1) * cellSize.toFloat(), paint)
            }
        }

        // Grid lines
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(100, 255, 255, 255)
        for (i in 0..8) {
            c.drawLine(0f, i * cellSize.toFloat(), 256f, i * cellSize.toFloat(), paint)
            c.drawLine(i * cellSize.toFloat(), 0f, i * cellSize.toFloat(), 256f, paint)
        }

        // Text
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(240, 210, 120)
        paint.textSize = 14f
        c.drawText("Sheet #$id: $title", 10f, 22f, paint)
        paint.textSize = 10f
        paint.color = Color.WHITE
        c.drawText("(Tap 'Load PNG' to replace)", 10f, 40f, paint)

        return bmp
    }
}
