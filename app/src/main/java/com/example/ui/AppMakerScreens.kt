package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppMakerContent(
    viewModel: AppMakerViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    
    // Animate between navigation screens smoothly
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onCreateManualApp = { viewModel.navigateTo(Screen.ManualCreate(1)) },
                        onCreatePromptApp = { viewModel.navigateTo(Screen.TextPrompt) },
                        onSelectApp = { appId -> viewModel.navigateTo(Screen.AppMain(appId)) }
                    )
                }
                is Screen.TextPrompt -> {
                    TextPromptCreateScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.ManualCreate -> {
                    ManualCreateScreen(
                        viewModel = viewModel,
                        step = screen.step,
                        onNavigateStep = { step -> viewModel.navigateTo(Screen.ManualCreate(step)) },
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.AppMain -> {
                    AppMainScreen(
                        viewModel = viewModel,
                        appMainState = screen,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.AiDiagnostics -> {
                    AiDiagnosticsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }
        }
    }
}

data class AppTemplateInfo(
    val name: String,
    val icon: String,
    val description: String,
    val fields: List<ParsedField>,
    val records: List<Map<String, String>>
)

// ----------------------------------------------------
// HOME SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppMakerViewModel,
    onCreateManualApp: () -> Unit,
    onCreatePromptApp: () -> Unit,
    onSelectApp: (Int) -> Unit
) {
    val savedApps by viewModel.savedApps.collectAsState()
    val totalRecords by viewModel.totalAppRecordsCount.collectAsState()
    val appRecordsCounts by viewModel.appRecordsCounts.collectAsState()
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // Four Beautiful Preset Templates for instant sandbox generation
    val presetTemplates = listOf(
        AppTemplateInfo(
            name = "Landlord Expense Tracker",
            icon = "home",
            description = "Manage rent, maintenance, service invoices and clear payment status logs.",
            fields = listOf(
                ParsedField("Property Name", "text"),
                ParsedField("Category", "dropdown", "Rent,Repairs,Tax,Utilities,Management Fee"),
                ParsedField("Expense Amount ($)", "number"),
                ParsedField("Billing Date", "date"),
                ParsedField("Paid", "checkbox")
            ),
            records = listOf(
                mapOf("Property Name" to "Oakwood Villa #4A", "Category" to "Repairs", "Expense Amount ($)" to "345", "Billing Date" to "2026-06-18", "Paid" to "true"),
                mapOf("Property Name" to "Sunnyvale Apt #2C", "Category" to "Utilities", "Expense Amount ($)" to "120", "Billing Date" to "2026-06-21", "Paid" to "false"),
                mapOf("Property Name" to "Oakwood Villa #1B", "Category" to "Rent", "Expense Amount ($)" to "1800", "Billing Date" to "2026-06-01", "Paid" to "true")
            )
        ),
        AppTemplateInfo(
            name = "Gym Workout Journal",
            icon = "star",
            description = "Log fitness exercises, target reps, heavier weight loads and set completions.",
            fields = listOf(
                ParsedField("Exercise Name", "text"),
                ParsedField("Category", "dropdown", "Chest & Arms,Legs & Calf,Back & Shoulders,Cardio,Abs"),
                ParsedField("Target Reps", "number"),
                ParsedField("Weight Load (kg)", "number"),
                ParsedField("Goal Accomplished", "checkbox")
            ),
            records = listOf(
                mapOf("Exercise Name" to "Incline Dumbbell Press", "Category" to "Chest & Arms", "Target Reps" to "12", "Weight Load (kg)" to "26", "Goal Accomplished" to "true"),
                mapOf("Exercise Name" to "Barbell Back Squat", "Category" to "Legs & Calf", "Target Reps" to "10", "Weight Load (kg)" to "90", "Goal Accomplished" to "true"),
                mapOf("Exercise Name" to "Lat Pull-down", "Category" to "Back & Shoulders", "Target Reps" to "12", "Weight Load (kg)" to "55", "Goal Accomplished" to "false")
            )
        ),
        AppTemplateInfo(
            name = "Plant Moisture Companion",
            icon = "favorite",
            description = "Check indoor botanical specimen moisture logs, watering ml volume, and locations.",
            fields = listOf(
                ParsedField("Plant Specimen Name", "text"),
                ParsedField("Room Location", "dropdown", "Living Room,Bedroom Office,Sunlit Patio,Kitchen Shelf"),
                ParsedField("Water Added (ml)", "number"),
                ParsedField("Last Hydration Date", "date"),
                ParsedField("Indoor Specimen", "checkbox")
            ),
            records = listOf(
                mapOf("Plant Specimen Name" to "Elegant Monstera", "Room Location" to "Living Room", "Water Added (ml)" to "450", "Last Hydration Date" to "2026-06-20", "Indoor Specimen" to "true"),
                mapOf("Plant Specimen Name" to "Spotted Pink Orchid", "Room Location" to "Sunlit Patio", "Water Added (ml)" to "200", "Last Hydration Date" to "2026-06-22", "Indoor Specimen" to "false")
            )
        ),
        AppTemplateInfo(
            name = "Vehicle Care Log",
            icon = "list",
            description = "Track vehicle engine oil checks, service costs, odometer mileage and task action items.",
            fields = listOf(
                ParsedField("Car Model", "text"),
                ParsedField("Odometer (Miles)", "number"),
                ParsedField("Service Cost ($)", "number"),
                ParsedField("Mechanic Notes", "text"),
                ParsedField("Check Completed", "checkbox")
            ),
            records = listOf(
                mapOf("Car Model" to "Toyota RAV4 Hybrid", "Odometer (Miles)" to "42000", "Service Cost ($)" to "89", "Mechanic Notes" to "Synthetic oil swap and tire rotation", "Check Completed" to "true"),
                mapOf("Car Model" to "Tesla Model Y", "Odometer (Miles)" to "21500", "Service Cost ($)" to "0", "Mechanic Notes" to "Cabin air element replacement", "Check Completed" to "true")
            )
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Free App Maker",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Dashboard Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Admin Indicator")
                                Text(
                                    "ADMIN DASHBOARD",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.AiDiagnostics) },
                                modifier = Modifier.testTag("admin_diagnostics_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Diagnostics Settings",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Simulated Users",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "1 Active User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Column {
                                Text(
                                    "Apps Created",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${savedApps.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    "Total Records",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "$totalRecords",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick App Building Entry Options
            item {
                Text(
                    "Create an App Without Coding",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCreatePromptApp() }
                            .testTag("create_by_prompt_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Prompt Creator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Magic Prompt",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                              )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Type description",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCreateManualApp() }
                            .testTag("create_app_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = "Manual Creator",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Step-by-Step",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Pick fields manually",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Quick App Preset Templates Horizontal scroll Section
            item {
                Column {
                    Text(
                        "🚀 Instantly Spin Up a Classic Template",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "One-click complete database structures preloaded with sample entries:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    presetTemplates.forEach { template ->
                        Card(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable {
                                    viewModel.createPresetTemplate(
                                        name = template.name,
                                        icon = template.icon,
                                        description = template.description,
                                        fields = template.fields,
                                        prepopulateRecords = template.records
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            getIconVector(template.icon),
                                            contentDescription = "Template Icon",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        template.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.height(34.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${template.fields.size} fields | ${template.records.size} items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Create",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Generated Apps Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Your Generated Applications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (savedApps.isNotEmpty()) {
                        Text(
                            "${savedApps.size} total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search Bar for apps
            if (savedApps.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("app_search_bar"),
                        placeholder = { Text("Search your apps...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            val filteredApps = savedApps.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }

            if (savedApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "No apps icon",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Apps Created Yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Select a recipe from 'Classic Templates' or use the 'Magic Prompt' creator above!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "No results icon",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No Matching Applications Found",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "We couldn't find any app matching '$searchQuery'. Try adjusting your spelling or words.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredApps) { app ->
                    val recordCount = appRecordsCounts[app.id] ?: 0
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectApp(app.id) },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getIconVector(app.icon),
                                    contentDescription = "App Icon",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        app.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    
                                    SuggestionChip(
                                        onClick = { /* noop */ },
                                        label = { Text("$recordCount entries") },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    app.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = { viewModel.deleteEntireApp(app.id) },
                                modifier = Modifier.testTag("delete_app_${app.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete App",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CREATE BY TEXT PROMPT SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPromptCreateScreen(
    viewModel: AppMakerViewModel,
    onBack: () -> Unit
) {
    var promptText by remember { mutableStateOf("") }
    val isGenerating by viewModel.isGeneratingApp.collectAsState()
    val geminiError by viewModel.geminiErrorState.collectAsState()
    
    if (geminiError != null) {
        val error = geminiError!!
        var showDetails by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { viewModel.clearGeminiError() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = error.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = error.userFriendlyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (error.details != null) {
                        Button(
                            onClick = { showDetails = !showDetails },
                            colors = ButtonDefaults.textButtonColors(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (showDetails) "Hide Technical Logs ▲" else "Show Technical Logs ▼")
                        }
                        
                        if (showDetails) {
                            SelectionContainer {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 150.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = error.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp),
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (error.canFallback && error.agent != null) {
                        Button(
                            onClick = {
                                viewModel.generateAppLocalFallback(error.promptText, error.agent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Use Local Offline Fallback", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(
                        onClick = { viewModel.clearGeminiError() },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Magic Text Builder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Describe your App concept",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Enter what you want to track, and our parser will automatically define appropriate field names, data types, icons, and screens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("prompt_input_field"),
                        placeholder = {
                            Text("e.g., I need an app to track my pets' vaccinations with pet name, vaccine type, cost, and date given.")
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Text(
                        "Select Your AI Genius Specialist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Each specialist applies a different structural perspective and attaches extra professional attributes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val selectedAgent by viewModel.selectedCreatorAgent.collectAsState()
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CreatorAgent.values().forEach { agent ->
                            val isSelected = agent == selectedAgent
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectCreatorAgent(agent) }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(agent.avatar, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                agent.displayName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                agent.role,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            agent.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "How it works:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                              )
                              Spacer(modifier = Modifier.height(8.dp))
                              Text(
                                "• Nouns generate Custom Text fields (e.g. 'Pet Name', 'Notes')\n" +
                                "• Numbers indicate numeric fields (e.g. 'Cost', 'Amount')\n" +
                                "• Words like 'when', 'date' become Date fields (e.g. 'Date Given')\n" +
                                "• Words like 'done', 'paid' or 'status' add active/inactive checklist checkboxes",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (promptText.isNotBlank()) {
                        viewModel.generateAppFromPrompt(promptText)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_app_from_text"),
                enabled = promptText.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generating Schema...")
                } else {
                    Icon(Icons.Default.Star, contentDescription = "Sparkle Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate App From Text")
                }
            }
        }
    }
}

// ----------------------------------------------------
// STEP-BY-STEP MANUAL BUILDER SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCreateScreen(
    viewModel: AppMakerViewModel,
    step: Int,
    onNavigateStep: (Int) -> Unit,
    onBack: () -> Unit
) {
    // Stage states
    var appName by remember { mutableStateOf("") }
    var appDesc by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf("list") }
    var currentLayout by remember { mutableStateOf("list_detail_form") }

    // Temp field states
    var newFieldName by remember { mutableStateOf("") }
    var newFieldType by remember { mutableStateOf("text") }
    var newFieldOptions by remember { mutableStateOf("") }

    val manualDraftFields by viewModel.manualDraftFields.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step-by-Step App Builder (Step $step/3)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Screen content dependent on steps
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (step) {
                    1 -> {
                        // STEP 1: METADATA
                        Text(
                            "App Identity details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("App Name") },
                            placeholder = { Text("e.g. My Contact Book") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_name_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = appDesc,
                            onValueChange = { appDesc = it },
                            label = { Text("App Description") },
                            placeholder = { Text("e.g. Track contact information and list notes.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_desc_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "Select App Icon",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val iconList = listOf("list", "notes", "contacts", "inventory", "star", "phone", "email", "custom")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            iconList.forEach { iconName ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (appIcon == iconName) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { appIcon = iconName }
                                        .border(
                                            2.dp,
                                            if (appIcon == iconName) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        getIconVector(iconName),
                                        contentDescription = iconName,
                                        tint = if (appIcon == iconName) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // STEP 2: FIELD CREATION
                        Text(
                            "Add Fields to App Definition",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newFieldName,
                                onValueChange = { newFieldName = it },
                                label = { Text("Field Name") },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("field_name_input"),
                                singleLine = true
                            )

                            // Dropdown choice mapping type
                            var showTypeDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = newFieldType.uppercase(),
                                    onValueChange = {},
                                    label = { Text("Type") },
                                    readOnly = true,
                                    modifier = Modifier.clickable { showTypeDropdown = true },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.ArrowForward, // generic down/expand representation
                                            contentDescription = "Choose Type",
                                            modifier = Modifier.clickable { showTypeDropdown = true }
                                        )
                                    }
                                )

                                DropdownMenu(
                                    expanded = showTypeDropdown,
                                    onDismissRequest = { showTypeDropdown = false }
                                ) {
                                    listOf("text", "number", "date", "checkbox", "dropdown").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.uppercase()) },
                                            onClick = {
                                                newFieldType = type
                                                showTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (newFieldType == "dropdown") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newFieldOptions,
                                onValueChange = { newFieldOptions = it },
                                label = { Text("Options (comma-separated)") },
                                placeholder = { Text("e.g. Small,Medium,Large") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newFieldName.isNotBlank()) {
                                    viewModel.addManualField(
                                        ParsedField(
                                            name = newFieldName.trim(),
                                            type = newFieldType,
                                            options = newFieldOptions
                                        )
                                    )
                                    newFieldName = ""
                                    newFieldOptions = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_field_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Field Option")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Defined Fields (${manualDraftFields.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (manualDraftFields.isEmpty()) {
                            Text(
                                "No fields created yet. Please add fields like 'Item Name', 'Price' to database definition.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(manualDraftFields) { index, field ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(field.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Type: ${field.type.uppercase()}" + if (field.options.isNotBlank()) " (${field.options})" else "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { viewModel.removeManualField(index) }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove Field",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // STEP 3: LAYOUT SELECTOR & SUBMIT
                        Text(
                            "Choose Screens Layout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentLayout = "list_detail_form" }
                                .border(
                                    2.dp,
                                    if (currentLayout == "list_detail_form") MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentLayout == "list_detail_form",
                                    onClick = { currentLayout = "list_detail_form" }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("List + Form + Detail View Structure", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Generates 3 connected screens: record listing, field adding/editing form sheet, and detailed single entry viewing.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Next/Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { onNavigateStep(step - 1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Button(
                    onClick = {
                        if (step < 3) {
                            if (step == 1) {
                                viewModel.setManualMeta(
                                    title = appName,
                                    desc = appDesc,
                                    icon = appIcon,
                                    layout = currentLayout
                                )
                            }
                            onNavigateStep(step + 1)
                        } else {
                            viewModel.saveDraftApp()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag(if (step == 3) "save_created_app" else "next_step"),
                    enabled = (step != 1 || appName.isNotBlank())
                ) {
                    if (step == 3) {
                        Text("Save & Open App")
                    } else {
                        Text("Next Step")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// DYNAMIC APP WORKSPACE SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(
    viewModel: AppMakerViewModel,
    appMainState: Screen.AppMain,
    onBack: () -> Unit
) {
    val activeApp by viewModel.activeApp.collectAsState()
    val activeFields by viewModel.activeFields.collectAsState()
    val activeRecords by viewModel.activeRecords.collectAsState()
    val activeDetailRecord by viewModel.activeDetailRecord.collectAsState()

    if (activeApp == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getIconVector(activeApp?.icon ?: "list"), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(activeApp?.name ?: "Created App", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retreat back")
                    }
                },
                actions = {
                    // Option to delete entire app definition
                    IconButton(
                        onClick = { viewModel.deleteEntireApp(activeApp!!.id) },
                        modifier = Modifier.testTag("delete_app_action")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Application", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = appMainState.tab == AppTab.List,
                    onClick = {
                        viewModel.navigateTo(Screen.AppMain(appId = activeApp!!.id, tab = AppTab.List))
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "Records List") },
                    label = { Text("Records") }
                )
                NavigationBarItem(
                    selected = appMainState.tab == AppTab.Form,
                    onClick = {
                        viewModel.navigateTo(Screen.AppMain(appId = activeApp!!.id, tab = AppTab.Form))
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Entry") },
                    label = { Text("Add") }
                )
                NavigationBarItem(
                    selected = appMainState.tab == AppTab.Agent,
                    onClick = {
                        viewModel.navigateTo(Screen.AppMain(appId = activeApp!!.id, tab = AppTab.Agent))
                    },
                    icon = { Icon(Icons.Default.Face, contentDescription = "AI Copilot") },
                    label = { Text("AI Copilot") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (appMainState.tab != AppTab.Agent) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        activeApp?.description ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (appMainState.tab) {
                    AppTab.List -> {
                        // Dynamic List view screen
                        AppRecordListScreen(
                            records = activeRecords,
                            fields = activeFields,
                            viewModel = viewModel,
                            onAddRecord = {
                                viewModel.navigateTo(
                                    Screen.AppMain(
                                        appId = activeApp!!.id,
                                        tab = AppTab.Form
                                    )
                                )
                            },
                            onSelectRecord = { recordId ->
                                viewModel.navigateTo(
                                    Screen.AppMain(
                                        appId = activeApp!!.id,
                                        tab = AppTab.Detail,
                                        detailRecordId = recordId
                                    )
                                )
                            }
                        )
                    }

                    AppTab.Form -> {
                        // Dynamic Form screen
                        AppRecordFormScreen(
                            appId = activeApp!!.id,
                            fields = activeFields,
                            recordToEdit = activeDetailRecord,
                            viewModel = viewModel,
                            onSave = { detailsMap ->
                                if (appMainState.editRecordId != null) {
                                    viewModel.updateExistingRecord(
                                        recordId = appMainState.editRecordId,
                                        appId = activeApp!!.id,
                                        map = detailsMap
                                    )
                                } else {
                                    viewModel.saveNewRecord(
                                        appId = activeApp!!.id,
                                        map = detailsMap
                                    )
                                }
                            },
                            onCancel = {
                                viewModel.navigateTo(
                                    Screen.AppMain(
                                        appId = activeApp!!.id,
                                        tab = AppTab.List
                                    )
                                )
                            }
                        )
                    }

                    AppTab.Detail -> {
                        // Dynamic Detail Screen
                        AppRecordDetailScreen(
                            appId = activeApp!!.id,
                            record = activeDetailRecord,
                            fields = activeFields,
                            viewModel = viewModel,
                            onEdit = {
                                viewModel.navigateTo(
                                    Screen.AppMain(
                                        appId = activeApp!!.id,
                                        tab = AppTab.Form,
                                        editRecordId = activeDetailRecord!!.id
                                    )
                                )
                            },
                            onDelete = {
                                viewModel.deleteRecord(activeDetailRecord!!.id, activeApp!!.id)
                            },
                            onBack = {
                                viewModel.navigateTo(
                                    Screen.AppMain(
                                        appId = activeApp!!.id,
                                        tab = AppTab.List
                                    )
                                )
                            }
                        )
                    }

                    AppTab.Agent -> {
                        AppCopilotChatScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// DYNAMIC COMPOSABLE SUB-SCREENS FOR RECORD DETAILS
// ----------------------------------------------------
@Composable
fun AppRecordListScreen(
    records: List<AppRecord>,
    fields: List<AppField>,
    viewModel: AppMakerViewModel,
    onAddRecord: () -> Unit,
    onSelectRecord: (Int) -> Unit
) {
    val context = LocalContext.current
    var recordSearchQuery by rememberSaveable { mutableStateOf("") }

    val filteredRecords = remember(records, recordSearchQuery) {
        if (recordSearchQuery.isBlank()) {
            records
        } else {
            records.filter { record ->
                val data = viewModel.parseRecordData(record)
                data.values.any { valString ->
                    valString.contains(recordSearchQuery, ignoreCase = true)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "No Records Icon",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Entries Saved Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tap down to ADD a new item record following your dynamically configured schemas!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddRecord,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add First Record")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add First Entry")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search and Export Tool Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = recordSearchQuery,
                        onValueChange = { recordSearchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_search_input"),
                        placeholder = { Text("Search records...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (recordSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { recordSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Export / Share Button option
                    IconButton(
                        onClick = {
                            try {
                                val allRecordsData = records.map { rec ->
                                    viewModel.parseRecordData(rec).filterValues { it.isNotBlank() }
                                }
                                val jsonString = StringBuilder().apply {
                                    append("[\n")
                                    allRecordsData.forEachIndexed { index, rowMap ->
                                        append("  {\n")
                                        val entryStrings = rowMap.map { (k, v) -> "    \"$k\": \"$v\"" }
                                        append(entryStrings.joinToString(",\n"))
                                        append("\n  }")
                                        if (index < allRecordsData.lastIndex) append(",")
                                        append("\n")
                                    }
                                    append("]")
                                }.toString()

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Exported App Data", jsonString)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied all database entries to clipboard as JSON!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to copy: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Share, 
                            contentDescription = "Export Data", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (filteredRecords.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "No filtered records",
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Match Found", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No records contain the query '$recordSearchQuery'", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredRecords) { record ->
                                val data = viewModel.parseRecordData(record)
                                val firstTextField = fields.firstOrNull { it.type == "text" } ?: fields.firstOrNull()
                                val title = data[firstTextField?.name ?: ""] ?: "Entry #${record.id}"
                                
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectRecord(record.id) },
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Render secondary values briefly
                                        fields.filter { it.id != firstTextField?.id }.take(3).forEach { f ->
                                            val valString = data[f.name] ?: ""
                                            if (valString.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "${f.name}:",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.Gray
                                                    )
                                                    Text(
                                                        if (f.type == "checkbox") {
                                                            if (valString == "true") "✓ Yes" else "✗ No"
                                                        } else valString,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    FloatingActionButton(
                        onClick = onAddRecord,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("add_record_button"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add New DB Entry")
                    }
                }
            }
        }
    }
}

@Composable
fun AppRecordFormScreen(
    appId: Int,
    fields: List<AppField>,
    recordToEdit: AppRecord?,
    viewModel: AppMakerViewModel,
    onSave: (Map<String, String>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val initialData = if (recordToEdit != null) viewModel.parseRecordData(recordToEdit) else emptyMap()
    
    // Save state map
    val formData = remember { mutableStateMapOf<String, String>() }
    
    // Seed initial values
    LaunchedEffect(recordToEdit, fields) {
        fields.forEach { f ->
            formData[f.name] = initialData[f.name] ?: if (f.type == "checkbox") "false" else ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                if (recordToEdit != null) "Edit Entry Record" else "Add New Record",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Populate fields according to application constraints.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        items(fields) { field ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    field.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                when (field.type) {
                    "text" -> {
                        OutlinedTextField(
                            value = formData[field.name] ?: "",
                            onValueChange = { formData[field.name] = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter text") }
                        )
                    }

                    "number" -> {
                        OutlinedTextField(
                            value = formData[field.name] ?: "",
                            onValueChange = { formData[field.name] = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    "checkbox" -> {
                        val isChecked = formData[field.name] == "true"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { formData[field.name] = (!isChecked).toString() }
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { formData[field.name] = it.toString() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isChecked) "Enabled/True" else "Disabled/False")
                        }
                    }

                    "date" -> {
                        val currentValue = formData[field.name] ?: ""
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { formData[field.name] = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("YYYY-MM-DD") }
                            )

                            // Native android dialog builder
                            IconButton(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    val datePickerDialog = DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            formData[field.name] = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    )
                                    datePickerDialog.show()
                                }
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = "Open Calendar Selector")
                            }
                        }
                    }

                    "dropdown" -> {
                        val currentValue = formData[field.name] ?: ""
                        var showDrop by remember { mutableStateOf(false) }
                        val options = field.options.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDrop = true },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Home, // clean custom menu icon
                                        contentDescription = "Drop Menu",
                                        modifier = Modifier.clickable { showDrop = true }
                                    )
                                }
                            )

                            DropdownMenu(
                                expanded = showDrop,
                                onDismissRequest = { showDrop = false }
                            ) {
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            formData[field.name] = option
                                            showDrop = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val finalMap = formData.toMap()
                        onSave(finalMap)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_record_button")
                ) {
                    Text("Save Record")
                }
            }
        }
    }
}

@Composable
fun AppRecordDetailScreen(
    appId: Int,
    record: AppRecord?,
    fields: List<AppField>,
    viewModel: AppMakerViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    if (record == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error loading item details.")
        }
        return
    }

    val data = viewModel.parseRecordData(record)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return back")
                    }
                    Text(
                        "Entry Details",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(fields) { f ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            f.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (f.type == "checkbox") {
                                if (data[f.name] == "true") "✓ Yes" else "✗ No"
                            } else data[f.name] ?: "N/A",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Entry")
            }

            Button(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .testTag("delete_record_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Entry")
            }
        }
    }
}

// Map key string to material icons vector
fun getIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "list" -> Icons.Default.List
        "notes" -> Icons.Default.Edit
        "contacts" -> Icons.Default.Person
        "inventory" -> Icons.Default.ShoppingCart
        "star" -> Icons.Default.Star
        "phone" -> Icons.Default.Phone
        "email" -> Icons.Default.Email
        "date" -> Icons.Default.DateRange
        "home" -> Icons.Default.Home
        "favorite" -> Icons.Default.Favorite
        else -> Icons.Default.Build
    }
}

@Composable
fun AppCopilotChatScreen(
    viewModel: AppMakerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCopilotAgent by viewModel.selectedCopilotAgent.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    
    var userText by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Selector Row for multiple AI Copilot Agents!
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Activate AI Copilot Room Agent:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CopilotAgent.values().forEach { agent ->
                        val isSelected = agent == selectedCopilotAgent
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { viewModel.selectCopilotAgent(agent) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(agent.avatar, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    agent.displayName.substringBefore(" ("),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Agent Description Callout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(selectedCopilotAgent.avatar, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                selectedCopilotAgent.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // 2. Chat Pane (Messages List)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedCopilotAgent.avatar, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            if (isUser) "You" else msg.sender,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 12.dp
                                    )
                                )
                                .background(
                                    if (isUser) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedCopilotAgent.avatar, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Agent is working...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Commands Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp
        ) {
            Column {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedCopilotAgent) {
                        CopilotAgent.ANALYST -> {
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendCopilotMessage("Run a visual review analytical summary of our current records list metrics.")
                                },
                                label = { Text("📊 Summarize Analytics") }
                            )
                        }
                        CopilotAgent.WRITER -> {
                            SuggestionChip(
                                onClick = {
                                    viewModel.populateMockRecords()
                                },
                                label = { Text("✍️ Inject 3 Mock Records") }
                            )
                        }
                        CopilotAgent.AUDITOR -> {
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendCopilotMessage("Perform QA scans on database rows to audit fields.")
                                },
                                label = { Text("🐛 Run Scan Audit") }
                            )
                        }
                        CopilotAgent.SEARCHER -> {
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendCopilotMessage("Synthesize deep-dive web-inspired insights on our database schema and topic.")
                                },
                                label = { Text("🔍 Synthesize Insights") }
                            )
                        }
                        CopilotAgent.COACH -> {
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendCopilotMessage("Give me a goal breakdown and mindfulness coaching review of our current database concept.")
                                },
                                label = { Text("🧘 Wellness Review") }
                            )
                        }
                        CopilotAgent.TRANSLATOR -> {
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendCopilotMessage("Translate our current database records into Spanish, French, and Japanese.")
                                },
                                label = { Text("🌐 Translate Records") }
                            )
                        }
                        CopilotAgent.CODER -> {
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendCopilotMessage("Generate a clean developer-ready JSON representation of this database schema and records.")
                                },
                                label = { Text("🤖 Export Developer JSON") }
                            )
                        }
                    }
                }

                // 4. Input Text Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userText,
                        onValueChange = { userText = it },
                        placeholder = { Text("Ask ${selectedCopilotAgent.displayName.substringBefore(" (")} anything...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copilot_chat_input"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        trailingIcon = {
                            if (userText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val query = userText
                                        userText = ""
                                        viewModel.sendCopilotMessage(query)
                                    },
                                    modifier = Modifier.testTag("copilot_send_button")
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Send Message",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDiagnosticsScreen(
    viewModel: AppMakerViewModel,
    onBack: () -> Unit
) {
    val errorHistory by viewModel.errorHistoryLogs.collectAsState()
    val testResult by viewModel.testProbeResult.collectAsState()
    val isTesting by viewModel.isTestingProgress.collectAsState()
    val simulatedError by viewModel.simulatedErrorState.collectAsState()
    val simulateLatency by viewModel.simulateLatencyState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Engine & Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (testResult?.isSuccess == true) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else if (testResult != null) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (testResult == null) {
                                    Icons.Default.Info
                                } else if (testResult!!.isSuccess) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Warning
                                },
                                contentDescription = "Status Icon",
                                tint = if (testResult == null) {
                                    MaterialTheme.colorScheme.secondary
                                } else if (testResult!!.isSuccess) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Live Engine Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val statusSummary = when {
                            testResult == null -> "Diagnostics not yet run. Tap the button below to execute a live connection test to Google Gemini API."
                            testResult!!.isSuccess -> "System Connected and Operational! Response round-trip latency: ${testResult!!.latencyMs}ms."
                            else -> "Inoperational / Blocked: ${testResult!!.message}"
                        }
                        
                        Text(
                            text = statusSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.runLiveApiTest() },
                            modifier = Modifier.fillMaxWidth().testTag("run_live_test_button"),
                            enabled = !isTesting
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting to Google Cloud...")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Run Test")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Connection Test Probe")
                            }
                        }
                    }
                }
            }

            // 2. Error Outage Simulator Section (CRITICAL FOR IN-BROWSER TESTING)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Simulator",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Outage & Error Simulator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Simulate connection bottleneck latencies and formatting errors. Let's see how our custom boundaries, retry handlers, and offline local analyzers react to various server failure conditions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Select Simulation Error Dropdown/Option list
                        Text(
                            "Simulate Server Error Case:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        SimulatedErrorType.values().forEach { errorType ->
                            val isSelected = simulatedError == errorType
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSimulatedError(errorType) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setSimulatedError(errorType) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    errorType.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Simulate latency toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Simulate Network Latency",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Injects a 3.0 second artificial overhead loading delay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = simulateLatency,
                                onCheckedChange = { viewModel.setSimulateLatency(it) }
                            )
                        }
                    }
                }
            }

            // 3. Technical Logs / Error History Logger
            item {
                Text(
                    "Error Diagnostics Logger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (errorHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "No errors Logged",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No Gemini API errors logged in this session.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(errorHistory) { logText ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        SelectionContainer {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    logText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
