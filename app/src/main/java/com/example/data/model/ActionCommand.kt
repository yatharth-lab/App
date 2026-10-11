package com.example.data.model

data class ActionCommand(
    val type: String,
    val target: String? = null,
    val query: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val spokenResponse: String = "",
    val subCommands: List<ActionCommand> = emptyList(),
    val requiresConfirmation: Boolean = false
) {
    companion object {
        const val TYPE_NORMAL_RESPONSE = "NORMAL_RESPONSE"
        const val TYPE_OPEN_APP = "OPEN_APP"
        const val TYPE_WEB_SEARCH = "WEB_SEARCH"
        const val TYPE_OPEN_URL = "OPEN_URL"
        const val TYPE_SET_TIMER = "SET_TIMER"
        const val TYPE_SET_ALARM = "SET_ALARM"
        const val TYPE_REMEMBER = "REMEMBER"
        const val TYPE_FORGET = "FORGET"
        const val TYPE_LIST_MEMORIES = "LIST_MEMORIES"
        const val TYPE_DEVICE_INFO = "DEVICE_INFO"
        const val TYPE_OPEN_SETTINGS = "OPEN_SETTINGS"
        const val TYPE_CALL_CONTACT = "CALL_CONTACT"
        const val TYPE_SEND_MESSAGE = "SEND_MESSAGE"
        const val TYPE_COPY_CLIPBOARD = "COPY_CLIPBOARD"
        const val TYPE_SHARE_TEXT = "SHARE_TEXT"

        // New Advanced Jarvis Accessibility & System Actions
        const val TYPE_READ_SCREEN = "READ_SCREEN"
        const val TYPE_CLICK_ELEMENT = "CLICK_ELEMENT"
        const val TYPE_WRITE_TEXT = "WRITE_TEXT"
        const val TYPE_SEARCH_IN_APP = "SEARCH_IN_APP"
        const val TYPE_GENERATE_IMAGE = "GENERATE_IMAGE"
        const val TYPE_WRITE_PARAGRAPH = "WRITE_PARAGRAPH"
        const val TYPE_GLOBAL_ACTION = "GLOBAL_ACTION"
        const val TYPE_FLASHLIGHT = "FLASHLIGHT"
        const val TYPE_SHOW_OVERLAY = "SHOW_OVERLAY"
        const val TYPE_COMPOUND = "COMPOUND"
    }
}
