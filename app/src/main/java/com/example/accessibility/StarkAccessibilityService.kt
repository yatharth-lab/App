package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.lang.ref.WeakReference

class StarkAccessibilityService : AccessibilityService() {

    companion object {
        private var instanceRef: WeakReference<StarkAccessibilityService>? = null

        fun isServiceConnected(): Boolean {
            return instanceRef?.get() != null
        }

        fun getInstance(): StarkAccessibilityService? {
            return instanceRef?.get()
        }

        /**
         * Resolves the primary active window root node, falling back to windows list if rootInActiveWindow is null.
         */
        private fun getBestRootNode(service: StarkAccessibilityService): AccessibilityNodeInfo? {
            // 1. Try standard rootInActiveWindow
            val activeRoot = service.rootInActiveWindow
            if (activeRoot != null) return activeRoot

            // 2. Try window list (API 21+)
            val windows = service.windows
            if (windows != null && windows.isNotEmpty()) {
                // Find focused window or top application window
                val appWindow = windows.firstOrNull { it.isFocused }
                    ?: windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                    ?: windows.firstOrNull()
                val windowRoot = appWindow?.root
                if (windowRoot != null) return windowRoot
            }
            return null
        }

        /**
         * Recursively extracts all visible text, content descriptions, and interactive labels from the active screen.
         */
        fun readScreenContent(): ScreenContentReport {
            val service = getInstance() ?: return ScreenContentReport(
                fullText = "Accessibility service is not active. Please grant accessibility permissions in Settings.",
                interactiveSummary = "",
                packageName = "Unknown",
                nodeCount = 0,
                success = false
            )

            val rootNode = getBestRootNode(service) ?: return ScreenContentReport(
                fullText = "Screen content is currently empty or protected by the active application.",
                interactiveSummary = "",
                packageName = service.currentPackage ?: "Unknown",
                nodeCount = 0,
                success = false
            )

            val collectedTexts = mutableListOf<String>()
            val clickableLabels = mutableListOf<String>()
            var nodeCount = 0

            fun traverse(node: AccessibilityNodeInfo?) {
                if (node == null) return
                nodeCount++

                val text = node.text?.toString()?.trim()
                val desc = node.contentDescription?.toString()?.trim()

                if (!text.isNullOrEmpty()) {
                    collectedTexts.add(text)
                } else if (!desc.isNullOrEmpty()) {
                    collectedTexts.add(desc)
                }

                if (node.isClickable) {
                    val label = text ?: desc
                    if (!label.isNullOrBlank()) {
                        clickableLabels.add(label)
                    }
                }

                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i))
                }
            }

            traverse(rootNode)

            val distinctTexts = collectedTexts.distinct()
            val textSummary = if (distinctTexts.isEmpty()) {
                "No readable text found on current screen."
            } else {
                distinctTexts.joinToString(separator = "\n")
            }

            val pkg = service.currentPackage ?: rootNode.packageName?.toString() ?: "Unknown"

            return ScreenContentReport(
                fullText = textSummary,
                interactiveSummary = clickableLabels.distinct().take(10).joinToString(", "),
                packageName = pkg,
                nodeCount = nodeCount,
                success = true
            )
        }

        /**
         * Finds an element matching the given text or description or view ID and performs a click.
         */
        fun clickElementWithText(target: String): Boolean {
            val service = getInstance() ?: return false
            val rootNode = getBestRootNode(service) ?: return false
            val lowerTarget = target.lowercase().trim()

            var clicked = false

            fun searchAndClick(node: AccessibilityNodeInfo?): Boolean {
                if (node == null || clicked) return false

                val text = node.text?.toString()?.lowercase()?.trim()
                val desc = node.contentDescription?.toString()?.lowercase()?.trim()
                val viewId = node.viewIdResourceName?.lowercase()?.trim()

                val matches = (text != null && text.contains(lowerTarget)) ||
                        (desc != null && desc.contains(lowerTarget)) ||
                        (viewId != null && viewId.contains(lowerTarget))

                if (matches) {
                    if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        clicked = true
                        return true
                    } else {
                        // Check parent nodes up the hierarchy
                        var parent = node.parent
                        while (parent != null) {
                            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                clicked = true
                                return true
                            }
                            parent = parent.parent
                        }
                    }
                }

                for (i in 0 until node.childCount) {
                    if (searchAndClick(node.getChild(i))) return true
                }
                return false
            }

            searchAndClick(rootNode)
            return clicked
        }

        /**
         * Sets text into the currently focused or first available editable field.
         */
        fun writeTextToInput(textToSet: String): Boolean {
            val service = getInstance() ?: return false
            val rootNode = getBestRootNode(service) ?: return false

            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet)
            }

            // Check focused input first
            val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null && focused.isEditable) {
                if (focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                    return true
                }
            }

            // Search for first editable field
            var typed = false
            fun findEditable(node: AccessibilityNodeInfo?): Boolean {
                if (node == null || typed) return false

                if (node.isEditable) {
                    // Try focusing first if not focused
                    node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                        typed = true
                        return true
                    }
                }

                for (i in 0 until node.childCount) {
                    if (findEditable(node.getChild(i))) return true
                }
                return false
            }

            findEditable(rootNode)
            return typed
        }

        /**
         * Performs in-screen search by clicking search bar icon, writing text, and pressing submit.
         */
        fun searchInActiveScreen(query: String): Boolean {
            val service = getInstance() ?: return false
            val rootNode = getBestRootNode(service) ?: return false

            // 1. If an editable field is already visible, type query into it
            val wrote = writeTextToInput(query)
            if (wrote) return true

            // 2. Click search icon or search button on screen
            val searchClicked = clickElementWithText("search") ||
                    clickElementWithText("dhoondo") ||
                    clickElementWithText("find") ||
                    clickElementWithText("khoj")

            if (searchClicked) {
                try { Thread.sleep(400) } catch (_: Exception) {}
                val updatedRoot = getBestRootNode(service) ?: rootNode
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
                }
                val focused = updatedRoot.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focused != null && focused.isEditable) {
                    return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
            }
            return false
        }

        /**
         * Performs a global action: Back, Home, Recents, Notifications, etc.
         */
        fun performGlobal(action: Int): Boolean {
            val service = getInstance() ?: return false
            return service.performGlobalAction(action)
        }
    }

    var currentPackage: String? = null
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null && event.packageName != null) {
            currentPackage = event.packageName.toString()
        }
    }

    override fun onInterrupt() {
        // Required callback
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() == this) {
            instanceRef = null
        }
    }
}

data class ScreenContentReport(
    val fullText: String,
    val interactiveSummary: String = "",
    val packageName: String,
    val nodeCount: Int,
    val success: Boolean
)
