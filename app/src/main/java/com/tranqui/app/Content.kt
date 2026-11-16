package com.tranqui.app

/** Plantillas de objetivos con el paso diario sugerido por nivel (casi nunca, a veces, seguido). */
data class Template(val id: String, val name: String, val levels: List<Int>, val step: Int)

val TEMPLATES = listOf(
    Template("leer", "Leer", listOf(5, 10, 20), 3),
    Template("caminar", "Caminar", listOf(5, 10, 20), 5),
    Template("ingles", "Practicar inglés", listOf(5, 10, 15), 3),
    Template("guitarra", "Tocar la guitarra", listOf(5, 10, 20), 5),
    Template("escribir", "Escribir", listOf(2, 5, 10), 3),
    Template("meditar", "Meditar", listOf(2, 5, 10), 2),
    Template("ejercicio", "Hacer ejercicio", listOf(5, 10, 20), 5),
    Template("ordenar", "Ordenar la casa", listOf(5, 10, 15), 5),
    Template("estudiar", "Estudiar", listOf(10, 15, 25), 5),
    Template("propio", "Otro objetivo", listOf(5, 10, 20), 5)
)

val LEVELS = listOf("Casi nunca", "A veces", "Seguido")

data class QuickOption(
    val label: String, val minutes: Int, val goalId: String? = null, val note: String = "",
    val practice: String? = null // si no es null, abre la práctica guiada en vez del cronómetro
)

/** Opciones de los tres botones: siempre hay algo que se puede hacer en cualquier lugar. */
fun quickOptions(size: Int, goals: List<Goal>): List<QuickOption> = when (size) {
    1 -> listOf(
        QuickOption("Respiración lenta", 1, note = "Guiada · en cualquier lugar, sin que se note", practice = "suspiro"),
        QuickOption("Estirar cuello y hombros", 1, note = "Sentado o parado"),
        QuickOption("Tomar agua y mirar lejos", 1),
        QuickOption("Planear el día: 3 tareas", 2),
        QuickOption("Escribir 3 líneas", 2, note = "Diario o gratitud"),
        QuickOption("Mensaje a alguien que querés", 2),
        QuickOption("2 minutos de la tarea que estás evitando", 2, note = "Solo empezar")
    )
    5 -> goals.map { QuickOption("${it.name}: versión mínima", minOf(it.minutes, 5), it.id, it.anchor.let { a -> if (a.isBlank()) "" else "Después de $a" }) } + listOf(
        QuickOption("Caminata corta", 5, note = "Si podés levantarte"),
        QuickOption("Ejercicio corto", 5, note = "Sentadillas o flexiones"),
        QuickOption("Ordenar una cosa", 5),
        QuickOption("Leer 2 páginas", 5, note = "Con un libro o e-book a mano")
    )
    else -> goals.map { QuickOption("Sesión de foco: ${it.name}", maxOf(15, it.minutes), it.id) } + listOf(
        QuickOption("Foco libre", 15, note = "Una sola tarea, 15 minutos"),
        QuickOption("Pomodoro clásico", 25, note = "25 minutos y después una pausa")
    )
}

enum class MethodAction {
    NEW_GOAL, WOOP, POMODORO, MICRO, KAIZEN, KANBAN, HANSEI, IKIGAI, SAPO,
    // Herramientas guiadas
    EISENHOWER, IVY, UNO35, DOSMIN, VACIAR, PASOS, BLOQUES, PERDON, PLAZO, CIERRE, PLACER
}

data class Method(
    val id: String, val name: String, val jp: String?, val tag: String, val short: String,
    val que: String, val porque: String, val evidencia: String, val pasos: List<String>, val action: MethodAction?,
    val group: String = ""
)

