package com.example

import com.example.data.*
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    private val dummyDao = object : AppMakerDao {
        override fun getAllApps(): Flow<List<AppDefinition>> = throw NotImplementedError()
        override suspend fun getAppById(id: Int): AppDefinition? = null
        override fun getFieldsForApp(appId: Int): Flow<List<AppField>> = throw NotImplementedError()
        override suspend fun getFieldsForAppSync(appId: Int): List<AppField> = emptyList()
        override fun getRecordsForApp(appId: Int): Flow<List<AppRecord>> = throw NotImplementedError()
        override suspend fun insertApp(app: AppDefinition): Long = 0
        override suspend fun insertField(field: AppField): Long = 0
        override suspend fun insertRecord(record: AppRecord): Long = 0
        override suspend fun updateApp(app: AppDefinition) {}
        override suspend fun updateRecord(record: AppRecord) {}
        override suspend fun deleteAppById(id: Int) {}
        override suspend fun deleteRecordById(id: Int) {}
    }

    @Test
    fun testParserHeuristics_pets() {
        val repository = AppMakerRepository(dummyDao)
        val prompt = "I need an app to track my pets' vaccinations."
        val result = repository.parsePromptToApp(prompt)
        
        assertNotNull(result)
        assertEquals("Pet Vaccinations", result.name)
        assertEquals("pets", result.icon)
        
        val hasPetName = result.fields.any { it.name == "Pet Name" && it.type == "text" }
        val hasDate = result.fields.any { it.name == "Date Given" && it.type == "date" }
        
        assertEquals(true, hasPetName)
        assertEquals(true, hasDate)
    }

    @Test
    fun testParserHeuristics_todos() {
        val repository = AppMakerRepository(dummyDao)
        val prompt = "Create a tasks or todo checklist list app with reminders"
        val result = repository.parsePromptToApp(prompt)

        assertNotNull(result)
        assertEquals("Task Tracker", result.name)
        assertEquals("list", result.icon)
        assertTrue(result.fields.any { it.name == "Task Title" })
        assertTrue(result.fields.any { it.name == "Category" && it.options == "High Priority,Medium Priority,Low Priority" })
    }

    @Test
    fun testParserHeuristics_notes() {
        val repository = AppMakerRepository(dummyDao)
        val prompt = "A custom daily log journal or notes keeping system"
        val result = repository.parsePromptToApp(prompt)

        assertNotNull(result)
        assertEquals("Quick Notes", result.name)
        assertEquals("notes", result.icon)
        assertTrue(result.fields.any { it.name == "Body Text" && it.type == "text" })
    }

    @Test
    fun testParserHeuristics_inventory() {
        val repository = AppMakerRepository(dummyDao)
        val prompt = "Inventory check for stock and shop materials"
        val result = repository.parsePromptToApp(prompt)

        assertNotNull(result)
        assertEquals("Stock Tracker", result.name)
        assertEquals("inventory", result.icon)
        assertTrue(result.fields.any { it.name == "Unit Price" && it.type == "number" })
    }

    @Test
    fun testParserHeuristics_contacts() {
        val repository = AppMakerRepository(dummyDao)
        val prompt = "Contact app for my friends and phone number database"
        val result = repository.parsePromptToApp(prompt)

        assertNotNull(result)
        assertEquals("Client Directory", result.name)
        assertEquals("contacts", result.icon)
        assertTrue(result.fields.any { it.name == "Phone Number" })
    }

    @Test
    fun testParserHeuristics_finance() {
        val repository = AppMakerRepository(dummyDao)
        val prompt = "Manage my personal expenditures and daily budget"
        val result = repository.parsePromptToApp(prompt)

        assertNotNull(result)
        assertEquals("Finance Ledger", result.name)
        assertEquals("star", result.icon)
        assertTrue(result.fields.any { it.name == "Amount" && it.type == "number" })
    }

    @Test
    fun testParserHeuristics_genericFallback() {
        val repository = AppMakerRepository(dummyDao)
        val prompt = "Track my custom game scores"
        val result = repository.parsePromptToApp(prompt)

        assertNotNull(result)
        assertTrue(result.name.contains("Game"))
        assertTrue(result.fields.any { it.name == "Subject Name" })
        assertTrue(result.fields.any { it.name == "Value / Number" }) // Triggered by 'score'
    }

    @Test
    fun testSimulatedErrorEnumLabels() {
        // Asserting simulated error labels for correctness and display
        assertEquals("No Simulated Outage (Live API Calls)", SimulatedErrorType.NONE.label)
        assertEquals("Simulate Missing API Key", SimulatedErrorType.MISSING_KEY.label)
        assertEquals("Simulate HTTP 429 (Resource Exhausted)", SimulatedErrorType.HTTP_429_RATE_LIMIT.label)
        assertEquals("Simulate HTTP 500 (Google AI Server Error)", SimulatedErrorType.HTTP_500_MOCK_OUTAGE.label)
        assertEquals("Simulate Malformed JSON Schema Response", SimulatedErrorType.MALFORMED_JSON_STRUCTURE.label)
    }

    @Test
    fun testGeminiAgentHelperSimulatedStates() {
        // Test setup and retrieval of simulate states
        GeminiAgentHelper.simulateLatencyState = true
        assertEquals(true, GeminiAgentHelper.simulateLatencyState)
        
        GeminiAgentHelper.simulatedErrorState = SimulatedErrorType.HTTP_429_RATE_LIMIT
        assertEquals(SimulatedErrorType.HTTP_429_RATE_LIMIT, GeminiAgentHelper.simulatedErrorState)

        // Reset to default
        GeminiAgentHelper.simulateLatencyState = false
        GeminiAgentHelper.simulatedErrorState = SimulatedErrorType.NONE
        assertEquals(false, GeminiAgentHelper.simulateLatencyState)
        assertEquals(SimulatedErrorType.NONE, GeminiAgentHelper.simulatedErrorState)
    }
}
