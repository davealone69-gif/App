package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Screen {
    object Home : Screen
    object TextPrompt : Screen
    object AiDiagnostics : Screen
    
    // Step configuration for manual builder
    data class ManualCreate(val step: Int = 1) : Screen
    
    // Workspace of a launched app
    data class AppMain(
        val appId: Int,
        val tab: AppTab = AppTab.List,
        val editRecordId: Int? = null,
        val detailRecordId: Int? = null
    ) : Screen
}

enum class AppTab {
    List, Form, Detail, Agent
}

class AppMakerViewModel(private val repository: AppMakerRepository) : ViewModel() {

    // View navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation backstack
    private val screenHistory = mutableListOf<Screen>()

    // All available apps
    val savedApps: StateFlow<List<AppDefinition>> = repository.allApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active workspace app definition
    private val _activeApp = MutableStateFlow<AppDefinition?>(null)
    val activeApp: StateFlow<AppDefinition?> = _activeApp.asStateFlow()

    // Active workspace fields list
    private val _activeFields = MutableStateFlow<List<AppField>>(emptyList())
    val activeFields: StateFlow<List<AppField>> = _activeFields.asStateFlow()

    // Active workspace records list
    private val _activeRecords = MutableStateFlow<List<AppRecord>>(emptyList())
    val activeRecords: StateFlow<List<AppRecord>> = _activeRecords.asStateFlow()

    // Active single record details
    private val _activeDetailRecord = MutableStateFlow<AppRecord?>(null)
    val activeDetailRecord: StateFlow<AppRecord?> = _activeDetailRecord.asStateFlow()

    // Draft for Manual Creation (AppDefinition and List of AppFields)
    private val _manualDraftTitle = MutableStateFlow("")
    val manualDraftTitle = _manualDraftTitle.asStateFlow()

    private val _manualDraftDesc = MutableStateFlow("")
    val manualDraftDesc = _manualDraftDesc.asStateFlow()

    private val _manualDraftIcon = MutableStateFlow("list")
    val manualDraftIcon = _manualDraftIcon.asStateFlow()

    private val _manualDraftLayout = MutableStateFlow("list_detail_form")
    val manualDraftLayout = _manualDraftLayout.asStateFlow()

    private val _manualDraftFields = MutableStateFlow<List<ParsedField>>(emptyList())
    val manualDraftFields = _manualDraftFields.asStateFlow()

    // Total records count (cross-app stats for Admin Dashboard)
    private val _totalAppRecordsCount = MutableStateFlow(0)
    val totalAppRecordsCount = _totalAppRecordsCount.asStateFlow()

    private val _appRecordsCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val appRecordsCounts = _appRecordsCounts.asStateFlow()

    fun refreshStats() {
        viewModelScope.launch {
            val apps = savedApps.value
            val counts = mutableMapOf<Int, Int>()
            var total = 0
            apps.forEach { app ->
                val records = repository.getRecordsForApp(app.id).firstOrNull() ?: emptyList()
                counts[app.id] = records.size
                total += records.size
            }
            _appRecordsCounts.value = counts
            _totalAppRecordsCount.value = total
        }
    }

    init {
        // Collect cross-app count for Admin Dashboard
        viewModelScope.launch {
            savedApps.collect { apps ->
                refreshStats()
            }
        }
    }

    fun createPresetTemplate(
        name: String,
        icon: String,
        description: String,
        fields: List<ParsedField>,
        prepopulateRecords: List<Map<String, String>>
    ) {
        viewModelScope.launch {
            val appId = repository.createApp(
                name = name,
                icon = icon,
                description = description,
                layoutType = "list_detail_form",
                fields = fields
            )
            // Prepopulate some default records
            prepopulateRecords.forEach { recordMap ->
                repository.saveRecord(appId, recordMap)
            }
            refreshStats()
            navigateTo(Screen.AppMain(appId = appId, tab = AppTab.List))
        }
    }

    fun navigateTo(screen: Screen) {
        screenHistory.add(_currentScreen.value)
        _currentScreen.value = screen
        
        // If entering an App workspace, load its content
        if (screen is Screen.AppMain) {
            loadAppWorkspace(screen)
        }
    }

