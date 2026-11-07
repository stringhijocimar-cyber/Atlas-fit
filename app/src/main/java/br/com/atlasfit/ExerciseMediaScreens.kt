package br.com.atlasfit

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

private val secondaryColor = 0xffedbf71.toInt()
private const val FRAME_HOLD_MS = 1050L

internal class ExerciseMediaScreens(private val ui: MainActivity) {
    fun overview(page: LinearLayout, exercise: Exercise, compact: Boolean = false) {
        val guide = ExerciseGuides[exercise]
        val panel = ui.card(page, highlighted = true)
        if (compact) {
            val row = ui.row(panel).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(ExerciseArt.phase(ui, guide, end = true),
                LinearLayout.LayoutParams(ui.dp(105), ui.dp(105)).apply { marginEnd = ui.dp(11) })
            val labels = LinearLayout(ui).apply { orientation = LinearLayout.VERTICAL }
            row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            muscleLegend(labels, guide)
        } else {
            ui.line(panel, guide.equipment.uppercase(ptBr), 12, accentColor, bold = true)
            val row = ui.row(panel)
            listOf("INÍCIO", "FINAL").forEachIndexed { phase, title ->
                val column = LinearLayout(ui).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                row.addView(column, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (phase == 0) marginEnd = ui.dp(6)
                })
                column.addView(ExerciseArt.phase(ui, guide, end = phase == 1),
                    LinearLayout.LayoutParams(-1, ui.dp(142)))
                ui.line(column, title, 11, mutedColor, bold = true)
            }
            muscleLegend(panel, guide)
        }
        val controls = ui.row(panel)
        ui.button(controls, "▶  Animar fases", weighted = true, primary = true) { movement(exercise, guide) }
        ExerciseGuides.apparatus(guide)?.let { apparatus ->
            ui.button(controls, "Equipamento", weighted = true) { equipment(guide, apparatus) }
        }
    }

    private fun muscleLegend(panel: LinearLayout, guide: ExerciseGuide) {
        val mobility = guide.apparatus == null
        ui.line(panel, if (mobility) "OBJETIVO" else "●  PRINCIPAIS", 10, successColor, bold = true)
            .letterSpacing = .07f
        ui.line(panel, guide.primary, 13).setPadding(0, 0, 0, ui.dp(4))
        if (guide.secondary.isNotBlank()) {
            ui.line(panel, "●  AUXILIARES", 10, secondaryColor, bold = true).letterSpacing = .07f
            ui.line(panel, guide.secondary, 13, mutedColor).setPadding(0, 0, 0, ui.dp(4))
        }
    }

    private fun movement(exercise: Exercise, guide: ExerciseGuide) {
        val body = dialogBody()
        val art = ExerciseArt.phase(ui, guide)
        body.addView(art, LinearLayout.LayoutParams(-1, ui.dp(245)))
        val caption = ui.line(body, "Fase 1 de 2 · Início", 13, accentColor, bold = true)
        val phases = ui.row(body)
        lateinit var animationButton: Button
        var playing = true

        fun showFrame(phase: Int) {
            art.frame = phase
            caption.text = if (phase == 0) "Fase 1 de 2 · Início" else "Fase 2 de 2 · Final"
        }
        val playback = object : Runnable {
            override fun run() {
                if (!playing || !art.isAttachedToWindow) return
                showFrame(1 - art.frame)
                art.postDelayed(this, FRAME_HOLD_MS)
            }
        }
        fun play(enabled: Boolean) {
            playing = enabled
            art.removeCallbacks(playback)
            animationButton.text = if (enabled) "Pausar" else "▶  Reproduzir"
            if (enabled) art.postDelayed(playback, FRAME_HOLD_MS)
        }

        ui.button(phases, "Início", weighted = true) { play(false); showFrame(0) }
        ui.button(phases, "Final", weighted = true) { play(false); showFrame(1) }
        animationButton = ui.button(phases, "Pausar", weighted = true, primary = true) { play(!playing) }
        muscleLegend(body, guide)
        videoButton(body, exercise)

        val dialog = dialog(exercise.name, body)
        dialog.setOnDismissListener { art.removeCallbacks(playback) }
        dialog.show()
        play(true)
    }

    private fun equipment(guide: ExerciseGuide, apparatus: Apparatus) {
        val body = dialogBody()
        body.addView(ExerciseArt(ui, apparatus.imagePath), LinearLayout.LayoutParams(-1, ui.dp(245)))
        ui.line(body, "Modelo ilustrativo", 12, mutedColor)
        ui.section(body, "Como ajustar")
        ui.line(body, guide.setup, 15)
        dialog(apparatus.name, body).show()
    }

    fun videoButton(panel: LinearLayout, exercise: Exercise) {
        val video = ExerciseVideos[ExerciseGuides[exercise]]
        ui.button(panel, "▶  Ver execução no YouTube") { ui.video(exercise) }
        ui.line(panel, video.credit, 12, mutedColor)
    }

    private fun dialogBody() = LinearLayout(ui).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ui.dp(17), ui.dp(6), ui.dp(17), ui.dp(8))
    }

    private fun dialog(title: String, body: LinearLayout): AlertDialog =
        AlertDialog.Builder(ui).setTitle(title)
            .setView(ScrollView(ui).apply { addView(body) })
            .setNegativeButton("Fechar", null)
            .create()
            .apply {
                setOnShowListener {
                    window?.setBackgroundDrawable(ui.surface(panelColor, 22, outlineColor))
                    getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(accentColor)
                }
            }
}
