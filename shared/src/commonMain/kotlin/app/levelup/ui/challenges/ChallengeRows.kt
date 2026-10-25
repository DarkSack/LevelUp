package app.levelup.ui.challenges

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.levelup.content.Categories
import app.levelup.domain.Challenge
import app.levelup.domain.ChallengeInstance
import app.levelup.domain.CustomCategory
import app.levelup.domain.InstanceStatus
import app.levelup.ui.Fmt
import app.levelup.ui.theme.Space

fun statusText(i: ChallengeInstance): String = when (i.status) {
    InstanceStatus.PENDING -> "Pendiente"
    InstanceStatus.STARTED -> "En curso"
    InstanceStatus.PAUSED -> "En pausa"
    InstanceStatus.COMPLETED -> "Hecho"
    InstanceStatus.SKIPPED -> "Para otro día"
    InstanceStatus.REPLACED -> "Cambiado"
}

fun metaLine(i: ChallengeInstance, custom: List<CustomCategory>): String = buildList {
    add(Categories.name(i.category, custom))
    add(Fmt.minutes(i.minutes))
    add("+${i.xp} XP")
    if (i.isHabit) add("Hábito")
}.joinToString(" · ")

/**
 * Fila de un reto del día. El botón redondo lo completa; el resto de la fila abre el detalle.
 * El estado se dice con texto e icono, no solo con color.
 */
@Composable
fun InstanceRow(i: ChallengeInstance, custom: List<CustomCategory>, onComplete: () -> Unit, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = Space.xs),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.heightIn(min = 72.dp).padding(start = Space.s, end = Space.s), verticalAlignment = Alignment.CenterVertically) {
            if (i.isDone) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp).padding(Space.s))
            } else if (i.isOpen) {
                FilledTonalIconButton(onClick = onComplete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = "Completar ${i.title}")
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Column(
                Modifier.weight(1f).clickable(onClickLabel = "Ver detalles", onClick = onOpen).padding(Space.m)
                    .semantics(mergeDescendants = true) { stateDescription = statusText(i) },
            ) {
                Text(
                    i.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (i.isDone) TextDecoration.LineThrough else null,
                )
                Text(metaLine(i, custom), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!i.isOpen || i.status != InstanceStatus.PENDING) {
                    Text(statusText(i), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Fila del catálogo o de retos propios. */
@Composable
fun ChallengeRow(c: Challenge, custom: List<CustomCategory>, extra: String? = null, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = Space.xs).clickable(onClickLabel = "Ver detalles", onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.heightIn(min = 64.dp).padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(c.title, style = MaterialTheme.typography.titleSmall)
                val meta = listOfNotNull(
                    Categories.name(c.category, custom), Fmt.difficulty(c.difficulty), Fmt.minutes(c.minutes), "+${c.xp} XP", extra,
                ).joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