/** Grupos de métodos según para qué sirven, en el orden en que se muestran. */
val METHOD_GROUPS = listOf(
    "arrancar" to ("Para arrancar cuando cuesta" to "Cuando no te sale empezar o venís de un día flojo."),
    "foco" to ("Para enfocarte" to "Cuando te distraés o te cansás a mitad de camino."),
    "elegir" to ("Para elegir qué hacer primero" to "Cuando tenés mucho y no sabés por dónde agarrar."),
    "ordenar" to ("Para ordenar lo pendiente" to "Cuando tenés la cabeza llena de cosas sueltas."),
    "sostener" to ("Para sostener metas y hábitos" to "Para no abandonar a las dos semanas."),
    "revisar" to ("Para revisar y mejorar" to "Para aprender de cómo te fue, sin castigarte.")
)

private val GRUPO_DE = mapOf(
    "minimos" to "arrancar", "kaizen" to "arrancar", "dosmin" to "arrancar", "sapo" to "arrancar",
    "perdon" to "arrancar", "placer" to "arrancar",
    "pomodoro" to "foco", "micro" to "foco", "bloques" to "foco",
    "eisenhower" to "elegir", "ivylee" to "elegir", "unotrescinco" to "elegir",
    "vaciar" to "ordenar", "pasos" to "ordenar", "kanban" to "ordenar",
    "sientonces" to "sostener", "woop" to "sostener", "plazo" to "sostener", "ikigai" to "sostener", "wabisabi" to "sostener",
    "cierre" to "revisar", "hansei" to "revisar"
)

val METHODS: List<Method> by lazy {
    (BASE_METHODS + NEW_METHODS).map { m ->
        val accion = when (m.id) {
            "placer" -> MethodAction.PLACER
            "dosmin" -> MethodAction.DOSMIN
            else -> m.action
        }
        m.copy(group = GRUPO_DE[m.id] ?: "sostener", action = accion)
    }
}

