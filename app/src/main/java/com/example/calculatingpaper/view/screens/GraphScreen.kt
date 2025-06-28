package com.example.calculatingpaper.view.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.view.components.MpLineGraph
import kotlinx.serialization.json.Json
import java.math.BigDecimal


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    navController: NavController,
    equation: String,
    noteId: Long,
    variablesJson: String,
    onNavigateBack: () -> Unit
) {
    var xMinInput by remember { mutableStateOf("-10") }
    var xMaxInput by remember { mutableStateOf("10") }

    var xMinCommitted by remember { mutableStateOf("-10") }
    var xMaxCommitted by remember { mutableStateOf("10") }

    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    var isDegreesMode by remember { mutableStateOf(appPreferences.getCalculationMode()) }

    val focusManager = LocalFocusManager.current

    val variables by remember(variablesJson) {
        mutableStateOf(
            try {
                if (variablesJson.isNotEmpty()) {
                    val stringVariables = Json.decodeFromString<Map<String, String>>(variablesJson)

                    stringVariables.mapValues { BigDecimal(it.value) }

                } else {
                    emptyMap<String, BigDecimal>()
                }
            } catch (e: Exception) {
                emptyMap<String, BigDecimal>()
            }
        )
    }

    val commitChanges = {
        xMinCommitted = xMinInput
        xMaxCommitted = xMaxInput
        focusManager.clearFocus()
    }


    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Graph: $equation", maxLines = 1, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = xMinInput,
                    onValueChange = { xMinInput = it.filterNot { c -> c.isWhitespace() } },
                    label = { Text("X Min") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = xMaxInput,
                    onValueChange = { xMaxInput = it.filterNot { c -> c.isWhitespace() } },
                    label = { Text("X Max") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { commitChanges() }
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        xMinInput = "-10"
                        xMaxInput = "10"
                        commitChanges()
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Reset", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = {
                        val newMode = !isDegreesMode
                        isDegreesMode = newMode
                        appPreferences.saveCalculationMode(newMode)
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 70.dp),
                    border = BorderStroke(1.dp, Color.Black),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(if (isDegreesMode) "DEG" else "RAD", style = MaterialTheme.typography.labelMedium)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                MpLineGraph(
                    equation = equation,
                    variables = variables,
                    isDegreesMode = isDegreesMode,
                    xMinText = xMinCommitted,
                    xMaxText = xMaxCommitted,
                    onXMinTextChange = { xMinInput = it },
                    onXMaxTextChange = { xMaxInput = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}