package com.scenescout.app.ui.brief

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.scenescout.app.data.SpotBrief
import java.io.File

/**
 * Renders a [SpotBrief] as a cinematic one-page PDF a videographer can send
 * to a client or artist. Phone-portrait page (1080x1920), dark theme.
 */
object SpotBriefPdf {

    private const val PAGE_W = 1080f
    private const val PAGE_H = 1920f
    private const val MARGIN = 72f

    private val bg = 0xFF14141C.toInt()
    private val surface = 0xFF1E1E28.toInt()
    private val accent = 0xFFE8A33D.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val gray = 0xFFB9B9C7.toInt()
    private val dim = 0xFF7A7A88.toInt()

    fun render(context: Context, brief: SpotBrief, hero: Bitmap?): File {
        val doc = PdfDocument()
        val page = doc.startPage(
            PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create(),
        )
        try {
            draw(page.canvas, brief, hero)
        } finally {
            doc.finishPage(page)
        }
        val dir = File(context.cacheDir, "briefs").apply { mkdirs() }
        val file = File(dir, "scenescout-brief.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun draw(canvas: Canvas, brief: SpotBrief, hero: Bitmap?) {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val grayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gray }
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dim }

        canvas.drawColor(bg)
        // Top accent bar.
        canvas.drawRect(0f, 0f, PAGE_W, 14f, accentPaint)

        var y = 120f
        // Title.
        text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        text.textSize = 64f
        y = drawWrapped(canvas, brief.title, text, MARGIN, PAGE_W - MARGIN, y, 78f) + 8f
        // Score line.
        accentPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        accentPaint.textSize = 40f
        canvas.drawText(brief.scoreLine, MARGIN, y + 40f, accentPaint)
        y += 110f

        // Hero image band.
        val heroTop = y
        val heroH = 420f
        if (hero != null) {
            val dest = RectF(MARGIN, heroTop, PAGE_W - MARGIN, heroTop + heroH)
            val src = centerCrop(hero, dest.width() / dest.height())
            canvas.drawBitmap(hero, src, dest, null)
        } else {
            val grad = LinearGradient(
                0f, heroTop, 0f, heroTop + heroH,
                0xFF3A3A4A.toInt(), 0xFF14141C.toInt(), Shader.TileMode.CLAMP,
            )
            val p = Paint().apply { shader = grad }
            canvas.drawRect(MARGIN, heroTop, PAGE_W - MARGIN, heroTop + heroH, p)
            dimPaint.textSize = 34f
            dimPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "SCENESCOUT", PAGE_W / 2f, heroTop + heroH / 2f, dimPaint,
            )
            dimPaint.textAlign = Paint.Align.LEFT
        }
        y = heroTop + heroH + 48f

        y = section(canvas, "THE SCENE", brief.description, y, grayPaint, accentPaint)
        if (brief.tags.isNotEmpty()) {
            y = drawTags(canvas, brief.tags.take(5), y, accentPaint, grayPaint) + 16f
        }
        y = section(
            canvas, "BEST LIGHT",
            "${brief.goldenHour}\n${brief.sunriseSunset}", y, grayPaint, accentPaint,
        )
        y = section(
            canvas, "PERMIT — ${brief.permitLevel.uppercase()}",
            brief.permitSummary, y, grayPaint, accentPaint,
        )
        if (brief.bestFor.isNotEmpty()) {
            y = section(
                canvas, "GREAT FOR", brief.bestFor.joinToString(" · "),
                y, grayPaint, accentPaint,
            )
        }
        if (brief.reviews.isNotEmpty()) {
            accentPaint.textSize = 34f
            accentPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("COMMUNITY INTEL", MARGIN, y, accentPaint)
            y += 52f
            brief.reviews.forEach { review ->
                if (y > PAGE_H - 220f) return@forEach
                accentPaint.textSize = 32f
                canvas.drawText("★".repeat(review.stars.coerceIn(1, 5)),
                    MARGIN, y, accentPaint)
                y += 10f
                grayPaint.textSize = 34f
                grayPaint.typeface = Typeface.DEFAULT
                y = drawWrapped(
                    canvas, "\"${review.text}\"", grayPaint,
                    MARGIN, PAGE_W - MARGIN, y + 34f, 44f,
                )
                dimPaint.textSize = 30f
                val byline = "— ${review.author}" +
                    (if (review.bestTime.isNotBlank()) " · Best light: ${review.bestTime}" else "")
                canvas.drawText(byline, MARGIN, y + 36f, dimPaint)
                y += 84f
            }
        }

        // Footer.
        dimPaint.textSize = 30f
        dimPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(brief.footer, PAGE_W / 2f, PAGE_H - 64f, dimPaint)
        canvas.drawText(brief.coordinates, PAGE_W / 2f, PAGE_H - 110f, dimPaint)
        dimPaint.textAlign = Paint.Align.LEFT
    }

    private fun section(
        canvas: Canvas,
        header: String,
        body: String,
        startY: Float,
        bodyPaint: Paint,
        headerPaint: Paint,
    ): Float {
        if (startY > PAGE_H - 260f) return startY
        headerPaint.textSize = 34f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(header, MARGIN, startY, headerPaint)
        bodyPaint.textSize = 36f
        bodyPaint.typeface = Typeface.DEFAULT
        var y = startY + 52f
        body.split("\n").forEach { line ->
            y = drawWrapped(canvas, line, bodyPaint, MARGIN, PAGE_W - MARGIN, y, 48f) + 12f
        }
        return y + 28f
    }

    private fun drawTags(
        canvas: Canvas,
        tags: List<String>,
        startY: Float,
        stroke: Paint,
        textPaint: Paint,
    ): Float {
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = accent
            strokeWidth = 2.5f
        }
        textPaint.textSize = 30f
        var x = MARGIN
        var y = startY
        tags.forEach { tag ->
            val w = textPaint.measureText(tag) + 44f
            if (x + w > PAGE_W - MARGIN) {
                x = MARGIN
                y += 74f
            }
            val rect = RectF(x, y, x + w, y + 58f)
            canvas.drawRoundRect(rect, 29f, 29f, outline)
            canvas.drawText(tag, x + 22f, y + 40f, textPaint)
            x += w + 18f
        }
        return y + 58f
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        right: Float,
        startY: Float,
        lineHeight: Float,
    ): Float {
        var y = startY
        var line = StringBuilder()
        text.split(" ").forEach { word ->
            val trial = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(trial) > right - x && line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
                line = StringBuilder(word)
            } else {
                line = StringBuilder(trial)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
            y += lineHeight
        }
        return y
    }

    private fun centerCrop(bitmap: Bitmap, targetAspect: Float): android.graphics.Rect {
        val bmpAspect = bitmap.width.toFloat() / bitmap.height
        return if (bmpAspect > targetAspect) {
            // Too wide: crop sides.
            val w = (bitmap.height * targetAspect).toInt()
            val left = (bitmap.width - w) / 2
            android.graphics.Rect(left, 0, left + w, bitmap.height)
        } else {
            // Too tall: crop top/bottom.
            val h = (bitmap.width / targetAspect).toInt()
            val top = (bitmap.height - h) / 2
            android.graphics.Rect(0, top, bitmap.width, top + h)
        }
    }
}
