package br.com.atlasfit

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class BioScreen(private val ui: MainActivity) {
    private data class Metric(val key: String, val label: String, val unit: String, val max: Double? = null) {
        fun unitFor(entry: JSONObject): String = entry.optString(key + "Unit", unit)
        fun accepts(value: Double, recordedUnit: String): Boolean =
            value.isFinite() && value >= 0 && (recordedUnit != "%" || value <= 100) &&
                (max == null || value <= max)
    }

    private val metrics = listOf(
        Metric("weight", "Peso", "kg"), Metric("fat", "Gordura corporal", "%", 100.0),
        Metric("fatKg", "Massa adiposa", "kg"), Metric("leanKg", "Massa livre de gordura", "kg"),
        Metric("muscle", "Massa muscular", "%"), Metric("water", "Água corporal", "%"),
        Metric("waist", "Cintura", "cm"), Metric("bmi", "IMC", ""),
        Metric("visceralLevel", "Nível de gordura visceral", ""), Metric("bmrKcal", "Metabolismo basal", "kcal")
    )

    fun show() {
        val page = ui.screen("bio", "Bioimpedância")
        ui.line(page, "Compare as avaliações por data. A leitura pode variar entre equipamentos e com a hidratação.", 15, mutedColor)
        ui.button(page, "Registrar avaliação") { form(null) }
        ui.button(page, "Importar histórico de avaliações") { openFile() }
        val entries = ui.store.assessments()
        if (entries.isEmpty()) {
            ui.line(page, "Nenhuma avaliação registrada.", 16, mutedColor)
            return
        }
        ui.section(page, "Evolução por indicador")
        for (metric in metrics.take(3)) {
            val points = entries.mapNotNull { entry ->
                val value = entry.number(metric.key) ?: return@mapNotNull null
                entry.optString("date").localDate()?.let { it to value }
            }
            if (points.isEmpty()) continue
            val item = ui.card(page)
            ui.line(item, metric.label, 16, bold = true)
            val first = points.first()
            val last = points.last()
            ui.line(item, withUnit(last.second, metric.unit), 24, accentColor, bold = true)
            if (first.first != last.first) {
                val change = last.second - first.second
                val delta = (if (change > 0) "+" else "") + change.br()
                val unit = if (metric.unit == "%") "p.p." else metric.unit
                ui.line(item, "$delta $unit · ${first.first.br()} a ${last.first.br()}", 14, mutedColor)
                item.addView(TrendView(ui, points), LinearLayout.LayoutParams(-1, ui.dp(96)))
            } else ui.line(item, last.first.br(), 14, mutedColor)
        }
        ui.section(page, "Avaliações · ${entries.size}")
        for (entry in entries.asReversed()) {
            val item = ui.card(page)
            ui.line(item, entry.optString("date").localDate()?.br().orEmpty(), 19, accentColor, bold = true)
            val source = entry.optString("source")
            if (source.isNotBlank()) ui.line(item, source, 14, mutedColor)
            for (metric in metrics) {
                val value = entry.number(metric.key) ?: continue
                ui.line(item, "${metric.label} · ${withUnit(value, metric.unitFor(entry))}", 14)
            }
            ui.button(item, "Editar avaliação") { form(entry) }
        }
    }

    private fun form(previous: JSONObject?) {
        val page = ui.screen("bioForm", if (previous == null) "Nova avaliação" else "Editar avaliação", ::show)
        var date = previous?.optString("date")?.localDate() ?: LocalDate.now()
        lateinit var dateButton: android.widget.Button
        dateButton = ui.button(page, "Data · ${date.br()}") {
            ui.pickDate(date) { chosen ->
                date = chosen
                dateButton.text = "Data · ${date.br()}"
            }
        }
        val source = ui.field(page, "Equipamento ou origem (opcional)", previous?.optString("source").orEmpty(), numeric = false)
        ui.line(page, "Preencha os indicadores disponíveis no laudo.", 14, mutedColor)
        val units = mutableMapOf<String, String>()
        val fields = metrics.associateWith { metric ->
            val unit = previous?.let(metric::unitFor) ?: metric.unit
            units[metric.key] = unit
            val field = ui.field(page, caption(metric, unit), previous?.number(metric.key)?.br().orEmpty())
            val options = when (metric.key) {
                "muscle" -> listOf("%", "kg", "kg esquelética")
                "water" -> listOf("%", "kg")
                else -> emptyList()
            }
            if (options.isNotEmpty()) {
                val group = ui.row(page)
                options.forEach { choice ->
                    ui.button(group, choice, weighted = true) {
                        units[metric.key] = choice
                        field.hint = caption(metric, choice)
                        field.contentDescription = field.hint
                    }
                }
            }
            field
        }
        ui.button(page, "Salvar avaliação") {
            if (date > LocalDate.now()) {
                ui.message("Selecione uma data de avaliação até hoje.")
                return@button
            }
            val assessment = JSONObject().put("date", date.toString())
                .put("source", source.text.toString().trim())
            for ((metric, field) in fields) {
                if (field.text.isBlank()) continue
                val value = field.text.toString().decimal()
                val unit = units.getValue(metric.key)
                if (value == null || !metric.accepts(value, unit) ||
                    (metric.key == "weight" && value == 0.0)) {
                    field.error = "Valor inválido"
                    return@button
                }
                assessment.put(metric.key, value)
                if (unit != metric.unit) assessment.put(metric.key + "Unit", unit)
            }
            if (metrics.none { assessment.has(it.key) }) {
                ui.message("Informe pelo menos um indicador.")
                return@button
            }
            if (ui.store.saveAssessment(assessment, previous)) {
                ui.message("Avaliação salva")
                show()
            } else ui.message("Falha ao salvar. Tente novamente.")
        }
    }

    private fun caption(metric: Metric, unit: String): String =
        if (unit.isEmpty()) metric.label else "${metric.label} ($unit)"

    private fun withUnit(value: Double, unit: String): String =
        when (unit) {
            "" -> value.br()
            "%" -> "${value.br()}%"
            else -> "${value.br()} $unit"
        }

    @Suppress("DEPRECATION")
    private fun openFile() {
        try {
            ui.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }, IMPORT_REQUEST)
        } catch (_: ActivityNotFoundException) {
            ui.message("Seletor de arquivos indisponível.")
        }
    }

    fun importFile(uri: android.net.Uri) {
        val incoming = runCatching {
            val content = ui.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(4096)
                val result = StringBuilder()
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    require(result.length + count <= 256_000)
                    result.append(buffer, 0, count)
                }
                result.toString()
            } ?: error("Arquivo indisponível")
            val rows = JSONArray(content)
            require(rows.length() in 1..500)
            (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                val date = requireNotNull(row.optString("date").localDate())
                require(date <= LocalDate.now())
                val entry = JSONObject().put("date", date.toString())
                    .put("source", row.optString("source").trim().take(120))
                for (metric in metrics) {
                    if (row.isNull(metric.key)) continue
                    val value = requireNotNull(row.number(metric.key))
                    val unit = metric.unitFor(row)
                    val acceptedUnits = when (metric.key) {
                        "muscle" -> listOf("%", "kg", "kg esquelética")
                        "water" -> listOf("%", "kg")
                        else -> listOf(metric.unit)
                    }
                    require(unit in acceptedUnits && metric.accepts(value, unit))
                    require(metric.key != "weight" || value > 0)
                    entry.put(metric.key, value)
                    if (unit != metric.unit) entry.put(metric.key + "Unit", unit)
                }
                require(metrics.any { entry.has(it.key) })
                entry
            }
        }.getOrElse {
            ui.message("Arquivo inválido. Selecione um histórico JSON de avaliações Atlas Fit.")
            return
        }
        when (val added = ui.store.importAssessments(incoming)) {
            null -> ui.message("Falha ao importar. Tente novamente.")
            0 -> ui.message("As avaliações já estão no histórico.")
            else -> ui.message("$added avaliações importadas")
        }
        show()
    }

    companion object {
        const val IMPORT_REQUEST = 31
    }
}

