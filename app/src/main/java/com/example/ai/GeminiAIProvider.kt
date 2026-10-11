package com.example.ai

import com.example.data.model.ActionCommand
import com.example.data.model.ConversationMessage
import com.example.data.model.MemoryItem
import com.example.security.SecurityManager
import com.example.settings.StarkPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAIProvider(
    private val securityManager: SecurityManager,
    private val preferences: StarkPreferences
) : AIProvider {

    override val providerName: String = "Google Gemini"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are "Mr. Stark", an intelligent, calm, professional, and powerful personal AI voice assistant on Android (like Jarvis).
        Activation phrase is "Mr. Stark".
        You understand English, Hindi, and Hinglish fluently.
        Always reply in the language or dialect used by the user (if user speaks Hindi/Hinglish, reply in Hindi/Hinglish; if English, reply in English).
        Address the user respectfully as "Sir".

        You have direct control over Android device actions. When the user gives a command, output a single JSON object conforming to this schema:
        {
          "type": "OPEN_APP" | "WEB_SEARCH" | "SEARCH_IN_APP" | "GENERATE_IMAGE" | "WRITE_PARAGRAPH" | "READ_SCREEN" | "CLICK_ELEMENT" | "WRITE_TEXT" | "OPEN_URL" | "SET_TIMER" | "SET_ALARM" | "REMEMBER" | "FORGET" | "DEVICE_INFO" | "OPEN_SETTINGS" | "CALL_CONTACT" | "SEND_MESSAGE" | "COPY_CLIPBOARD" | "SHARE_TEXT" | "COMPOUND" | "NORMAL_RESPONSE",
          "target": "string or null (e.g. app name like 'youtube', element name, prompt, topic, phone number, url)",
          "query": "string or null (e.g. search query, typed text, paragraph topic)",
          "parameters": { "key": "value" },
          "spokenResponse": "The concise, polite natural language response you speak aloud to the user"
        }

        Important Action Mapping Guidelines:
        1. When user asks to search specifically on YouTube or in an app (e.g. "yt per search karo", "yaha search karo", "youtube pe search karo"):
           Use type: "SEARCH_IN_APP", target: "youtube" (or "current"), query: "<search term>".
        2. When user asks to generate/create/draw an image (e.g. "image generate karo", "draw a picture of ..."):
           Use type: "GENERATE_IMAGE", target: "<image prompt>".
        3. When user asks to write an essay, article, or paragraph (like ChatGPT with web knowledge/sources):
           Use type: "WRITE_PARAGRAPH", target: "<topic>".
        4. When user asks to open an app (e.g. "yt kholo", "youtube open karo"):
           Use type: "OPEN_APP", target: "YouTube".
        5. For multi-step commands (e.g. "yt kholo aur search karo iron man", "flashlight on karo and read screen"):
           You can return a compound action or perform the primary goal with SEARCH_IN_APP or OPEN_APP.
        6. Always output ONLY valid JSON without markdown fences.
    """.trimIndent()

    override suspend fun processQuery(
        userMessage: String,
        history: List<ConversationMessage>,
        memories: List<MemoryItem>
    ): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = securityManager.getResolvedGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext AIResponse(
                spokenText = "Sir, no Gemini API key is configured. Please enter one in Settings or switch to offline rule mode.",
                error = "API key missing"
            )
        }

        val model = preferences.aiModel.ifEmpty { "gemini-3.5-flash" }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            // Build contents array
            val contentsArray = JSONArray()

            // Include relevant memories as context
            if (memories.isNotEmpty()) {
                val memorySummary = memories.take(15).joinToString("; ") { "${it.key}: ${it.value}" }
                val memContent = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Context: User's saved memories: [$memorySummary]")
                        })
                    })
                }
                contentsArray.put(memContent)
                val ackContent = JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Acknowledged, Sir. Memory context loaded.")
                        })
                    })
                }
                contentsArray.put(ackContent)
            }

            // Include recent conversation turns (up to 6)
            val recentTurns = history.takeLast(6)
            for (turn in recentTurns) {
                val role = if (turn.sender == com.example.data.model.MessageSender.USER) "user" else "model"
                val contentObj = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", turn.text) })
                    })
                }
                contentsArray.put(contentObj)
            }

            // Add current user message
            val currentContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userMessage) })
                })
            }
            contentsArray.put(currentContent)

            // System instruction
            val systemInstructionObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemInstructionObj)
                put("generationConfig", JSONObject().apply {
                    put("temperature", preferences.aiTemperature.toDouble())
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                val code = response.code
                return@withContext AIResponse(
                    spokenText = "Sir, I encountered a communication issue with the AI core (Error $code).",
                    error = "HTTP $code: $responseBody"
                )
            }

            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            AIResponse(
                spokenText = "Sir, I was unable to connect to the neural server. You may be offline.",
                error = e.localizedMessage,
                isOffline = true
            )
        }
    }

    private fun parseGeminiResponse(jsonString: String): AIResponse {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

            if (text.isEmpty()) {
                return AIResponse(spokenText = "Sir, I received an empty response from the AI network.")
            }

            // Clean any potential markdown backticks ```json ... ```
            val cleanedJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            if (cleanedJson.startsWith("{") && cleanedJson.endsWith("}")) {
                val actionObj = JSONObject(cleanedJson)
                val type = actionObj.optString("type", ActionCommand.TYPE_NORMAL_RESPONSE)
                val target = if (actionObj.has("target") && !actionObj.isNull("target")) actionObj.getString("target") else null
                val query = if (actionObj.has("query") && !actionObj.isNull("query")) actionObj.getString("query") else null
                val spokenResponse = actionObj.optString("spokenResponse", "")

                val paramsMap = mutableMapOf<String, String>()
                val paramsObj = actionObj.optJSONObject("parameters")
                if (paramsObj != null) {
                    val keys = paramsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        paramsMap[k] = paramsObj.optString(k, "")
                    }
                }

                val command = ActionCommand(
                    type = type,
                    target = target,
                    query = query,
                    parameters = paramsMap,
                    spokenResponse = spokenResponse
                )

                AIResponse(
                    spokenText = spokenResponse.ifEmpty { "Executing command, Sir." },
                    actionCommand = command
                )
            } else {
                AIResponse(
                    spokenText = text,
                    actionCommand = ActionCommand(
                        type = ActionCommand.TYPE_NORMAL_RESPONSE,
                        spokenResponse = text
                    )
                )
            }
        } catch (e: Exception) {
            AIResponse(spokenText = "Sir, error interpreting AI response: ${e.message}", error = e.message)
        }
    }
}
