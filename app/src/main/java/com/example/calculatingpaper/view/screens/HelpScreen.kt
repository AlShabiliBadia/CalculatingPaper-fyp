package com.example.calculatingpaper.view.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to CalculatingPaper!",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your all-in-one solution for mathematical note-taking, calculations, and graphing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Expandable Accordion Sections for each help topic
            AccordionSection(title = "Getting Started") {
                HelpContent(
                    subTitle = "Main Interface",
                    points = listOf(
                        "A top navigation bar with search and menu options",
                        "The main content area showing your notes and folders",
                        "A floating action button to create new notes or folders"
                    )
                )
                HelpContent(
                    subTitle = "Creating Your First Note",
                    isOrdered = true,
                    points = listOf(
                        "Tap the \"+\" button in the bottom right corner",
                        "Select \"New Note\"",
                        "Start typing your content",
                        "The note will automatically save as you type",
                        "To return to the main screen, tap the back arrow"
                    )
                )
            }

            AccordionSection(title = "Note Management") {
                HelpContent(
                    subTitle = "Creating and Editing Notes",
                    points = listOf(
                        "Create a new note: Tap the \"+\" button and select \"New Note\"",
                        "Edit an existing note: Tap on any note to open it",
                        "Auto-save: Notes save automatically as you type",
                        "Note titles: Titles are auto-generated from content, but you can change them manually"
                    )
                )
                HelpContent(
                    subTitle = "Note Actions",
                    points = listOf(
                        "Pin important notes: Swipe right on a note and tap the pin icon",
                        "Archive a note: Swipe left on a note and select the archive option",
                        "Delete a note: Swipe left on a note and select the delete option",
                        "Move a note: Long press on a note and select where to move the note",
                        "Rename a note: Press on a note title and rename it"
                    )
                )
                HelpContent(
                    subTitle = "Restoring Notes",
                    points = listOf(
                        "Access archived notes by selecting \"Archive Folder\" from the menu",
                        "Access deleted notes by selecting \"Recycle Bin Folder\" from the menu",
                        "Restore notes by swiping right on them and selecting restore"
                    )
                )
            }

            AccordionSection(title = "Mathematical Input & Calculations") {
                HelpContent(
                    subTitle = "Using the Math Keyboard",
                    isOrdered = true,
                    points = listOf(
                        "While editing a note, tap the math keyboard button to switch to the math keyboard",
                        "Use the keyboard to enter mathematical expressions",
                        "Tap the \"ABC\" button to switch back to the regular keyboard"
                    )
                )
                HelpContent(
                    subTitle = "Math Keyboard Features",
                    points = listOf(
                        "Basic operations: Addition (+), subtraction (-), multiplication (*), division (/), modulo (%)",
                        "Functions: Trigonometric (sin, cos, tan, etc.), logarithmic (ln, log), exponential (e^x, exp)",
                        "Constants: π (pi), e (Euler's number)",
                        "Special keys: Parentheses, power (^), square root (√), absolute value (abs)",
                        "Mode toggle: Switch between degrees (DEG) and radians (RAD)"
                    )
                )
                HelpContent(
                    subTitle = "Precision Settings",
                    isOrdered = true,
                    points = listOf(
                        "Tap the infinity (∞) button on the math keyboard",
                        "Enter your desired number of decimal places",
                        "Tap \"Save\""
                    ),
                    note = "Setting precision above 50 decimal places may cause performance issues."
                )
            }

            AccordionSection(title = "Graphing Functions") {
                HelpContent(
                    subTitle = "Creating a Graph",
                    isOrdered = true,
                    points = listOf(
                        "Enter a mathematical expression in your note",
                        "Tap the graph icon or select \"Graph\" from the menu",
                        "The expression will be plotted on a graph"
                    )
                )
                HelpContent(
                    subTitle = "Graph Interaction",
                    points = listOf(
                        "Zoom: Pinch to zoom in or out",
                        "Pan: Drag to move around the graph"
                    ),
                    note = "Graphs are limited to functions of a single variable (typically x)."
                )
            }

            // Add other sections in the same pattern...
            // e.g., AccordionSection(title = "Organizing Your Notes") { ... }

            AccordionSection(title = "Backup & Restore") {
                HelpContent(
                    subTitle = "Creating a Backup",
                    isOrdered = true,
                    points = listOf(
                        "Go to Settings",
                        "Select \"Backup & Restore\"",
                        "Tap \"Create Backup\"",
                        "Choose a location to save your backup file"
                    )
                )
                HelpContent(
                    subTitle = "Restoring from Backup",
                    isOrdered = true,
                    points = listOf(
                        "Go to Settings",
                        "Select \"Backup & Restore\"",
                        "Tap \"Restore from Backup\"",
                        "Choose whether to clear existing data or merge",
                        "Select your backup file"
                    ),
                    note = "Backup files contain all your notes and folders. There is no option for selective backup or restore."
                )
            }

            AccordionSection(title = "Cloud Synchronization") {
                HelpContent(
                    subTitle = "Setting Up Sync",
                    isOrdered = true,
                    points = listOf(
                        "Go to Settings",
                        "Sign in with your account",
                        "Toggle on the real time syncing"
                    ),
                    note = "Sync requires an internet connection and syncs all data."
                )
            }
        }
    }
}


/**
 * A reusable, expandable accordion-style section.
 * @param title The title to display on the card header.
 * @param content The content to show when the card is expanded.
 */
@Composable
private fun AccordionSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    content()
                }
            }
        }
    }
}


@Composable
private fun HelpContent(
    subTitle: String,
    isOrdered: Boolean = false,
    points: List<String>,
    note: String? = null
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = subTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        points.forEachIndexed { index, point ->
            Text(
                text = if (isOrdered) "${index + 1}. $point" else "• $point",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp),
                lineHeight = 20.sp
            )
        }
        if (note != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Note: $note",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}