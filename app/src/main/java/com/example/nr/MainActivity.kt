package com.example.nr

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.room.Room
import coil.compose.rememberAsyncImagePainter
import com.example.nr.ui.theme.NRTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import androidx.compose.runtime.collectAsState

private const val STATUS_CHANNEL_ID = "status_updates"

class MainActivity : ComponentActivity() {

    private lateinit var db: NoteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createStatusNotificationChannel(this)

        db = Room.databaseBuilder(
            applicationContext,
            NoteDatabase::class.java,
            "notes_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        setContent {
            NRTheme {
                AppRoot(db = db)
            }
        }
    }
}

@Composable
private fun AppRoot(db: NoteDatabase) {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var splashFinished by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (!splashFinished) {
        SplashScreen(onFinished = { splashFinished = true })
        return
    }

    val user = currentUser
    if (user == null) {
        LoginScreen(
            onUserLogin = { currentUser = auth.currentUser },
            onAdminLogin = { currentUser = auth.currentUser }
        )
    } else if (user.email.orEmpty().equals(ADMIN_EMAIL, ignoreCase = true)) {
        AdminScreen(
            onLogout = {
                auth.signOut()
                currentUser = null
            }
        )
    } else {
        NotesScreen(
            db = db,
            user = user,
            onLogout = {
                auth.signOut()
                currentUser = null
            }
        )
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.82f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(1900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020817),
                        Color(0xFF0B1D2C),
                        Color(0xFF07111F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x5514B8A6),
                            Color(0x2214B8A6),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
        ) {
            Surface(
                modifier = Modifier.size(118.dp),
                shape = CircleShape,
                color = Color(0xFF0F3A3A),
                tonalElevation = 8.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_logo),
                    contentDescription = "Namma Raste Report logo",
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = "Namma Raste Report",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Smart Civic Issue Reporting",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotesScreen(
    db: NoteDatabase,
    user: FirebaseUser,
    onLogout: () -> Unit
) {
    val dao = db.reportDao()
    val currentUserId = user.uid
    val currentUserEmail = user.email.orEmpty()
    val reports by dao.getReportsForUser(currentUserId).collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val snackbarHostState = remember { SnackbarHostState() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var selectedTab by rememberSaveable { mutableStateOf("Home") }
    var selectedTicketId by rememberSaveable { mutableStateOf<String?>(null) }
    var description by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Pothole") }
    var priority by rememberSaveable { mutableStateOf("Medium") }
    var searchId by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var imageUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var imageAttached by rememberSaveable { mutableStateOf(false) }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Report?>(null) }

    val imageUri = remember(imageUriText) {
        imageUriText?.let(Uri::parse)
    }
    val selectedReport = remember(reports, selectedTicketId) {
        reports.firstOrNull { it.ticketId == selectedTicketId }
    }

    fun reportFromDocument(document: DocumentSnapshot): Report {
        return document.toReport(fallbackUserId = currentUserId)
    }

    fun showLocalStatusOrMissing(ticketId: String) {
        scope.launch {
            val localReport = dao.getReportByTicketForUser(ticketId, currentUserId)
            statusMessage = if (localReport != null) {
                formatReportStatusMessage(localReport)
            } else {
                "No report found for ticket ID $ticketId"
            }
        }
    }

    fun showFirestoreReport(report: Report) {
        if (report.userId != currentUserId) {
            statusMessage = "No report found for this account."
            return
        }

        statusMessage = formatReportStatusMessage(report)
        scope.launch {
            dao.insert(report)
        }
    }

    fun resetReportForm() {
        description = ""
        type = "Pothole"
        priority = "Medium"
        imageUriText = null
        imageAttached = false
        latitude = ""
        longitude = ""
    }

    fun saveReport(ticketId: String, timestamp: Long) {
        val localImagePath = imageUri?.toString().orEmpty()
        val encodedHistory = initialStatusHistory(timestamp)
        val firestoreHistory = listOf(
            mapOf(
                "status" to "Submitted",
                "timestamp" to timestamp,
                "note" to "Report submitted"
            )
        )
        val reportData = hashMapOf<String, Any>(
            "ticketId" to ticketId,
            "userId" to currentUserId,
            "userEmail" to currentUserEmail,
            "type" to type,
            "priority" to priority,
            "severity" to priority,
            "description" to description.trim(),
            "timestamp" to timestamp,
            "imagePath" to localImagePath,
            "imageUrl" to "",
            "latitude" to latitude,
            "longitude" to longitude,
            "status" to "Pending",
            "statusUpdatedAt" to timestamp,
            "adminUpdate" to "",
            "statusHistory" to firestoreHistory
        )

        firestore.collection(REPORT_COLLECTION)
            .document(ticketId)
            .set(reportData)
            .addOnSuccessListener {
                scope.launch {
                    dao.insert(
                        Report(
                            ticketId = ticketId,
                            userId = currentUserId,
                            type = type,
                            severity = priority,
                            description = description.trim(),
                            timestamp = timestamp,
                            imagePath = localImagePath,
                            imageUrl = "",
                            latitude = latitude,
                            longitude = longitude,
                            status = "Pending",
                            statusUpdatedAt = timestamp,
                            statusHistory = encodedHistory
                        )
                    )
                    snackbarHostState.showSnackbar("Report submitted: $ticketId")
                }

                resetReportForm()
                isSubmitting = false
                selectedTicketId = ticketId
                selectedTab = "Details"
                statusMessage = "Report submitted. Ticket ID: $ticketId"
            }
            .addOnFailureListener {
                isSubmitting = false
                statusMessage = "Unable to submit report."
            }
    }

    fun requestUniqueTicketId(onReady: (String) -> Unit, attempt: Int = 0) {
        if (attempt >= 8) {
            statusMessage = "Unable to reserve a unique ticket ID. Please try again."
            isSubmitting = false
            return
        }

        val candidate = newTicketCandidate(type)
        firestore.collection(REPORT_COLLECTION)
            .document(candidate)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    requestUniqueTicketId(onReady = onReady, attempt = attempt + 1)
                } else {
                    onReady(candidate)
                }
            }
            .addOnFailureListener {
                statusMessage = "Unable to reserve ticket ID."
                isSubmitting = false
            }
    }

    fun submitReport() {
        if (description.isBlank()) {
            statusMessage = "Add a clear description before submitting."
            return
        }
        if (isSubmitting) return

        isSubmitting = true
        statusMessage = "Submitting report..."

        requestUniqueTicketId(onReady = { ticketId ->
            val timestamp = System.currentTimeMillis()
            val selectedImageUri = imageUri

            if (selectedImageUri != null && imageAttached) {
                val imageError = validateReadableImageUri(context, selectedImageUri)
                if (imageError != null) {
                    isSubmitting = false
                    statusMessage = imageError
                    return@requestUniqueTicketId
                }

                saveReport(ticketId, timestamp)
            } else {
                saveReport(ticketId, timestamp)
            }
        })
    }

    fun checkStatus() {
        val ticketId = searchId.trim().uppercase(Locale.US)

        if (ticketId.isEmpty()) {
            statusMessage = "Enter a ticket ID."
            return
        }

        statusMessage = "Checking status..."

        firestore.collection(REPORT_COLLECTION)
            .document(ticketId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val report = reportFromDocument(document)
                    showFirestoreReport(report)
                } else {
                    firestore.collection(REPORT_COLLECTION)
                        .whereEqualTo("ticketId", ticketId)
                        .whereEqualTo("userId", currentUserId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val matchedDocument = querySnapshot.documents.firstOrNull()
                            if (matchedDocument != null) {
                                showFirestoreReport(reportFromDocument(matchedDocument))
                            } else {
                                showLocalStatusOrMissing(ticketId)
                            }
                        }
                        .addOnFailureListener {
                            showLocalStatusOrMissing(ticketId)
                        }
                }
            }
            .addOnFailureListener {
                showLocalStatusOrMissing(ticketId)
            }
    }

    fun deleteReport(report: Report) {
        if (report.userId != currentUserId) {
            statusMessage = "You can delete only your own reports."
            return
        }

        statusMessage = "Deleting report..."
        firestore.collection(REPORT_COLLECTION)
            .document(report.ticketId)
            .delete()
            .addOnSuccessListener {
                scope.launch {
                    dao.deleteReport(report.ticketId, currentUserId)
                    snackbarHostState.showSnackbar("Report deleted")
                }
                selectedTicketId = null
                selectedTab = "History"
                statusMessage = "Report deleted."
            }
            .addOnFailureListener {
                statusMessage = it.message ?: "Unable to delete report."
            }
    }

    DisposableEffect(currentUserId) {
        val registration = firestore.collection(REPORT_COLLECTION)
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    statusMessage = error.message ?: "Unable to sync reports."
                    return@addSnapshotListener
                }

                val syncedReports = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        runCatching { reportFromDocument(document) }.getOrNull()
                    }
                    .filter { it.userId == currentUserId }
                    .sortedByDescending { it.timestamp }

                scope.launch {
                    dao.deleteReportsNotOwnedBy(currentUserId)
                    if (syncedReports.isEmpty()) {
                        dao.deleteReportsForUser(currentUserId)
                    } else {
                        val ticketIds = syncedReports.map { it.ticketId }
                        syncedReports.forEach { report ->
                            val existing = dao.getReportByTicketForUser(report.ticketId, currentUserId)
                            if (existing != null && existing.status != report.status) {
                                notifyStatusUpdate(context, report)
                            }
                            dao.insert(report)
                        }
                        dao.deleteReportsForUserExcept(currentUserId, ticketIds)
                    }
                }
            }

        onDispose { registration.remove() }
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            imageAttached = success && imageUri != null
            if (!success) {
                imageUriText = null
                statusMessage = "Photo capture cancelled."
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri == null) {
                statusMessage = "Image selection cancelled."
            } else {
                val localUri = copyImageToLocalStorage(context, uri)
                if (localUri == null) {
                    imageUriText = null
                    imageAttached = false
                    statusMessage = "Selected image could not be saved. Choose another image."
                } else if (validateReadableImageUri(context, localUri) == null) {
                    imageUriText = localUri.toString()
                    imageAttached = true
                    statusMessage = "Image attached."
                } else {
                    imageUriText = null
                    imageAttached = false
                    statusMessage = "Selected image could not be read. Choose another image."
                }
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                openCamera(context) { uri ->
                    imageUriText = uri.toString()
                    imageAttached = false
                    cameraLauncher.launch(uri)
                }
            } else {
                statusMessage = "Camera permission is required to attach evidence."
            }
        }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            latitude = location.latitude.toString()
                            longitude = location.longitude.toString()
                            statusMessage = "Location pinned."
                        } else {
                            statusMessage = "Unable to get current location."
                        }
                    }.addOnFailureListener {
                        statusMessage = it.message ?: "Unable to get current location."
                    }
                }
            } else {
                statusMessage = "Location permission is required for map previews."
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    LaunchedEffect(currentUserId) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (deleteCandidate != null) {
        val report = deleteCandidate
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete report?") },
            text = {
                Text("This removes the Firestore report and local history entry for ${report?.ticketId}.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate = null
                        if (report != null) deleteReport(report)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Namma Raste Report",
                subtitle = currentUserEmail.ifBlank { "Civic issue reporting" },
                actionText = "Logout",
                onAction = onLogout
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onSelected = {
                    selectedTab = it
                    if (it != "Details") selectedTicketId = null
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            start = 18.dp,
            top = innerPadding.calculateTopPadding() + 18.dp,
            end = 18.dp,
            bottom = innerPadding.calculateBottomPadding() + 18.dp
        )

        when (selectedTab) {
            "Home" -> UserHomeTab(
                reports = reports,
                searchId = searchId,
                statusMessage = statusMessage,
                onSearchIdChange = { searchId = it },
                onCheckStatus = ::checkStatus,
                onOpenReport = { selectedTab = "Report" },
                onOpenDetails = {
                    selectedTicketId = it.ticketId
                    selectedTab = "Details"
                },
                contentPadding = contentPadding
            )

            "Report" -> ReportTab(
                type = type,
                priority = priority,
                description = description,
                imageUri = imageUri,
                imageAttached = imageAttached,
                latitude = latitude,
                longitude = longitude,
                isSubmitting = isSubmitting,
                statusMessage = statusMessage,
                onTypeChange = { type = it },
                onPriorityChange = { priority = it },
                onDescriptionChange = { description = it },
                onCapturePhoto = {
                    if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        openCamera(context) { uri ->
                            imageUriText = uri.toString()
                            imageAttached = false
                            cameraLauncher.launch(uri)
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onChooseImage = {
                    galleryLauncher.launch("image/*")
                },
                onGetLocation = {
                    if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                onSubmit = ::submitReport,
                onOpenMaps = { lat, lon ->
                    runCatching { uriHandler.openUri(googleMapsUrl(lat, lon)) }
                },
                contentPadding = contentPadding
            )

            "Map" -> ComplaintMapTab(
                reports = reports,
                onOpenMaps = { lat, lon ->
                    runCatching { uriHandler.openUri(googleMapsUrl(lat, lon)) }
                },
                contentPadding = contentPadding
            )

            "History" -> HistoryTab(
                reports = reports,
                onOpenDetails = {
                    selectedTicketId = it.ticketId
                    selectedTab = "Details"
                },
                contentPadding = contentPadding
            )

            "Profile" -> ProfileTab(
                reports = reports,
                userEmail = currentUserEmail,
                onLogout = onLogout,
                contentPadding = contentPadding
            )

            "Details" -> {
                if (selectedReport == null) {
                    MissingReportScreen(
                        onBack = { selectedTab = "History" },
                        contentPadding = contentPadding
                    )
                } else {
                    ReportDetailScreen(
                        report = selectedReport,
                        onBack = { selectedTab = "History" },
                        onDelete = { deleteCandidate = selectedReport },
                        onCopyTicket = { ticketId ->
                            clipboard.setText(AnnotatedString(ticketId))
                            scope.launch { snackbarHostState.showSnackbar("Ticket ID copied") }
                        },
                        onOpenMaps = { lat, lon ->
                            runCatching { uriHandler.openUri(googleMapsUrl(lat, lon)) }
                        },
                        contentPadding = contentPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: String,
    onSelected: (String) -> Unit
) {
    val items = remember {
        listOf(
            BottomNavItem("Home", "Home", R.drawable.ic_nav_home),
            BottomNavItem("Report", "Report", R.drawable.ic_nav_report),
            BottomNavItem("Map", "Map", R.drawable.ic_nav_map),
            BottomNavItem("History", "History", R.drawable.ic_nav_history),
            BottomNavItem("Profile", "Profile", R.drawable.ic_nav_profile)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val selected = selectedTab == item.route || (selectedTab == "Details" && item.route == "History")
            NavigationBarItem(
                selected = selected,
                onClick = { onSelected(item.route) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label, maxLines = 1) }
            )
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: Int
)

@Composable
private fun UserHomeTab(
    reports: List<Report>,
    searchId: String,
    statusMessage: String?,
    onSearchIdChange: (String) -> Unit,
    onCheckStatus: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenDetails: (Report) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "Civic reporting hub",
                subtitle = "Track issues, map reports, and follow ticket progress from your account."
            )
        }

        item {
            DashboardGrid(reports = reports)
        }

        item {
            Button(
                onClick = onOpenReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Create New Report")
            }
        }

        item {
            TicketLookupCard(
                searchId = searchId,
                onSearchIdChange = onSearchIdChange,
                onCheckStatus = onCheckStatus
            )
        }

        if (statusMessage != null) {
            item {
                StatusMessageCard(message = statusMessage)
            }
        }

        item {
            SectionTitle(
                title = "Recent activity",
                subtitle = "Latest reports submitted from this account."
            )
        }

        if (reports.isEmpty()) {
            item { EmptyReportsCard() }
        } else {
            items(
                items = reports.take(3),
                key = { report -> report.ticketId }
            ) { report ->
                UserReportCard(report = report, onOpen = { onOpenDetails(report) })
            }
        }
    }
}

@Composable
private fun DashboardGrid(reports: List<Report>) {
    val metrics = listOf(
        "Total Reports" to reports.size,
        "Pending" to reports.count { it.status == "Pending" },
        "Under Review" to reports.count { it.status == "Under Review" },
        "In Progress" to reports.count { it.status == "In Progress" },
        "Completed" to reports.count { it.status == "Completed" },
        "Rejected" to reports.count { it.status == "Rejected" }
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (label, value) ->
                    SummaryCard(
                        label = label,
                        value = value.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReportTab(
    type: String,
    priority: String,
    description: String,
    imageUri: Uri?,
    imageAttached: Boolean,
    latitude: String,
    longitude: String,
    isSubmitting: Boolean,
    statusMessage: String?,
    onTypeChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onChooseImage: () -> Unit,
    onGetLocation: () -> Unit,
    onSubmit: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "New report",
                subtitle = "Add issue details, priority, evidence, and a map location."
            )
        }

        item {
            ReportDetailsInputCard(
                selectedType = type,
                selectedPriority = priority,
                description = description,
                onTypeChange = onTypeChange,
                onPriorityChange = onPriorityChange,
                onDescriptionChange = onDescriptionChange
            )
        }

        item {
            EvidenceCard(
                imageUri = imageUri,
                imageAttached = imageAttached,
                onCapturePhoto = onCapturePhoto,
                onChooseImage = onChooseImage
            )
        }

        item {
            LocationCard(
                latitude = latitude,
                longitude = longitude,
                onGetLocation = onGetLocation,
                onOpenMaps = onOpenMaps
            )
        }

        item {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = description.isNotBlank() && !isSubmitting,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit Report")
            }
        }

        if (statusMessage != null) {
            item {
                StatusMessageCard(message = statusMessage)
            }
        }
    }
}

@Composable
private fun ComplaintMapTab(
    reports: List<Report>,
    onOpenMaps: (Double, Double) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "Complaint map",
                subtitle = "Markers show only your reports and are colored by current status."
            )
        }

        item {
            ComplaintMapCard(
                reports = reports,
                onOpenMaps = onOpenMaps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            )
        }
    }
}

@Composable
private fun HistoryTab(
    reports: List<Report>,
    onOpenDetails: (Report) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "Report history",
                subtitle = "${reports.size} submitted complaint${if (reports.size == 1) "" else "s"}"
            )
        }

        if (reports.isEmpty()) {
            item { EmptyReportsCard() }
        }

        items(
            items = reports,
            key = { report -> report.ticketId }
        ) { report ->
            UserReportCard(report = report, onOpen = { onOpenDetails(report) })
        }
    }
}

@Composable
private fun ProfileTab(
    reports: List<Report>,
    userEmail: String,
    onLogout: () -> Unit,
    contentPadding: PaddingValues
) {
    val points = reports.size * 10 + reports.count {
        it.status.equals("Completed", ignoreCase = true)
    } * 5

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "Profile",
                subtitle = "Account details and civic contribution summary."
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = userEmail.ifBlank { "Citizen reporter" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "$points points earned",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DashboardGrid(reports = reports)
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingReportScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EmptyReportsCard()
        }
        item {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to History")
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
private fun StatusMessageCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReportDetailsInputCard(
    selectedType: String,
    selectedPriority: String,
    description: String,
    onTypeChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
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
            SectionTitle(title = "Report details")

            ChipSelector(
                label = "Issue type",
                options = IssueTypes,
                selected = selectedType,
                onSelected = onTypeChange
            )

            ChipSelector(
                label = "Priority",
                options = Priorities,
                selected = selectedPriority,
                onSelected = onPriorityChange
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun ChipSelector(
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
private fun EvidenceCard(
    imageUri: Uri?,
    imageAttached: Boolean,
    onCapturePhoto: () -> Unit,
    onChooseImage: () -> Unit
) {
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
            SectionTitle(
                title = "Evidence",
                subtitle = "Attach a clear photo for faster verification."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCapturePhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Camera")
                }
                OutlinedButton(
                    onClick = onChooseImage,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Gallery")
                }
            }

            if (imageAttached && imageUri != null) {
                ReportImage(
                    source = imageUri.toString(),
                    height = 210,
                    onClick = { fullScreenImage = imageUri.toString() }
                )
            }
        }
    }

    fullScreenImage?.let { source ->
        FullScreenImageDialog(source = source, onDismiss = { fullScreenImage = null })
    }
}

@Composable
private fun LocationCard(
    latitude: String,
    longitude: String,
    onGetLocation: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit
) {
    val lat = latitude.toDoubleOrNull()
    val lon = longitude.toDoubleOrNull()

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
                title = "Location",
                subtitle = if (lat != null && lon != null) {
                    "Pinned at $latitude, $longitude"
                } else {
                    "Pin the location to enable map previews and routing."
                }
            )

            OutlinedButton(
                onClick = onGetLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Get Location")
            }

            if (lat != null && lon != null) {
                SingleReportMap(
                    latitude = lat,
                    longitude = lon,
                    title = "Pinned Location",
                    status = "Pending",
                    height = 250
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
                EmptyMapPreview()
            }
        }
    }
}

@Composable
private fun TicketLookupCard(
    searchId: String,
    onSearchIdChange: (String) -> Unit,
    onCheckStatus: () -> Unit
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
                title = "Check status",
                subtitle = "Use one of your ticket IDs to view the latest synced status."
            )

            OutlinedTextField(
                value = searchId,
                onValueChange = { onSearchIdChange(it.uppercase(Locale.US)) },
                label = { Text("Ticket ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = onCheckStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Check Status")
            }
        }
    }
}

