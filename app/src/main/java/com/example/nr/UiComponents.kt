package com.example.nr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimelineEntry(
    val status: String,
    val timestamp: Long,
    val note: String = ""
)

@Composable
fun AppTopBar(
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.brand_logo),
                        contentDescription = null,
                        modifier = Modifier.padding(5.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TextButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val label = status.ifBlank { "Pending" }
    val colors = statusColors(label)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.first,
        contentColor = colors.second
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PriorityPill(
    priority: String,
    modifier: Modifier = Modifier
) {
    val label = priority.ifBlank { "Medium" }
    val normalized = label.lowercase()
    val containerColor = when (normalized) {
        "emergency" -> Color(0xFF4C0519)
        "high" -> Color(0xFF451A03)
        "low" -> Color(0xFF052E2B)
        else -> Color(0xFF172554)
    }
    val contentColor = when (normalized) {
        "emergency" -> Color(0xFFFFA6C1)
        "high" -> Color(0xFFFBBF24)
        "low" -> Color(0xFF7DD3FC)
        else -> Color(0xFFBFDBFE)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SeverityPill(
    severity: String,
    modifier: Modifier = Modifier
) {
    PriorityPill(priority = severity, modifier = modifier)
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.widthIn(min = 0.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun statusColors(status: String): Pair<Color, Color> {
    return when (status.trim().lowercase()) {
        "completed" -> Color(0xFF052E16) to Color(0xFF86EFAC)
        "in progress" -> Color(0xFF133B3A) to Color(0xFF67E8F9)
        "under review" -> Color(0xFF172554) to Color(0xFFBFDBFE)
        "rejected" -> Color(0xFF450A0A) to Color(0xFFFCA5A5)
        else -> Color(0xFF3B2F12) to Color(0xFFFDE68A)
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "-"

    return SimpleDateFormat(
        "dd MMM yyyy, h:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}

fun markerHueForStatus(status: String): Float {
    return when (status.trim().lowercase()) {
        "completed" -> BitmapDescriptorFactory.HUE_GREEN
        "in progress" -> BitmapDescriptorFactory.HUE_AZURE
        "under review" -> BitmapDescriptorFactory.HUE_VIOLET
        "rejected" -> BitmapDescriptorFactory.HUE_RED
        else -> BitmapDescriptorFactory.HUE_ORANGE
    }
}

fun googleMapsUrl(latitude: Double, longitude: Double): String {
    return "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
}

fun initialStatusHistory(timestamp: Long): String {
    return encodeTimeline(listOf(TimelineEntry("Submitted", timestamp, "Report submitted")))
}

fun encodeTimeline(entries: List<TimelineEntry>): String {
    return entries
        .filter { it.status.isNotBlank() }
        .joinToString("\n") { entry ->
            val note = entry.note.replace("|", " ").replace("\n", " ")
            val status = entry.status.replace("|", " ").replace("\n", " ")
            "${entry.timestamp}|$status|$note"
        }
}

fun appendTimelineEntry(history: String, entry: TimelineEntry): String {
    val entries = decodeTimeline(history).toMutableList()
    entries.add(entry)
    return encodeTimeline(entries)
}

fun decodeTimeline(history: String): List<TimelineEntry> {
    if (history.isBlank()) return emptyList()

    return history.lineSequence()
        .mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size < 2) {
                null
            } else {
                TimelineEntry(
                    status = parts[1],
                    timestamp = parts[0].toLongOrNull() ?: 0L,
                    note = parts.getOrElse(2) { "" }
                )
            }
        }
        .toList()
}

fun activeTimelineStatuses(currentStatus: String): Set<String> {
    val normalized = currentStatus.trim().lowercase()
    return when (normalized) {
        "completed" -> setOf("Submitted", "Under Review", "In Progress", "Completed")
        "in progress" -> setOf("Submitted", "Under Review", "In Progress")
        "under review" -> setOf("Submitted", "Under Review")
        "rejected" -> setOf("Submitted", "Rejected")
        else -> setOf("Submitted")
    }
}
