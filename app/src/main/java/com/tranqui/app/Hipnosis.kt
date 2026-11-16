package com.tranqui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Autohipnosis con guiones propios para completar con tus palabras.
 * Estructura clásica: inducción → profundización (escalera) → sugerencias → reorientación (contar del 1 al 5).
 * Los guiones de metas usan contraste mental (meta + obstáculo + plan), no solo imaginar el éxito.
 */
data class Campo(val key: String, val label: String, val ejemplo: String)

data class HipnoGuion(
    val id: String, val title: String, val para: String,
    val campos: List<Campo>, val partes: List<String>, val dormir: Boolean = false
)

private val INDUCCION = listOf(
    "Buscá un lugar donde nadie te interrumpa por un rato. Sentate o acostate, como estés más cómodo.",
    "Elegí un punto frente a vos, un poco más arriba de los ojos, y miralo fijo… Mientras lo mirás, los párpados se van poniendo pesados.",
    "Cuando quieras, dejalos cerrar. Tomá una respiración profunda… y soltala despacio.",
    "Con cada exhalación, el cuerpo se afloja un poco más. La frente… los ojos… la mandíbula.",
    "Los hombros bajan. Los brazos se ponen pesados, sueltos, cómodos.",
    "El pecho y la panza se mueven solos, tranquilos. Las piernas descansan, hasta los pies.",
    "Todo el tiempo estás vos al mando. Podés abrir los ojos cuando quieras… Solo estás eligiendo dejarte llevar."
)

private val PROFUNDIZAR = listOf(
    "Ahora imaginá una escalera de diez escalones que baja hacia un lugar tranquilo. Puede ser de madera, de piedra, como vos quieras.",
    "Vamos a bajar despacio. Con cada número, un escalón, y un poco más de calma… Diez… nueve… ocho…",
    "Siete… seis… cinco… cada vez más hondo, más tranquilo.",
    "Cuatro… tres… dos… uno… Llegaste. Estás en un lugar de calma profunda.",
    "Desde esta calma, la mente está abierta a lo que elegís para vos."
)

private const val REORIENTAR =
    "En un momento voy a contar del uno al cinco, y con cada número vas a ir volviendo, sintiéndote bien… " +
        "Uno: empezás a notar el cuerpo… Dos: la respiración se hace un poco más profunda… " +
        "Tres: movés despacio las manos y los pies… Cuatro: notás los sonidos del lugar… " +
        "Cinco: abrí los ojos. Despierto, despejado, sintiéndote bien."

private const val DORMIR_CIERRE = "Seguí descansando… dejá que el sueño te lleve."

