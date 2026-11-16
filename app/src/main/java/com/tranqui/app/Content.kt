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
    ESTUDIO, EISENHOWER, IVY, UNO35, DOSMIN, VACIAR, PASOS, BLOQUES, PERDON, PLAZO, CIERRE, PLACER
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
    "estudio" to "foco", "minimos" to "arrancar", "kaizen" to "arrancar", "dosmin" to "arrancar", "sapo" to "arrancar",
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

/**
 * Para qué lo querés usar. Cada categoría recomienda UN método (el mejor para eso, por
 * evidencia o por ser el que más directo lleva a la acción) y lista otros que ayudan.
 */
data class Situacion(
    val id: String, val title: String, val ejemplos: String, val best: String,
    val porQue: String, val combinar: String, val otros: List<String>
)

val SITUACIONES = listOf(
    Situacion(
        "estudiar", "Estudiar", "Un examen, un curso, un idioma, aprender algo nuevo",
        "estudio",
        "Es la técnica con más evidencia para recordar lo que estudiás: preguntarte en vez de releer, y repasar con días de por medio.",
        "Estudiá en Pomodoros de 25 minutos y, al final de cada uno, pasá lo importante a preguntas.",
        listOf("pomodoro", "sientonces", "micro", "pasos")
    ),
    Situacion(
        "organizar", "Organizar algo concreto", "Un viaje, una mudanza, un evento, un trámite",
        "vaciar",
        "Primero sacás de la cabeza todo lo que hay que hacer, y después la app te hace decidir qué pasa con cada cosa: lo grande se divide en pasos y lo que tiene fecha recibe aviso.",
        "Lo que tiene día (pasajes, reservas) ponelo en «Tu propia fecha límite».",
        listOf("pasos", "plazo", "kanban", "bloques")
    ),
    Situacion(
        "dia", "Trabajo y el día a día", "Cuando tenés mucho para hoy o esta semana",
        "ivylee",
        "Decidís el orden de antemano y hacés una sola cosa por vez hasta terminarla. Es simple y saca la duda de «¿con qué sigo?».",
        "Si no sabés qué va primero, pasá antes por «Urgente o importante». Al final del día, hacé el cierre.",
        listOf("eisenhower", "unotrescinco", "bloques", "pomodoro", "dosmin", "cierre")
    ),
    Situacion(
        "arrancar", "Cuando no podés arrancar", "Esa tarea que venís pateando hace días",
        "kaizen",
        "Empezar es lo más difícil. Un minuto es tan poco que no da para negarse, y casi siempre seguís después.",
        "Si ya te sentís mal por haberlo postergado, empezá por «Perdonarte y volver».",
        listOf("perdon", "pasos", "sapo", "placer", "dosmin")
    ),
    Situacion(
        "metas", "Metas a futuro", "Algo que querés lograr en meses o años",
        "woop",
        "Imaginar el resultado y también el obstáculo, con un plan para cuando aparezca, funciona mejor que solo pensar en positivo. Tiene ensayos a favor.",
        "Cuando tengas la meta, dividila en pasos y ponele fecha al primero.",
        listOf("ikigai", "pasos", "plazo", "kanban")
    ),
    Situacion(
        "habitos", "Hábitos y desarrollo personal", "Hacer ejercicio, leer, meditar, cuidarte",
        "sientonces",
        "Es de los métodos con más evidencia: atar el hábito a un momento concreto («después de cenar, leo») hace que se dispare solo.",
        "Empezá con una versión mínima (Hábitos mínimos) y sumale algo que te guste mientras lo hacés.",
        listOf("minimos", "placer", "kaizen", "wabisabi")
    ),
    Situacion(
        "revisar", "Revisar cómo te fue", "Al final del día o de la semana",
        "hansei",
        "Mirar qué funcionó y qué no, sin culparte, te deja ajustar el plan en vez de abandonarlo.",
        "Hacé el cierre cada día y el Hansei una vez por semana.",
        listOf("cierre", "perdon", "kaizen")
    )
)