@Composable
private fun ComplaintMapCard(
    reports: List<Report>,
    onOpenMaps: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val mappedReports = remember(reports) {
        reports.mapNotNull { report ->
            val lat = report.latitude.toDoubleOrNull()
            val lon = report.longitude.toDoubleOrNull()
            if (lat != null && lon != null) report to LatLng(lat, lon) else null
        }
    }
    val firstLocation = mappedReports.firstOrNull()?.second ?: LatLng(12.9716, 77.5946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(firstLocation, 12f)
    }
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                cameraPositionState = cameraPositionState
            ) {
                mappedReports.forEach { (report, position) ->
                    Marker(
                        state = MarkerState(position = position),
                        title = report.type,
                        snippet = "${report.status} - ${report.ticketId}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            markerHueForStatus(report.status)
                        ),
                        onClick = {
                            selectedReport = report
                            false
                        }
                    )
                }
            }

            selectedReport?.let { report ->
                val lat = report.latitude.toDoubleOrNull()
                val lon = report.longitude.toDoubleOrNull()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = report.type,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(status = report.status)
                    }
                    Text(
                        text = report.description.ifBlank { "No description provided" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (lat != null && lon != null) {
                        OutlinedButton(
                            onClick = { onOpenMaps(lat, lon) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Open in Google Maps")
                        }
                    }
                }
            }

            if (mappedReports.isEmpty()) {
                Text(
                    text = "No mapped reports yet. Pin a location while reporting to see markers here.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserReportCard(
    report: Report,
    onOpen: () -> Unit
) {
    val imageSource = report.imageUrl.ifBlank { report.imagePath }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (imageSource.isNotBlank()) {
                ReportImage(
                    source = imageSource,
                    height = 172,
                    onClick = { fullScreenImage = imageSource }
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
                        text = report.type,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = report.ticketId,
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
                    PriorityPill(priority = report.severity)
                }
            }

            Text(
                text = report.description.ifBlank { "No description provided" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    fullScreenImage?.let { source ->
        FullScreenImageDialog(source = source, onDismiss = { fullScreenImage = null })
    }
}

@Composable
private fun ReportDetailScreen(
    report: Report,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onCopyTicket: (String) -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
    contentPadding: PaddingValues
) {
    val imageSource = report.imageUrl.ifBlank { report.imagePath }
    val lat = report.latitude.toDoubleOrNull()
    val lon = report.longitude.toDoubleOrNull()
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = { onCopyTicket(report.ticketId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Copy Ticket")
                }
            }
        }

        item {
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
                        ReportImage(
                            source = imageSource,
                            height = 240,
                            onClick = { fullScreenImage = imageSource }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = report.type,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = report.ticketId,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(status = report.status)
                            PriorityPill(priority = report.severity)
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
                        InfoRow("Submitted", formatTimestamp(report.timestamp), Modifier.weight(1f))
                        InfoRow("Last update", formatTimestamp(report.statusUpdatedAt), Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoRow("Latitude", report.latitude, Modifier.weight(1f))
                        InfoRow("Longitude", report.longitude, Modifier.weight(1f))
                    }

                    SectionTitle(
                        title = "Admin updates",
                        subtitle = report.adminUpdate.ifBlank { "No admin update yet." }
                    )
                }
            }
        }

        item {
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
                    SectionTitle(title = "Map preview")
                    if (lat != null && lon != null) {
                        SingleReportMap(
                            latitude = lat,
                            longitude = lon,
                            title = report.type,
                            status = report.status,
                            height = 250
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
                        EmptyMapPreview()
                    }
                }
            }
        }

        item {
            StatusTimelineCard(report = report)
        }

        item {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Delete Report")
            }
        }
    }

    fullScreenImage?.let { source ->
        FullScreenImageDialog(source = source, onDismiss = { fullScreenImage = null })
    }
}

