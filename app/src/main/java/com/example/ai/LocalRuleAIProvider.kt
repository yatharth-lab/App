package com.example.ai

import com.example.data.model.ActionCommand
import com.example.data.model.ConversationMessage
import com.example.data.model.MemoryItem
import java.util.Locale

class LocalRuleAIProvider : AIProvider {

    override val providerName: String = "Local Engine (Offline)"

    override suspend fun processQuery(
        userMessage: String,
        history: List<ConversationMessage>,
        memories: List<MemoryItem>
    ): AIResponse {
        val trimmed = userMessage.trim()
        val lower = trimmed.lowercase(Locale.getDefault())

        // 1. Remove activation phrase if present in message
        val clean = lower
            .replace("mr stark", "")
            .replace("mr. stark", "")
            .replace("mister stark", "")
            .replace("hey stark", "")
            .replace("stark", "")
            .trim()

        // 2. Battery / Telemetry / Device Info
        if (clean.contains("battery") || clean.contains("charging") || clean.contains("telemetry") ||
            clean.contains("device status") || clean.contains("system status") ||
            clean.contains("kitni charging") || clean.contains("battery kitni")
        ) {
            return AIResponse(
                spokenText = "Checking device telemetry, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_DEVICE_INFO,
                    spokenResponse = ""
                )
            )
        }