private val NEW_METHODS = listOf(
    Method(
        "estudio", "Preguntas y repasos", null, "Fuerte", "Preguntarte en vez de releer, con repasos espaciados",
        "Después de estudiar un tema, lo pasás a preguntas. La app te las va tomando: hoy, mañana, en 3 días, en una semana… Cada vez que te sale, el siguiente repaso queda más lejos.",
        "Tratar de recordar fortalece el recuerdo mucho más que volver a leer. Y repasar con días de por medio rinde más que estudiar todo junto el día antes.",
        "Una revisión de 10 técnicas de estudio calificó como de «alta utilidad» solo dos: autoevaluarse y espaciar los repasos; releer y subrayar quedaron como de baja utilidad (Dunlosky y otros, 2013). Un metaanálisis de 118 artículos encontró que hacerse pruebas de práctica rinde más que volver a estudiar, en laboratorio y en clase (Adesope y otros, 2017).",
        listOf("Estudiá un tema (por ejemplo, un Pomodoro).", "Escribí 3 a 5 preguntas con su respuesta.", "Cada día, abrí «Repasar hoy»: pensá la respuesta antes de verla.", "Marcá si te salió o no; la app decide cuándo volver a preguntarte."),
        MethodAction.ESTUDIO
    ),
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
    val phases: List<Phase> = emptyList(), val steps: List<String> = emptyList(),
    /** Ritmo en pocas palabras, por ejemplo «4 · 6 segundos». */
    val ritmo: String = "",
    /** Si no está vacío, el ritmo cambia por tramos a lo largo de la práctica (se alarga la exhalación). */
    val stages: List<List<Phase>> = emptyList(),
    val cuidado: String? = null,
    /** Frase final propia (por ejemplo, para dormir no se dice «volvé»). Vacío = cierre común. */
    val cierre: String = ""
) {
    /** Las fases que tocan según cuánto de la práctica ya pasó (0 a 1). */
    fun phasesAt(fraction: Float): List<Phase> =
        if (stages.isEmpty()) phases else stages[(fraction * stages.size).toInt().coerceIn(0, stages.lastIndex)]

    val allPhases: List<Phase> get() = phases + stages.flatten()
}

/** Para qué querés respirar. Cada grupo recomienda UNA práctica y muestra otras que ayudan. */
data class PracticeGroup(
    val id: String, val title: String, val cuando: String, val best: String, val porQue: String, val otros: List<String>
)