@Composable
private fun StatusTimelineCard(report: Report) {
    val decodedHistory = remember(report.statusHistory) { decodeTimeline(report.statusHistory) }
    val activeStatuses = remember(report.status) { activeTimelineStatuses(report.status) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Status timeline",
                subtitle = "Submitted, reviewed, worked on, completed, or rejected."
            )

            TimelineStatuses.forEachIndexed { index, status ->
                val entry = decodedHistory
                    .filter { it.status.equals(status, ignoreCase = true) }
                    .maxByOrNull { it.timestamp }
                val active = activeStatuses.contains(status)
                TimelineRow(
                    status = status,
                    entry = entry,
                    active = active,
                    showLine = index < TimelineStatuses.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    status: String,
    entry: TimelineEntry?,
    active: Boolean,
    showLine: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {}
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showLine) 8.dp else 0.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry != null) {
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportImage(
    source: String,
    height: Int,
    onClick: () -> Unit
) {
    Image(
        painter = rememberAsyncImagePainter(source),
        contentDescription = "Report image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    )
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

@Composable
private fun SingleReportMap(
    latitude: Double,
    longitude: Double,
    title: String,
    status: String,
    height: Int
) {
    val location = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 16f)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp)),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = location),
            title = title,
            snippet = status,
            icon = BitmapDescriptorFactory.defaultMarker(markerHueForStatus(status))
        )
    }
}

