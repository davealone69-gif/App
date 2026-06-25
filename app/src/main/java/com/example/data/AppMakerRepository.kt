package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Locale

data class ParsedField(
    val name: String,
    val type: String, // "text", "number", "date", "checkbox", "dropdown"
    val options: String = "" // For dropdowns, comma-separated values
)

data class ParsedApp(
    val name: String,
    val description: String,
    val icon: String,
    val fields: List<ParsedField>
)

class AppMakerRepository(private val dao: AppMakerDao) {
    val allApps: Flow<List<AppDefinition>> = dao.getAllApps()

    suspend fun getAppById(id: Int): AppDefinition? = dao.getAppById(id)

    fun getFieldsForApp(appId: Int): Flow<List<AppField>> = dao.getFieldsForApp(appId)

    suspend fun getFieldsForAppSync(appId: Int): List<AppField> = dao.getFieldsForAppSync(appId)

    fun getRecordsForApp(appId: Int): Flow<List<AppRecord>> = dao.getRecordsForApp(appId)

    suspend fun createApp(name: String, icon: String, description: String, layoutType: String, fields: List<ParsedField>): Int {
        val appId = dao.insertApp(
            AppDefinition(
                name = name,
                icon = icon,
                description = description,
                layoutType = layoutType
            )
        ).toInt()
        
        fields.forEach { pf ->
            dao.insertField(
                AppField(
                    appId = appId,
                    name = pf.name,
                    type = pf.type,
                    options = pf.options
                )
            )
        }
        return appId
    }

    suspend fun deleteApp(appId: Int) {
        dao.deleteAppById(appId)
    }

    suspend fun saveRecord(appId: Int, data: Map<String, String>): Long {
        val json = serializeMap(data)
        return dao.insertRecord(AppRecord(appId = appId, dataJson = json))
    }

    suspend fun updateRecord(recordId: Int, appId: Int, data: Map<String, String>) {
        val json = serializeMap(data)
        dao.updateRecord(AppRecord(id = recordId, appId = appId, dataJson = json))
    }

    suspend fun deleteRecord(recordId: Int) {
        dao.deleteRecordById(recordId)
    }

