package br.com.atlasfit

internal data class ExerciseGuide(
    val id: String,
    val primary: String,
    val secondary: String,
    val equipment: String,
    val apparatus: String?,
    val setup: String,
    val execution: String
) {
    val target: String get() = listOf(primary, secondary).filter(String::isNotBlank).joinToString(" · ")
    val movementPath: String get() = "movements/$id.webp"
}

internal data class Apparatus(val name: String, val id: String) {
    val imagePath: String get() = "equipment/$id.webp"
}

internal object ExerciseGuides {
    private val all = listOf(
        guide("pec_deck", "Peitoral maior", "Deltoide anterior", "Máquina", "pec_deck",
            "Regule o banco para as manoplas ficarem na linha do peito.",
            "Com os cotovelos levemente flexionados, aproxime as manoplas em arco e retorne devagar."),
        guide("chest_press", "Peitoral maior", "Tríceps · deltoide anterior", "Máquina", "chest_press",
            "Apoie as costas no encosto e segure as manoplas com as palmas voltadas entre si.",
            "Empurre para a frente, acompanhando o arco da máquina, e volte com controle."),
        guide("incline", "Peitoral maior, porção clavicular", "Deltoide anterior · tríceps", "Barra", "incline",
            "Apoie os pés e posicione os olhos abaixo da barra. Use as travas ou um parceiro conforme a carga.",
            "Desça em direção à parte superior do peito e empurre mantendo os punhos sobre os cotovelos."),
        guide("bench", "Peitoral maior", "Tríceps · deltoide anterior", "Barra", "bench",
            "Apoie os pés no chão e mantenha os ombros firmes no banco. Use as travas ou um parceiro conforme a carga.",
            "Desça a barra de forma controlada em direção ao meio do peito e empurre para cima."),
        guide("shoulder_press", "Deltoides anterior e lateral", "Tríceps", "Máquina", "shoulder_press",
            "Regule o banco para as manoplas começarem próximas à altura dos ombros.",
            "Empurre para cima mantendo o tronco apoiado e retorne devagar."),
        guide("front_raise", "Deltoide anterior", "Peitoral maior, porção clavicular", "Halteres", "dumbbells",
            "Comece com os halteres à frente das coxas, joelhos relaxados e tronco estável.",
            "Eleve os braços à frente até a altura dos ombros e desça sem usar impulso."),
        guide("pushdown", "Tríceps braquial", "Ancôneo", "Polia", "cable",
            "Use a barra na polia alta e mantenha os cotovelos próximos ao tronco.",
            "Estenda os cotovelos para baixo e retorne sem deixar os braços avançarem."),
        guide("overhead_triceps", "Tríceps braquial", "Ancôneo", "Polia", "cable",
            "De costas para a polia alta, firme os pés e posicione a barra atrás da cabeça.",
            "Estenda os cotovelos à frente e para cima mantendo os braços estáveis. Retorne lentamente."),
        guide("treadmill", "Quadríceps · glúteos · panturrilhas", "Posteriores da coxa · tibial anterior", "Esteira", "treadmill",
            "Prenda a chave de segurança e inicie em velocidade baixa.",
            "Caminhe com passada natural e aumente a velocidade gradualmente."),
        guide("leg_extension", "Quadríceps", "", "Máquina", "leg_extension",
            "Alinhe o joelho ao eixo da máquina e posicione o rolo logo acima do tornozelo.",
            "Estenda os joelhos com controle e retorne sem deixar a carga bater."),
        guide("squat", "Quadríceps · glúteo máximo", "Adutor magno · eretores da coluna", "Barra", "rack",
            "Ajuste as travas do rack e apoie a barra na parte alta das costas.",
            "Flexione quadris e joelhos mantendo os pés firmes. Suba com os joelhos acompanhando a direção dos pés."),
        guide("leg_press", "Quadríceps · glúteo máximo", "Adutor magno · posteriores da coxa", "Máquina", "leg_press",
            "Apoie as costas e coloque os pés na plataforma em posição confortável.",
            "Empurre a plataforma e retorne até onde consegue manter a pelve apoiada."),
        guide("seated_curl", "Posteriores da coxa", "Gastrocnêmio", "Máquina", "seated_curl",
            "Ajuste o encosto, alinhe o joelho ao eixo e firme o apoio sobre as coxas.",
            "Flexione os joelhos puxando o rolo para baixo e retorne sem levantar o quadril."),
        guide("lying_curl", "Posteriores da coxa", "Gastrocnêmio", "Máquina", "lying_curl",
            "Deite de bruços e ajuste o rolo acima dos calcanhares.",
            "Aproxime os calcanhares dos glúteos sem tirar o quadril do banco. Retorne com controle."),
        guide("adductor", "Adutores do quadril", "", "Máquina", "adductor",
            "Sente-se com as costas apoiadas e ajuste a abertura em uma amplitude confortável.",
            "Aproxime as pernas contra os apoios e abra devagar."),
        guide("calf", "Sóleo", "Gastrocnêmio", "Máquina", "calf",
            "Apoie a parte da frente dos pés e ajuste a almofada sobre as coxas.",
            "Eleve e abaixe os calcanhares com controle, mantendo os joelhos apoiados."),
        guide("pulldown", "Latíssimo do dorso", "Bíceps · braquial · redondo maior", "Polia", "pulldown",
            "Ajuste o apoio das coxas e segure a barra com pegada aberta.",
            "Puxe a barra em direção à parte alta do peito e estenda os braços no retorno com controle."),
        guide("row_triangle", "Latíssimo do dorso · romboides", "Trapézio médio · bíceps", "Polia", "cable_row",
            "Apoie os pés e segure o triângulo com as palmas voltadas entre si.",
            "Puxe a alça em direção ao abdômen, sem balançar o tronco."),
        guide("row_wide", "Romboides · trapézio médio · deltoide posterior", "Latíssimo do dorso · bíceps", "Polia", "cable_row",
            "Apoie os pés e segure a barra com pegada aberta.",
            "Puxe com os cotovelos mais abertos e retorne mantendo o tronco estável."),
        guide("row_machine", "Latíssimo do dorso · romboides", "Trapézio médio · bíceps", "Máquina", "row_machine",
            "Ajuste o banco e firme o peito no apoio. Segure as manoplas em pegada neutra.",
            "Traga os cotovelos para trás e retorne sem tirar o peito do apoio."),
        guide("reverse_fly", "Deltoide posterior", "Romboides · trapézio médio", "Máquina", "pec_deck",
            "Sente-se de frente para o encosto, com as manoplas próximas à altura dos ombros.",
            "Abra os braços em arco, com os cotovelos levemente flexionados, e retorne devagar."),
        guide("shrug", "Trapézio superior", "Elevador da escápula", "Halteres", "dumbbells",
            "Segure os halteres ao lado do corpo com o pescoço em posição neutra.",
            "Eleve os ombros em direção às orelhas e abaixe sem girá-los."),
        guide("hammer", "Braquial · bíceps braquial", "Braquiorradial", "Halteres", "incline_dumbbells",
            "Apoie as costas no banco inclinado e mantenha as palmas voltadas entre si.",
            "Flexione os cotovelos mantendo os braços estáveis e retorne sem embalo."),
        guide("preacher", "Bíceps braquial", "Braquial · braquiorradial", "Máquina", "preacher",
            "Ajuste o banco e apoie os braços na almofada da máquina.",
            "Flexione os cotovelos para trazer a barra em sua direção e retorne devagar."),
        guide("bike", "Quadríceps · glúteos", "Posteriores da coxa · panturrilhas", "Bicicleta", "bike",
            "Regule o banco para deixar uma leve flexão no joelho quando o pedal estiver embaixo.",
            "Pedale em ritmo confortável e ajuste a resistência de forma gradual."),
        guide("mobility", "Mobilidade de ombros", "", "Peso corporal", null,
            "Em pé, com o tronco estável, comece com os braços relaxados. A imagem mostra um exemplo.",
            "Eleve um braço na diagonal em amplitude confortável, baixe devagar e alterne."),
        guide("dumbbell_curl", "Bíceps braquial no exemplo", "Braquial · braquiorradial", "Halteres", "dumbbells",
            "Selecione o movimento e a carga disponíveis no condomínio. A imagem mostra uma rosca com halteres.",
            "Na rosca, mantenha os braços próximos ao tronco e flexione os cotovelos sem embalo. Registre o tempo do bloco.")
    )
    private val byName = (Workout.A.exercises + Workout.B.exercises.filterNot { it.cardio } +
        Workout.C.exercises + Workout.CARDIO.exercises.last() + Workout.PESOS.exercises.last())
        .map(Exercise::name).zip(all).toMap()