val HIPNO_GUIONES = listOf(
    HipnoGuion(
        "ansiedad", "Calma ante lo que te preocupa", "Para una situación que te pone nervioso: ensayarla desde la calma.",
        listOf(
            Campo("situacion", "¿Qué te pone nervioso?", "hablar en público"),
            Campo("lugar", "Un lugar donde te sentís a salvo", "la rambla al atardecer"),
            Campo("palabra", "Una palabra que te calme", "tranqui")
        ),
        listOf(
            "Ahora estás en {lugar}. Mirá los colores, la luz… escuchá los sonidos… sentí la temperatura en la piel.",
            "Acá estás a salvo. Notá cómo se siente el cuerpo cuando está así de tranquilo.",
            "Ponele una palabra a esta calma: {palabra}. Decila para adentro, despacio… {palabra}.",
            "Desde esta calma, imaginá que se acerca {situacion}. Lo ves como en una pantalla, de lejos, chiquito.",
            "Mientras lo mirás, seguís respirando tranquilo. El cuerpo sigue suelto. Vos seguís en {lugar}.",
            "Ahora imaginate entrando en esa situación con esta misma calma. Respirás despacio, los hombros sueltos, y lo hacés a tu manera.",
            "Si aparecen nervios, está bien: es energía. Respirás, decís {palabra}, y la calma vuelve.",
            "Cada vez que en tu día digas {palabra} y respires despacio, el cuerpo va a recordar esta sensación, cada vez con más facilidad."
        )
    ),
    HipnoGuion(
        "futuro", "Tu yo del futuro", "Para una meta: imaginar el logro, pero también el obstáculo y tu plan para superarlo.",
        listOf(
            Campo("meta", "¿Qué querés lograr?", "terminar la carrera"),
            Campo("cuando", "¿Para cuándo?", "dentro de un año"),
            Campo("obstaculo", "¿Qué es lo que más te frena?", "dejar todo para último momento"),
            Campo("plan", "Tu plan para ese obstáculo (si pasa X, hago Y)", "si me agarra la pereza, hago solo diez minutos"),
            Campo("paso", "El primer paso, concreto", "abrir el cuaderno mañana después de desayunar")
        ),
        listOf(
            "Imaginá un camino delante tuyo. Al final del camino está tu vida {cuando}.",
            "Caminá despacio hasta ahí. Llegás, y ya lograste {meta}.",
            "Mirá cómo es ese día. Dónde estás, qué ves, qué escuchás… cómo te sentís por dentro.",
            "Ese vos del futuro te mira con cariño. Sabe lo que le costó llegar.",
            "Preguntale qué fue lo más difícil del camino. Y escuchá: {obstaculo}.",
            "Ahora imaginá ese obstáculo con claridad, como si pasara hoy. No lo esquives: miralo de frente.",
            "Escuchá lo que te dice tu yo del futuro: {plan}. Imaginate haciéndolo, paso por paso.",
            "Repetilo para adentro: {plan}. Cuando llegue ese momento, ya vas a saber qué hacer.",
            "Antes de volver, te recuerda el primer paso: {paso}. Es chico, y alcanza.",
            "Volvé por el camino, despacio, trayendo con vos esa sensación y ese primer paso."
        )
    ),
    HipnoGuion(
        "empezar", "Arrancar lo que postergás", "Para esa tarea que venís pateando: que empezar se sienta fácil.",
        listOf(
            Campo("tarea", "¿Qué venís postergando?", "ordenar los papeles"),
            Campo("momento", "¿Cuándo la vas a empezar?", "hoy a las seis"),
            Campo("paso", "El primer paso, muy chico", "sacar los papeles del cajón"),
            Campo("alivio", "¿Cómo te vas a sentir al terminar?", "liviano y tranquilo")
        ),
        listOf(
            "Ahora pensá en {tarea}. Notá qué pasa en el cuerpo cuando la pensás. No hace falta cambiarlo.",
            "Esa incomodidad es normal. No dice nada malo de vos: es la parte de empezar, que siempre es la más dura.",
            "Imaginá que llega {momento}. Te ves levantándote tranquilo, sin pensarlo mucho.",
            "Lo primero que hacés es {paso}. Solo eso. Es tan chico que casi no cuesta.",
            "Y una vez que empezaste, seguís solo. Te ves avanzando, de a poco, a tu ritmo.",
            "Ahora imaginá el momento en que terminás. Te sentís {alivio}. Quedate un rato con esa sensación.",
            "Cada vez que llegue {momento}, tu cuerpo va a recordar lo fácil que es empezar con {paso}."
        )
    ),
    HipnoGuion(
        "confianza", "Confianza", "Para algo que te intimida: traer la seguridad que ya tuviste otras veces.",
        listOf(
            Campo("situacion", "¿Para qué querés más confianza?", "una entrevista de trabajo"),
            Campo("recuerdo", "Un momento en que algo te salió bien", "cuando aprendí a tocar la guitarra"),
            Campo("cualidad", "Una cualidad tuya", "constante")
        ),
        listOf(
            "Traé a la mente un momento en que te salió bien algo que te costaba: {recuerdo}.",
            "Volvé a ese momento como si estuvieras ahí. Qué veías, qué escuchabas, cómo te sentías.",
            "Notá en qué parte del cuerpo se siente esa seguridad. Dejá que crezca y ocupe un poco más de lugar.",
            "Esa persona que lo logró sos vos. Sos {cualidad}. Ya lo demostraste.",
            "Ahora llevá esa sensación a {situacion}. Imaginate ahí, con esa misma postura y esa misma respiración.",
            "Te ves tranquilo, con la espalda derecha y la mirada firme. Si algo sale distinto, lo resolvés.",
            "Para traer esta seguridad cuando la necesites, juntá suavemente el pulgar con el índice… Así. Cada vez que lo hagas, la sensación vuelve."
        )
    ),
    HipnoGuion(
        "habito", "Un hábito nuevo", "Para algo que querés hacer seguido: atarlo a un momento de tu día.",
        listOf(
            Campo("habito", "¿Qué hábito querés?", "leer"),
            Campo("ancla", "¿Después de qué lo vas a hacer?", "después de cenar"),
            Campo("minima", "La versión más chica posible", "leer una página")
        ),
        listOf(
            "Imaginá tu día, normal, como cualquier otro. Llega el momento: {ancla}.",
            "Te ves terminando, y casi sin pensar, empezás a {minima}.",
            "Es tan chico que no cuesta nada. Lo hacés y listo. Notá lo bien que se siente cumplir.",
            "Ahora imaginá lo mismo mañana… y pasado… y la semana que viene. {ancla}, y {minima}.",
            "Con el tiempo, {habito} se vuelve parte de vos. No porque te obligues, sino porque ya es lo que hacés.",
            "Si algún día no podés, está bien. Al otro día volvés, sin culpa. Lo importante es volver."
        )
    ),
    HipnoGuion(
        "dormir", "Dormir profundo", "Para la noche: termina sin despertarte, para que sigas durmiendo.",
        listOf(Campo("lugar", "Un lugar donde descansarías muy bien", "una cabaña en el monte, con lluvia afuera")),
        listOf(
            "Ahora imaginá {lugar}. Estás acostado, calentito, a salvo.",
            "Afuera todo sigue su curso, y no necesitás hacer nada. Por hoy, ya está.",
            "Cada respiración te lleva un poco más hondo, como hundirte despacio en una cama muy blanda.",
            "Si aparece un pensamiento, lo ves pasar como una hoja en un arroyo… y se va.",
            "El cuerpo se pone cada vez más pesado… más tibio… más quieto.",
            "Podés dejarte ir. El sueño llega solo, cuando quiere, y vos lo dejás venir."
        ),
        dormir = true
    )
)