    fun navigateBack() {
        if (screenHistory.isNotEmpty()) {
            val prev = screenHistory.removeAt(screenHistory.size - 1)
            _currentScreen.value = prev
            if (prev is Screen.AppMain) {
                loadAppWorkspace(prev)
            }
        } else {
            _currentScreen.value = Screen.Home
        }
    }

    // Creator Agent selection for App builder screen
    private val _selectedCreatorAgent = MutableStateFlow<CreatorAgent>(CreatorAgent.ARCHITECT)
    val selectedCreatorAgent = _selectedCreatorAgent.asStateFlow()

    fun selectCreatorAgent(agent: CreatorAgent) {
        _selectedCreatorAgent.value = agent
    }

    // Chat Message Class
    data class ChatMessage(
        val sender: String,
        val text: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Current active Copilot Agent
    private val _selectedCopilotAgent = MutableStateFlow<CopilotAgent>(CopilotAgent.ANALYST)
    val selectedCopilotAgent = _selectedCopilotAgent.asStateFlow()

    // Chat messages list
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    // Chat loading indicator state
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    fun selectCopilotAgent(agent: CopilotAgent) {
        _selectedCopilotAgent.value = agent
        resetChatWithAgent(agent)
    }

    private fun resetChatWithAgent(agent: CopilotAgent) {
        val appName = _activeApp.value?.name ?: "Custom Database"
        val customGreeting = agent.initialMessage.replace("your database", "your database (**$appName**)")
        _chatMessages.value = listOf(
            ChatMessage(
                sender = agent.displayName,
                text = customGreeting,
                isUser = false
            )
        )
    }

    fun addAssistantMessage(text: String) {
        val current = _chatMessages.value.toMutableList()
        val agent = _selectedCopilotAgent.value
        current.add(ChatMessage(sender = agent.displayName, text = text, isUser = false))
        _chatMessages.value = current
    }

    fun sendCopilotMessage(userText: String) {
        if (userText.isBlank()) return
        
        // 1. Append user message
        val current = _chatMessages.value.toMutableList()
        current.add(ChatMessage(sender = "User", text = userText, isUser = true))
        _chatMessages.value = current

        _isChatLoading.value = true

        val activeAppVal = _activeApp.value
        val fieldsVal = _activeFields.value
        val recordsVal = _activeRecords.value
        val activeAgent = _selectedCopilotAgent.value

        viewModelScope.launch {
            try {
                // If the app is active, build its state
                val appName = activeAppVal?.name ?: "Custom Database"
                
                // Deserialize records so Gemini can inspect them
                val recordsMaps = recordsVal.map { parseRecordData(it) }

                // Query Gemini
                val response = GeminiAgentHelper.runCopilotAgent(
                    agent = activeAgent,
                    appName = appName,
                    fields = fieldsVal,
                    records = recordsMaps,
                    customPrompt = userText
                )

                _isChatLoading.value = false
                val updated = _chatMessages.value.toMutableList()
                updated.add(ChatMessage(sender = activeAgent.displayName, text = response, isUser = false))
                _chatMessages.value = updated
            } catch (e: Exception) {
                _isChatLoading.value = false
                val updated = _chatMessages.value.toMutableList()
                updated.add(ChatMessage(sender = activeAgent.displayName, text = "An error occurred: ${e.message}. Please try again.", isUser = false))
                _chatMessages.value = updated
            }
        }
    }

    fun populateMockRecords() {
        val app = _activeApp.value ?: return
        val fields = _activeFields.value
        
        _isChatLoading.value = true
        
        viewModelScope.launch {
            // Generate 3 rows
            val row1 = fields.associate { field ->
                val mockVal = when (field.type) {
                    "number" -> "25"
                    "checkbox" -> "true"
                    "date" -> "2026-06-23"
                    "dropdown" -> field.options.split(",").firstOrNull() ?: "Option A"
                    else -> {
                        if (field.name.lowercase().contains("name")) {
                            if (app.name.lowercase().contains("pet")) "Buddy" else "John Doe"
                        } else if (field.name.lowercase().contains("vaccine")) {
                            "Rabies Shot"
                        } else if (field.name.lowercase().contains("title") || field.name.lowercase().contains("task")) {
                            "Follow up with client"
                        } else {
                            "Pristine Entry Alpha"
                        }
                    }
                }
                field.name to mockVal
            }
            val row2 = fields.associate { field ->
                val mockVal = when (field.type) {
                    "number" -> "150"
                    "checkbox" -> "false"
                    "date" -> "2026-07-14"
                    "dropdown" -> {
                        val opts = field.options.split(",")
                        opts.getOrNull(1) ?: opts.firstOrNull() ?: "Option B"
                    }
                    else -> {
                        if (field.name.lowercase().contains("name")) {
                            if (app.name.lowercase().contains("pet")) "Max" else "Jane Smith"
                        } else if (field.name.lowercase().contains("vaccine")) {
                            "Parvovirus Boost"
                        } else if (field.name.lowercase().contains("title") || field.name.lowercase().contains("task")) {
                            "Schedule dental cleanup"
                        } else {
                            "Pristine Entry Beta"
                        }
                    }
                }
                field.name to mockVal
            }
            val row3 = fields.associate { field ->
                val mockVal = when (field.type) {
                    "number" -> "8"
                    "checkbox" -> "true"
                    "date" -> "2026-06-25"
                    "dropdown" -> {
                        val opts = field.options.split(",")
                        opts.lastOrNull() ?: "Option C"
                    }
                    else -> {
                        if (field.name.lowercase().contains("name")) {
                            if (app.name.lowercase().contains("pet")) "Luna" else "Vip Member"
                        } else if (field.name.lowercase().contains("vaccine")) {
                            "Feline Distemper"
                        } else if (field.name.lowercase().contains("title") || field.name.lowercase().contains("task")) {
                            "Update stock inventories"
                        } else {
                            "Pristine Entry Gamma"
                        }
                    }
                }
                field.name to mockVal
            }
            
            repository.saveRecord(app.id, row1)
            repository.saveRecord(app.id, row2)
            repository.saveRecord(app.id, row3)
            refreshStats()
            
            _isChatLoading.value = false
            addAssistantMessage("Successfully injected **3 mock records** conforming directly to your fields into the database! Head to the **Records List** tab to see them immediately.")
        }
    }

    // Load active app information
    private fun loadAppWorkspace(appMain: Screen.AppMain) {
        viewModelScope.launch {
            val app = repository.getAppById(appMain.appId)
            _activeApp.value = app
            if (app != null) {
                // Collect fields
                repository.getFieldsForApp(app.id).collect { fields ->
                    _activeFields.value = fields
                }
            }
        }
        
        // Collect records
        viewModelScope.launch {
            repository.getRecordsForApp(appMain.appId).collect { records ->
                _activeRecords.value = records
                
                if (appMain.detailRecordId != null) {
                    _activeDetailRecord.value = records.find { it.id == appMain.detailRecordId }
                } else if (appMain.editRecordId != null) {
                    _activeDetailRecord.value = records.find { it.id == appMain.editRecordId }
                } else {
                    _activeDetailRecord.value = null
                }
            }
        }

        // Initialize Copilot Chat
        resetChatWithAgent(_selectedCopilotAgent.value)
    }

    // Create by Text Prompt logic
    data class GeminiErrorState(
        val title: String,
        val userFriendlyMessage: String,
        val details: String? = null,
        val canFallback: Boolean = true,
        val promptText: String = "",
        val agent: CreatorAgent? = null
    )

    private val _geminiError = MutableStateFlow<GeminiErrorState?>(null)
    val geminiErrorState = _geminiError.asStateFlow()

    private val _isGeneratingApp = MutableStateFlow(false)
    val isGeneratingApp = _isGeneratingApp.asStateFlow()

    // Interactive diagnostics suite states
    private val _testProbeResult = MutableStateFlow<DiagnosticResult?>(null)
    val testProbeResult = _testProbeResult.asStateFlow()

    private val _isTestingProgress = MutableStateFlow(false)
    val isTestingProgress = _isTestingProgress.asStateFlow()

    private val _errorHistoryLogs = MutableStateFlow<List<String>>(emptyList())
    val errorHistoryLogs = _errorHistoryLogs.asStateFlow()

    private val _simulatedErrorState = MutableStateFlow(SimulatedErrorType.NONE)
    val simulatedErrorState = _simulatedErrorState.asStateFlow()

    private val _simulateLatencyState = MutableStateFlow(false)
    val simulateLatencyState = _simulateLatencyState.asStateFlow()

    fun clearGeminiError() {
        _geminiError.value = null
    }

    private fun logTechnicalError(title: String, message: String?, details: String?) {
        val current = _errorHistoryLogs.value.toMutableList()
        val timeStamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timeStamp] $title\nMessage: ${message ?: "N/A"}\nDetails: ${details ?: "No extra trace information."}"
        current.add(0, logEntry)
        _errorHistoryLogs.value = current
    }

