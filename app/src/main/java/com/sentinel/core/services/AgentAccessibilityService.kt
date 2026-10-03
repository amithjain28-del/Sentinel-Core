package com.sentinel.core.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AgentAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SentinelA11y", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // For ReAct: capture structural changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val dom = compressToSemanticDOM(rootNode)
                // Cache it for the LLM to read when requested
                DOMCache.latestDOM = dom
            }
        }
    }

    override fun onInterrupt() {
        Log.d("SentinelA11y", "Accessibility Service Interrupted")
    }

    private fun compressToSemanticDOM(node: AccessibilityNodeInfo): String {
        val sb = java.lang.StringBuilder()
        traverseNode(node, 0, sb)
        return sb.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, depth: Int, sb: java.lang.StringBuilder) {
        if (!node.isVisibleToUser) return

        val indent = "  ".repeat(depth)
        val text = node.text ?: node.contentDescription ?: ""
        val isClickable = if (node.isClickable) "[CLICKABLE]" else ""

        if (text.isNotBlank() || node.isClickable) {
            val viewId = node.viewIdResourceName?.substringAfterLast("/") ?: "unknown_id"
            sb.append("$indent- <$viewId> '$text' $isClickable\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, depth + 1, sb)
                child.recycle()
            }
        }
    }
}

object DOMCache {
    var latestDOM: String = ""
}