@Composable
private fun EmptyMapPreview() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Map preview will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyReportsCard() {
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
                text = "No reports yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "New users start with an empty history. Submitted tickets will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatReportStatusMessage(report: Report): String {
    return buildString {
        appendLine("Ticket: ${report.ticketId}")
        appendLine("Type: ${report.type}")
        appendLine("Priority: ${report.severity}")
        appendLine("Status: ${report.status}")
        if (report.adminUpdate.isNotBlank()) {
            appendLine("Admin update: ${report.adminUpdate}")
        }
    }.trim()
}

fun openCamera(
    context: Context,
    onUriReady: (Uri) -> Unit
) {
    val file = createLocalImageFile(context)

    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        file
    )

    onUriReady(uri)
}

private fun copyImageToLocalStorage(context: Context, sourceUri: Uri): Uri? {
    return runCatching {
        val destination = createLocalImageFile(context)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            destination
        )
    }.getOrNull()
}

private fun createLocalImageFile(context: Context): File {
    val imageDirectory = File(context.filesDir, "report_images")
    imageDirectory.mkdirs()
    return File(imageDirectory, "report_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
}

private fun validateReadableImageUri(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            if (input.read() == -1) {
                return "Selected image is empty. Choose another image."
            }
        } ?: return "Selected image could not be opened. Choose another image."
        null
    }.getOrElse {
        "Selected image could not be read. Choose another image."
    }
}

