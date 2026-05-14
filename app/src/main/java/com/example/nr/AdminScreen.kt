package com.example.nr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

data class FirestoreReport(
    val id: String = "",
    val ticketId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val type: String = "",
    val priority: String = "Medium",
    val description: String = "",
    val status: String = "Pending",
    val timestamp: Long = 0L,
    val statusUpdatedAt: Long = 0L,
    val adminUpdate: String = "",
    val statusHistory: String = "",
    val imagePath: String = "",
    val imageUrl: String = "",
    val latitude: String = "",
    val longitude: String = ""
)

@Composable
fun AdminScreen(onLogout: () -> Unit) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val uriHandler = LocalUriHandler.current

    var reports by remember { mutableStateOf(emptyList<FirestoreReport>()) }
    var adminError by remember { mutableStateOf<String?>(null) }
    var statusFilter by rememberSaveable { mutableStateOf("All") }
    var priorityFilter by rememberSaveable { mutableStateOf("All") }
    var typeFilter by rememberSaveable { mutableStateOf("All") }

    fun updateReportStatus(report: FirestoreReport, status: String, note: String) {
        val now = System.currentTimeMillis()
        val adminUpdate = note.trim().ifBlank { "Status changed to $status" }
        val historyEntry = mapOf(
            "status" to status,
            "timestamp" to now,
            "note" to adminUpdate
        )

        firestore.collection(REPORT_COLLECTION)
            .document(report.id)
            .update(
                mapOf(
                    "status" to status,
                    "statusUpdatedAt" to now,
                    "adminUpdate" to adminUpdate,
                    "statusHistory" to FieldValue.arrayUnion(historyEntry)
                )
            )
            .addOnFailureListener {
                adminError = it.message ?: "Unable to update status."
            }
    }

    DisposableEffect(Unit) {
        val registration = firestore.collection(REPORT_COLLECTION)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    adminError = error.message ?: "Unable to load reports."
                    reports = emptyList()
                    return@addSnapshotListener
                }

                adminError = null
                reports = value?.documents.orEmpty()
                    .mapNotNull { document ->
                        runCatching { document.toFirestoreReport() }.getOrNull()
                    }
                    .sortedByDescending { it.timestamp }
            }

        onDispose { registration.remove() }
    }

    val typeOptions = remember(reports) {
        listOf("All") + reports.map { it.type.ifBlank { "Report" } }.distinct().sorted()
    }
    val filteredReports = reports.filter { report ->
        (statusFilter == "All" || report.status == statusFilter) &&
            (priorityFilter == "All" || report.priority == priorityFilter) &&
            (typeFilter == "All" || report.type == typeFilter)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Admin Dashboard",
                subtitle = "${filteredReports.size} of ${reports.size} civic reports",
                actionText = "Logout",
                onAction = onLogout
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = innerPadding.calculateTopPadding() + 18.dp,
                end = 18.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AdminAnalyticsDashboard(reports = reports)
            }

            if (adminError != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text(
                            text = adminError.orEmpty(),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                AdminFilters(
                    statusFilter = statusFilter,
                    priorityFilter = priorityFilter,
                    typeFilter = typeFilter,
                    typeOptions = typeOptions,
                    onStatusFilterChange = { statusFilter = it },
                    onPriorityFilterChange = { priorityFilter = it },
                    onTypeFilterChange = { typeFilter = it }
                )
            }

            item {
                SectionTitle(
                    title = "Report queue",
                    subtitle = "Review evidence, location, owner, status history, and resolution state."
                )
            }

            if (filteredReports.isEmpty()) {
                item { EmptyAdminCard() }
            }

            items(
                items = filteredReports,
                key = { report -> report.id }
            ) { report ->
                AdminReportCard(
                    report = report,
                    onUpdateStatus = ::updateReportStatus,
                    onOpenMaps = { lat, lon ->
                        runCatching { uriHandler.openUri(googleMapsUrl(lat, lon)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminAnalyticsDashboard(reports: List<FirestoreReport>) {
    val metrics = listOf(
        "Total Reports" to reports.size,
        "Pending" to reports.count { it.status == "Pending" },
        "Under Review" to reports.count { it.status == "Under Review" },
        "In Progress" to reports.count { it.status == "In Progress" },
        "Completed" to reports.count { it.status == "Completed" },
        "Rejected" to reports.count { it.status == "Rejected" }
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(
                title = "Operations analytics",
                subtitle = "Global snapshot across every citizen report."
            )

            metrics.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (label, value) ->
                        AdminMetric(label, value.toString(), Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            DistributionLine(
                label = "Priority distribution",
                value = Priorities.joinToString("  |  ") { priority ->
                    "$priority ${reports.count { it.priority == priority }}"
                }
            )
            DistributionLine(
                label = "Issue distribution",
                value = IssueTypes.joinToString("  |  ") { type ->
                    "$type ${reports.count { it.type == type }}"
                }
            )
        }
    }
}

@Composable
private fun AdminFilters(
    statusFilter: String,
    priorityFilter: String,
    typeFilter: String,
    typeOptions: List<String>,
    onStatusFilterChange: (String) -> Unit,
    onPriorityFilterChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(
                title = "Filters",
                subtitle = "Narrow the queue by status, priority, or issue type."
            )

            FilterGroup(
                label = "Status",
                options = listOf("All") + ReportStatuses,
                selected = statusFilter,
                onSelected = onStatusFilterChange
            )

            FilterGroup(
                label = "Priority",
                options = listOf("All") + Priorities,
                selected = priorityFilter,
                onSelected = onPriorityFilterChange
            )

            FilterGroup(
                label = "Type",
                options = typeOptions,
                selected = typeFilter,
                onSelected = onTypeFilterChange
            )
        }
    }
}

@Composable
private fun FilterGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option, maxLines = 1) }
                )
            }
        }
    }
}

