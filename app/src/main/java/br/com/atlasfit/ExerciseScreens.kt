package br.com.atlasfit

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.text.Normalizer
import java.time.LocalDate

internal class ExerciseScreens(private val ui: MainActivity) {
    private var librarySearch = ""
    private var filter = "Todos"

    fun library() {
        val page = ui.screen("library", "Biblioteca visual")
        ui.line(page, "Busque por exercício, músculo ou equipamento.", 14, mutedColor)
        val search = ui.field(page, "Buscar exercício", librarySearch, numeric = false).apply {
            background = ui.surface(panelColor, 14, outlineColor)
            setPadding(ui.dp(14), ui.dp(11), ui.dp(14), ui.dp(11))
        }
        val chips = LinearLayout(ui)
        page.addView(HorizontalScrollView(ui).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        })
        val list = LinearLayout(ui).apply { orientation = LinearLayout.VERTICAL }
        page.addView(list)

        fun render() {
            list.removeAllViews()
            val query = librarySearch.searchKey()
            val matches = ExerciseGuides.exercises.filter { exercise ->
                val guide = ExerciseGuides[exercise]
                (filter == "Todos" || filter == guide.equipment) &&
                    "${exercise.name} ${guide.target} ${guide.equipment}".searchKey().contains(query)
            }
            ui.line(list, "${matches.size} EXERCÍCIOS", 11, accentColor, bold = true).letterSpacing = .12f
            if (matches.isEmpty()) ui.line(list, "Nenhum exercício com esses filtros.", 15, mutedColor)
            matches.forEach { exercise -> tile(list, exercise, ExerciseGuides[exercise].equipment) { guide(exercise) } }
        }

