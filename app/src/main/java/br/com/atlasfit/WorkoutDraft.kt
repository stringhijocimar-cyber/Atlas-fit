package br.com.atlasfit

import org.json.JSONArray
import org.json.JSONObject

internal data class SetResult(val number: Int, val reps: Int?, val load: Double?, val restSeconds: Int? = null) {
    fun json() = JSONObject().put("number", number).put("reps", reps).put("load", load)
        .put("rest", restSeconds)

    companion object {
        fun fromJson(row: JSONObject) = SetResult(row.optInt("number"), row.number("reps")?.toInt(),
            row.number("load"), row.number("rest")?.toInt())
    }
}

internal data class SetEntry(var reps: String = "", var load: String = "", var done: Boolean = false, var restSeconds: Int? = null) {
    fun valid(): Boolean = (reps.isBlank() || reps.toIntOrNull() in 1..999) &&
        (load.isBlank() || load.decimal()?.let { it in 0.0..9999.0 } == true)

    fun json() = JSONObject().put("reps", reps).put("load", load).put("done", done).put("rest", restSeconds)

    companion object {
        fun fromJson(row: JSONObject) = SetEntry(row.optString("reps"), row.optString("load"), row.optBoolean("done"), row.number("rest")?.toInt())
    }
}

internal data class ExerciseDraft(
    val rounds: MutableList<SetEntry> = MutableList(3) { SetEntry() },
    var minutes: String = "",
    var cardioDone: Boolean = false,
    var changed: Boolean = false
) {
    fun log(exercise: Exercise, previous: ExerciseLog?): ExerciseLog? {
        if (!changed && previous != null) return previous
        if (exercise.cardio) {
            return if (cardioDone) ExerciseLog(exercise.name, minutes = minutes.toIntOrNull()) else null
        }
        val results = rounds.mapIndexedNotNull { index, entry ->
            if (entry.done && entry.valid()) SetResult(index + 1, entry.reps.toIntOrNull(), entry.load.decimal(), entry.restSeconds)
            else null
        }
        if (results.isEmpty()) return null
        return ExerciseLog(exercise.name, results.mapNotNull(SetResult::load).maxOrNull(),
            results.size, results.map(SetResult::reps).distinct().singleOrNull(), rounds = results)
    }

    fun json() = JSONObject().put("rounds", JSONArray(rounds.map(SetEntry::json)))
        .put("minutes", minutes).put("cardio", cardioDone).put("changed", changed)

    companion object {
        fun fromLog(previous: ExerciseLog?): ExerciseDraft {
            if (previous == null) return ExerciseDraft()
            if (previous.minutes != null) return ExerciseDraft(minutes = previous.minutes.toString(), cardioDone = true)
            val last = previous.rounds.maxOfOrNull(SetResult::number)
            val count = (last ?: previous.sets ?: 1).coerceIn(1, 30)
            val rounds = MutableList(maxOf(3, count)) { index ->
                val result = previous.rounds.find { it.number == index + 1 }
                if (result != null) SetEntry(result.reps?.toString().orEmpty(), result.load?.toString().orEmpty(), true, result.restSeconds)
                else if (previous.rounds.isEmpty() && index < count) {
                    SetEntry(previous.reps?.toString().orEmpty(), previous.load?.toString().orEmpty(), true)
                } else SetEntry()
            }
            return ExerciseDraft(rounds, cardioDone = true)
        }

        fun fromJson(row: JSONObject) = ExerciseDraft(
            row.optJSONArray("rounds")?.objects().orEmpty().map(SetEntry::fromJson).toMutableList(),
            row.optString("minutes"), row.optBoolean("cardio"), row.optBoolean("changed")
        )
    }
}

internal class WorkoutDraft(
    val startedAt: Long = System.currentTimeMillis(),
    var restSeconds: Int = 60,
    var restUntil: Long = 0,
    val exercises: MutableMap<String, ExerciseDraft> = mutableMapOf()
) {
    fun elapsed(now: Long) = ((now - startedAt) / 1000).coerceAtLeast(0)
    fun remaining(now: Long) = ((restUntil - now + 999) / 1000).coerceIn(0, restSeconds.toLong())

    fun json() = JSONObject().put("start", startedAt).put("rest", restSeconds)
        .put("until", restUntil).put("exercises", JSONObject(exercises.mapValues { it.value.json() }))

    companion object {
        fun fromJson(row: JSONObject): WorkoutDraft {
            val records = row.getJSONObject("exercises")
            val exercises = records.keys().asSequence().associateWith { ExerciseDraft.fromJson(records.getJSONObject(it)) }
            return WorkoutDraft(row.getLong("start"), row.getInt("rest"), row.getLong("until"), exercises.toMutableMap())
        }
    }
}

internal fun clockText(seconds: Long): String =
    String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