private const val HIPNO_EVIDENCIA =
    "En 22 ensayos, la autohipnosis tuvo un efecto mediano a grande en dolor, parto, estrés y ansiedad, y funcionó mejor cuando se practicó al menos 3 veces (Eason y Parris, 2019). " +
        "En un ensayo con 250 mujeres, la autohipnosis con audio redujo los sofocos más que un control con ruido blanco (Elkins y otros, 2025). " +
        "Imaginar solo el éxito puede bajar el esfuerzo; por eso los guiones de metas incluyen el obstáculo y un plan (Oettingen)."

const val HIPNO_CUIDADO =
    "No la uses manejando ni haciendo algo que requiera atención. Si tenés o tuviste psicosis, o un trastorno disociativo, consultá antes con un profesional. " +
        "Si aparece mareo, dolor de cabeza o angustia, abrí los ojos y respirá normal. No reemplaza un tratamiento."

fun hipnoValores(id: String): Map<String, String> = Store.items("hip_$id").associate { it.id to it.text }

fun hipnoCompleto(g: HipnoGuion): Boolean {
    val v = hipnoValores(g.id)
    return g.campos.all { !v[it.key].isNullOrBlank() }
}

private fun llenar(t: String, v: Map<String, String>): String =
    Regex("\\{(\\w+)\\}").replace(t) { m -> v[m.groupValues[1]]?.trim().orEmpty() }