    fun setSimulatedError(type: SimulatedErrorType) {
        _simulatedErrorState.value = type
        GeminiAgentHelper.simulatedErrorState = type
    }

    fun setSimulateLatency(active: Boolean) {
        _simulateLatencyState.value = active
        GeminiAgentHelper.simulateLatencyState = active
    }

    fun runLiveApiTest() {
        _isTestingProgress.value = true
        _testProbeResult.value = null
        viewModelScope.launch {
            try {
                val res = GeminiAgentHelper.testApiConnection()
                _testProbeResult.value = res
                if (!res.isSuccess) {
                    logTechnicalError("Live API Connectivity Probe Failed", res.message, "Roundtrip response latency: ${res.latencyMs}ms")
                }
            } catch (e: Exception) {
                _testProbeResult.value = DiagnosticResult(false, e.localizedMessage ?: "Unexpected connection error", 0)
                logTechnicalError("Live API Connection Throw", e.localizedMessage, e.stackTraceToString())
            } finally {
                _isTestingProgress.value = false
            }
        }
    }

    fun generateAppLocalFallback(prompt: String, agent: CreatorAgent) {
        _geminiError.value = null
        _isGeneratingApp.value = true
        viewModelScope.launch {
            try {
                val parsed = repository.parsePromptToApp(prompt, agent)
                val generatedAppId = repository.createApp(
                    name = parsed.name,
                    icon = parsed.icon,
                    description = parsed.description,
                    layoutType = "list_detail_form",
                    fields = parsed.fields
                )
                _isGeneratingApp.value = false
                navigateTo(Screen.AppMain(appId = generatedAppId, tab = AppTab.List))
            } catch (e: Exception) {
                _isGeneratingApp.value = false
                logTechnicalError("Local Fallback Parse Critical Fail", e.localizedMessage, e.stackTraceToString())
                _geminiError.value = GeminiErrorState(
                    title = "Fallback Generation Failed",
                    userFriendlyMessage = "Even the local offline text analyzer struggled with this prompt. Let's try simplifying the wording slightly.",
                    details = e.localizedMessage,
                    canFallback = false
                )
            }
        }
    }

