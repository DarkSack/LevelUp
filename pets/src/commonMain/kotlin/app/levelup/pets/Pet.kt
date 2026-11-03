package app.levelup.pets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh

/** Cómo está la mascota. */
enum class PetMood {
    SAD, SLEEPY, CALM, HAPPY, PARTY;

    companion object {
        /**
         * Fiesta si hoy ya hizo algo y lleva [partyFrom] días, feliz si hoy ya hizo algo, tranquila con racha,
         * triste si la racha se apagó ([hadStreak]: alguna vez tuvo una) y dormida si nunca empezó.
         */
        fun of(streak: Int, doneToday: Boolean, hadStreak: Boolean = false, partyFrom: Int = 7): PetMood = when {
            doneToday && streak >= partyFrom -> PARTY
            doneToday -> HAPPY
            streak > 0 -> CALM
            hadStreak -> SAD
            else -> SLEEPY
        }
    }
}

/** [exclusive]: no se compra; la app decide quién la tiene (el alebrije es de quienes probaron la beta). */
enum class Species(val key: String, val exclusive: Boolean = false) {
    GATO("gato"), PERRO("perro"), TLACUACHE("tlacuache"), AJOLOTE("ajolote"), XOLO("xolo"), CONEJO("conejo"),
    PANDA("panda"), RANA("rana"), ZORRO("zorro"), ALEBRIJE("alebrije", exclusive = true);

    companion object {
        fun byKey(key: String): Species? = entries.firstOrNull { it.key == key }
    }
}

/** Dónde caen los ojos y la nariz de cada especie, para lentes, antifaces y bigotes. */
internal class Face(val left: Float, val right: Float, val eyeY: Float, val noseY: Float)

internal val Species.face: Face
    get() = when (this) {
        Species.AJOLOTE -> Face(37f, 63f, 45f, 51f)
        Species.RANA -> Face(38f, 62f, 31f, 45f)
        Species.CONEJO -> Face(42f, 58f, 44f, 51f)
        Species.PANDA -> Face(40f, 60f, 43f, 51f)
        Species.XOLO, Species.ALEBRIJE -> Face(41f, 59f, 42f, 50f)
        Species.GATO -> Face(41f, 59f, 43f, 50.5f)
        Species.PERRO -> Face(41f, 59f, 43f, 50f)
        Species.ZORRO -> Face(41f, 59f, 42f, 53f)
        Species.TLACUACHE -> Face(41f, 59f, 43f, 55.5f)
    }

/** Un bucle dura esto; los ánimos activos dan dos vueltas por bucle, así cambiar de ánimo no hace saltar la animación. */
private const val LOOP_MS = 4800

/**
 * Mascota animada en bucle; al tocarla salta como gelatina, suelta un corazón y llama a [onClick]. Con [animated] en
 * false queda quieta (el toque sigue llamando a [onClick]).
 */
@Composable
fun Pet(
    species: Species, mood: PetMood, modifier: Modifier = Modifier, accessories: Set<Accessory> = emptySet(),
    animated: Boolean = true, onClick: (() -> Unit)? = null,
) {
    val phase = if (animated) {
        rememberInfiniteTransition(label = "pet").animateFloat(
            0f, 1f, infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing), RepeatMode.Restart), label = "phase",
        ).value
    } else STILL
    val poke = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val tap = Modifier.pointerInput(animated, onClick) {
        detectTapGestures {
            onClick?.invoke()
            if (animated) scope.launch { poke.snapTo(0f); poke.animateTo(1f, tween(900, easing = LinearEasing)); poke.snapTo(0f) }
        }
    }
    PetFrame(species, mood, phase, modifier.then(tap), accessories, poke.value)
}

/** Un fotograma fijo: [phase] va de 0 a 1 en el bucle y [poke] de 0 a 1 en la reacción al toque. */
@Composable
fun PetFrame(
    species: Species, mood: PetMood, phase: Float, modifier: Modifier = Modifier, accessories: Set<Accessory> = emptySet(), poke: Float = 0f,
) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2, (size.height - 100f * s) / 2) {
            scale(s, s, pivot = Offset.Zero) { drawPet(species, Pose(phase, mood, poke), accessories) }
        }
    }
}

/** Fotograma de las mascotas quietas: ojos abiertos y sin salto. */
const val STILL = 0.25f

private val TAU = 2 * PI.toFloat()

/** Lo que cambia con el tiempo, en un lienzo de 100 × 100 con el suelo en y = 92. */
internal class Pose(phase: Float, val mood: PetMood, val poke: Float = 0f) {
    private val slow = mood == PetMood.SLEEPY || mood == PetMood.SAD
    /** Posición dentro de la vuelta: las tranquilas dan una por bucle y las activas dos. */
    val beat = if (slow) phase else (phase * 2f) % 1f
    private val wave = sin(TAU * beat)
    /** Igual que [wave] pero un poco retrasada: cola y orejas siguen al cuerpo con inercia. */
    private val lag = sin(TAU * (beat - 0.08f))
    private val jumping = mood == PetMood.HAPPY || mood == PetMood.PARTY
    private val hopT = (beat * 2f) % 1f
    /** 0 en el suelo, 1 en lo alto: una parábola entre el impulso y la caída. */
    private val air = if (jumping && hopT in 0.2f..0.8f) ((hopT - 0.2f) / 0.6f).let { 4f * it * (1f - it) } else 0f
    /** Se agacha antes de saltar y al caer. */
    private val crouch = when {
        !jumping -> 0f
        hopT < 0.2f -> sin(PI.toFloat() * hopT / 0.2f)
        hopT > 0.8f -> sin(PI.toFloat() * (hopT - 0.8f) / 0.2f)
        else -> 0f
    }
    private val pokeArc = sin(PI.toFloat() * poke)
    /** Rebote de gelatina que se apaga al final del toque. */
    private val wobble = if (poke > 0f) sin(6f * PI.toFloat() * poke) * (1f - poke) * 0.12f else 0f

