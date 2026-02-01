package com.openclaw.assistant

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.assistant.api.OpenClawClient
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.ui.theme.OpenClawAssistantTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository.getInstance(this)

        setContent {
            OpenClawAssistantTheme {
                SettingsScreen(
                    settings = settings,
                    onSave = { 
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var webhookUrl by remember { mutableStateOf(settings.webhookUrl) }
    var authToken by remember { mutableStateOf(settings.authToken) }
    var ttsEnabled by remember { mutableStateOf(settings.ttsEnabled) }
    var continuousMode by remember { mutableStateOf(settings.continuousMode) }
    var wakeWordPreset by remember { mutableStateOf(settings.wakeWordPreset) }
    var customWakeWord by remember { mutableStateOf(settings.customWakeWord) }
    
    var showAuthToken by remember { mutableStateOf(false) }
    var showWakeWordMenu by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiClient = remember { OpenClawClient() }
    
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }

    // Wake word options
    val wakeWordOptions = listOf(
        SettingsRepository.WAKE_WORD_OPEN_CLAW to "OpenClaw",
        SettingsRepository.WAKE_WORD_HEY_ASSISTANT to "Hey Assistant",
        SettingsRepository.WAKE_WORD_JARVIS to "Jarvis",
        SettingsRepository.WAKE_WORD_COMPUTER to "Computer",
        SettingsRepository.WAKE_WORD_CUSTOM to "Custom..."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            settings.webhookUrl = webhookUrl
                            settings.authToken = authToken
                            settings.ttsEnabled = ttsEnabled
                            settings.continuousMode = continuousMode
                            settings.wakeWordPreset = wakeWordPreset
                            settings.customWakeWord = customWakeWord
                            onSave()
                        },
                        enabled = webhookUrl.isNotBlank() && !isTesting
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // === CONNECTION SECTION ===
            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Webhook URL
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { 
                            webhookUrl = it
                            testResult = null
                        },
                        label = { Text("Webhook URL *") },
                        placeholder = { Text("https://your-server/hooks/voice") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        isError = webhookUrl.isBlank()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auth Token
                    OutlinedTextField(
                        value = authToken,
                        onValueChange = { 
                            authToken = it
                            testResult = null
                        },
                        label = { Text("Auth Token (optional)") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showAuthToken = !showAuthToken }) {
                                Icon(
                                    if (showAuthToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        visualTransformation = if (showAuthToken) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Connection Button
                    Button(
                        onClick = {
                            if (webhookUrl.isBlank()) return@Button
                            scope.launch {
                                try {
                                    isTesting = true
                                    testResult = null
                                    val result = apiClient.testConnection(webhookUrl, authToken)
                                    result.fold(
                                        onSuccess = {
                                            testResult = TestResult(success = true, message = "Connected!")
                                            settings.webhookUrl = webhookUrl
                                            settings.authToken = authToken
                                            settings.isVerified = true
                                        },
                                        onFailure = {
                                            testResult = TestResult(success = false, message = "Failed: ${it.message}")
                                        }
                                    )
                                } catch (e: Exception) {
                                    testResult = TestResult(success = false, message = "Error: ${e.message}")
                                } finally {
                                    isTesting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                testResult?.success == true -> Color(0xFF4CAF50)
                                testResult?.success == false -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        enabled = webhookUrl.isNotBlank() && !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(
                                when {
                                    testResult?.success == true -> Icons.Default.Check
                                    testResult?.success == false -> Icons.Default.Error
                                    else -> Icons.Default.NetworkCheck
                                },
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(testResult?.message ?: "Test Connection")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === VOICE SECTION ===
            Text(
                text = "Voice",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Voice Output", style = MaterialTheme.typography.bodyLarge)
                            Text("Read AI responses aloud", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = ttsEnabled, onCheckedChange = { ttsEnabled = it })
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Conversation", style = MaterialTheme.typography.bodyLarge)
                            Text("Auto-start mic after AI speaks", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = continuousMode, onCheckedChange = { continuousMode = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === WAKE WORD SECTION ===
            Text(
                text = "Wake Word",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = showWakeWordMenu,
                        onExpandedChange = { showWakeWordMenu = it }
                    ) {
                        OutlinedTextField(
                            value = wakeWordOptions.find { it.first == wakeWordPreset }?.second ?: "OpenClaw",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Activation Phrase") },
                            leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showWakeWordMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showWakeWordMenu,
                            onDismissRequest = { showWakeWordMenu = false }
                        ) {
                            wakeWordOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        wakeWordPreset = value
                                        showWakeWordMenu = false
                                    },
                                    leadingIcon = {
                                        if (wakeWordPreset == value) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    if (wakeWordPreset == SettingsRepository.WAKE_WORD_CUSTOM) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customWakeWord,
                            onValueChange = { customWakeWord = it.lowercase() },
                            label = { Text("Custom Wake Word") },
                            placeholder = { Text("e.g., hey buddy") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text("Enter 2-3 words, lowercase", color = Color.Gray, fontSize = 12.sp)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

data class TestResult(
    val success: Boolean,
    val message: String
)