@Composable
private fun AdminMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DistributionLine(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AdminReportCard(
    report: FirestoreReport,
    onUpdateStatus: (FirestoreReport, String, String) -> Unit,
    onOpenMaps: (Double, Double) -> Unit
) {
    val imageSource = report.imageUrl.ifBlank { report.imagePath }
    val lat = report.latitude.toDoubleOrNull()
    val lon = report.longitude.toDoubleOrNull()
    var adminNote by rememberSaveable(report.id) { mutableStateOf("") }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (imageSource.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(imageSource),
                    contentDescription = "Report image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { fullScreenImage = imageSource }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = report.type.ifBlank { "Road report" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = report.ticketId.ifBlank { report.id },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(report.timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(status = report.status)
                    PriorityPill(priority = report.priority)
                }
            }

            Text(
                text = report.description.ifBlank { "No description provided" },
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(
                    label = "User",
                    value = report.userEmail.ifBlank { report.userId },
                    modifier = Modifier.weight(1f)
                )
                InfoRow(
                    label = "Last update",
                    value = formatTimestamp(report.statusUpdatedAt),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow("Latitude", report.latitude, Modifier.weight(1f))
                InfoRow("Longitude", report.longitude, Modifier.weight(1f))
            }

            if (lat != null && lon != null) {
                AdminMapPreview(
                    report = report,
                    latitude = lat,
                    longitude = lon
                )
                OutlinedButton(
                    onClick = { onOpenMaps(lat, lon) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Open in Google Maps")
                }
            } else {
                EmptyAdminMapCard()
            }

            SectionTitle(
                title = "Current admin update",
                subtitle = report.adminUpdate.ifBlank { "No admin update yet." }
            )

            AdminTimeline(history = report.statusHistory, currentStatus = report.status)

            OutlinedTextField(
                value = adminNote,
                onValueChange = { adminNote = it },
                label = { Text("Admin update note") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            FilterGroup(
                label = "Update status",
                options = ReportStatuses,
                selected = report.status,
                onSelected = { status ->
                    onUpdateStatus(report, status, adminNote)
                    adminNote = ""
                }
            )
        }
    }

    fullScreenImage?.let { source ->
        FullScreenImageDialog(source = source, onDismiss = { fullScreenImage = null })
    }
}

@Composable
private fun AdminMapPreview(
    report: FirestoreReport,
    latitude: Double,
    longitude: Double
) {
    val position = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 15f)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp)),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = position),
            title = report.type,
            snippet = "${report.status} - ${report.ticketId}",
            icon = BitmapDescriptorFactory.defaultMarker(
                markerHueForStatus(report.status)
            )
        )
    }
}