private val NEW_METHODS = listOf(
    Method(
        "eisenhower", "Urgente o importante", null, "Mixta", "Separar lo que apura de lo que importa",
        "Ordenás cada tarea según dos preguntas: ¿es urgente? ¿es importante? Lo importante y urgente se hace ya; lo importante sin apuro se agenda; lo urgente que no importa se acota; lo demás se suelta.",
        "Sin darnos cuenta elegimos lo que tiene fecha cerca aunque valga menos. Mirar las tareas en cuatro cuadros hace visible lo importante que venís postergando.",
        "En cinco experimentos, la gente eligió tareas urgentes de menor valor antes que tareas importantes de mayor valor, aun teniendo tiempo de sobra («efecto de la mera urgencia», Zhu, Yang y Hsee, 2018). Recordar qué gana cada opción en el momento de elegir redujo ese sesgo. La matriz en sí no fue puesta a prueba.",
        listOf("Anotá tus pendientes.", "Ubicá cada uno en un cuadro.", "Antes de decidir, mirá qué ganás con cada tarea.", "Empezá por «Hacer ahora»; agendá lo importante sin apuro."),
        MethodAction.EISENHOWER
    ),
    Method(
        "ivylee", "Método Ivy Lee", null, "Tradición", "Seis tareas, en orden, una por vez",
        "Al terminar el día anotás las 6 tareas más importantes para mañana, en orden. Al día siguiente hacés la primera hasta terminarla, después la segunda. Lo que no termines pasa a la lista del día siguiente.",
        "Decidís el orden la noche anterior, con la cabeza fría, y a la mañana no perdés energía eligiendo. Hacer una sola por vez evita saltar de tarea en tarea.",
        "Método de 1918 sin estudios propios. Coincide con lo que sí está estudiado: planificar con anticipación y concretar el próximo paso.",
        listOf("Anotá hasta 6 tareas.", "Ordenalas por importancia.", "Hacé solo la primera hasta terminarla.", "Lo que quede, pasa a mañana."),
        MethodAction.IVY
    ),
    Method(
        "unotrescinco", "Regla 1-3-5", null, "Tradición", "Un día realista: 1 grande, 3 medianas, 5 chicas",
        "Cada día elegís 1 tarea grande, 3 medianas y 5 chicas. Nada más.",
        "Pone un límite a la lista: si querés agregar algo, tenés que sacar otra cosa. Así el día deja de ser imposible antes de empezar.",
        "Sin estudios propios. Ayuda contra la «falacia de la planificación»: solemos subestimar cuánto llevan las tareas.",
        listOf("Elegí la tarea grande del día.", "Sumá 3 medianas y 5 chicas.", "Tachá a medida que avanzás."),
        MethodAction.UNO35
    ),
    Method(
        "vaciar", "Vaciar la mente", null, "Mixta", "Sacar todo de la cabeza y decidir qué hacer con cada cosa",
        "Anotás todo lo que te da vueltas, sin ordenar. Después lo procesás de a una cosa: ¿lo hago ya, es para hoy, va al tablero, o es para algún día?",
        "Lo pendiente sigue ocupando la cabeza aunque no lo estés haciendo. Anotarlo con un plan concreto la libera.",
        "En varios experimentos, las metas sin terminar generaron pensamientos intrusivos y peor rendimiento en otras tareas; armar un plan específico para ellas eliminó esa interferencia, aun sin haberlas cumplido (Masicampo y Baumeister, 2011). La versión completa viene de «Getting Things Done» (David Allen), sin estudios propios.",
        listOf("Anotá todo lo que tengas en la cabeza.", "Tomá cada cosa y decidí: ya, hoy, tablero, algún día o borrar.", "Cuando la lista queda vacía, listo."),
        MethodAction.VACIAR
    ),
    Method(
        "pasos", "Dividir en pasos", null, "Tradición", "Que el próximo paso sea obvio y corto",
        "Una tarea grande y vaga («ordenar la casa») se transforma en pasos concretos de menos de 10 minutos («vaciar la mesa del living»).",
        "Lo que paraliza suele ser no saber por dónde empezar. Un primer paso físico y corto baja la resistencia.",
        "Sin estudios del método en sí. Las metas concretas y específicas funcionan mejor que las vagas en la investigación sobre metas e intenciones.",
        listOf("Escribí la tarea grande.", "Preguntate: ¿cuál es la primera acción física?", "Que cada paso dure menos de 10 minutos.", "Empezá el primero ahora."),
        MethodAction.PASOS
    ),
    Method(
        "bloques", "Bloques de tiempo", null, "Mixta", "Darle a cada tarea su lugar en el día",
        "En vez de una lista suelta, le das a cada tarea un horario: «de 10 a 10:45, informe». Tranqui te avisa cuando empieza cada bloque.",
        "Una tarea con hora deja de competir con todo lo demás. Además ves si el día entra o no.",
        "Un metaanálisis de 158 estudios encontró que manejar el tiempo se relaciona moderadamente con el rendimiento y más todavía con el bienestar (Aeon, Faber y Panaccio, 2021). No compara técnicas puntuales: los bloques son una forma de planificar.",
        listOf("Elegí 2 o 3 cosas importantes.", "Dale a cada una un horario y una duración.", "Sumá un margen: casi todo lleva más de lo que pensamos.", "Cuando suena el aviso, empezá."),
        MethodAction.BLOQUES
    ),
    Method(
        "perdon", "Perdonarte y volver", null, "Mixta", "Soltar la culpa para poder retomar",
        "Cuando postergaste algo, reconocés lo que pasó sin castigarte, te hablás como a un amigo y elegís un paso chico para volver.",
        "La culpa hace que la tarea se sienta todavía peor, y eso empuja a seguir evitándola. Soltarla saca ese peso.",
        "En 119 estudiantes, los que más habían postergado y se perdonaron por eso postergaron menos antes del siguiente examen, en parte porque sintieron menos emociones negativas (Wohl, Pychyl y Bennett, 2010). Es un estudio observacional, no un ensayo.",
        listOf("Nombrá lo que pasó, sin adornarlo.", "Decite algo amable, como a un amigo.", "Elegí un paso de 2 minutos y hacelo ahora."),
        MethodAction.PERDON
    ),
    Method(
        "plazo", "Tu propia fecha límite", null, "Mixta", "Ponerle fecha a lo que no la tiene",
        "Lo importante sin fecha se posterga para siempre. Le ponés una fecha concreta y Tranqui te avisa el día anterior y el mismo día.",
        "Una fecha transforma «algún día» en «esta semana», y le da a lo importante la urgencia que le falta.",
        "Un estudio clásico encontró que la gente se pone plazos a sí misma y que eso ayuda en parte (Ariely y Wertenbroch, 2002), pero una réplica de 2025 no encontró el mismo efecto. Es útil como recordatorio, no como garantía.",
        listOf("Elegí algo importante sin fecha.", "Poné una fecha realista.", "Si es grande, dividilo en pasos con sus propias fechas."),
        MethodAction.PLAZO
    ),
    Method(
        "cierre", "Cierre del día", null, "Tradición", "Terminar el día con la cabeza tranquila",
        "Al final del día repasás lo que hiciste, anotás lo que quedó pendiente y elegís las 3 primeras cosas de mañana. Después cerrás con una frase: «listo por hoy».",
        "Lo pendiente con plan deja de dar vueltas a la noche, y mañana arrancás sin decidir de cero.",
        "El ritual viene de Cal Newport, sin estudios propios. Se apoya en que planificar lo pendiente reduce los pensamientos intrusivos (Masicampo y Baumeister, 2011).",
        listOf("Mirá lo que hiciste hoy.", "Anotá lo que quedó pendiente.", "Elegí las 3 primeras de mañana.", "Decí «listo por hoy» y cortá."),
        MethodAction.CIERRE
    )
)