private fun newTicketCandidate(type: String): String {
    val token = UUID.randomUUID()
        .toString()
        .replace("-", "")
        .take(8)
        .uppercase(Locale.US)
    return "${ticketPrefixForIssue(type)}-$token"
}

private fun DocumentSnapshot.toReport(fallbackUserId: String = ""): Report {
    val status = stringField("status").ifBlank { "Pending" }
    val timestamp = longField("timestamp")
    val statusUpdatedAt = longField("statusUpdatedAt").takeIf { it > 0L } ?: timestamp
    val adminUpdate = stringField("adminUpdate")
    val history = encodedHistoryFromFirestore(
        value = get("statusHistory"),
        submittedAt = timestamp,
        currentStatus = status,
        statusUpdatedAt = statusUpdatedAt,
        adminUpdate = adminUpdate
    )

    return Report(
        ticketId = stringField("ticketId").ifBlank { id },
        userId = stringField("userId").ifBlank { fallbackUserId },
        type = stringField("type").ifBlank { "Report" },
        severity = stringField("priority").ifBlank { stringField("severity").ifBlank { "Medium" } },
        description = stringField("description"),
        timestamp = timestamp,
        imagePath = stringField("imagePath"),
        imageUrl = stringField("imageUrl").ifBlank { stringField("imagePath") },
        latitude = stringField("latitude"),
        longitude = stringField("longitude"),
        status = status,
        statusUpdatedAt = statusUpdatedAt,
        adminUpdate = adminUpdate,
        statusHistory = history
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

private fun createStatusNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            STATUS_CHANNEL_ID,
            "Status updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when report status changes"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

@SuppressLint("MissingPermission")
private fun notifyStatusUpdate(context: Context, report: Report) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        report.ticketId.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_nav_report)
        .setContentTitle("Report status updated")
        .setContentText("${report.ticketId}: ${report.status}")
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText("${report.type} is now ${report.status}. ${report.adminUpdate}")
        )
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context)
        .notify(report.ticketId.hashCode(), notification)
}