    fun generateAppFromPrompt(promptText: String) {
        _geminiError.value = null
        _isGeneratingApp.value = true
        viewModelScope.launch {
            val agent = _selectedCreatorAgent.value
            try {
                // Call Gemini API (will throw our custom detailed exceptions upon failure)
                val parsed = GeminiAgentHelper.generateAppJson(promptText, agent)
                
                val generatedAppId = repository.createApp(
                    name = parsed.name,
                    icon = parsed.icon,
                    description = parsed.description,
                    layoutType = "list_detail_form",
                    fields = parsed.fields
                )
                _isGeneratingApp.value = false
                navigateTo(Screen.AppMain(appId = generatedAppId, tab = AppTab.List))
            } catch (e: GeminiApiKeyException) {
                _isGeneratingApp.value = false
                logTechnicalError("API Key Exception on Generator", e.message, e.stackTraceToString())
                _geminiError.value = GeminiErrorState(
                    title = "API Key Missing or Placeholder",
                    userFriendlyMessage = "Your Gemini API key is currently not configured or is set to a placeholder.\n\nTo unlock high-fidelity AI generation, configure your 'GEMINI_API_KEY' in the AI Studio Secrets panel.\n\nAlternatively, you can proceed with our fast offline local analyzer.",
                    details = e.message,
                    canFallback = true,
                    promptText = promptText,
                    agent = agent
                )
            } catch (e: GeminiNetworkException) {
                _isGeneratingApp.value = false
                logTechnicalError("Server Status Return Exception (HTTP ${e.code})", e.message, e.stackTraceToString())
                _geminiError.value = GeminiErrorState(
                    title = "Server Connection Blocked (HTTP ${e.code})",
                    userFriendlyMessage = "The Google AI services returned a server error.\n\nThis usually occurs due to rate limits, invalid API keys, or temporary downtime. You can check your API key in Secrets or continue using local parsing.",
                    details = e.message,
                    canFallback = true,
                    promptText = promptText,
                    agent = agent
                )
            } catch (e: GeminiStructureException) {
                _isGeneratingApp.value = false
                logTechnicalError("Invalid Response Structure Generated", e.message, "Raw Result: ${e.rawResponse ?: "N/A"}")
                _geminiError.value = GeminiErrorState(
                    title = "Invalid Template Format",
                    userFriendlyMessage = "The AI prompt structure was interpreted but didn't output standard database syntax.\n\nTry rephrasing your requirements so there are clear entities/fields mentioned, or fallback to the local analyzer.",
                    details = "Prompt Raw Response Output:\n${e.rawResponse ?: "Empty response body"}\n\nTechnical Error:\n${e.message}",
                    canFallback = true,
                    promptText = promptText,
                    agent = agent
                )
            } catch (e: Exception) {
                _isGeneratingApp.value = false
                logTechnicalError("Unexpected Uncaught Call Exception", e.localizedMessage, e.stackTraceToString())
                _geminiError.value = GeminiErrorState(
                    title = "Connection Unreachable",
                    userFriendlyMessage = "Failed to communicate with Gemini.\n\nVerify that your mobile device/workspace has internet connectivity and that the GEMINI_API_KEY is properly initialized.",
                    details = e.localizedMessage ?: e.toString(),
                    canFallback = true,
                    promptText = promptText,
                    agent = agent
                )
            }
        }
    }