internal class TrendView(
    private val ui: MainActivity,
    private val points: List<Pair<LocalDate, Double>>
) : View(ui) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = ui.dp(9).toFloat()
        val plotWidth = (width - pad * 2).coerceAtLeast(1f)
        val plotHeight = (height - pad * 2).coerceAtLeast(1f)
        val min = points.minOf { it.second }
        val max = points.maxOf { it.second }
        val period = ChronoUnit.DAYS.between(points.first().first, points.last().first).coerceAtLeast(1)
        paint.strokeWidth = ui.dp(1).toFloat()
        paint.color = mutedColor
        paint.alpha = 55
        repeat(3) { i ->
            val y = pad + plotHeight * i / 2
            canvas.drawLine(pad, y, pad + plotWidth, y, paint)
        }
        paint.color = accentColor
        paint.strokeWidth = ui.dp(2).toFloat()
        val coordinates = points.map { (date, value) ->
            val x = pad + plotWidth * (ChronoUnit.DAYS.between(points.first().first, date).toFloat() / period)
            val y = if (min == max) pad + plotHeight / 2 else
                pad + plotHeight * (1 - (value - min) / (max - min)).toFloat()
            x to y
        }
        coordinates.zipWithNext().forEach { (start, end) ->
            canvas.drawLine(start.first, start.second, end.first, end.second, paint)
        }
        coordinates.forEach { (x, y) -> canvas.drawCircle(x, y, ui.dp(4).toFloat(), paint) }
    }
}
