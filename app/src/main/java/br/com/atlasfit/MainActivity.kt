package br.com.atlasfit

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ptBr = Locale.forLanguageTag("pt-BR")
internal val backgroundColor = Color.rgb(12, 17, 28)
internal val panelColor = Color.rgb(23, 32, 49)
internal val accentColor = Color.rgb(121, 213, 253)
internal val mutedColor = Color.rgb(181, 195, 217)
internal fun LocalDate.br(): String = format(DateTimeFormatter.ofPattern("dd/MM/uuuu", ptBr))
internal fun Double.br(): String = java.text.NumberFormat.getNumberInstance(ptBr).apply {
    isGroupingUsed = false
    maximumFractionDigits = 8
}.format(this)

class MainActivity : Activity() {
    internal val store by lazy { TrainingStore(getSharedPreferences("atlas_fit", MODE_PRIVATE)) }
    private val bio by lazy { BioScreen(this) }
    private lateinit var content: LinearLayout
    private var back: (() -> Unit)? = null
    private var reportDays = 30
    private var currentPage = "home"
    private var calendarWeek: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
        reportDays = savedInstanceState?.getInt("reportDays", 30) ?: 30
        calendarWeek = savedInstanceState?.getString("week")?.localDate()
        when (savedInstanceState?.getString("page")) {
            "calendar" -> calendar()
            "report" -> report()
            "bio" -> bio.show()
            else -> home()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("page", currentPage)
        outState.putInt("reportDays", reportDays)
        outState.putString("week", calendarWeek?.toString())
    }

    @Deprecated("Handles back on API 26–32 and legacy back dispatch on newer Android versions.")
    override fun onBackPressed() {
        back?.invoke() ?: super.onBackPressed()
    }