/** La sesión completa como práctica (para el reproductor común). */
fun hipnoPractice(id: String): Practice? {
    if (!id.startsWith("hip_")) return null
    val g = HIPNO_GUIONES.firstOrNull { it.id == id.removePrefix("hip_") } ?: return null
    val v = hipnoValores(g.id)
    return Practice(
        id, g.title, "15 a 25 min", "Mixta", g.para, HIPNO_EVIDENCIA,
        listOf("Hacela en un lugar tranquilo, sentado o acostado.", "Mejor con tu voz: grabá las frases antes de empezar.", "Si cortás antes, igual se hace la cuenta para volver."),
        listOf(15, 20, 25), 20,
        steps = INDUCCION + PROFUNDIZAR + g.partes.map { llenar(it, v) },
        cuidado = HIPNO_CUIDADO,
        cierre = if (g.dormir) DORMIR_CIERRE else REORIENTAR
    )
}

fun practiceById(id: String): Practice = hipnoPractice(id) ?: PRACTICES.firstOrNull { it.id == id } ?: PRACTICES.first()

/** Frases de los guiones ya completados, para «Grabar mi voz». */
fun hipnoFrases(): List<String> = HIPNO_GUIONES.filter { hipnoCompleto(it) }.flatMap { g ->
    val p = hipnoPractice("hip_${g.id}")!!
    p.steps + p.cierre
}

@Composable
fun HipnosisScreen(nav: Nav) {
    Store.tick.intValue
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Autohipnosis") { nav.pop() }
        Body("Un estado de mucha atención y calma, guiado por tu propia voz. Vos estás al mando todo el tiempo.")
        Small("Elegí un guion, completalo con tus palabras, grabalo y escuchalo. Rinde más si lo repetís varios días.")
        TCard(bg = C.ArcillaClara, bordered = false) { Small(HIPNO_CUIDADO, color = C.ArcillaOscura) }
        HIPNO_GUIONES.forEach { g ->
            val listo = hipnoCompleto(g)
            TCard(onClick = { nav.push(Route.HipnoEdit(g.id)) }) {
                Row {
                    Column(Modifier.weight(1f)) {
                        SerifText(g.title, 19)
                        Small(g.para)
                        Small(if (listo) "Listo para escuchar" else "Para completar", color = if (listo) C.Musgo else C.Texto2)
                    }
                    Chevron()
                }
            }
        }
        Small("Fondos que ayudan: tambor (unos 4 golpes por segundo), didgeridoo o un sonido suave de la naturaleza. Se eligen al empezar.")
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun HipnoEditScreen(id: String, nav: Nav) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Store.tick.intValue
    val g = HIPNO_GUIONES.firstOrNull { it.id == id } ?: HIPNO_GUIONES.first()
    val guardado = remember(id) { hipnoValores(g.id) }
    val valores = remember(id) { mutableStateMapOf<String, String>().apply { putAll(guardado) } }
    fun guardar() = Store.saveItems("hip_${g.id}", g.campos.map { Item(it.key, valores[it.key].orEmpty()) })
    val listo = g.campos.all { !valores[it.key].isNullOrBlank() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar(g.title) { guardar(); nav.pop() }
        Small(g.para)
        SectionLabel("Con tus palabras")
        g.campos.forEach { c ->
            OutlinedTextField(
                value = valores[c.key].orEmpty(), onValueChange = { valores[c.key] = it },
                label = { Text(c.label) }, placeholder = { Text("Por ejemplo: ${c.ejemplo}") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (listo) {
            SectionLabel("Así queda la parte central")
            TCard(bg = C.Papel) {
                g.partes.forEach { Body(llenar(it, valores), size = 15) }
            }
            PrimaryButton("Grabar esta sesión con mi voz") { guardar(); val vid = Voces.elegidaOCrear(ctx); Store.practiceVoice = vid; nav.push(Route.VoiceRecord("hip_${g.id}", vid)) }
            SoftButton("Empezar la sesión", Modifier.fillMaxWidth()) { guardar(); nav.push(Route.PracticeRun("hip_${g.id}")) }
            Small("Al grabar: voz más grave y más baja que la normal, pareja y lenta. A medida que avanza la sesión, andá bajando un poco más el ritmo.")
        } else {
            Small("Completá todos los espacios para ver cómo queda y grabarlo.")
        }
        TextButton(onClick = { guardar(); nav.pop() }) { Text("Guardar y volver", color = C.Bosque) }
        Spacer(Modifier.height(16.dp))
    }
}