        val controls = (listOf("Todos") + ExerciseGuides.exercises.map { ExerciseGuides[it].equipment }.distinct())
            .associateWith { name ->
                ui.button(chips, name, compact = true, primary = filter == name) {
                    filter = name
                    chips.children { button -> (button as TextView).apply {
                        val active = text == filter
                        setTextColor(if (active) 0xff071726.toInt() else accentColor)
                        background = ui.surface(if (active) accentColor else 0xff223348.toInt(), 13, outlineColor)
                        isSelected = active
                    } }
                    render()
                }.apply { isSelected = filter == name }
            }
        controls.getValue(filter).post { (chips.parent as HorizontalScrollView).scrollTo(controls.getValue(filter).left, 0) }
        search.watch { librarySearch = it; render() }
        render()
    }

    private fun guide(exercise: Exercise) {
        val page = ui.screen("guide", exercise.name, ::library)
        overview(page, exercise)
        instructions(page, exercise)
        ui.addLoadHistory(page, exercise.name)
    }

    fun workout(slot: Session, returnTo: () -> Unit) {
        val plan = slot.workout
        val page = ui.screen("workout", plan.title(slot.full), returnTo)
        val saved = ui.store.sessions().find(slot::sameSlot)
        val draft = ui.store.draft(slot)
        val items = plan.items(slot.full)
        val enabled = slot.date <= LocalDate.now()
        fun begin(): WorkoutDraft {
            if (draft != null) return draft
            return WorkoutDraft().also { ui.store.saveDraft(slot, it) }
        }
        fun open(index: Int) {
            if (enabled) exercise(slot, items, index, saved, begin(), returnTo)
            else {
                val preview = ui.screen("guide", items[index].name) { workout(slot, returnTo) }
                overview(preview, items[index])
                instructions(preview, items[index])
            }
        }
        val summary = ui.card(page, highlighted = true)
        ui.line(summary, when {
            draft != null -> "SESSÃO EM ANDAMENTO"
            saved != null -> "SESSÃO REGISTRADA"
            !enabled -> "TREINO PROGRAMADO"
            else -> "FICHA DE TREINO"
        }, 11, accentColor, bold = true).letterSpacing = .12f
        val count = items.count { item ->
            val old = saved?.logs?.find { it.name == item.name }
            if (draft?.exercises?.get(item.name) == null) old != null
            else draft.exercises[item.name]?.log(item, old) != null
        }
        ui.line(summary, "$count de ${items.size} exercícios registrados", 20, bold = true)
        val progress = ProgressBar(ui, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = items.size
            this.progress = count
            progressTintList = android.content.res.ColorStateList.valueOf(successColor)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(outlineColor)
        }
        summary.addView(progress, LinearLayout.LayoutParams(-1, ui.dp(8)).apply { bottomMargin = ui.dp(7) })
        ui.line(summary, slot.date.br(), 14, mutedColor)
        val note = when (plan) {
            Workout.CARDIO -> "20 min de esteira ou bicicleta + 5–10 min de mobilidade."
            Workout.PESOS -> "10–15 min de bicicleta + 15 min de pesos leves."
            else -> if (slot.full) null else "Versão expressa · até 30 min"
        }
        if (note != null) ui.line(summary, note, 13, mutedColor)
        if (enabled) {
            val first = items.indexOfFirst { item ->
                val old = saved?.logs?.find { it.name == item.name }
                val entry = draft?.exercises?.get(item.name)
                if (entry == null) old == null else entry.log(item, old) == null
            }.coerceAtLeast(0)
            ui.button(summary, when {
                draft != null -> "Continuar treino  ›"
                saved != null -> "Editar treino  ›"
                else -> "Iniciar treino  ›"
            }, primary = true) { open(first) }
        }
        ui.button(summary, "Alterar data", compact = true) { ui.pickDate(slot.date) { workout(slot.copy(date = it), returnTo) } }
        ui.section(page, "Exercícios da ficha")
        items.forEachIndexed { index, item ->
            val old = saved?.logs?.find { it.name == item.name }
            val entry = draft?.exercises?.get(item.name)
            val log = if (entry == null) old else entry.log(item, old)
            val status = if (log == null) "${index + 1} / ${items.size}" else "✓ ${ui.describe(log)}"
            tile(page, item, status) { open(index) }
        }
        if (enabled && draft != null) ui.button(page, "Finalizar e salvar treino", primary = true) {
            finish(slot, saved, draft, items)
        }
    }

    private fun exercise(slot: Session, items: List<Exercise>, index: Int, saved: Session?,
                         draft: WorkoutDraft, returnTo: () -> Unit) {
        val exercise = items[index]
        val old = saved?.logs?.find { it.name == exercise.name }
        val entry = draft.exercises.getOrPut(exercise.name) { ExerciseDraft.fromLog(old) }
        val page = ui.screen("exercise", exercise.name) { workout(slot, returnTo) }
        ui.line(page, "${slot.workout.title(slot.full)}   /   ${index + 1} de ${items.size}", 13, accentColor, bold = true)
        overview(page, exercise)
        val timer = ui.card(page, highlighted = true)
        val clocks = ui.row(timer)
        val elapsed = clockTile(clocks, "TEMPO DE TREINO")
        val rest = clockTile(clocks, "DESCANSO")
        var restToggle: android.widget.Button? = null
        fun tick() {
            val now = System.currentTimeMillis()
            elapsed.text = clockText(draft.elapsed(now))
            val seconds = draft.remaining(now)
            rest.text = when {
                seconds > 0 -> clockText(seconds)
                draft.restUntil > 0 -> "Fim"
                else -> clockText(draft.restSeconds.toLong())
            }
            val toggleTitle = if (seconds > 0) "Encerrar descanso" else "Iniciar descanso"
            restToggle?.takeIf { it.text != toggleTitle }?.text = toggleTitle
        }
        tick()
        elapsed.post(object : Runnable {
            override fun run() {
                if (!elapsed.isAttachedToWindow) return
                tick()
                elapsed.postDelayed(this, 250)
            }
        })
        val timerButtons = ui.row(timer)
        lateinit var interval: android.widget.Button
        interval = ui.button(timerButtons, "Intervalo ${draft.restSeconds}s", weighted = true) {
            val durations = intArrayOf(30, 45, 60, 90, 120, 180)
            AlertDialog.Builder(ui).setTitle("Intervalo entre séries")
                .setItems(durations.map { "$it segundos" }.toTypedArray()) { _, choice ->
                    val active = draft.remaining(System.currentTimeMillis()) > 0
                    draft.restSeconds = durations[choice]
                    if (active) draft.restUntil = System.currentTimeMillis() + draft.restSeconds * 1000L
                    ui.store.saveDraft(slot, draft)
                    interval.text = "Intervalo ${draft.restSeconds}s"
                    tick()
                }.show()
        }
        restToggle = ui.button(timerButtons, "Iniciar descanso", weighted = true) {
            val now = System.currentTimeMillis()
            draft.restUntil = if (draft.remaining(now) > 0) 0 else now + draft.restSeconds * 1000L
            ui.store.saveDraft(slot, draft)
            tick()
        }
        tick()

        val previous = ui.store.sessions().asSequence().filter { it.date <= slot.date && !it.sameSlot(slot) }
            .flatMap { it.logs.asSequence() }.firstOrNull { it.name == exercise.name }?.let(ui::describe)
            ?: ui.store.legacyLoad(slot.workout, exercise).takeIf(String::isNotBlank)
        if (previous != null) ui.line(page, "Último registro · $previous", 13, mutedColor)

        fun persist() {
            entry.changed = true
            ui.store.saveDraft(slot, draft)
        }
        if (exercise.cardio) {
            val inputs = ui.card(page)
            ui.section(inputs, "Registrar atividade")
            val minutes = ui.field(inputs, "Minutos", entry.minutes, decimals = false)
            minutes.watch { entry.minutes = it; persist() }
            lateinit var done: android.widget.Button
            done = ui.button(inputs, if (entry.cardioDone) "✓ Atividade concluída" else "Concluir atividade",
                primary = entry.cardioDone) {
                if (!entry.cardioDone && !validMinutes(entry.minutes)) {
                    minutes.error = "Informe entre 1 e 999 minutos."
                    return@button
                }
                entry.cardioDone = !entry.cardioDone
                done.text = if (entry.cardioDone) "✓ Atividade concluída" else "Concluir atividade"
                persist()
            }
        } else {
            ui.section(page, "Séries")
            ui.line(page, "Toque no ✓ ao concluir. O descanso começa automaticamente.", 13, mutedColor)
            val series = ui.card(page)
            val rows = LinearLayout(ui).apply { orientation = LinearLayout.VERTICAL }
            series.addView(rows)
            fun drawRows() {
                rows.removeAllViews()
                entry.rounds.forEachIndexed { position, set ->
                    val row = ui.row(rows).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, ui.dp(4), 0, ui.dp(5))
                    }
                    val number = position + 1
                    row.addView(TextView(ui).apply {
                        text = number.toString()
                        textSize = 13f
                        setTextColor(mutedColor)
                        gravity = Gravity.CENTER
                        background = ui.surface(0xff273545.toInt(), 12)
                    }, LinearLayout.LayoutParams(ui.dp(26), ui.dp(28)).apply { marginEnd = ui.dp(6) })
                    val reps = metricInput(row, "Repetições", set.reps, false, number)
                    val load = metricInput(row, "Carga (kg)", set.load.replace('.', ','), true, number)
                    reps.watch { set.reps = it; persist() }
                    load.watch { set.load = it; persist() }
                    lateinit var mark: android.widget.Button
                    fun updateMark() {
                        mark.text = if (set.done) "✓" else "○"
                        mark.contentDescription = if (set.done) "Desmarcar série $number" else "Concluir série $number"
                        mark.background = ui.surface(if (set.done) successColor else 0xff29394c.toInt(), 13, outlineColor)
                        mark.setTextColor(if (set.done) 0xff0d2018.toInt() else accentColor)
                    }
                    mark = ui.button(row, "○", compact = true) {
                        if (!set.done && !set.valid()) {
                            ui.message("Confira as repetições e a carga da série $number.")
                            return@button
                        }
                        set.done = !set.done
                        if (set.done) {
                            set.restSeconds = draft.restSeconds
                            draft.restUntil = System.currentTimeMillis() + draft.restSeconds * 1000L
                        }
                        persist()
                        updateMark()
                        tick()
                    }.apply {
                        minWidth = ui.dp(48)
                        minHeight = ui.dp(48)
                        textSize = 20f
                    }
                    updateMark()
                }
            }
            drawRows()
            val setActions = ui.row(series)
            ui.button(setActions, "+ Série", weighted = true) {
                if (entry.rounds.size >= 30) ui.message("Limite de 30 séries por exercício.")
                else {
                    entry.rounds.add(SetEntry())
                    persist()
                    drawRows()
                }
            }
            ui.button(setActions, "− Última", weighted = true) {
                val last = entry.rounds.lastOrNull()
                if (entry.rounds.size <= 1 || last == null) return@button
                fun remove() {
                    entry.rounds.removeAt(entry.rounds.lastIndex)
                    persist()
                    drawRows()
                }
                if (last.done || last.reps.isNotBlank() || last.load.isNotBlank()) {
                    AlertDialog.Builder(ui).setTitle("Remover a última série?")
                        .setNegativeButton("Cancelar", null).setPositiveButton("Remover") { _, _ -> remove() }.show()
                } else remove()
            }
        }
        val navigation = ui.row(page)
        if (index > 0) ui.button(navigation, "‹ Anterior", weighted = true) {
            exercise(slot, items, index - 1, saved, draft, returnTo)
        }
        if (index < items.lastIndex) ui.button(navigation, "Próximo  ›", weighted = true, primary = true) {
            exercise(slot, items, index + 1, saved, draft, returnTo)
        }
        ui.button(page, "Revisar ficha") { workout(slot, returnTo) }
        ui.button(page, "Finalizar e salvar", primary = index == items.lastIndex) {
            finish(slot, saved, draft, items)
        }
        instructions(page, exercise)
    }

    private fun finish(slot: Session, saved: Session?, draft: WorkoutDraft, items: List<Exercise>) {
        val invalid = items.firstOrNull { item ->
            val entry = draft.exercises[item.name] ?: return@firstOrNull false
            if (item.cardio) entry.cardioDone && !validMinutes(entry.minutes)
            else entry.rounds.any { it.done && !it.valid() }
        }
        if (invalid != null) {
            ui.message("Confira os valores marcados em ${invalid.name}.")
            return
        }
        val logs = items.mapNotNull { item ->
            val old = saved?.logs?.find { it.name == item.name }
            val entry = draft.exercises[item.name]
            if (entry == null) old else entry.log(item, old)
        }
        if (logs.isEmpty()) {
            ui.message("Conclua pelo menos uma série ou atividade antes de salvar.")
            return
        }
        AlertDialog.Builder(ui).setTitle("Salvar sessão?")
            .setMessage("${logs.size} exercícios registrados no dia ${slot.date.br()}.")
            .setNegativeButton("Continuar treino", null).setPositiveButton("Salvar") { _, _ ->
                val duration = if (saved == null) draft.elapsed(System.currentTimeMillis()) else saved.durationSeconds
                val session = slot.copy(logs = logs, durationSeconds = duration)
                if (ui.store.save(session)) ui.sessionDetails(session)
                else ui.message("Falha ao salvar. Tente novamente.")
            }.show()
    }

    private fun tile(page: LinearLayout, exercise: Exercise, label: String, action: () -> Unit) {
        val guide = ExerciseGuides[exercise]
        val tile = ui.card(page)
        tile.isFocusable = true
        tile.setOnClickListener { action() }
        tile.foreground = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x2269d3ff), null, ui.surface(Color.WHITE, 20)
        )
        val split = ui.row(tile).apply { gravity = Gravity.CENTER_VERTICAL }
        split.addView(ExerciseArt(ui, guide.motion, true), LinearLayout.LayoutParams(ui.dp(92), ui.dp(84)))
        val info = LinearLayout(ui).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(12), 0, ui.dp(4), 0)
        }
        split.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        ui.line(info, exercise.name, 16, bold = true)
        ui.line(info, "• ${guide.target}", 12, successColor)
        ui.line(info, label, 12, mutedColor)
        ui.line(split, "›", 24, accentColor)
    }

    private fun overview(page: LinearLayout, exercise: Exercise) {
        val guide = ExerciseGuides[exercise]
        val item = ui.card(page, highlighted = true)
        ui.line(item, guide.equipment.uppercase(ptBr) + "    •    " + guide.target, 13, accentColor, bold = true)
        val frames = ui.row(item)
        listOf(false to "POSIÇÃO INICIAL", true to "FASE DO MOVIMENTO").forEach { (end, title) ->
            val side = LinearLayout(ui).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            frames.addView(side, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = ui.dp(4) })
            side.addView(ExerciseArt(ui, guide.motion, end), LinearLayout.LayoutParams(-1, ui.dp(112)))
            ui.line(side, title, 10, mutedColor, bold = true)
        }
    }

    private fun instructions(page: LinearLayout, exercise: Exercise) {
        val guide = ExerciseGuides[exercise]
        val item = ui.card(page)
        ui.section(item, "Como executar")
        ui.line(item, "01   Preparação", 13, accentColor, bold = true)
        ui.line(item, guide.setup, 15)
        ui.line(item, "02   Movimento", 13, accentColor, bold = true)
        ui.line(item, guide.execution, 15)
        ui.button(item, "▶  Buscar vídeo no YouTube") { ui.video(exercise) }
    }

    private fun metricInput(row: LinearLayout, label: String, value: String, decimals: Boolean, number: Int): EditText {
        val cell = LinearLayout(ui).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = ui.surface(0xff255f8b.toInt(), 15, 0xff4784ae.toInt(), 0xff1a3450.toInt())
            setPadding(ui.dp(3), ui.dp(5), ui.dp(3), ui.dp(2))
        }
        cell.minimumHeight = ui.dp(69)
        row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = ui.dp(6) })
        val input = ui.field(cell, "—", value, decimals = decimals).apply {
            contentDescription = "$label da série $number"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = null
            setPadding(0, 0, 0, 0)
            minHeight = ui.dp(40)
            setSelectAllOnFocus(true)
        }
        ui.line(cell, label, 11, 0xffd0e6fa.toInt()).apply {
            setPadding(0, 0, 0, 0)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        return input
    }

    private fun clockTile(row: LinearLayout, label: String): TextView {
        val block = LinearLayout(ui).apply { orientation = LinearLayout.VERTICAL }
        row.addView(block, LinearLayout.LayoutParams(0, -2, 1f))
        ui.line(block, label, 10, mutedColor, bold = true).letterSpacing = .07f
        return ui.line(block, "", 25, if (label == "DESCANSO") successColor else Color.WHITE, bold = true)
    }

    private fun validMinutes(value: String) = value.isBlank() || value.toIntOrNull()?.let { it in 1..999 } == true

    private fun String.searchKey() = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(ptBr).trim()

    private fun LinearLayout.children(action: (View) -> Unit) {
        for (index in 0 until childCount) action(getChildAt(index))
    }

    private fun EditText.watch(change: (String) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(value: Editable?) { change(value?.toString().orEmpty()) }
        })
    }
}
