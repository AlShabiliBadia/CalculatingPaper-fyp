package com.example.calculatingpaper.view.screens
import android.app.Activity.RESULT_OK
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calculatingpaper.R
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.viewmodel.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
private data class SyncConfirmationDetails(
    val state: SyncCheckState,
    val title: String,
    val text: String,
    val confirmAction: () -> Unit
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    appPreferences: AppPreferences,
    noteViewModel: NoteViewModel = viewModel()
) {
    var decimalPrecisionText by remember {
        mutableStateOf(AppPreferences.decimalPrecision.toString())
    }
    var showInvalidInputError by remember { mutableStateOf(false) }
    var showHighPrecisionWarning by remember { mutableStateOf(false) }
    val syncCheckState by noteViewModel.syncCheckState.collectAsState()
    val syncActivationState by noteViewModel.syncActivationState.collectAsState()
    var showSyncConfirmDialog by remember { mutableStateOf(false) }
    var syncConfirmationDetails by remember { mutableStateOf<SyncConfirmationDetails?>(null) }
    var isLoggedIn by remember { mutableStateOf(appPreferences.isLoggedIn()) }
    var userEmail by remember { mutableStateOf(appPreferences.getUserEmail()) }
    var firebaseUid by remember { mutableStateOf(appPreferences.getUserId()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    val resetState by noteViewModel.resetState.collectAsState()
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var importUriToProcess by remember { mutableStateOf<Uri?>(null) }
    var showImportOptionsDialog by remember { mutableStateOf(false) }
    val importState by noteViewModel.importState.collectAsState()
    var isRealtimeSyncEnabled by remember(isLoggedIn, syncActivationState) {
        mutableStateOf(if (isLoggedIn) appPreferences.isRealtimeSyncEnabled() else false)
    }
    LaunchedEffect(syncCheckState) {
        val userId = appPreferences.getUserId()
        if (userId == null && (syncCheckState is SyncCheckState.RequiresDownloadConfirmation || syncCheckState is SyncCheckState.RequiresUploadConfirmation)) {
            Toast.makeText(context, "Error: Not logged in.", Toast.LENGTH_SHORT).show()
            isRealtimeSyncEnabled = false
            noteViewModel.resetSyncCheckState()
            return@LaunchedEffect
        }

        when (val state = syncCheckState) {
            is SyncCheckState.RequiresDownloadConfirmation -> {
                syncConfirmationDetails = SyncConfirmationDetails(
                    state = state,
                    title = "Enable Cloud Sync?",
                    text = "Cloud data found. Enabling sync will REPLACE all your current local data with data from the cloud. Backup local data first if needed. Continue?",
                    confirmAction = { noteViewModel.proceedWithSyncActivation(userId!!) }
                )
                showSyncConfirmDialog = true
                noteViewModel.resetSyncCheckState()
            }
            is SyncCheckState.RequiresUploadConfirmation -> {
                syncConfirmationDetails = SyncConfirmationDetails(
                    state = state,
                    title = "Enable Cloud Sync?",
                    text = "Local data found, but no cloud data. Enabling sync will UPLOAD your current local notes and folders to the cloud. Continue?",
                    confirmAction = { noteViewModel.activateSyncAndUploadLocalData(userId!!) }
                )
                showSyncConfirmDialog = true
                noteViewModel.resetSyncCheckState()
            }
            is SyncCheckState.CanEnableDirectly -> {
                appPreferences.saveRealtimeSyncEnabled(true)
                isRealtimeSyncEnabled = true
                noteViewModel.startSyncListeners()
                Toast.makeText(context, "Sync enabled", Toast.LENGTH_SHORT).show()
                noteViewModel.resetSyncCheckState()
            }
            is SyncCheckState.Error -> {
                Toast.makeText(context, "Error checking sync status: ${state.message}", Toast.LENGTH_LONG).show()
                isRealtimeSyncEnabled = false
                appPreferences.saveRealtimeSyncEnabled(false)
                noteViewModel.resetSyncCheckState()
            }
            is SyncCheckState.Idle -> { }
        }
    }
    LaunchedEffect(syncActivationState) {
        when (val state = syncActivationState) {
            is SyncActivationState.Running -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is SyncActivationState.Success -> {
                appPreferences.saveRealtimeSyncEnabled(true)
                isRealtimeSyncEnabled = true
                Toast.makeText(context, "Sync activated successfully!", Toast.LENGTH_LONG).show()
                noteViewModel.resetSyncActivationState()
            }
            is SyncActivationState.Error -> {
                Toast.makeText(context, "Error activating sync: ${state.message}", Toast.LENGTH_LONG).show()
                isRealtimeSyncEnabled = false
                appPreferences.saveRealtimeSyncEnabled(false)
                noteViewModel.resetSyncActivationState()
            }
            is SyncActivationState.Idle -> {   }
        }
    }
    LaunchedEffect(resetState) {
        when (val currentState = resetState) {
            is DataResetState.Success -> {
                Toast.makeText(context, "Reset successful!", Toast.LENGTH_LONG).show()
                delay(4000)
                noteViewModel.clearResetState()
            }
            is DataResetState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                delay(4000)
                noteViewModel.clearResetState()
            }
            else -> {  }
        }
    }
    LaunchedEffect(importState) {
        when (val currentState = importState) {
            is DataImportState.Success -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                delay(4000)
                noteViewModel.clearImportState()
            }
            is DataImportState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                delay(4000)
                noteViewModel.clearImportState()
            }
            else -> {  }
        }
    }
    val webClientId = context.getString(R.string.default_web_client_id)
    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }
    val firebaseAuth = remember { Firebase.auth }
    val handleSignInResult: (Task<GoogleSignInAccount>) -> Unit = { task ->
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            if (account?.idToken != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { firebaseTask ->
                        if (firebaseTask.isSuccessful) {
                            val firebaseUser = firebaseAuth.currentUser
                            appPreferences.saveLoginInfo(
                                true,
                                firebaseUser?.email,
                                firebaseUser?.uid
                            )
                            isLoggedIn = true
                            userEmail = firebaseUser?.email
                            firebaseUid = firebaseUser?.uid
                            isRealtimeSyncEnabled = appPreferences.isRealtimeSyncEnabled()
                        } else {
                            appPreferences.clearLoginInfo()
                            isLoggedIn = false
                            userEmail = null
                            firebaseUid = null
                            isRealtimeSyncEnabled = false
                            googleSignInClient.signOut()
                        }
                    }
            } else {
                appPreferences.clearLoginInfo()
                isLoggedIn = false
                userEmail = null
                firebaseUid = null
                isRealtimeSyncEnabled = false
            }
        } catch (e: ApiException) {
            appPreferences.clearLoginInfo()
            isLoggedIn = false
            userEmail = null
            firebaseUid = null
            isRealtimeSyncEnabled = false
        }
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(context, "Google Sign-In failed or cancelled.", Toast.LENGTH_SHORT).show()
        }
    }
    fun signIn() {
        if (firebaseAuth.currentUser != null) {
            firebaseAuth.signOut()
        }
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }
    fun signOut() {
        if (isRealtimeSyncEnabled) {
            noteViewModel.disableSyncing()
        }
        firebaseAuth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            appPreferences.clearLoginInfo()
            isLoggedIn = false
            userEmail = null
            firebaseUid = null
            isRealtimeSyncEnabled = false
        }
    }
    fun startImportProcess(clearFirst: Boolean) {
        importUriToProcess?.let { uri ->
            val message = if (clearFirst) "Replacing existing data..." else "Merging data..."
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            noteViewModel.importBackupData(uri, clearExistingDataFirst = clearFirst)
        }
        importUriToProcess = null
    }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Starting backup...", Toast.LENGTH_SHORT).show()
            scope.launch {
                val backupData = noteViewModel.prepareBackupData()
                if (backupData != null) {
                    val success = noteViewModel.writeBackupToFile(backupData, uri)
                    if (success) {
                        Toast.makeText(context, "Backup successful!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Backup failed during write.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Backup failed: Could not prepare data.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "Backup cancelled.", Toast.LENGTH_SHORT).show()
        }
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importUriToProcess = uri
            scope.launch {
                if (noteViewModel.hasUserData()) {
                    showImportOptionsDialog = true
                } else {
                    startImportProcess(clearFirst = false)
                }
            }
        } else {
            Toast.makeText(context, "Import cancelled.", Toast.LENGTH_SHORT).show()
            importUriToProcess = null
        }
    }
    fun startBackupProcess() {
        scope.launch {
            val filename = noteViewModel.createBackupFilename()
            createDocumentLauncher.launch(filename)
        }
    }
    fun handleSaveSettings() {
        val precision = decimalPrecisionText.toIntOrNull()
        when {
            precision == null || precision < 0 -> {
                showInvalidInputError = true
            }
            precision > 50 -> {
                showHighPrecisionWarning = true
            }
            else -> {
                appPreferences.saveDecimalPrecision(precision)
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                onBack()
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
                Button(
                    onClick = ::handleSaveSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Save Settings")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            if (isLoggedIn) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccountCircle, "Logged In User", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Signed in as:", style = MaterialTheme.typography.bodyMedium)
                        Text(userEmail ?: "N/A", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = ::signOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(MaterialTheme.colorScheme.error))
                ) { Text("Sign Out") }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Real-time Sync", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isRealtimeSyncEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                val userId = appPreferences.getUserId()
                                if (userId != null) {
                                    noteViewModel.checkFirestoreStatusAndInitiateSync(userId)
                                } else {
                                    Toast.makeText(context, "Error: Not logged in. Please sign in again.", Toast.LENGTH_LONG).show()
                                    isRealtimeSyncEnabled = false
                                }
                            } else {
                                noteViewModel.disableSyncing()
                                isRealtimeSyncEnabled = false
                                Toast.makeText(context, "Sync disabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = syncActivationState == SyncActivationState.Idle && isLoggedIn
                    )
                }
                if (syncActivationState is SyncActivationState.Running) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text((syncActivationState as SyncActivationState.Running).message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Text("Sign in with Google to sync your notes across devices.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = ::signIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in with Google") }
            }
            Divider(modifier = Modifier.padding(vertical = 20.dp))
            Text("Calculation Precision", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            Text("Number of decimal places in calculations", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = decimalPrecisionText,
                onValueChange = { newValue ->
                    decimalPrecisionText = newValue.filter { it.isDigit() }
                    showInvalidInputError = false
                },
                label = { Text("Decimal places") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showInvalidInputError,
                supportingText = { if (showInvalidInputError) Text("Please enter a valid positive number") },
                modifier = Modifier.fillMaxWidth()
            )
            Divider(modifier = Modifier.padding(vertical = 20.dp))
            Text("Data Management", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Button(onClick = ::startBackupProcess, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Backup Data") }
            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                enabled = importState == DataImportState.Idle || importState is DataImportState.Success || importState is DataImportState.Error
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdateAlt, "Import")
                    Spacer(Modifier.width(8.dp))
                    if (importState is DataImportState.Running) {
                        Text((importState as DataImportState.Running).message)
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Import Data")
                    }
                }
            }
            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(MaterialTheme.colorScheme.error)),
                enabled = resetState == DataResetState.Idle || resetState is DataResetState.Success || resetState is DataResetState.Error
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestoreFromTrash, "Reset")
                    Spacer(Modifier.width(8.dp))
                    if (resetState is DataResetState.Running) {
                        Text("Resetting...")
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Reset Application Data")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (showHighPrecisionWarning) {
            AlertDialog(
                onDismissRequest = { showHighPrecisionWarning = false },
                title = { Text("High Precision Warning") },
                text = { Text("High precision above 50 may cause slow calculations or crashes. You can lower it later in settings if needed.") },
                confirmButton = {
                    Button({
                        showHighPrecisionWarning = false
                        val precision = decimalPrecisionText.toIntOrNull()
                        if (precision != null) {
                            appPreferences.saveDecimalPrecision(precision)
                            Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            showInvalidInputError = true
                        }
                    }) { Text("OK") }
                },
                dismissButton = { Button(onClick = { showHighPrecisionWarning = false }) { Text("Cancel") } }
            )
        }
        if (showImportOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showImportOptionsDialog = false; importUriToProcess = null },
                title = { Text("Import Options") },
                text = { Text("Merge: Adds new items, renames conflicts.\nReplace: Deletes ALL local data before importing.") },
                confirmButton = {
                    Button(onClick = {
                        showImportOptionsDialog = false
                        startImportProcess(clearFirst = false)
                    }) { Text("Merge") }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showImportOptionsDialog = false
                            showImportConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Replace", color = MaterialTheme.colorScheme.onError) }
                }
            )
        }
        if (showImportConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showImportConfirmDialog = false; importUriToProcess = null },
                title = { Text("Confirm Replace Data") },
                text = { Text("REPLACING data will first DELETE all current notes and folders permanently. This cannot be undone. Are you absolutely sure?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showImportConfirmDialog = false
                            startImportProcess(clearFirst = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Replace All Data", color = MaterialTheme.colorScheme.onError) }
                },
                dismissButton = { Button(onClick = { showImportConfirmDialog = false; importUriToProcess = null }) { Text("Cancel") } }
            )
        }
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Confirm Reset") },
                text = { Text("Are you sure? This will delete ALL local notes and folders permanently and cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            noteViewModel.resetApplicationData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Reset Data", color = MaterialTheme.colorScheme.onError) }
                },
                dismissButton = { Button(onClick = { showResetConfirmDialog = false }) { Text("Cancel") } }
            )
        }
        if (showSyncConfirmDialog && syncConfirmationDetails != null) {
            val details = syncConfirmationDetails!!
            AlertDialog(
                onDismissRequest = {
                    showSyncConfirmDialog = false
                    syncConfirmationDetails = null
                    isRealtimeSyncEnabled = false
                },
                title = { Text(details.title) },
                text = { Text(details.text) },
                confirmButton = {
                    Button(
                        onClick = {
                            showSyncConfirmDialog = false
                            details.confirmAction()
                            syncConfirmationDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (details.state == SyncCheckState.RequiresDownloadConfirmation) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Continue",
                            color = if (details.state == SyncCheckState.RequiresDownloadConfirmation) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showSyncConfirmDialog = false
                        syncConfirmationDetails = null
                        isRealtimeSyncEnabled = false
                    }) { Text("Cancel") }
                }
            )
        }
    }
}