@Composable
private fun AdminTimeline(
    history: String,
    currentStatus: String
) {
    val entries = remember(history) { decodeTimeline(history) }
    val activeStatuses = remember(currentStatus) { activeTimelineStatuses(currentStatus) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Status timeline",
            style = MaterialTheme.typography.titleMedium
        )
        TimelineStatuses.forEach { status ->
            val active = activeStatuses.contains(status)
            val entry = entries
                .filter { it.status.equals(status, ignoreCase = true) }
                .maxByOrNull { it.timestamp }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(12.dp),
                    shape = CircleShape,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {}
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry != null) {
                        Text(
                            text = "${formatTimestamp(entry.timestamp)} ${entry.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAdminMapCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No map location provided",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyAdminCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "No reports in queue",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Newly submitted citizen reports will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FullScreenImageDialog(
    source: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(source),
                contentDescription = "Full screen report image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(12.dp)
            )
        }
    }
}

private fun DocumentSnapshot.toFirestoreReport(): FirestoreReport {
    val status = stringField("status").ifBlank { "Pending" }
    val timestamp = longField("timestamp")
    val statusUpdatedAt = longField("statusUpdatedAt").takeIf { it > 0L } ?: timestamp
    val adminUpdate = stringField("adminUpdate")

    return FirestoreReport(
        id = id,
        ticketId = stringField("ticketId").ifBlank { id },
        userId = stringField("userId"),
        userEmail = stringField("userEmail"),
        type = stringField("type").ifBlank { "Report" },
        priority = stringField("priority").ifBlank { stringField("severity").ifBlank { "Medium" } },
        description = stringField("description"),
        status = status,
        timestamp = timestamp,
        statusUpdatedAt = statusUpdatedAt,
        adminUpdate = adminUpdate,
        statusHistory = encodedHistoryFromFirestore(
            value = get("statusHistory"),
            submittedAt = timestamp,
            currentStatus = status,
            statusUpdatedAt = statusUpdatedAt,
            adminUpdate = adminUpdate
        ),
        imagePath = stringField("imagePath"),
        imageUrl = stringField("imageUrl").ifBlank { stringField("imagePath") },
        latitude = stringField("latitude"),
        longitude = stringField("longitude")
    )
}

private fun DocumentSnapshot.stringField(field: String): String {
    return when (val value = get(field)) {
        is String -> value
        is Number -> value.toString()
        else -> ""
    }
}

private fun DocumentSnapshot.longField(field: String): Long {
    return when (val value = get(field)) {
        is Long -> value
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: 0L
        else -> 0L
    }
}

private fun encodedHistoryFromFirestore(
    value: Any?,
    submittedAt: Long,
    currentStatus: String,
    statusUpdatedAt: Long,
    adminUpdate: String
): String {
    val entries = mutableListOf<TimelineEntry>()

    when (value) {
        is String -> entries.addAll(decodeTimeline(value))
        is List<*> -> {
            value.forEach { item ->
                val map = item as? Map<*, *> ?: return@forEach
                val status = map["status"] as? String ?: return@forEach
                val timestamp = when (val rawTimestamp = map["timestamp"]) {
                    is Number -> rawTimestamp.toLong()
                    is String -> rawTimestamp.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val note = map["note"] as? String ?: ""
                entries.add(TimelineEntry(status, timestamp, note))
            }
        }
    }

    if (entries.none { it.status.equals("Submitted", ignoreCase = true) }) {
        entries.add(TimelineEntry("Submitted", submittedAt, "Report submitted"))
    }

    if (
        currentStatus != "Pending" &&
        entries.none { it.status.equals(currentStatus, ignoreCase = true) }
    ) {
        entries.add(TimelineEntry(currentStatus, statusUpdatedAt, adminUpdate))
    }

    return encodeTimeline(entries.sortedBy { it.timestamp })
}