    // Helper functions to parse prompts
    fun parsePromptToApp(prompt: String, agent: CreatorAgent = CreatorAgent.ARCHITECT): ParsedApp {
        val clean = prompt.trim().lowercase(Locale.ROOT)
        
        val baseApp = if (clean.contains("vaccinat") || clean.contains("pet")) {
            ParsedApp(
                name = "Pet Vaccinations",
                description = "Track vaccinations, appointment dates, and health records for your pets.",
                icon = "pets",
                fields = listOf(
                    ParsedField("Pet Name", "text"),
                    ParsedField("Vaccine Name", "text"),
                    ParsedField("Date Given", "date"),
                    ParsedField("Completed", "checkbox"),
                    ParsedField("Next Due Date", "date")
                )
            )
        } else if (clean.contains("todo") || clean.contains("task") || clean.contains("remind")) {
            ParsedApp(
                name = "Task Tracker",
                description = "Manage tasks, deadlines, completion checklists, and category types.",
                icon = "list",
                fields = listOf(
                    ParsedField("Task Title", "text"),
                    ParsedField("Category", "dropdown", "High Priority,Medium Priority,Low Priority"),
                    ParsedField("Due Date", "date"),
                    ParsedField("Done", "checkbox"),
                    ParsedField("Notes", "text")
                )
            )
        } else if (clean.contains("note") || clean.contains("journal") || clean.contains("log")) {
            ParsedApp(
                name = "Quick Notes",
                description = "Keep personal notes, daily journal entries, tags, and date logs.",
                icon = "notes",
                fields = listOf(
                    ParsedField("Title", "text"),
                    ParsedField("Category", "dropdown", "Personal,Work,Ideas"),
                    ParsedField("Body Text", "text"),
                    ParsedField("Date Created", "date"),
                    ParsedField("Add to Favorites", "checkbox")
                )
            )
        } else if (clean.contains("inventory") || clean.contains("stock") || clean.contains("item") || clean.contains("shop")) {
            ParsedApp(
                name = "Stock Tracker",
                description = "Keep records of parts, stock quantities, and purchases.",
                icon = "inventory",
                fields = listOf(
                    ParsedField("Item Name", "text"),
                    ParsedField("Quantity", "number"),
                    ParsedField("Unit Price", "number"),
                    ParsedField("Restock Date", "date"),
                    ParsedField("In Stock", "checkbox")
                )
            )
        } else if (clean.contains("contact") || clean.contains("friend") || clean.contains("phone") || clean.contains("client")) {
            ParsedApp(
                name = "Client Directory",
                description = "A portable custom list of clients, contacts, and emails.",
                icon = "contacts",
                fields = listOf(
                    ParsedField("First Name", "text"),
                    ParsedField("Last Name", "text"),
                    ParsedField("Phone Number", "number"),
                    ParsedField("Email Address", "text"),
                    ParsedField("Vip Client", "checkbox")
                )
            )
        } else if (clean.contains("expense") || clean.contains("budget") || clean.contains("money") || clean.contains("financ")) {
            ParsedApp(
                name = "Finance Ledger",
                description = "Manage income, expenditures, payment dates, and statuses.",
                icon = "star",
                fields = listOf(
                    ParsedField("Transaction Title", "text"),
                    ParsedField("Category", "dropdown", "Food,Rent,Entertainment,Utilities,Salary"),
                    ParsedField("Amount", "number"),
                    ParsedField("Date", "date"),
                    ParsedField("Cleared / Paid", "checkbox")
                )
            )
        } else {
            // 2. Generic heuristic fallback parser
            // We will extract prominent nouns & keywords
            var appName = "My Custom Tracker"
            val description = "An app auto-generated from your custom prompt."
            val icon = "custom"
            val fields = mutableListOf<ParsedField>()

            // Try extracting a reasonable subject
            val words = clean.split("\\s+".toRegex()).filter { it.length > 3 }
            val subjectWord = words.firstOrNull { 
                !listOf("need", "want", "have", "create", "build", "track", "make", "with", "from").contains(it)
            } ?: "item"
            
            appName = "My " + subjectWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } + " App"
            
            // Let's search the prompt for words that represent specific types:
            // Text field defaults: "title", "subject", "name", "owner", "person", "description" or the subject itself
            fields.add(ParsedField("Subject Name", "text"))

            // Date keyword search
            if (clean.contains("date") || clean.contains("when") || clean.contains("time") || clean.contains("during") || clean.contains("deadline") || clean.contains("birthday")) {
                fields.add(ParsedField("Date", "date"))
            }

            // Number keyword search
            if (clean.contains("quantity") || clean.contains("price") || clean.contains("cost") || clean.contains("amount") || clean.contains("phone") || clean.contains("count") || clean.contains("score") || clean.contains("total") || clean.contains("weight") || clean.contains("age")) {
                fields.add(ParsedField("Value / Number", "number"))
            }

            // Checkbox search
            if (clean.contains("done") || clean.contains("completed") || clean.contains("status") || clean.contains("active") || clean.contains("received") || clean.contains("checked") || clean.contains("paid")) {
                fields.add(ParsedField("Completed", "checkbox"))
            }

            // If we still have too few fields, add a secondary notes, status, and status checkbox
            if (fields.size < 3) {
                fields.add(ParsedField("Description / Notes", "text"))
            }
            if (fields.none { it.type == "checkbox" }) {
                fields.add(ParsedField("Active", "checkbox"))
            }

            ParsedApp(
                name = appName,
                description = description,
                icon = icon,
                fields = fields
            )
        }

