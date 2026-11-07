package br.com.atlasfit

internal enum class Motion {
    FLY, PRESS, INCLINE, BENCH, OVERHEAD, FRONT_RAISE, PUSHDOWN, EXTENSION,
    LEG_EXTENSION, SQUAT, LEG_PRESS, SEATED_CURL, LYING_CURL, ADDUCTOR, CALF,
    PULLDOWN, ROW, REVERSE_FLY, SHRUG, HAMMER, CURL, WALK, BIKE, MOBILITY
}

internal data class ExerciseGuide(
    val target: String,
    val equipment: String,
    val motion: Motion,
    val setup: String,
    val execution: String
)

internal object ExerciseGuides {
    private val all = listOf(
        guide("Peito", "Máquina", Motion.FLY, "Regule o banco para as manoplas ficarem na linha do peito.", "Com os cotovelos levemente flexionados, aproxime as manoplas em arco e retorne devagar."),
        guide("Peito · tríceps", "Máquina", Motion.PRESS, "Apoie as costas no encosto e segure as manoplas com as palmas voltadas entre si.", "Empurre para a frente, acompanhando o arco da máquina, e volte com controle."),
        guide("Peito superior · tríceps", "Barra", Motion.INCLINE, "Apoie os pés e posicione os olhos abaixo da barra. Use as travas ou um parceiro conforme a carga.", "Desça em direção à parte superior do peito e empurre mantendo os punhos sobre os cotovelos."),
        guide("Peito · tríceps", "Barra", Motion.BENCH, "Apoie os pés no chão e mantenha os ombros firmes no banco. Use as travas ou um parceiro conforme a carga.", "Desça a barra de forma controlada em direção ao meio do peito e empurre para cima."),
        guide("Ombros · tríceps", "Máquina", Motion.OVERHEAD, "Regule o banco para as manoplas começarem próximas à altura dos ombros.", "Empurre para cima mantendo o tronco apoiado e retorne devagar."),
        guide("Ombros", "Halteres", Motion.FRONT_RAISE, "Comece com os halteres à frente das coxas, joelhos relaxados e tronco estável.", "Eleve os braços à frente até a altura dos ombros e desça sem usar impulso."),
        guide("Tríceps", "Polia", Motion.PUSHDOWN, "Use a barra na polia alta e mantenha os cotovelos próximos ao tronco.", "Estenda os cotovelos para baixo e retorne sem deixar os braços avançarem."),
        guide("Tríceps", "Polia", Motion.EXTENSION, "De costas para a polia alta, firme os pés e posicione a barra atrás da cabeça.", "Estenda os cotovelos à frente mantendo os braços alinhados e retorne lentamente."),
        guide("Pernas · cardio", "Esteira", Motion.WALK, "Prenda a chave de segurança e inicie em velocidade baixa.", "Caminhe com passada natural e aumente a velocidade gradualmente."),
        guide("Quadríceps", "Máquina", Motion.LEG_EXTENSION, "Alinhe o joelho ao eixo da máquina e posicione o rolo logo acima do tornozelo.", "Estenda os joelhos com controle e retorne sem deixar a carga bater."),
        guide("Quadríceps · glúteos", "Barra", Motion.SQUAT, "Ajuste as travas do rack e apoie a barra na parte alta das costas.", "Flexione quadris e joelhos mantendo os pés firmes. Suba com os joelhos acompanhando a direção dos pés."),
        guide("Quadríceps · glúteos", "Máquina", Motion.LEG_PRESS, "Apoie as costas e coloque os pés na plataforma em posição confortável.", "Empurre a plataforma e retorne até onde consegue manter a pelve apoiada."),
        guide("Posteriores da coxa", "Máquina", Motion.SEATED_CURL, "Ajuste o encosto, alinhe o joelho ao eixo e firme o apoio sobre as coxas.", "Flexione os joelhos puxando o rolo para baixo e retorne sem levantar o quadril."),
        guide("Posteriores da coxa", "Máquina", Motion.LYING_CURL, "Deite de bruços e ajuste o rolo acima dos calcanhares.", "Aproxime os calcanhares dos glúteos sem tirar o quadril do banco. Retorne com controle."),
        guide("Adutores do quadril", "Máquina", Motion.ADDUCTOR, "Sente-se com as costas apoiadas e ajuste a abertura em uma amplitude confortável.", "Aproxime as pernas contra os apoios e abra devagar."),
        guide("Panturrilhas · sóleo", "Máquina", Motion.CALF, "Apoie a parte da frente dos pés e ajuste a almofada sobre as coxas.", "Eleve e abaixe os calcanhares com controle, mantendo os joelhos apoiados."),
        guide("Costas · bíceps", "Polia", Motion.PULLDOWN, "Ajuste o apoio das coxas e segure a barra com pegada aberta.", "Puxe a barra em direção à parte alta do peito e estenda os braços no retorno com controle."),
        guide("Costas · bíceps", "Polia", Motion.ROW, "Apoie os pés e segure o triângulo com as palmas voltadas entre si.", "Puxe a alça em direção ao abdômen, sem balançar o tronco."),
        guide("Costas · deltoide posterior", "Polia", Motion.ROW, "Apoie os pés e segure a barra com pegada aberta.", "Puxe com os cotovelos mais abertos e retorne mantendo o tronco estável."),
        guide("Costas · bíceps", "Máquina", Motion.ROW, "Ajuste o banco e firme o peito no apoio. Segure as manoplas em pegada neutra.", "Traga os cotovelos para trás e retorne sem tirar o peito do apoio."),
        guide("Deltoide posterior · costas", "Máquina", Motion.REVERSE_FLY, "Sente-se de frente para o encosto, com as manoplas próximas à altura dos ombros.", "Abra os braços em arco, com os cotovelos levemente flexionados, e retorne devagar."),
        guide("Trapézio", "Halteres", Motion.SHRUG, "Segure os halteres ao lado do corpo com o pescoço em posição neutra.", "Eleve os ombros em direção às orelhas e abaixe sem girá-los."),
        guide("Braquial · bíceps", "Halteres", Motion.HAMMER, "Apoie as costas no banco inclinado e mantenha as palmas voltadas entre si.", "Flexione os cotovelos mantendo os braços estáveis e retorne sem embalo."),
        guide("Bíceps · braquial", "Máquina", Motion.CURL, "Ajuste o banco e apoie os braços na almofada da máquina.", "Flexione os cotovelos para trazer a barra em sua direção e retorne devagar."),
        guide("Pernas · cardio", "Bicicleta", Motion.BIKE, "Regule o banco para deixar uma leve flexão no joelho quando o pedal estiver embaixo.", "Pedale em ritmo confortável e ajuste a resistência de forma gradual."),
        guide("Mobilidade geral", "Peso corporal", Motion.MOBILITY, "Escolha movimentos confortáveis para as articulações que deseja mobilizar.", "Faça repetições lentas em amplitude confortável."),
        guide("Força geral", "Halteres", Motion.CURL, "Selecione o movimento e a carga disponíveis no condomínio.", "Registre o tempo do bloco e execute os movimentos com controle.")
    )
    private val byName = (Workout.A.exercises + Workout.B.exercises.filterNot { it.cardio } +
        Workout.C.exercises + Workout.CARDIO.exercises.last() + Workout.PESOS.exercises.last())
        .map(Exercise::name).zip(all).toMap()

    val exercises = Workout.entries.flatMap(Workout::exercises).distinctBy(Exercise::name)
    operator fun get(exercise: Exercise): ExerciseGuide = byName.getValue(exercise.name)

    private fun guide(target: String, equipment: String, motion: Motion, setup: String, execution: String) =
        ExerciseGuide(target, equipment, motion, setup, execution)
}