    private val apparatus = mapOf(
        "bench" to "Banco reto com suporte de barra",
        "incline" to "Banco inclinado com suporte de barra",
        "chest_press" to "Máquina de supino convergente",
        "pec_deck" to "Peck deck e voador inverso",
        "shoulder_press" to "Máquina de desenvolvimento",
        "cable" to "Torre de polia com barra curta",
        "leg_extension" to "Cadeira extensora",
        "rack" to "Rack de agachamento",
        "leg_press" to "Leg press horizontal",
        "seated_curl" to "Cadeira flexora",
        "lying_curl" to "Mesa flexora",
        "adductor" to "Cadeira adutora",
        "calf" to "Banco de panturrilha sentado",
        "pulldown" to "Máquina de puxada alta",
        "cable_row" to "Estação de remada baixa",
        "row_machine" to "Máquina de remada convergente",
        "preacher" to "Máquina de rosca Scott",
        "dumbbells" to "Par de halteres",
        "treadmill" to "Esteira",
        "bike" to "Bicicleta ergométrica",
        "incline_dumbbells" to "Banco inclinado e halteres"
    )

    val exercises = Workout.entries.flatMap(Workout::exercises).distinctBy(Exercise::name)
    operator fun get(exercise: Exercise): ExerciseGuide = byName.getValue(exercise.name)
    fun apparatus(guide: ExerciseGuide): Apparatus? =
        guide.apparatus?.let { Apparatus(apparatus.getValue(it), it) }

    private fun guide(id: String, primary: String, secondary: String, equipment: String, apparatus: String?,
                      setup: String, execution: String) =
        ExerciseGuide(id, primary, secondary, equipment, apparatus, setup, execution)
}