val PRACTICE_GROUPS = listOf(
    PracticeGroup(
        "ansiedad", "Bajar la ansiedad ahora", "Nervios, enojo, un mal momento",
        "suspiro",
        "Fue la que más mejoró el ánimo en un ensayo que comparó varias respiraciones con meditación. La exhalación larga es lo que más calma, y con 1 o 2 suspiros ya se nota.",
        listOf("dificil", "exhala", "zumbido", "caja")
    ),
    PracticeGroup(
        "panico", "Si sentís pánico o te falta el aire", "El corazón acelerado, sensación de ahogo",
        "suave",
        "En el pánico solemos respirar de más, y eso da mareo y sensación de ahogo. Lo que ayuda no es tomar aire profundo: es respirar más lento y más suave, con poco aire.",
        listOf("diafragma", "exhala")
    ),
    PracticeGroup(
        "dormir", "Para dormir", "En la cama, antes de apagar la luz",
        "dormir",
        "Respirar lento unos 20 minutos antes de dormir acortó el tiempo para dormirse en un estudio con personas con insomnio. Esta va alargando la exhalación de a poco, sin que tengas que contar.",
        listOf("escaneo", "478", "relajacion", "zumbido")
    ),
    PracticeGroup(
        "estres", "Estrés del día a día", "Un hábito de 5 a 10 minutos",
        "coherente",
        "Respirar unas 6 veces por minuto es lo que más estudios tiene sobre el corazón y el sistema que calma el cuerpo. Un ensayo grande no la encontró mejor que respirar lento con atención: lo que más importa es hacerla seguido.",
        listOf("suspiro", "atencion", "fosas", "meditacion")
    ),
    PracticeGroup(
        "foco", "Antes de algo importante", "Un examen, una entrevista, hablar en público",
        "caja",
        "Contar 4-4-4-4 te ocupa la cabeza y baja el ritmo sin dejarte somnoliento. Tiene menos estudios que otras, pero es fácil de hacer en cualquier lado y sin que nadie lo note.",
        listOf("diafragma", "fosas", "exhala")
    ),
    PracticeGroup(
        "cuerpo", "Soltar el cuerpo y la mente", "Tensión, contracturas, la cabeza a mil",
        "relajacion",
        "Tensar y soltar los músculos tiene un efecto mediano sobre la ansiedad en muchos estudios, y se siente en el cuerpo enseguida.",
        listOf("meditacion", "zumbido", "dormir")
    ),
    PracticeGroup(
        "meditar", "Meditar", "Atención plena, para hacer seguido",
        "atencion",
        "Es la práctica de base: respiración, cuerpo y sonidos. Las otras son variaciones para momentos puntuales: lo que pesa, el sueño, el cariño.",
        listOf("dificil", "benevolencia", "escaneo", "meditacion")
    )
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
        ),
        ritmo = "2 + 1 · 6 segundos"
    ),
    Practice(
        "caja", "Respiración en caja", "2 a 5 min", "Mixta",
        "Inhalar, sostener, exhalar y sostener, 4 segundos cada uno.",
        "Mejoró el ánimo en un ensayo de un mes, algo menos que el suspiro cíclico (Balban y otros, 2023). En otro estudio con 84 estudiantes, respirar 6 veces por minuto subió más la variabilidad cardíaca que la caja o el 4-7-8 (Marchant y otros, 2025).",
        listOf("Inhalá 4 segundos.", "Sostené 4.", "Exhalá 4.", "Sostené 4.", "Repetí."),
        listOf(2, 3, 5), 3,
        phases = listOf(
            Phase("Inhalá", 4f, 1.1f, "Inhalá"),
            Phase("Sostené", 4f, 1.1f, "Sostené"),
            Phase("Soltá", 4f, 0.62f, "Soltá"),
            Phase("Sostené", 4f, 0.62f, "Sostené")
        ),
        ritmo = "4 · 4 · 4 · 4 segundos"
    ),
    Practice(
        "exhala", "Exhalación larga", "2 a 10 min", "Fuerte",
        "Inhalás 4 segundos y soltás en 6: unas 6 respiraciones por minuto, con la salida más larga que la entrada.",
        "Respirar lento sube la variabilidad cardíaca (señal de que el cuerpo se calma) durante y después de la práctica, en un metaanálisis de 223 estudios (Laborde y otros, 2022). Alargar la exhalación sola también la sube (Bae y otros, 2021).",
        listOf("Inhalá por la nariz contando 4.", "Soltá por la nariz o la boca contando 6, sin apurar.", "Que no cueste: si te falta el aire, probá 3 y 5."),
        listOf(2, 5, 10), 5,
        phases = listOf(
            Phase("Inhalá", 4f, 1.1f, "Inhalá"),
            Phase("Soltá despacio", 6f, 0.62f, "Soltá despacio")
        ),
        ritmo = "4 · 6 segundos"
    ),
    Practice(
        "coherente", "Respiración coherente", "5 a 20 min", "Mixta",
        "Inhalar y exhalar 5,5 segundos cada uno, suave y por la nariz: unas 5,5 respiraciones por minuto.",
        "Es el ritmo con más estudios sobre la variabilidad cardíaca (Laborde y otros, 2022). Pero en el ensayo más grande hasta ahora (400 personas, un mes), no bajó el estrés más que respirar a ritmo normal con la misma guía: los dos grupos mejoraron igual (Fincham y otros, 2023).",
        listOf("Sentate con la espalda cómoda.", "Inhalá por la nariz mientras el círculo crece.", "Soltá por la nariz mientras se achica, igual de largo.", "Que sea suave: no hace falta llenar los pulmones."),
        listOf(5, 10, 20), 10,
        phases = listOf(
            Phase("Inhalá", 5.5f, 1.1f, "Inhalá"),
            Phase("Soltá", 5.5f, 0.62f, "Soltá")
        ),
        ritmo = "5,5 · 5,5 segundos"
    ),
    Practice(
        "dormir", "Respiración para dormir", "10 o 20 min", "Mixta",
        "Empieza con un ritmo parejo y la exhalación se va alargando sola: 4 y 4, después 4 y 6, y al final 4 y 8.",
        "Veinte minutos de respiración lenta antes de dormir acortaron el tiempo para dormirse y redujeron los despertares en personas con insomnio (Tsai y otros, 2015). Fueron 14 personas: es una pista, no una prueba. No reemplaza un tratamiento del insomnio.",
        listOf("Acostate y apagá la luz; podés poner la pantalla boca abajo.", "Seguí la voz o la vibración.", "Si te dormís en el medio, mejor."),
        listOf(10, 20), 20,
        stages = listOf(
            listOf(Phase("Inhalá", 4f, 1.05f, "Inhalá"), Phase("Soltá", 4f, 0.7f, "Soltá")),
            listOf(Phase("Inhalá", 4f, 1.05f, "Inhalá"), Phase("Soltá despacio", 6f, 0.66f, "Soltá despacio")),
            listOf(Phase("Inhalá", 4f, 1.05f, "Inhalá"), Phase("Soltá muy despacio", 8f, 0.62f, "Soltá muy despacio"))
        ),
        ritmo = "4 · 4 → 4 · 6 → 4 · 8 segundos"
    ),
    Practice(
        "478", "Respiración 4-7-8", "2 a 5 min", "Tradición",
        "Inhalás 4 segundos por la nariz, sostenés 7 y soltás 8 por la boca. Popular para dormir.",
        "Tiene pocos estudios y chicos (Vierra y otros, 2022). En una comparación directa subió menos la variabilidad cardíaca que respirar 6 veces por minuto (Marchant y otros, 2025). Si te gusta y te ayuda, usala; si no, la «Respiración para dormir» tiene más apoyo.",
        listOf("Inhalá por la nariz contando 4.", "Sostené el aire contando 7.", "Soltá por la boca contando 8, con un soplido suave."),
        listOf(2, 3, 5), 3,
        phases = listOf(
            Phase("Inhalá por la nariz", 4f, 1.1f, "Inhalá por la nariz"),
            Phase("Sostené", 7f, 1.1f, "Sostené"),
            Phase("Soltá por la boca", 8f, 0.62f, "Soltá por la boca")
        ),
        ritmo = "4 · 7 · 8 segundos",
        cuidado = "Sostener 7 segundos puede costar. Si te mareás o te falta el aire, volvé a respirar normal."
    ),
    Practice(
        "suave", "Respiración suave", "2 a 5 min", "Mixta",
        "Inhalás poquito aire por la nariz y lo soltás lento. Menos aire, no más.",
        "En el pánico se suele respirar de más. Un tratamiento que enseña a respirar más lento y, sobre todo, más superficial redujo el pánico y la hiperventilación en ensayos con personas con trastorno de pánico (Meuret y otros, 2010). Ellos usaban un aparato que mide el aire; esta es una versión simple, sin aparato.",
        listOf("Cerrá la boca y respirá por la nariz.", "Tomá poquito aire, como si olieras una flor de lejos.", "Soltalo despacio.", "Las sensaciones raras pasan solas: no son peligrosas."),
        listOf(2, 3, 5), 3,
        phases = listOf(
            Phase("Poquito aire por la nariz", 3f, 0.9f, "Poquito aire por la nariz"),
            Phase("Soltá lento", 5f, 0.66f, "Soltá lento")
        ),
        ritmo = "3 · 5 segundos, poco aire",
        cuidado = "Si es la primera vez que te pasa, tenés dolor en el pecho o no mejora, pedí ayuda médica."
    ),
    Practice(
        "zumbido", "Zumbido de abeja", "3 a 10 min", "Mixta",
        "Inhalás por la nariz y al soltar hacés un «mmm» suave con la boca cerrada, como una abeja. Se llama bhramari, del yoga.",
        "El zumbido alarga la exhalación casi sin esfuerzo. En estudios chicos se asoció con menos estrés en la variabilidad cardíaca (Trivedi y otros, 2023) y con algo menos de ansiedad que respirar lento sin zumbar. Falta evidencia de calidad.",
        listOf("Sentate y cerrá la boca.", "Inhalá por la nariz.", "Soltá haciendo «mmm», suave y grave, hasta que se termine el aire.", "Si querés, tapate las orejas con los dedos para sentir la vibración."),
        listOf(3, 5, 10), 5,
        phases = listOf(
            Phase("Inhalá por la nariz", 4f, 1.1f, "Inhalá por la nariz"),
            Phase("Soltá zumbando: mmm", 8f, 0.62f, "Soltá zumbando")
        ),
        ritmo = "4 · 8 segundos, zumbando"
    ),
    Practice(
        "fosas", "Respiración alterna", "3 a 10 min", "Mixta",
        "Respirás por una fosa nasal por vez, tapando la otra con el dedo. Se llama nadi shodhana, del yoga.",
        "En 6 ensayos con 525 personas bajó la presión arterial, pero los estudios dieron resultados muy distintos entre sí (Nam y otros, 2024). Obliga a prestar atención, por eso también sirve para enfocarse.",
        listOf("Con el pulgar derecho tapá la fosa derecha.", "Inhalá por la izquierda.", "Tapá la izquierda con el anular y soltá por la derecha.", "Inhalá por la derecha, tapala y soltá por la izquierda. Esa es una vuelta."),
        listOf(3, 5, 10), 5,
        phases = listOf(
            Phase("Inhalá por la izquierda", 4f, 1.1f, "Inhalá por la izquierda"),
            Phase("Soltá por la derecha", 6f, 0.62f, "Soltá por la derecha"),
            Phase("Inhalá por la derecha", 4f, 1.1f, "Inhalá por la derecha"),
            Phase("Soltá por la izquierda", 6f, 0.62f, "Soltá por la izquierda")
        ),
        ritmo = "4 · 6 segundos, alternando"
    ),
    Practice(
        "diafragma", "Respiración con la panza", "3 a 10 min", "Mixta",
        "Una mano en el pecho y otra en la panza: al inhalar sube la panza, el pecho casi no se mueve.",
        "En un ensayo de 8 semanas, practicar respiración diafragmática lenta mejoró la atención sostenida y bajó el cortisol (la hormona del estrés) comparado con no practicar (Ma y otros, 2017). Fueron 40 personas.",
        listOf("Apoyá una mano en el pecho y otra en la panza.", "Inhalá por la nariz llevando el aire a la panza.", "Soltá despacio y sentí cómo baja.", "El pecho casi quieto."),
        listOf(3, 5, 10), 5,
        phases = listOf(
            Phase("Inhalá a la panza", 5f, 1.1f, "Inhalá a la panza"),
            Phase("Soltá despacio", 7f, 0.62f, "Soltá despacio")
        ),
        ritmo = "5 · 7 segundos"
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
        "atencion", "Respiración, cuerpo y sonidos", "5, 10 o 15 min", "Fuerte",
        "La práctica básica de atención plena: primero la respiración, después el cuerpo, después los sonidos.",
        "Es la forma más estudiada de meditación. En 45 ensayos, las apps de meditación bajaron un poco la ansiedad y la depresión (Linardon y otros, 2024). No reemplaza un tratamiento.",
        listOf("Sentate con la espalda derecha, sin rigidez.", "Seguí la voz o leé las frases.", "Distraerte es parte de la práctica: cada vez que volvés, entrenás."),
        listOf(5, 10, 15), 10,
        steps = listOf(
            "Buscá una postura cómoda, con la espalda derecha pero sin rigidez… Si querés, cerrá los ojos, o dejá la mirada apoyada en un punto del piso.",
            "Notá el peso de tu cuerpo. Qué partes se apoyan en la silla, en el piso, en el almohadón.",
            "Ahora llevá la atención a la respiración. No hace falta cambiarla… Solo notá cómo entra… y cómo sale.",
            "Elegí un lugar donde la sientas con claridad: la nariz, el pecho o la panza. Quedate ahí un rato.",
            "Es normal que la mente se vaya. Darte cuenta ya es atención… Volvé a la respiración, sin retarte.",
            "Ahora abrí la atención a todo el cuerpo. Calor, frío, hormigueo, presión… lo que aparezca.",
            "Si encontrás una zona tensa, no hace falta arreglarla. Respirá hacia ahí y dejala estar como está.",
            "Ahora dejá que lleguen los sonidos. Los de cerca… y los de lejos.",
            "No hace falta ponerles nombre ni decidir si te gustan. Escuchá cómo aparecen y cómo se van.",
            "Por último, dejá que todo esté a la vez: la respiración, el cuerpo y los sonidos.",
            "Antes de terminar, agradecete este rato que te diste."
        )
    ),
    Practice(
        "dificil", "Estar con lo que pesa", "8 o 12 min", "Mixta",
        "Para cuando algo te preocupa o te angustia: reconocerlo, sentirlo en el cuerpo, hacerle lugar y tratarte con amabilidad.",
        "Se inspira en las prácticas de autocompasión. En 27 ensayos, esas prácticas redujeron moderadamente la ansiedad, la depresión y el estrés (Ferrari y otros, 2019). Esta versión guiada no fue estudiada por sí sola.",
        listOf("Elegí algo que te preocupe, pero no lo más pesado de tu vida: algo mediano alcanza.", "Si en algún momento es demasiado, abrí los ojos y volvé a la respiración."),
        listOf(8, 12), 8,
        steps = listOf(
            "Acomodate y tomá un par de respiraciones un poco más largas que lo normal.",
            "Traé a la mente algo que te esté preocupando. Que no sea lo más difícil de tu vida: algo mediano alcanza.",
            "Primero, reconocelo. Ponele un nombre simple: «preocupación», «miedo», «enojo»… Decilo despacio, para adentro.",
            "Ahora fijate dónde lo sentís en el cuerpo. Puede ser el pecho, el estómago, la garganta… Buscá el lugar donde más se nota.",
            "Dejalo estar. No hace falta que se vaya. Solo le estás haciendo un lugar.",
            "Aflojá alrededor de esa zona, como si abrieras despacio un puño cerrado… No para que desaparezca: para que pese un poco menos.",
            "Si querés, apoyá una mano ahí, o en el pecho. Sentí el calor de tu mano.",
            "Decite algo amable, como se lo dirías a un amigo: «Esto es difícil. Es normal sentirse así. Estoy acá.»",
            "Preguntate qué necesitás en este momento. Puede ser descanso, paciencia, o que alguien te escuche.",
            "Dejá que la respuesta llegue sin forzarla… y ofrecétela, aunque sea un poco.",
            "Ahora soltá el tema y volvé a la respiración. Notá cómo está el cuerpo después de este rato."
        ),
        cuidado = "Si la angustia es muy fuerte o te dura, hablalo con alguien de confianza o con un profesional."
    ),
    Practice(
        "escaneo", "Escaneo para dormir", "10, 15 o 20 min", "Mixta",
        "Acostado, recorrés el cuerpo de los pies a la cabeza y dejás que cada parte se afloje y se ponga pesada.",
        "En 18 ensayos, la meditación de atención plena mejoró la calidad del sueño más que otras actividades que dan la misma atención y el mismo tiempo (Rusch y otros, 2019). No reemplaza un tratamiento del insomnio.",
        listOf("Acostate y apagá la luz; podés dejar la pantalla boca abajo.", "Si te dormís antes de terminar, mejor: el sonido baja solo."),
        listOf(10, 15, 20), 15,
        steps = listOf(
            "Acostate boca arriba, con los brazos al costado del cuerpo. Dejá que la cama te sostenga.",
            "Tomá tres respiraciones lentas… y con cada exhalación, aflojá un poco más.",
            "Llevá la atención a los pies. Los dedos, las plantas, los talones… Notá lo que sientas, sin cambiar nada.",
            "Subí despacio a los tobillos, las pantorrillas y las rodillas. Dejá que se pongan pesadas.",
            "Ahora los muslos y la cadera. Sentí cómo se hunden un poco en el colchón.",
            "La panza. Notá cómo sube y baja con la respiración, sola, sin esfuerzo.",
            "El pecho y la espalda. Con cada exhalación, la espalda se apoya un poco más.",
            "Los hombros. Dejalos caer, lejos de las orejas.",
            "Los brazos, hasta las manos. Los dedos sueltos, quietos.",
            "El cuello y la cara. Aflojá la mandíbula, la lengua, los ojos y la frente.",
            "Ahora sentí el cuerpo entero, pesado y tibio, de los pies a la cabeza.",
            "Si aparecen pensamientos, no pasa nada. Dejalos ir y volvé al peso del cuerpo.",
            "No hace falta hacer nada más… Podés quedarte así, y dejar que llegue el sueño."
        ),
        cierre = "Seguí descansando… no hace falta hacer nada más."
    ),
    Practice(
        "benevolencia", "Benevolencia", "8 o 12 min", "Mixta",
        "Desearte bien a vos y, de a poco, a otras personas: alguien que querés, alguien neutro, alguien difícil y, al final, todos.",
        "En 24 estudios con 1.759 personas, esta práctica aumentó las emociones positivas en el día a día (Zeng y otros, 2015). Es una práctica antigua del budismo, en una versión sin religión.",
        listOf("No hace falta sentir nada especial: alcanza con decir las frases con intención.", "Si con la persona difícil cuesta demasiado, quedate con alguien más fácil."),
        listOf(8, 12), 8,
        steps = listOf(
            "Acomodate y respirá tranquilo. Dejá que la cara se afloje, como si estuvieras por sonreír.",
            "Empezá por vos. Decite despacio, para adentro: «Que esté bien… que esté en paz… que me trate con cariño.»",
            "Si no sentís nada, está bien. Lo importante es la intención, no la emoción.",
            "Ahora pensá en alguien que querés mucho. Imaginalo frente a vos, sonriendo.",
            "Deseale lo mismo: «Que estés bien… que estés en paz… que te traten con cariño.»",
            "Ahora alguien que ves seguido pero casi no conocés: un vecino, alguien del almacén, del ómnibus.",
            "Esa persona también quiere estar bien, como vos. Deseale lo mismo: «Que estés bien… que estés en paz.»",
            "Si querés, pensá en alguien con quien tengas un roce. No el más difícil: alguien mediano.",
            "Sin justificar lo que pasó, probá desearle: «Que estés bien… que encuentres paz.» Si cuesta, volvé a vos.",
            "Ahora abrí el deseo a todos: tu barrio, tu país, el mundo entero. «Que todos estemos bien… que todos estemos en paz.»",
            "Volvé a vos, y quedate un momento con lo que haya quedado."
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
