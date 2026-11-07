package br.com.atlasfit

internal data class ExerciseVideo(val id: String, val credit: String)

internal object ExerciseVideos {
    private const val TWIN = "Leandro Twin"
    private const val BLUEFIT = "Leandro Twin no canal BlueFit"

    private val videos = mapOf(
        "pec_deck" to ExerciseVideo("FzCnfD0gOXo", TWIN),
        "chest_press" to ExerciseVideo("igoq9EMTruA", "Fitness Magazine Brasil"),
        "incline" to ExerciseVideo("lBCjPgnIzKk", TWIN),
        "bench" to ExerciseVideo("ZINbHcRi3U8", BLUEFIT),
        "shoulder_press" to ExerciseVideo("lo4byk1CIu4", BLUEFIT),
        "front_raise" to ExerciseVideo("kQTNDsaEIKc", TWIN),
        "pushdown" to ExerciseVideo("0YMP0o15oT0", BLUEFIT),
        "overhead_triceps" to ExerciseVideo("P4ayEuhNNmA", "Academia Coliseu"),
        "treadmill" to ExerciseVideo("s2fOUbUdlPg", "Minha Vida"),
        "leg_extension" to ExerciseVideo("el3oHblB5DM", TWIN),
        "squat" to ExerciseVideo("zgk71dUUt0Y", TWIN),
        "leg_press" to ExerciseVideo("gTo0HfVcLxo", "Panobianco Academia"),
        "seated_curl" to ExerciseVideo("BvedRRkARzY", BLUEFIT),
        "lying_curl" to ExerciseVideo("2-ULaRrQa7c", TWIN),
        "adductor" to ExerciseVideo("Wf602gn_9zU", TWIN),
        "calf" to ExerciseVideo("jMWs_p-W9gY", TWIN),
        "pulldown" to ExerciseVideo("H09EvebBsB4", TWIN),
        "row_triangle" to ExerciseVideo("wC1EsDy_esM", TWIN),
        "row_wide" to ExerciseVideo("1HUJIDEvD6w", "Treinador Rafael Alexandre"),
        "row_machine" to ExerciseVideo("C0-C0X7G8eQ", TWIN),
        "reverse_fly" to ExerciseVideo("EaQy_Xn0SNw", "Adrenalit"),
        "shrug" to ExerciseVideo("YeILDnoeYEk", "MyTrainingPRO"),
        "hammer" to ExerciseVideo("R_Ou6_gifJE", "Academia UMMA"),
        "preacher" to ExerciseVideo("JWRoHF7I2NQ", "Via Brasil Club"),
        "bike" to ExerciseVideo("YRx5puImWEE", "Gallant"),
        "mobility" to ExerciseVideo("0nDK4qVrH3A", "Leandro Twin e Rapha Barros"),
        "dumbbell_curl" to ExerciseVideo("s1vf8LEPpDA", "Peterson Roik")
    )

    operator fun get(guide: ExerciseGuide): ExerciseVideo = videos.getValue(guide.id)
}