        // 3. Time / Date
        if (clean.contains("what time") || clean.contains("time kya") || clean.contains("current time") ||
            clean.contains("aaj ki date") || clean.contains("what is today's date") || clean.contains("kitne baje")
        ) {
            return AIResponse(
                spokenText = "Reading telemetry time, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_DEVICE_INFO,
                    spokenResponse = ""
                )
            )
        }

        // 4. App Launching (English + Hindi + Hinglish)
        // "open youtube", "youtube kholo", "open chrome", "calculator open karo", "camera start karo"
        val openPrefixes = listOf("open ", "launch ", "start ")
        val openSuffixes = listOf(" kholo", " chalao", " open karo", " start karo", " kholein")

        for (prefix in openPrefixes) {
            if (clean.startsWith(prefix)) {
                val app = clean.removePrefix(prefix).trim()
                if (app.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Opening $app, Sir.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_OPEN_APP,
                            target = app,
                            spokenResponse = "Opening $app, Sir."
                        )
                    )
                }
            }
        }

        for (suffix in openSuffixes) {
            if (clean.endsWith(suffix)) {
                val app = clean.removeSuffix(suffix).trim()
                if (app.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Certainly, opening $app.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_OPEN_APP,
                            target = app,
                            spokenResponse = "Opening $app, Sir."
                        )
                    )
                }
            }
        }

        // 5. Timers
        // "set timer for 5 minutes", "timer 30 seconds", "5 minute ka timer lagao"
        if (clean.contains("timer")) {
            val seconds = extractSeconds(clean)
            return AIResponse(
                spokenText = "Setting a timer for $seconds seconds, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_SET_TIMER,
                    parameters = mapOf("seconds" to seconds.toString()),
                    spokenResponse = "Timer set for $seconds seconds, Sir."
                )
            )
        }

        // 6. Alarms
        // "set alarm for 7 am", "subah 6 baje ka alarm"
        if (clean.contains("alarm")) {
            val time = extractHourMinute(clean)
            return AIResponse(
                spokenText = "Configuring alarm, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_SET_ALARM,
                    parameters = mapOf(
                        "hour" to time.first.toString(),
                        "minutes" to time.second.toString()
                    ),
                    spokenResponse = "Alarm scheduled, Sir."
                )
            )
        }

        // 7. Memory: Remember
        // "remember that...", "yaad rakhna ki..."
        if (clean.startsWith("remember that ") || clean.startsWith("remember ") ||
            clean.startsWith("yaad rakhna ki ") || clean.startsWith("yaad rakho ")
        ) {
            val fact = clean
                .removePrefix("remember that ")
                .removePrefix("remember ")
                .removePrefix("yaad rakhna ki ")
                .removePrefix("yaad rakho ")
                .trim()
            return AIResponse(
                spokenText = "I have recorded that in local memory, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_REMEMBER,
                    target = fact,
                    spokenResponse = "I have safely stored that in memory, Sir."
                )
            )
        }

        // 8. Memory: Recall
        // "what do you remember about me?", "mere baare me kya yaad hai?"
        if (clean.contains("what do you remember") || clean.contains("kya yaad hai") ||
            clean.contains("list memories") || clean.contains("kya pata hai")
        ) {
            val summary = if (memories.isEmpty()) {
                "My memory banks are currently clear, Sir."
            } else {
                "Here is what I have saved: " + memories.take(5).joinToString(". ") { it.value }
            }
            return AIResponse(
                spokenText = summary,
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_NORMAL_RESPONSE,
                    spokenResponse = summary
                )
            )
        }

        // 9. Memory: Forget
        if (clean.startsWith("forget that ") || clean.startsWith("forget ") || clean.startsWith("bhool jao ")) {
            val target = clean.removePrefix("forget that ").removePrefix("forget ").removePrefix("bhool jao ").trim()
            return AIResponse(
                spokenText = "Removing matching records from memory, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_FORGET,
                    target = target,
                    spokenResponse = "Erased from memory banks, Sir."
                )
            )
        }

        // 10. Web search
        if (clean.startsWith("search for ") || clean.startsWith("google ") ||
            clean.startsWith("search ") || clean.contains("dhoondo") || clean.contains("search karo")
        ) {
            val query = clean
                .removePrefix("search for ")
                .removePrefix("google ")
                .removePrefix("search ")
                .replace("dhoondo", "")
                .replace("search karo", "")
                .trim()
            return AIResponse(
                spokenText = "Searching for $query, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_WEB_SEARCH,
                    query = query,
                    spokenResponse = "Searching online for $query, Sir."
                )
            )
        }

        // 11. Settings
        if (clean.contains("open settings") || clean.contains("settings kholo")) {
            val sub = when {
                clean.contains("wifi") || clean.contains("wi-fi") -> "wifi"
                clean.contains("bluetooth") -> "bluetooth"
                clean.contains("sound") || clean.contains("volume") -> "sound"
                clean.contains("display") || clean.contains("brightness") -> "display"
                else -> null
            }
            return AIResponse(
                spokenText = "Opening settings, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_OPEN_SETTINGS,
                    target = sub,
                    spokenResponse = "Opening settings, Sir."
                )
            )
        }

        // 12. Conversational greetings
        if (clean.contains("hello") || clean.contains("hi") || clean.contains("namaste") || clean.contains("hey")) {
            return AIResponse(
                spokenText = "At your service, Sir. How may I assist you today?",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_NORMAL_RESPONSE,
                    spokenResponse = "At your service, Sir. How may I assist you today?"
                )
            )
        }

        if (clean.contains("who are you") || clean.contains("kaun ho") || clean.contains("your name")) {
            val desc = "I am Mr. Stark, your personal cinematic AI assistant. All local systems, app launching, timers, and offline routines are fully operational."
            return AIResponse(
                spokenText = desc,
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_NORMAL_RESPONSE,
                    spokenResponse = desc
                )
            )
        }

        if (clean.contains("how are you") || clean.contains("kaise ho")) {
            return AIResponse(
                spokenText = "Operating at peak computational capacity, Sir. Standing by for your instructions.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_NORMAL_RESPONSE,
                    spokenResponse = "Operating at peak computational capacity, Sir. Standing by for your instructions."
                )
            )
        }

        // Fallback
        val fallback = "I understood: \"$trimmed\". In offline rule mode, I can open apps, set timers, control alarms, check battery telemetry, and manage memories. For open-ended queries, please enable Gemini AI in Settings."
        return AIResponse(
            spokenText = fallback,
            actionCommand = ActionCommand(
                type = ActionCommand.TYPE_NORMAL_RESPONSE,
                spokenResponse = fallback
            ),
            isOffline = true
        )
    }

    private fun extractSeconds(text: String): Int {
        val words = text.split(" ")
        for (i in words.indices) {
            val num = words[i].toIntOrNull()
            if (num != null) {
                val unit = words.getOrNull(i + 1) ?: ""
                return when {
                    unit.startsWith("min") -> num * 60
                    unit.startsWith("sec") -> num
                    unit.startsWith("hour") || unit.startsWith("ghante") -> num * 3600
                    else -> num * 60
                }
            }
        }
        return 60
    }

    private fun extractHourMinute(text: String): Pair<Int, Int> {
        val digits = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        return if (digits.isNotEmpty()) {
            val hour = digits[0].coerceIn(0, 23)
            val minute = if (digits.size > 1) digits[1].coerceIn(0, 59) else 0
            Pair(hour, minute)
        } else {
            Pair(7, 0)
        }
    }
}