        // Apply specialized agent enrichment to the fields list
        val enrichedFields = baseApp.fields.toMutableList()
        when (agent) {
            CreatorAgent.CREATIVE -> {
                if (enrichedFields.none { it.name.lowercase().contains("rating") }) {
                    enrichedFields.add(ParsedField("Rating Score", "dropdown", "⭐⭐⭐⭐⭐ Top,⭐⭐⭐⭐ Good,⭐⭐⭐ Mid,⭐⭐ Low,⭐ Bad"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("tag") }) {
                    enrichedFields.add(ParsedField("Creative Tags", "dropdown", "Hobby,Fun,Aesthetic,Retro,Futuristic"))
                }
            }
            CreatorAgent.ENTERPRISE -> {
                if (enrichedFields.none { it.name.lowercase().contains("status") }) {
                    enrichedFields.add(ParsedField("Status Cycle", "dropdown", "Draft,Active,For Review,Approved,Closed"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("assign") }) {
                    enrichedFields.add(ParsedField("Assigned Lead", "text"))
                }
                if (enrichedFields.none { it.type == "date" }) {
                    enrichedFields.add(ParsedField("SLA Target Date", "date"))
                }
            }
            CreatorAgent.SCIENTIST -> {
                if (enrichedFields.none { it.type == "number" }) {
                    enrichedFields.add(ParsedField("Scientific Metric Value", "number"))
                    enrichedFields.add(ParsedField("Metric Sum Tally", "number"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("unit") }) {
                    enrichedFields.add(ParsedField("Unit Type", "dropdown", "kg,meters,liters,seconds,percentage"))
                }
            }
            CreatorAgent.DEVELOPER -> {
                if (enrichedFields.none { it.name.lowercase().contains("hash") || it.name.lowercase().contains("id") }) {
                    enrichedFields.add(ParsedField("Item Unique UUID", "text"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("latency") || it.name.lowercase().contains("speed") }) {
                    enrichedFields.add(ParsedField("Execution Delay (ms)", "number"))
                }
            }
            CreatorAgent.MARKETER -> {
                if (enrichedFields.none { it.name.lowercase().contains("source") }) {
                    enrichedFields.add(ParsedField("Lead Source", "dropdown", "Social Media,Search Engine,Direct Referral,Paid Ads"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("conversion") }) {
                    enrichedFields.add(ParsedField("Is Converted Value", "checkbox"))
                }
            }
            CreatorAgent.LEGAL -> {
                if (enrichedFields.none { it.name.lowercase().contains("agree") || it.name.lowercase().contains("consent") }) {
                    enrichedFields.add(ParsedField("Sign-off Consent Check", "checkbox"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("term") }) {
                    enrichedFields.add(ParsedField("Legal Terms Tier", "dropdown", "Standard NDA v1.2,Privacy Policy 2026,Full Terms of Use"))
                }
            }
            CreatorAgent.FITNESS -> {
                if (enrichedFields.none { it.name.lowercase().contains("set") || it.name.lowercase().contains("rep") }) {
                    enrichedFields.add(ParsedField("Routine Sets Count", "number"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("badge") || it.name.lowercase().contains("award") }) {
                    enrichedFields.add(ParsedField("Wellness Achievement Badge", "dropdown", "🔥 Streak Master,💧 Hydration Guru,⚡ Power Champion"))
                }
            }
            CreatorAgent.GAMER -> {
                if (enrichedFields.none { it.name.lowercase().contains("xp") || it.name.lowercase().contains("score") }) {
                    enrichedFields.add(ParsedField("Award XP Points", "number"))
                }
                if (enrichedFields.none { it.name.lowercase().contains("difficulty") }) {
                    enrichedFields.add(ParsedField("Milestone Rank Level", "dropdown", "Bronze Recruit,Silver Captain,Gold Overlord,Diamond Immortal"))
                }
            }
            CreatorAgent.ARCHITECT -> {
                // standard architect does not inject additional fields
            }
        }

        return baseApp.copy(fields = enrichedFields)
    }

    // A small lightweight serializer/deserializer for Map<String, String> using simple string-splitting so it doesn't depend on external JSON libraries or complex serialization setup
    private fun serializeMap(map: Map<String, String>): String {
        return map.entries.joinToString("||") { 
            "${escape(it.key)}::${escape(it.value)}" 
        }
    }

    fun deserializeMap(json: String): Map<String, String> {
        if (json.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        json.split("||").forEach { pair ->
            val parts = pair.split("::")
            if (parts.size == 2) {
                result[unescape(parts[0])] = unescape(parts[1])
            }
        }
        return result
    }

    private fun escape(s: String): String = s.replace("|", "%7C").replace(":", "%3A")
    private fun unescape(s: String): String = s.replace("%7C", "|").replace("%3A", ":")
}
