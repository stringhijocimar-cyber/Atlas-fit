package br.com.atlasfit

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasFitTest {
    @Test fun rotatesCompleteSheetAndKeepsOriginalExpressExercises() {
        val start = TrainingPlan.start
        assertEquals(null, TrainingPlan.planned(start.minusDays(1)))
        assertEquals(listOf(Workout.A, Workout.B, Workout.C, Workout.A),
            (0L..3L).map { TrainingPlan.complete(start.plusWeeks(it).plusDays(6)) })
        assertEquals(listOf(Workout.A, Workout.CARDIO, Workout.B, null, Workout.C, Workout.PESOS, null),
            (0L..6L).map { TrainingPlan.planned(start.plusDays(it)) })
        assertEquals(listOf(9, 8, 9), TrainingPlan.gym.map { it.items(true).size })
        TrainingPlan.gym.forEach {
            assertEquals(4, it.items(false).size)
            assertTrue(it.items(true).containsAll(it.items(false)))
        }
    }

    @Test fun preservesLegacyAttendanceAndReplacesSavedSessionWithoutDuplicating() {
        val prefs = preferences(mutableMapOf(
            "sessions" to """[{"date":"2026-11-09","sheet":"A","full":true}]""",
            "A-Fly pegada neutra" to "30 kg / 3x10"
        ))
        val store = TrainingStore(prefs)
        val old = store.sessions().single()
        val exercise = Workout.A.exercises.first()
        assertTrue(old.logs.isEmpty())
        assertEquals("30 kg / 3x10", store.legacyLoad(Workout.A, exercise))
        val updated = old.copy(logs = listOf(ExerciseLog(exercise.name, 32.5, 3, 10)))
        assertTrue(store.save(updated))
        assertTrue(store.save(updated))
        val condo = Session(LocalDate.parse("2026-11-09"), Workout.CARDIO, false,
            listOf(ExerciseLog("Bicicleta", minutes = 20)))
        assertTrue(store.save(condo))
        assertEquals(setOf(updated, condo), store.sessions().toSet())
        assertEquals(1, store.sessions().map(Session::date).distinct().size)
    }

    @Test fun retainsMeasurementUnitsAndAvoidsReimportingLegacyReading() {
        val prefs = preferences(mutableMapOf(
            "bio" to """[{"date":"2025-04-09","weight":90,"fat":24,"muscle":66,"water":52}]""",
            "last" to "02/11/2026", "weight" to "85,5", "waist" to "90"
        ))
        val store = TrainingStore(prefs)
        val historical = JSONObject("""{"date":"2025-04-09","source":"Histórico","weight":90,"fat":24}""")
        val new = JSONObject("""{"date":"2026-11-07","source":"Balança","weight":85,"muscle":40,"muscleUnit":"kg esquelética","water":51,"waterUnit":"kg"}""")
        assertEquals(1, store.importAssessments(listOf(historical, new)))
        assertEquals(0, store.importAssessments(listOf(historical, new)))
        val entries = store.assessments()
        assertEquals(3, entries.size)
        assertEquals(85.5, entries[1].number("weight"))
        assertEquals("kg esquelética", entries[2].optString("muscleUnit"))
        assertEquals("kg", entries[2].optString("waterUnit"))
    }

    private fun preferences(data: MutableMap<String, String>): SharedPreferences {
        fun editor(): SharedPreferences.Editor = Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)
        ) { instance, method, args ->
            when (method.name) {
                "putString" -> instance.also { data[args[0] as String] = args[1] as String }
                "remove" -> instance.also { data.remove(args[0] as String) }
                "commit" -> true
                else -> error(method.name)
            }
        } as SharedPreferences.Editor

        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString" -> data[args[0] as String] ?: args[1]
                "edit" -> editor()
                else -> error(method.name)
            }
        } as SharedPreferences
    }
}
