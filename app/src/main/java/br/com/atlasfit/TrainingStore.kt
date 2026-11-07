package br.com.atlasfit

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class ExerciseLog(
    val name: String,
    val load: Double? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val minutes: Int? = null
) {
    fun json() = JSONObject().put("name", name).put("load", load).put("sets", sets)
        .put("reps", reps).put("minutes", minutes)
}

internal data class Session(
    val date: LocalDate,
    val workout: Workout,
    val full: Boolean,
    val logs: List<ExerciseLog> = emptyList()
) {
    fun json() = JSONObject().put("date", date.toString()).put("sheet", workout.name)
        .put("full", full).put("logs", JSONArray(logs.map(ExerciseLog::json)))

    fun sameSlot(other: Session): Boolean =
        date == other.date && workout == other.workout && full == other.full
}

internal class TrainingStore(private val prefs: SharedPreferences) {
    fun sessions(): List<Session> = records("sessions").mapNotNull { row ->
        val date = row.optString("date").localDate() ?: return@mapNotNull null
        val workout = Workout.entries.find { it.name == row.optString("sheet") }
            ?: return@mapNotNull null
        val logs = row.optJSONArray("logs")?.objects().orEmpty().map { item ->
            ExerciseLog(item.optString("name"), item.number("load"), item.number("sets")?.toInt(),
                item.number("reps")?.toInt(), item.number("minutes")?.toInt())
        }
        Session(date, workout, row.optBoolean("full"), logs)
    }.sortedByDescending { it.date }

    fun save(session: Session): Boolean {
        val rows = records("sessions").filterNot { row ->
            row.optString("date") == session.date.toString() &&
                row.optString("sheet") == session.workout.name && row.optBoolean("full") == session.full
        }
        return write("sessions", rows + session.json())
    }

    fun legacyLoad(workout: Workout, exercise: Exercise): String =
        prefs.getString("${workout.name}-${exercise.name}", "").orEmpty()

    fun legacyMeasures(): JSONObject? {
        val rawDate = prefs.getString("last", null) ?: return null
        val date = runCatching { LocalDate.parse(rawDate, DateTimeFormatter.ofPattern("dd/MM/uuuu")) }
            .getOrNull() ?: return null
        val weight = prefs.getString("weight", null)?.decimal()?.takeIf { it > 0 }
        val waist = prefs.getString("waist", null)?.decimal()?.takeIf { it > 0 }
        if (weight == null && waist == null) return null
        return JSONObject().put("date", date.toString()).put("source", "Registro de medidas anterior")
            .put("weight", weight).put("waist", waist)
    }

    fun assessments(): List<JSONObject> {
        val rows = records("bio").filter { it.optString("date").localDate() != null }
        val old = legacyMeasures()?.takeUnless { legacy -> rows.any { sameAssessment(it, legacy) } }
        return (rows + listOfNotNull(old)).sortedBy { it.optString("date") }
    }

    fun saveAssessment(assessment: JSONObject, previous: JSONObject?): Boolean {
        val rows = records("bio").filterNot { row ->
            row.toString() == previous?.toString() || sameAssessment(row, assessment)
        }
        val editor = prefs.edit().putString("bio", JSONArray(rows + assessment).toString())
        if (previous != null && legacyMeasures()?.toString() == previous.toString()) {
            editor.remove("last").remove("weight").remove("waist")
        }
        return editor.commit()
    }

    fun importAssessments(incoming: List<JSONObject>): Int? {
        val rows = records("bio").toMutableList()
        var added = 0
        for (assessment in incoming) {
            if (rows.any {
                sameAssessment(it, assessment) ||
                    (it.optString("date") == assessment.optString("date") &&
                        it.optString("source").isBlank() &&
                        it.number("weight") == assessment.number("weight") &&
                        it.number("fat") == assessment.number("fat"))
            }) continue
            rows.add(assessment)
            added++
        }
        return if (added == 0 || write("bio", rows)) added else null
    }

    private fun sameAssessment(a: JSONObject, b: JSONObject): Boolean =
        a.optString("date") == b.optString("date") &&
            a.optString("source").trim().equals(b.optString("source").trim(), ignoreCase = true)

    private fun records(key: String): List<JSONObject> =
        runCatching { JSONArray(prefs.getString(key, "[]")).objects() }.getOrDefault(emptyList())

    private fun write(key: String, records: List<JSONObject>): Boolean =
        prefs.edit().putString(key, JSONArray(records).toString()).commit()
}

internal fun JSONArray.objects(): List<JSONObject> =
    (0 until length()).mapNotNull(::optJSONObject)

internal fun JSONObject.number(key: String): Double? = optDouble(key).takeIf(Double::isFinite)