    val hop = -(when (mood) { PetMood.HAPPY -> 5f; PetMood.PARTY -> 8f; else -> 0f }) * air - 10f * pokeArc
    /** Estira en el aire y aplasta al tocar el suelo. */
    val stretch = 1f + 0.07f * air - 0.09f * crouch + wobble
    val breath = 1f + (if (slow) 0.035f else 0.02f) * wave
    /** Grados que se balancea todo el cuerpo (baile de fiesta). */
    val lean = if (mood == PetMood.PARTY) 8f * wave else 0f
    /** Grados que ladea la cabeza. */
    val tilt = when (mood) {
        PetMood.CALM -> 5f * wave
        PetMood.HAPPY -> 4f * lag
        PetMood.PARTY -> -7f * lag
        PetMood.SLEEPY -> 9f + 1.5f * wave
        PetMood.SAD -> -4f + 1.5f * wave
    }
    /** La cabeza baja si duerme o está triste y acompaña la respiración un poco después que el cuerpo. */
    val headDrop = (when (mood) { PetMood.SLEEPY -> 2.5f; PetMood.SAD -> 4f; else -> 0f }) + 0.7f * lag
    /** Las pupilas van a un lado, se quedan un rato y van al otro. */
    val look = if (mood == PetMood.CALM) 1.4f * tanh(3f * wave) / tanh(3f) else 0f
    private val twitch = if (!slow && beat in 0.55f..0.67f) 14f * sin(PI.toFloat() * (beat - 0.55f) / 0.12f) else 0f
    private val droop = if (mood == PetMood.SAD && poke == 0f) 28f else 0f
    /** Grados hacia fuera de cada oreja (o branquias): positivos = caídas o sacudida. */
    val earLeft = droop + 3f * lag
    val earRight = droop + 3f * lag + twitch
    /** Grados de la cola, un poco por detrás del cuerpo. */
    val sway = when (mood) {
        PetMood.SLEEPY -> 3f * lag
        PetMood.SAD -> 22f + 3f * lag
        PetMood.CALM -> 10f * lag
        else -> 20f * sin(TAU * (2f * beat - 0.1f))
    }
    val glad = jumping || poke > 0f
    val sad = mood == PetMood.SAD && poke == 0f
    val happyEyes = (jumping && air > 0.5f) || poke in 0.05f..0.95f
    val sleeping = mood == PetMood.SLEEPY && poke == 0f
    /** 1 con los ojos abiertos; baja y sube suave al parpadear. */
    val eyeOpen = if (beat in 0.86f..0.94f) 1f - sin(PI.toFloat() * (beat - 0.86f) / 0.08f) else 1f
    /** Lágrima: cae del ojo y se desvanece. */
    val tearDrop = 11f * beat
    val tearAlpha = if (beat < 0.75f) 1f else (1f - beat) / 0.25f
}

/** La cabeza se mueve sola (baja, se ladea); lo que se dibuje aquí dentro la sigue. */
internal fun DrawScope.head(p: Pose, content: DrawScope.() -> Unit) {
    translate(0f, p.headDrop) { rotate(p.tilt, pivot = Offset(50f, 60f)) { content() } }
}

private fun DrawScope.drawPet(species: Species, p: Pose, extras: Set<Accessory>) {
    shadow(p)
    if (p.sad) rainCloud(p.beat)
    translate(0f, p.hop) {
        rotate(p.lean, pivot = Offset(50f, 92f)) {
            scale(1f / p.stretch, p.breath * p.stretch, pivot = Offset(50f, 92f)) {
                when (species) {
                    Species.TLACUACHE -> tlacuache(p)
                    Species.AJOLOTE -> ajolote(p)
                    Species.GATO -> gato(p)
                    Species.PERRO -> perro(p)
                    Species.XOLO -> xolo(p)
                    Species.CONEJO -> conejo(p)
                    Species.PANDA -> panda(p)
                    Species.RANA -> rana(p)
                    Species.ZORRO -> zorro(p)
                    Species.ALEBRIJE -> alebrije(p)
                }
                // Cuello con el cuerpo; cara y cabeza siguen a la cabeza.
                for (a in extras.filter { it.slot != Slot.SIDE }.sortedByDescending { it.slot.ordinal }) {
                    if (a.slot == Slot.NECK) accessory(a, species, p.beat) else head(p) { accessory(a, species, p.beat) }
                }
            }
        }
    }
    // Lo que va al lado vuela o flota por su cuenta.
    extras.filter { it.slot == Slot.SIDE }.forEach { accessory(it, species, p.beat) }
    when {
        p.poke > 0f -> heart(p.poke)
        p.mood == PetMood.SLEEPY -> zzz(p.beat)
        p.mood == PetMood.PARTY -> { confetti(p.beat); sparkles(p.beat) }
    }
}

/** Huevo que se mece: lo que se ve antes de elegir la primera mascota. */
@Composable
fun PetEgg(modifier: Modifier = Modifier, animated: Boolean = true, alebrije: Boolean = false) {
    val phase = if (animated) {
        rememberInfiniteTransition(label = "egg").animateFloat(
            0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "phase",
        ).value
    } else STILL
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2, (size.height - 100f * s) / 2) {
            scale(s, s, pivot = Offset.Zero) { egg(phase, alebrije) }
        }
    }
}