    internal fun screen(page: String, title: String, onBack: (() -> Unit)? = ::home): LinearLayout {
        currentPage = page
        back = onBack
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            if (Build.VERSION.SDK_INT < 30) fitsSystemWindows = true
        }
        if (Build.VERSION.SDK_INT >= 30) {
            shell.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val keyboard = insets.getInsets(WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
                WindowInsets.CONSUMED
            }
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(20))
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            isFocusableInTouchMode = true
        }
        shell.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        val navigation = LinearLayout(this).apply { setBackgroundColor(panelColor) }
        val tabs = listOf(
            Triple("home", "Treinos", ::home),
            Triple("calendar", "Agenda", ::calendar),
            Triple("report", "Relatório", ::report),
            Triple("bio", "Evolução", bio::show)
        )
        tabs.forEach { (key, name, action) ->
            navigation.addView(TextView(this).apply {
                text = name
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                minHeight = dp(56)
                setTextColor(if (page == key) accentColor else mutedColor)
                typeface = Typeface.DEFAULT_BOLD
                isSelected = page == key
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, -1, 1f))
        }
        shell.addView(navigation, LinearLayout.LayoutParams(-1, dp(56)))
        setContentView(shell)
        line(content, "ATLAS FIT  /  TREINO PESSOAL", 12, accentColor, bold = true)
        line(content, title, 30, bold = true)
        content.requestFocus()
        return content
    }

    private fun home() {
        val page = screen("home", "Seu treino", null)
        val date = LocalDate.now()
        val today = TrainingPlan.planned(date)
        card(page).also {
            line(it, date.br(), 13, accentColor)
            line(it, today?.title(TrainingPlan.complete(date) == today) ?: "Sem sessão programada", 23, bold = true)
            line(it, "Ficha completa da semana · ${TrainingPlan.complete(date).name}", 15, mutedColor)
            if (today != null) button(it, "Abrir treino de hoje") {
                workout(today, TrainingPlan.complete(date) == today, date)
            }
        }
        section(page, "Academia")
        for (plan in TrainingPlan.gym) {
            val item = card(page)
            line(item, plan.title, 20, bold = true)
            line(item, when (plan) {
                Workout.A -> "Peito, ombros e tríceps"
                Workout.B -> "Pernas e panturrilhas"
                else -> "Costas, trapézio e bíceps"
            }, 14, mutedColor)
            val row = row(item)
            button(row, "Completa", weighted = true) { workout(plan, true, date) }
            button(row, "Expressa", weighted = true) { workout(plan, false, date) }
        }
        section(page, "Condomínio")
        val condo = card(page)
        button(condo, "Cardio e mobilidade") { workout(Workout.CARDIO, false, date) }
        button(condo, "Cardio e pesos") { workout(Workout.PESOS, false, date) }
    }

    private fun calendar() {
        val page = screen("calendar", "Calendário")
        val today = LocalDate.now()
        val start = calendarWeek ?: maxOf(TrainingPlan.monday(today), TrainingPlan.start)
        val full = TrainingPlan.complete(start)
        val picker = row(page)
        button(picker, "Anterior", weighted = true) {
            calendarWeek = start.minusWeeks(1)
            calendar()
        }
        button(picker, "Próxima", weighted = true) {
            calendarWeek = start.plusWeeks(1)
            calendar()
        }
        line(page, "${start.br()} a ${start.plusDays(6).br()}", 19, bold = true)
        line(page, "Rodízio A → B → C · 1 completa, 2 expressas e 2 no condomínio", 14, mutedColor)
        val sessions = store.sessions().groupBy(Session::date)
        repeat(7) { offset ->
            val date = start.plusDays(offset.toLong())
            val plan = TrainingPlan.planned(date)
            val item = card(page)
            val day = date.format(DateTimeFormatter.ofPattern("EEEE", ptBr))
                .replaceFirstChar { it.titlecase(ptBr) }
            line(item, "$day · ${date.br()}", 14, accentColor, bold = true)
            line(item, plan?.title(plan == full) ?: "Sem sessão programada", 19, bold = true)
            val logged = sessions[date].orEmpty()
            if (logged.isNotEmpty()) line(item, logged.joinToString(" · ") { it.workout.title(it.full) } +
                " ✓", 14, mutedColor)
            if (plan != null) button(item, if (date > today) "Ver ficha" else "Abrir ficha") {
                workout(plan, plan == full, date, ::calendar)
            }
        }
    }

    private data class ExerciseFields(
        val exercise: Exercise,
        val done: CheckBox,
        val load: EditText?,
        val sets: EditText?,
        val reps: EditText?,
        val minutes: EditText?
    ) {
        fun read(): ExerciseLog? {
            if (!done.isChecked) return null
            return ExerciseLog(exercise.name, load?.text?.toString()?.decimal(),
                sets?.text?.toString()?.toIntOrNull(), reps?.text?.toString()?.toIntOrNull(),
                minutes?.text?.toString()?.toIntOrNull())
        }

        fun invalid(): Boolean {
            if (!done.isChecked) return false
            if (load != null && load.text.isNotBlank() && load.text.toString().decimal()?.let { it >= 0 } != true) return true
            return listOfNotNull(sets, reps, minutes).any {
                it.text.isNotBlank() && it.text.toString().toIntOrNull()?.let { n -> n > 0 } != true
            }
        }
    }

    private fun workout(plan: Workout, full: Boolean, date: LocalDate, returnTo: () -> Unit = ::home) {
        val page = screen("workout", plan.title(full), returnTo)
        val sessions = store.sessions()
        val slot = Session(date, plan, full)
        val existing = sessions.find(slot::sameSlot)
        val previous = sessions.filter { it.date <= date && !slot.sameSlot(it) && it.workout == plan }
            .flatMap(Session::logs).groupBy(ExerciseLog::name)
        line(page, when (plan) {
            Workout.CARDIO -> "20 min de esteira ou bicicleta + 5–10 min de mobilidade."
            Workout.PESOS -> "10–15 min de bicicleta + 15 min de pesos leves, conforme sua recuperação."
            else -> if (full) "Ficha original completa." else "Seleção da ficha original para uma sessão de até 30 min."
        }, 15, mutedColor)
        button(page, "Data do treino · ${date.br()}") {
            pickDate(date) { workout(plan, full, it, returnTo) }
        }
        if (date > LocalDate.now()) line(page, "Prévia de treino. O registro fica disponível na data da sessão.", 14, accentColor)
        else line(page, "Marque os exercícios realizados. Os detalhes são opcionais.", 14, mutedColor)
        val fields = plan.items(full).mapIndexed { index, exercise ->
            val item = card(page)
            val saved = existing?.logs?.find { it.name == exercise.name }
            val done = CheckBox(this).apply {
                text = "${index + 1}. ${exercise.name}"
                textSize = 17f
                setTextColor(Color.WHITE)
                buttonTintList = ColorStateList.valueOf(accentColor)
                isChecked = saved != null
                isEnabled = date <= LocalDate.now()
            }
            item.addView(done)
            val last = previous[exercise.name]?.firstOrNull()?.let(::describe)
                ?: store.legacyLoad(plan, exercise).takeIf(String::isNotBlank)
            if (last != null) line(item, "Registro anterior · $last", 13, mutedColor)
            var load: EditText? = null
            var sets: EditText? = null
            var reps: EditText? = null
            var minutes: EditText? = null
            if (date <= LocalDate.now()) {
                if (exercise.cardio) {
                    minutes = field(item, "Minutos", saved?.minutes?.toString().orEmpty(), decimals = false)
                } else {
                    val inputs = row(item)
                    load = field(inputs, "Carga kg", saved?.load?.br().orEmpty(), weighted = true)
                    sets = field(inputs, "Séries", saved?.sets?.toString().orEmpty(), decimals = false, weighted = true)
                    reps = field(inputs, "Reps/série", saved?.reps?.toString().orEmpty(), decimals = false, weighted = true)
                }
            }
            if (plan in TrainingPlan.gym) button(item, "Buscar execução no YouTube") { video(exercise) }
            ExerciseFields(exercise, done, load, sets, reps, minutes)
        }
        if (date <= LocalDate.now()) button(page, if (existing == null) "Finalizar e salvar treino" else "Atualizar treino") {
            if (fields.any(ExerciseFields::invalid)) {
                message("Confira a carga, as séries, as repetições e os minutos informados.")
                return@button
            }
            val logs = fields.mapNotNull(ExerciseFields::read)
            if (logs.isEmpty()) {
                message("Marque pelo menos um exercício realizado.")
                return@button
            }
            val updated = slot.copy(logs = logs)
            if (store.save(updated)) {
                message("Treino salvo")
                sessionDetails(updated)
            } else message("Falha ao salvar. Tente novamente.")
        }
    }

    private fun report() {
        val page = screen("report", "Relatório de treinos")
        val tabs = row(page)
        listOf(7 to "7 dias", 30 to "30 dias", 0 to "Todos").forEach { (days, label) ->
            button(tabs, if (reportDays == days) "✓ $label" else label, weighted = true) {
                reportDays = days
                report()
            }
        }
        val today = LocalDate.now()
        val since = today.minusDays((reportDays - 1).toLong())
        val sessions = store.sessions().filter { reportDays == 0 || it.date in since..today }
        val dates = sessions.map(Session::date).distinct()
        val totals = card(page)
        line(totals, "${dates.size} dias ativos · ${sessions.size} sessões", 22, bold = true)
        val complete = sessions.count { it.full && it.workout in TrainingPlan.gym }
        val express = sessions.count { !it.full && it.workout in TrainingPlan.gym }
        val condo = sessions.size - complete - express
        line(totals, "$complete completas · $express expressas · $condo condomínio", 14, mutedColor)
        val countBySheet = TrainingPlan.gym.joinToString(" · ") { plan ->
            "${plan.name}: ${sessions.count { it.workout == plan }}"
        }
        line(totals, countBySheet, 14, mutedColor)
        if (sessions.isEmpty()) line(page, "Nenhum treino registrado no período.", 16, mutedColor)
        else section(page, "Dias praticados")
        sessions.groupBy(Session::date).forEach { (date, day) ->
            val item = card(page)
            line(item, date.br(), 17, accentColor, bold = true)
            day.forEach { session ->
                val count = if (session.logs.isEmpty()) "presença" else "${session.logs.size} exercícios"
                button(item, "${session.workout.title(session.full)} · $count") { sessionDetails(session) }
            }
        }
        val logs = sessions.flatMap(Session::logs)
        if (logs.isNotEmpty()) section(page, "Exercícios no período")
        logs.groupBy(ExerciseLog::name).toSortedMap(java.text.Collator.getInstance(ptBr))
            .forEach { (name, entries) ->
                val item = card(page)
                line(item, name, 17, bold = true)
                val totalsByExercise = mutableListOf("${entries.size} registros")
                entries.mapNotNull(ExerciseLog::load).maxOrNull()?.let { totalsByExercise.add("Maior carga ${it.br()} kg") }
                entries.mapNotNull(ExerciseLog::sets).takeIf { it.isNotEmpty() }
                    ?.sumOf(Int::toLong)?.let { totalsByExercise.add("$it séries") }
                entries.mapNotNull(ExerciseLog::minutes).takeIf { it.isNotEmpty() }
                    ?.sumOf(Int::toLong)?.let { totalsByExercise.add("$it min") }
                line(item, totalsByExercise.joinToString(" · "), 14, mutedColor)
            }
        if (reportDays == 0) {
            val legacy = TrainingPlan.gym.flatMap { plan ->
                plan.exercises.mapNotNull { exercise ->
                    store.legacyLoad(plan, exercise).takeIf(String::isNotBlank)?.let {
                        "${plan.name} · ${exercise.name} · $it"
                    }
                }
            }
            if (legacy.isNotEmpty()) {
                section(page, "Cargas anteriores sem data")
                val item = card(page)
                legacy.forEach { line(item, it, 14, mutedColor) }
            }
        }
    }

    private fun sessionDetails(session: Session) {
        val page = screen("session", session.workout.title(session.full), ::report)
        line(page, session.date.br(), 16, accentColor)
        if (session.logs.isEmpty()) line(page, "Presença registrada. Detalhes de exercícios ainda não preenchidos.", 16, mutedColor)
        session.logs.forEach {
            val item = card(page)
            line(item, it.name, 17, bold = true)
            line(item, describe(it), 14, mutedColor)
        }
        button(page, "Editar treino") { workout(session.workout, session.full, session.date) { sessionDetails(session) } }
        button(page, "Voltar ao relatório", action = ::report)
    }

    private fun describe(log: ExerciseLog): String = listOfNotNull(
        log.load?.let { "${it.br()} kg" }, log.sets?.let { "$it séries" },
        log.reps?.let { "$it reps/série" }, log.minutes?.let { "$it min" }
    ).joinToString(" · ").ifEmpty { "Realizado" }

    private fun video(exercise: Exercise) {
        val query = Uri.encode("Leandro Twin execução ${exercise.name}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query")))
        } catch (_: ActivityNotFoundException) {
            message("Abra o YouTube e pesquise por ${exercise.name}.")
        }
    }

    internal fun pickDate(date: LocalDate, selected: (LocalDate) -> Unit) {
        DatePickerDialog(this, { _, year, month, day ->
            selected(LocalDate.of(year, month + 1, day))
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    internal fun dp(value: Int): Int = (resources.displayMetrics.density * value + 0.5f).toInt()
    internal fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    internal fun section(parent: LinearLayout, text: String) = line(parent, text, 20, bold = true)

    internal fun line(parent: LinearLayout, value: String, size: Int = 16, color: Int = Color.WHITE, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, dp(5))
        })
    }

    internal fun card(parent: LinearLayout): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply {
            setColor(panelColor)
            cornerRadius = dp(15).toFloat()
        }
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(5)
            bottomMargin = dp(8)
        })
    }

    internal fun row(parent: LinearLayout): LinearLayout = LinearLayout(this).also { parent.addView(it) }

    internal fun button(parent: LinearLayout, title: String, weighted: Boolean = false, action: () -> Unit): Button {
        val width = if (weighted) 0 else -1
        val button = Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 14f
            minHeight = dp(48)
            setTextColor(accentColor)
            backgroundTintList = ColorStateList.valueOf(Color.rgb(34, 49, 72))
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(width, -2, if (weighted) 1f else 0f).apply {
            if (weighted) marginEnd = dp(4)
        })
        return button
    }

    internal fun field(parent: LinearLayout, hint: String, value: String = "", decimals: Boolean = true,
                       numeric: Boolean = true, weighted: Boolean = false): EditText {
        val field = EditText(this).apply {
            this.hint = hint
            contentDescription = hint
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(mutedColor)
            val type = if (!numeric) InputType.TYPE_CLASS_TEXT else InputType.TYPE_CLASS_NUMBER or
                (if (decimals) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0)
            inputType = type
            if (numeric && decimals) {
                keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789,.")
                setRawInputType(type)
            }
            setText(value)
        }
        parent.addView(field, LinearLayout.LayoutParams(if (weighted) 0 else -1, -2, if (weighted) 1f else 0f))
        return field
    }

    @Deprecated("Receives files from the Android document picker.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BioScreen.IMPORT_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let(bio::importFile)
        }
    }
}