    // Manual creation methods
    fun setManualMeta(title: String, desc: String, icon: String, layout: String) {
        _manualDraftTitle.value = title
        _manualDraftDesc.value = desc
        _manualDraftIcon.value = icon
        _manualDraftLayout.value = layout
    }

    fun addManualField(field: ParsedField) {
        val current = _manualDraftFields.value.toMutableList()
        current.add(field)
        _manualDraftFields.value = current
    }

    fun removeManualField(index: Int) {
        val current = _manualDraftFields.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _manualDraftFields.value = current
        }
    }

    fun saveDraftApp() {
        val name = _manualDraftTitle.value.ifBlank { "Unofficial App" }
        val desc = _manualDraftDesc.value.ifBlank { "Created manually step-by-step." }
        val icon = _manualDraftIcon.value
        val layout = _manualDraftLayout.value
        val fields = _manualDraftFields.value.ifEmpty { 
            listOf(ParsedField("Title", "text"), ParsedField("Status", "checkbox"))
        }

        viewModelScope.launch {
            val appId = repository.createApp(
                name = name,
                icon = icon,
                description = desc,
                layoutType = layout,
                fields = fields
            )
            // Reset manual draft states
            _manualDraftTitle.value = ""
            _manualDraftDesc.value = ""
            _manualDraftIcon.value = "list"
            _manualDraftFields.value = emptyList()

            // Navigate into the newly created app!
            navigateTo(Screen.AppMain(appId = appId, tab = AppTab.List))
        }
    }

    // CRUD interface operations for active workspace app
    fun saveNewRecord(appId: Int, map: Map<String, String>) {
        viewModelScope.launch {
            repository.saveRecord(appId, map)
            refreshStats()
            // Navigate back to list
            navigateTo(Screen.AppMain(appId = appId, tab = AppTab.List))
        }
    }

    fun updateExistingRecord(recordId: Int, appId: Int, map: Map<String, String>) {
        viewModelScope.launch {
            repository.updateRecord(recordId, appId, map)
            refreshStats()
            // Navigate back to list or detail
            navigateTo(Screen.AppMain(appId = appId, tab = AppTab.List))
        }
    }

    fun deleteRecord(recordId: Int, appId: Int) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            refreshStats()
            // Navigate back to list
            navigateTo(Screen.AppMain(appId = appId, tab = AppTab.List))
        }
    }

    fun deleteEntireApp(appId: Int) {
        viewModelScope.launch {
            repository.deleteApp(appId)
            refreshStats()
            // Return back to home
            _currentScreen.value = Screen.Home
        }
    }

    // Helper utilities for deserializing dynamic fields
    fun parseRecordData(record: AppRecord): Map<String, String> {
        return repository.deserializeMap(record.dataJson)
    }
}