private val BASE_METHODS = listOf(
    Method(
        "sientonces", "Si pasa X, hago Y", null, "Fuerte", "Decidí de antemano cuándo y dónde",
        "Un plan concreto que une una situación con una acción: «Después de cenar, leo 5 minutos».",
        "Cuando llega la situación, la acción se dispara casi sola. No tenés que decidir en el momento, que es cuando aparece la duda.",
        "Efecto de mediano a grande (d = 0,65) en 94 pruebas independientes (Gollwitzer y Sheeran, 2006).",
        listOf("Elegí algo que ya hacés todos los días: cenar, lavarte los dientes, llegar a casa.", "Elegí la acción nueva, chica y concreta.", "Escribilo: «Después de ___, voy a ___».", "Repetilo siempre en el mismo momento."),
        MethodAction.NEW_GOAL
    ),
    Method(
        "woop", "WOOP", null, "Fuerte", "Deseo, resultado, obstáculo y plan",
        "Cuatro pasos: tu deseo, el mejor resultado, el obstáculo interno más probable y un plan para cuando aparezca.",
        "Imaginar solo el éxito relaja y frena la acción. Contrastarlo con el obstáculo real te prepara para superarlo.",
        "Efecto de chico a mediano (g = 0,34) en 24 resultados con casi 16.000 personas (Wang y otros, 2021). Puede ser algo menor por sesgo de publicación.",
        listOf("Deseo: algo importante y posible esta semana.", "Resultado: imaginá lo mejor de lograrlo.", "Obstáculo: ¿qué tuyo, por dentro, te puede frenar?", "Plan: «Si aparece ___, voy a ___»."),
        MethodAction.WOOP
    ),
    Method(
        "minimos", "Hábitos mínimos", null, "Fuerte", "Tan chico que no cueste empezar",
        "Achicar el hábito hasta que sea casi ridículo (2 páginas, 1 minuto), anclarlo a algo que ya hacés y celebrar al terminar.",
        "Lo que más cuesta es empezar. Un paso mínimo baja esa barrera, y repetido todos los días se vuelve automático.",
        "Con metas chicas que se ajustaban, la gente caminó más que con la meta fija de 10.000 pasos (Adams y otros, 2017). Un hábito tarda en promedio 66 días en volverse automático (Lally y otros, 2010).",
        listOf("Elegí el hábito.", "Achicalo hasta que no te dé pereza.", "Anclalo a algo que ya hacés.", "Al terminar, celebralo aunque sea chico."),
        MethodAction.NEW_GOAL
    ),
    Method(
        "placer", "Placer asociado", null, "Fuerte", "Algo que te gusta, solo mientras lo hacés",
        "Te permitís algo que disfrutás (un podcast, una serie, una playlist) únicamente mientras hacés la tarea que te cuesta.",
        "Las ganas de lo que te gusta te llevan a empezar lo que postergás.",
        "Al principio, la gente fue un 51% más seguido al gimnasio, aunque el efecto bajó con el tiempo (Milkman y otros, 2014).",
        listOf("Elegí algo que disfrutes mucho.", "Elegí la tarea que te cuesta.", "Permitite lo primero solo durante lo segundo.", "Anotalo en tu objetivo para que Tranqui te lo recuerde."),
        MethodAction.NEW_GOAL
    ),
    Method(
        "pomodoro", "Pomodoro", null, "Mixta", "Bloques de foco con pausas",
        "Trabajar en un bloque de tiempo sin interrupciones y después tomar una pausa corta.",
        "Un final a la vista hace más fácil empezar y sostener la atención.",
        "Mixta: las pausas fijas mejoraron el ánimo en un estudio (Biwer y otros, 2023), pero en otro el bloque 25/5 cansó más rápido que las pausas libres (Smits y otros, 2025). Por eso podés elegir la duración.",
        listOf("Elegí una sola tarea.", "Elegí el bloque: 15 o 25 minutos.", "No cambies de tarea hasta que termine.", "Tomá una pausa de 5 minutos, mejor si te movés."),
        MethodAction.POMODORO
    ),
    Method(
        "micro", "Micro-pausas activas", null, "Fuerte", "Pausas cortas, mejor si te movés",
        "Pausas de hasta 10 minutos durante el día: estirar, caminar, mirar lejos.",
        "Recuperan energía sin cortar el ritmo del día.",
        "Subieron la energía (d = 0,36) y bajaron la fatiga en un metaanálisis; las pausas físicas fueron las mejores (Albulescu y otros, 2022).",
        listOf("Cada tanto, pará un minuto.", "Movete: estirá, caminá, mirá lejos.", "Volvé a lo tuyo."),
        MethodAction.MICRO
    ),
    Method(
        "kaizen", "Kaizen", "改善 · buen cambio", "Tradición", "Mejorar un poquito cada día",
        "Una filosofía japonesa de mejora continua: cambios mínimos y constantes en vez de cambios bruscos.",
        "Un cambio tan chico no despierta resistencia, y sumado día a día se nota.",
        "Tradición: no hay estudios del método como técnica personal, pero coincide con lo que se sabe de los hábitos mínimos.",
        listOf("Elegí la tarea que estás evitando.", "Hacé solo 1 minuto.", "Si querés, seguí. Si no, ya cumpliste.", "Mañana, un poquito más."),
        MethodAction.KAIZEN
    ),
    Method(
        "kanban", "Kanban", "看板 · tablero", "Tradición", "Por hacer, haciendo, hecho",
        "Un tablero visual nacido en las fábricas de Toyota: las tareas pasan de «Por hacer» a «Haciendo» y a «Hecho».",
        "Ver todo ordenado baja la sensación de caos, y limitar lo que hacés a la vez evita dispersarte.",
        "Tradición: viene de la industria; no hay estudios como técnica personal.",
        listOf("Anotá tus tareas en «Por hacer».", "Pasá como máximo 3 a «Haciendo».", "Terminá una antes de empezar otra.", "Movela a «Hecho»."),
        MethodAction.KANBAN
    ),
    Method(
        "hansei", "Hansei", "反省 · reflexión", "Tradición", "Revisar la semana sin culparse",
        "Mirar con honestidad qué salió y qué no, para mejorar, sin castigarte.",
        "Aprender del tropiezo en vez de abandonarlo todo.",
        "Tradición: coincide con la revisión semanal y con la autocompasión, que se asoció con menos procrastinación (Wohl y otros, 2010).",
        listOf("¿Qué salió bien esta semana?", "¿Qué te frenó?", "¿Qué vas a cambiar la próxima?"),
        MethodAction.HANSEI
    ),
    Method(
        "wabisabi", "Wabi-sabi", "侘寂", "Tradición", "Hecho es mejor que perfecto",
        "Aceptar lo imperfecto e incompleto como parte de lo bello.",
        "El perfeccionismo es una causa común de postergar: si no puede salir perfecto, no empezás.",
        "Tradición: coincide con la autocompasión. Perdonarse un fallo se asoció con menos procrastinación después (Wohl y otros, 2010).",
        listOf("Hacé una versión imperfecta.", "Si fallás un día, no pasa nada: volvé al siguiente.", "Nunca dos días seguidos."),
        null
    ),
    Method(
        "ikigai", "Ikigai", "生き甲斐 · razón de ser", "Tradición", "Lo que te mueve",
        "Cruzar lo que te gusta, lo que hacés bien, lo que el mundo necesita y lo que te pueden pagar.",
        "Un objetivo con sentido cuesta menos de sostener.",
        "Tradición: es una idea útil para elegir objetivos, no una técnica probada.",
        listOf("¿Qué te gusta hacer?", "¿En qué sos bueno?", "¿Qué necesita el mundo?", "¿Qué te pueden pagar?"),
        MethodAction.IKIGAI
    ),
    Method(
        "sapo", "Comerse el sapo", null, "Sin estudios", "Lo más difícil, primero",
        "Hacer primero la tarea que más evitás.",
        "Después de hacerla, el resto del día pesa menos.",
        "Sin estudios: es un consejo popular, sin evidencia propia.",
        listOf("A la mañana, elegí la tarea que más evitás.", "Hacela antes que todo lo demás.", "Si es grande, empezá por 2 minutos."),
        MethodAction.SAPO
    ),
    Method(
        "dosmin", "Regla de los 2 minutos", null, "Sin estudios", "Si lleva poco, hacelo ya",
        "Si algo lleva menos de 2 minutos, hacelo en el momento (David Allen).",
        "Evita que las tareas chicas se acumulen.",
        "Sin estudios, y ojo: puede volverse una excusa para hacer cosas fáciles y evitar la importante.",
        listOf("Si algo lleva menos de 2 minutos, hacelo ya.", "No la uses para escapar de la tarea importante."),
        null
    )
)

