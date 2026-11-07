package br.com.atlasfit

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class Exercise(val name: String, val cardio: Boolean = false)

internal enum class Workout(val title: String, val exercises: List<Exercise>, val express: List<Int> = emptyList()) {
    A("Ficha A", listOf(
        Exercise("Fly pegada neutra"),
        Exercise("Supino convergente neutro"),
        Exercise("Supino barra banco inclinado"),
        Exercise("Supino reto barra"),
        Exercise("Desenvolvimento máquina"),
        Exercise("Elevação frontal halter"),
        Exercise("Tríceps pulley crossover barra polia alta"),
        Exercise("Tríceps francês crossover barra polia alta"),
        Exercise("Caminhada na esteira", cardio = true)
    ), listOf(0, 1, 2, 6)),
    B("Ficha B", listOf(
        Exercise("Extensora cadeira"),
        Exercise("Agachamento rack"),
        Exercise("Leg press horizontal máquina"),
        Exercise("Cadeira flexora máquina"),
        Exercise("Flexora mesa"),
        Exercise("Adutor cadeira"),
        Exercise("Banco sóleo"),
        Exercise("Caminhada na esteira", cardio = true)
    ), listOf(0, 1, 2, 3)),
    C("Ficha C", listOf(
        Exercise("Puxada aberta"),
        Exercise("Remada baixa triângulo"),
        Exercise("Remada baixa pegada aberta"),
        Exercise("Remada convergente neutra"),
        Exercise("Fly inverso"),
        Exercise("Encolhimento halter"),
        Exercise("Rosca martelo duplo halter banco inclinado"),
        Exercise("Rosca direta máquina"),
        Exercise("Bicicleta", cardio = true)
    ), listOf(0, 1, 4, 6)),
    CARDIO("Condomínio · cardio", listOf(
        Exercise("Caminhada na esteira", cardio = true),
        Exercise("Bicicleta", cardio = true),
        Exercise("Mobilidade", cardio = true)
    )),
    PESOS("Condomínio · cardio e pesos", listOf(
        Exercise("Bicicleta", cardio = true),
        Exercise("Pesos livres", cardio = true)
    ));

    fun title(full: Boolean): String = when {
        express.isEmpty() -> title
        full -> "$title · completa"
        else -> "$title · expressa"
    }

    fun items(full: Boolean): List<Exercise> =
        if (full || express.isEmpty()) exercises else express.map(exercises::get)
}

internal object TrainingPlan {
    val start: LocalDate = LocalDate.of(2026, 11, 9)
    val gym = listOf(Workout.A, Workout.B, Workout.C)
    val schedule = listOf(Workout.A, Workout.CARDIO, Workout.B, null, Workout.C, Workout.PESOS, null)

    fun monday(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - 1).toLong())

    fun complete(date: LocalDate): Workout {
        val weeks = ChronoUnit.WEEKS.between(start, monday(date)).coerceAtLeast(0)
        return gym[(weeks % gym.size).toInt()]
    }

    fun planned(date: LocalDate): Workout? =
        if (date.isBefore(start)) null else schedule[date.dayOfWeek.value - 1]
}

internal fun String.localDate(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

internal fun String.decimal(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
