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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ptBr = Locale.forLanguageTag("pt-BR")
internal val backgroundColor = Color.rgb(11, 15, 22)
internal val panelColor = Color.rgb(23, 31, 43)
internal val accentColor = Color.rgb(105, 211, 255)
internal val mutedColor = Color.rgb(171, 188, 208)
internal val successColor = Color.rgb(103, 230, 173)
internal val outlineColor = Color.rgb(50, 67, 86)
internal fun LocalDate.br(): String = format(DateTimeFormatter.ofPattern("dd/MM/uuuu", ptBr))
internal fun Double.br(): String = java.text.NumberFormat.getNumberInstance(ptBr).apply {
    isGroupingUsed = false
    maximumFractionDigits = 8
}.format(this)

class MainActivity : Activity() {
    internal val store by lazy { TrainingStore(getSharedPreferences("atlas_fit", MODE_PRIVATE)) }
    private val bio by lazy { BioScreen(this) }
    private val training by lazy { ExerciseScreens(this) }
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
            "library" -> training.library()
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
        val navigation = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = surface(panelColor, 24, outlineColor)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val selected = when (page) {
            "workout", "exercise" -> "home"
            "guide" -> "library"
            "session" -> "report"
            "bioForm" -> "bio"
            else -> page
        }
        val tabs = listOf(
            Triple("home", "Treino", ::home),
            Triple("library", "Exercícios", training::library),
            Triple("calendar", "Agenda", ::calendar),
            Triple("report", "Histórico", ::report),
            Triple("bio", "Evolução", bio::show)
        )
        tabs.forEach { (key, name, action) ->
            val active = key == selected
            val color = if (active) accentColor else mutedColor
            navigation.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                if (active) background = surface(0xff243e52.toInt(), 19)
                isSelected = active
                contentDescription = name
                isFocusable = true
                addView(TabIcon(this@MainActivity, key, color), LinearLayout.LayoutParams(dp(22), dp(22)))
                addView(TextView(this@MainActivity).apply {
                    text = name
                    setTextColor(color)
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, dp(51), 1f))
        }
        shell.addView(navigation, LinearLayout.LayoutParams(-1, dp(60)).apply {
            setMargins(dp(12), dp(4), dp(12), dp(6))
        })
        setContentView(shell)
        val mast = row(content).apply { gravity = Gravity.CENTER_VERTICAL }
        if (page in setOf("workout", "exercise", "guide", "session", "bioForm") && onBack != null) {
            button(mast, "Voltar", compact = true, action = onBack).contentDescription = "Voltar"
        }
        line(mast, "  ATLAS  /  FIT", 13, accentColor, bold = true).letterSpacing = .19f
        line(content, title, if (page in setOf("exercise", "guide")) 27 else 32, bold = true)
        content.requestFocus()
        return content
    }

    private fun home() {
        val page = screen("home", "Seu treino", null)
        val date = LocalDate.now()
        val today = TrainingPlan.planned(date)
        val complete = TrainingPlan.complete(date)
        val heroPlan = today ?: complete
        val full = heroPlan == complete
        card(page, highlighted = true).also { hero ->
            val split = row(hero).apply { gravity = Gravity.CENTER_VERTICAL }
            val intro = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            split.addView(intro, LinearLayout.LayoutParams(0, -2, 1f))
            line(intro, if (today == null) "FICHA DA SEMANA" else "PROGRAMADO PARA HOJE", 11, accentColor, bold = true).letterSpacing = .08f
            line(intro, heroPlan.title(full), 24, bold = true)
            line(intro, "${heroPlan.items(full).size} exercícios  ·  ${date.br()}", 13, mutedColor)
            val guide = ExerciseGuides[heroPlan.exercises.first()]
            split.addView(ExerciseArt.phase(this, guide, end = true), LinearLayout.LayoutParams(dp(106), dp(106)))
            button(hero, "Abrir ficha  ›", primary = true) { workout(heroPlan, full, date) }
        }
        val shortcut = row(page)
        button(shortcut, "Biblioteca visual", weighted = true, action = training::library)
        button(shortcut, "Agenda semanal", weighted = true, action = ::calendar)
        section(page, "Suas fichas")
        for (plan in TrainingPlan.gym) {
            val item = card(page)
            val header = row(item).apply { gravity = Gravity.CENTER_VERTICAL }
            TextView(this).apply {
                text = plan.name
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
                gravity = Gravity.CENTER
                background = surface(0xff254259.toInt(), 13)
                header.addView(this, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(12) })
            }
            val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
            line(titles, plan.title, 20, bold = true)
            line(titles, when (plan) {
                Workout.A -> "Peito, ombros e tríceps"
                Workout.B -> "Pernas e panturrilhas"
                else -> "Costas, trapézio e bíceps"
            }, 13, mutedColor)
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

    private fun workout(plan: Workout, full: Boolean, date: LocalDate, returnTo: () -> Unit = ::home) {
        training.workout(Session(date, plan, full), returnTo)
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
                if (entries.any { it.load != null }) button(item, "Evolução de carga") {
                    val detail = screen("session", name, ::report)
                    addLoadHistory(detail, name)
                }
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

    internal fun sessionDetails(session: Session) {
        val page = screen("session", session.workout.title(session.full), ::report)
        line(page, session.date.br(), 16, accentColor)
        session.durationSeconds?.let { line(page, "${clockText(it)} de treino", 14, mutedColor) }
        if (session.logs.isEmpty()) line(page, "Presença registrada. Detalhes de exercícios ainda não preenchidos.", 16, mutedColor)
        session.logs.forEach {
            val item = card(page)
            line(item, it.name, 17, bold = true)
            line(item, describe(it), 14, mutedColor)
            it.rounds.forEach { round ->
                val values = listOfNotNull(round.reps?.let { reps -> "$reps reps" },
                    round.load?.let { load -> "${load.br()} kg" },
                    round.restSeconds?.let { seconds -> "${seconds}s descanso" }).joinToString(" · ")
                line(item, "Série ${round.number}  ·  ${values.ifEmpty { "Concluída" }}", 14, accentColor)
            }
        }
        button(page, "Editar treino") { workout(session.workout, session.full, session.date) { sessionDetails(session) } }
        button(page, "Voltar ao relatório", action = ::report)
    }

    internal fun addLoadHistory(page: LinearLayout, name: String) {
        val points = store.sessions().mapNotNull { session ->
            session.logs.firstOrNull { it.name == name }?.load?.let { session.date to it }
        }.groupBy({ it.first }, { it.second }).map { (date, loads) -> date to loads.max() }.sortedBy { it.first }
        if (points.isEmpty()) return
        val panel = card(page)
        section(panel, "Evolução de carga")
        line(panel, "Maior carga registrada por dia", 13, mutedColor)
        line(panel, "${points.last().second.br()} kg", 23, accentColor, bold = true)
        if (points.size > 1) panel.addView(TrendView(this, points), LinearLayout.LayoutParams(-1, dp(100)))
        points.asReversed().take(15).forEach { (date, load) ->
            line(panel, "${date.br()}  ·  ${load.br()} kg", 14, mutedColor)
        }
    }

    internal fun describe(log: ExerciseLog): String = listOfNotNull(
        log.load?.let { "${it.br()} kg" }, log.sets?.let { "$it séries" },
        log.reps?.let { "$it reps/série" }, log.minutes?.let { "$it min" }
    ).joinToString(" · ").ifEmpty { "Realizado" }

    internal fun video(exercise: Exercise) {
        val demonstration = ExerciseVideos[ExerciseGuides[exercise]]
        val url = Uri.Builder().scheme("https").authority("www.youtube.com")
            .path("watch").appendQueryParameter("v", demonstration.id).build()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
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

    internal fun line(parent: LinearLayout, value: String, size: Int = 16, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(4), 0, dp(5))
            parent.addView(this)
        }

    internal fun surface(color: Int, radius: Int, stroke: Int? = null, gradientEnd: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            if (gradientEnd == null) setColor(color)
            else {
                orientation = GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(color, gradientEnd)
            }
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    internal fun card(parent: LinearLayout, highlighted: Boolean = false): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = if (highlighted) surface(0xff18334a.toInt(), 22, 0xff356182.toInt(), 0xff182230.toInt())
            else surface(panelColor, 20, outlineColor)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(5)
            bottomMargin = dp(7)
        })
    }

    internal fun row(parent: LinearLayout): LinearLayout = LinearLayout(this).also { parent.addView(it) }

    internal fun button(parent: LinearLayout, title: String, weighted: Boolean = false, primary: Boolean = false,
                        compact: Boolean = false, action: () -> Unit): Button {
        val width = if (weighted) 0 else if (compact) -2 else -1
        val button = Button(this).apply {
            text = title
            isAllCaps = false
            textSize = if (compact) 13f else 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            minimumWidth = 0
            minWidth = 0
            minHeight = dp(if (compact) 42 else 47)
            setPadding(dp(if (compact) 10 else 8), dp(6), dp(if (compact) 10 else 8), dp(6))
            setTextColor(if (primary) 0xff071726.toInt() else accentColor)
            background = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(0x2269d3ff), if (primary) surface(accentColor, 13, gradientEnd = 0xff3ca6f6.toInt())
                else surface(0xff223348.toInt(), 13, outlineColor), null
            )
            stateListAnimator = null
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(width, -2, if (weighted) 1f else 0f).apply {
            topMargin = dp(4)
            bottomMargin = dp(3)
            if (weighted || compact) marginEnd = dp(5)
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