/**
 * Una fase de una respiración guiada. [say] es lo que dice la voz (null = no dice nada,
 * solo cambia el círculo). [label] es lo que se lee en pantalla.
 */
data class Phase(val label: String, val seconds: Float, val scale: Float, val say: String? = label)

data class Practice(
    val id: String, val name: String, val durationLabel: String, val tag: String,
    val que: String, val evidencia: String, val pasos: List<String>,
    val options: List<Int>, val defaultMinutes: Int,
    val phases: List<Phase> = emptyList(), val steps: List<String> = emptyList()
)

/** Frases fijas de la voz: inicio y cierre de cada práctica. */
const val VOZ_INICIO_RESPIRAR = "Vamos a respirar juntos. Seguí el ritmo del círculo y de mi voz."
const val VOZ_INICIO_GUIA = "Acomodate. Empezamos en un momento."
const val VOZ_SEGUI = "Seguí así, a tu ritmo."
const val VOZ_CIERRE = "Muy bien. Quedate un momento así… y cuando quieras, volvé despacio."

val PRACTICES = listOf(
    Practice(
        "suspiro", "Suspiro cíclico", "1 a 5 min", "Fuerte",
        "Dos inhalaciones por la nariz y una exhalación larga por la boca.",
        "5 minutos por día mejoraron el ánimo y bajaron la frecuencia respiratoria más que la meditación, en un ensayo de un mes (Balban y otros, 2023).",
        listOf("Sentate o acostate cómodo.", "Inhalá por la nariz hasta llenar los pulmones.", "Sin soltar el aire, sumá una inhalación corta.", "Soltá el aire despacio por la boca, más largo que la inhalación.", "Repetí."),
        listOf(1, 3, 5), 3,
        // La segunda inhalación corta no se nombra: el círculo crece un poco y alcanza.
        phases = listOf(
            Phase("Inhalá por la nariz", 2f, 1.0f, "Inhalá por la nariz"),
            Phase("Inhalá por la nariz", 1f, 1.12f, null),
            Phase("Soltá despacio por la boca", 6f, 0.62f, "Soltá despacio")
        )
    ),
    Practice(
        "caja", "Respiración en caja", "2 a 5 min", "Fuerte",
        "Inhalar, sostener, exhalar y sostener, 4 segundos cada uno.",
        "También mejoró el ánimo en el mismo ensayo, algo menos que el suspiro cíclico (Balban y otros, 2023).",
        listOf("Inhalá 4 segundos.", "Sostené 4.", "Exhalá 4.", "Sostené 4.", "Repetí."),
        listOf(2, 3, 5), 3,
        phases = listOf(
            Phase("Inhalá", 4f, 1.1f, "Inhalá"),
            Phase("Sostené", 4f, 1.1f, "Sostené"),
            Phase("Soltá", 4f, 0.62f, "Soltá"),
            Phase("Sostené", 4f, 0.62f, "Sostené")
        )
    ),
    Practice(
        "meditacion", "Meditación guiada", "3, 5 o 10 min", "Fuerte",
        "Atención amable a la respiración y al cuerpo.",
        "En 45 ensayos, las apps de meditación bajaron un poco la ansiedad (g = 0,28) y la depresión (g = 0,24) (Linardon y otros, 2024). No reemplaza un tratamiento.",
        listOf("Sentate cómodo.", "Escuchá la voz; si preferís, también podés leer las frases.", "Si te distraés, está bien: volvé a la respiración."),
        listOf(3, 5, 10), 5,
        steps = listOf(
            "Acomodate en una posición cómoda. Si querés, cerrá los ojos.",
            "Dejá que la respiración siga su ritmo natural. No hace falta cambiar nada.",
            "Notá dónde sentís el aire: en la nariz, en el pecho o en la panza.",
            "Si aparece un pensamiento, está bien. Dejalo pasar, como una nube, y volvé a la respiración.",
            "Ahora llevá la atención al cuerpo entero, despacio, de la cabeza a los pies.",
            "Aflojá la frente, la mandíbula y los hombros.",
            "Con cada exhalación, dejá que el cuerpo se afloje.",
            "Tratate con la misma amabilidad con la que tratarías a alguien que querés.",
            "Poco a poco, volvé a notar los sonidos de alrededor. Cuando quieras, abrí los ojos."
        )
    ),
    Practice(
        "relajacion", "Relajación muscular", "5 o 10 min", "Fuerte",
        "Tensar suavemente y soltar cada grupo de músculos, de los pies a la cara.",
        "Las técnicas de relajación tuvieron un efecto mediano sobre la ansiedad (d = 0,51) en 27 estudios (Manzoni y otros, 2008). Ideal antes de dormir.",
        listOf("Acostate o sentate cómodo.", "Tensá cada zona con suavidad unos segundos, sin forzar.", "Soltá y notá la diferencia."),
        listOf(5, 10), 10,
        // «…» marca una pausa de unos segundos: primero tensás, después la voz dice cuándo soltar.
        steps = listOf(
            "Acostate o sentate cómodo. Respirá lento, sin apuro.",
            "Llevá la atención a los pies. Apretá suavemente los dedos… y soltá. Notá cómo se aflojan.",
            "Ahora las pantorrillas. Tensalas un poco… y soltá.",
            "Los muslos. Apretalos con suavidad… y soltá.",
            "La panza. Tensala un momento… y soltá, dejando salir el aire.",
            "Las manos. Cerrá los puños… y abrilas despacio.",
            "Los brazos. Tensalos… y dejalos caer, flojos.",
            "Los hombros. Subilos hacia las orejas… y dejalos bajar.",
            "La cara. Apretá suavemente los ojos y la mandíbula… y soltá.",
            "Respirá lento y sentí el cuerpo entero, pesado y tranquilo."
        )
    )
)
