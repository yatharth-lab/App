package com.example.actions

import android.content.Context
import com.example.data.model.ActionCommand
import com.example.memory.MemoryRepository

class ActionEngine(
    private val context: Context,
    private val memoryRepository: MemoryRepository
) {
    private val appLauncher = AppLauncher(context)
    private val deviceController = DeviceController(context)

    suspend fun executeAction(command: ActionCommand): ActionResult {
        return when (command.type) {
            ActionCommand.TYPE_OPEN_APP -> {
                val appName = command.target ?: return ActionResult(
                    spokenText = "Which application would you like me to open, Sir?",
                    success = false
                )
                when (val result = appLauncher.launchAppByName(appName)) {
                    is AppLaunchResult.Success -> ActionResult(
                        spokenText = command.spokenResponse.ifEmpty { "Opening ${result.appName}, Sir." },
                        actionBadge = "OPENED: ${result.appName}",
                        success = true
                    )
                    is AppLaunchResult.NotFound -> ActionResult(
                        spokenText = result.message,
                        actionBadge = "APP NOT FOUND",
                        success = false
                    )
                    is AppLaunchResult.Error -> ActionResult(
                        spokenText = result.message,
                        actionBadge = "ERROR",
                        success = false
                    )
                }
            }

            ActionCommand.TYPE_WEB_SEARCH -> {
                val query = command.query ?: command.target ?: ""
                if (query.isBlank()) {
                    ActionResult(spokenText = "What would you like me to search for, Sir?", success = false)
                } else {
                    val spoken = deviceController.performWebSearch(query)
                    ActionResult(
                        spokenText = command.spokenResponse.ifEmpty { spoken },
                        actionBadge = "SEARCH: $query",
                        success = true
                    )
                }
            }

            ActionCommand.TYPE_OPEN_URL -> {
                val url = command.target ?: command.query ?: ""
                val spoken = deviceController.openUrl(url)
                ActionResult(
                    spokenText = spoken,
                    actionBadge = "OPEN URL: $url",
                    success = true
                )
            }

            ActionCommand.TYPE_SET_TIMER -> {
                val secondsStr = command.parameters["seconds"] ?: command.target ?: "60"
                val seconds = secondsStr.filter { it.isDigit() }.toIntOrNull() ?: 60
                val label = command.parameters["label"] ?: "Timer"
                val spoken = deviceController.setTimer(seconds, label)
                ActionResult(
                    spokenText = command.spokenResponse.ifEmpty { spoken },
                    actionBadge = "TIMER: ${seconds}s",
                    success = true
                )
            }

            ActionCommand.TYPE_SET_ALARM -> {
                val hour = command.parameters["hour"]?.toIntOrNull() ?: 7
                val minutes = command.parameters["minutes"]?.toIntOrNull() ?: 0
                val label = command.parameters["label"] ?: "Alarm"
                val spoken = deviceController.setAlarm(hour, minutes, label)
                ActionResult(
                    spokenText = command.spokenResponse.ifEmpty { spoken },
                    actionBadge = "ALARM: $hour:$minutes",
                    success = true
                )
            }

            ActionCommand.TYPE_REMEMBER -> {
                val fact = command.target ?: command.query ?: ""
                if (fact.isNotBlank()) {
                    memoryRepository.saveMemory(
                        key = fact.take(40),
                        value = fact,
                        category = command.parameters["category"] ?: "general"
                    )
                    val spoken = command.spokenResponse.ifEmpty { "I've noted that in memory, Sir." }
                    ActionResult(
                        spokenText = spoken,
                        actionBadge = "REMEMBERED FACT",
                        success = true
                    )
                } else {
                    ActionResult(spokenText = "What fact should I remember, Sir?", success = false)
                }
            }

            ActionCommand.TYPE_FORGET -> {
                val key = command.target ?: command.query ?: ""
                if (key.isNotBlank()) {
                    memoryRepository.deleteMemoryByKey(key)
                    ActionResult(
                        spokenText = "Forgotten from memory, Sir.",
                        actionBadge = "DELETED MEMORY",
                        success = true
                    )
                } else {
                    ActionResult(spokenText = "What should I forget, Sir?", success = false)
                }
            }

            ActionCommand.TYPE_DEVICE_INFO -> {
                val info = deviceController.getDeviceInfo()
                ActionResult(
                    spokenText = info,
                    actionBadge = "DEVICE TELEMETRY",
                    success = true
                )
            }

            ActionCommand.TYPE_OPEN_SETTINGS -> {
                val target = command.target
                val spoken = deviceController.openSettings(target)
                ActionResult(
                    spokenText = spoken,
                    actionBadge = "SETTINGS: ${target ?: "GENERAL"}",
                    success = true
                )
            }

            ActionCommand.TYPE_CALL_CONTACT -> {
                val target = command.target ?: ""
                val spoken = deviceController.initiatePhoneCallUi(target)
                ActionResult(
                    spokenText = spoken,
                    actionBadge = "DIAL: $target",
                    success = true
                )
            }

            ActionCommand.TYPE_SEND_MESSAGE -> {
                val recipient = command.target ?: "Contact"
                val message = command.query ?: command.parameters["message"] ?: ""
                val spoken = deviceController.prepareSmsUi(recipient, message)
                ActionResult(
                    spokenText = spoken,
                    actionBadge = "SMS: $recipient",
                    success = true
                )
            }

            ActionCommand.TYPE_COPY_CLIPBOARD -> {
                val text = command.target ?: command.query ?: ""
                val spoken = deviceController.copyToClipboard(text)
                ActionResult(
                    spokenText = spoken,
                    actionBadge = "CLIPBOARD COPIED",
                    success = true
                )
            }

            ActionCommand.TYPE_SHARE_TEXT -> {
                val text = command.target ?: command.query ?: ""
                val spoken = deviceController.shareText(text)
                ActionResult(
                    spokenText = spoken,
                    actionBadge = "SHARED TEXT",
                    success = true
                )
            }

            else -> {
                ActionResult(
                    spokenText = command.spokenResponse,
                    actionBadge = null,
                    success = true
                )
            }
        }
    }
}

data class ActionResult(
    val spokenText: String,
    val actionBadge: String? = null,
    val success: Boolean = true
)
