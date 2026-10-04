package com.sentinel.core.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ConcurrentHashMap

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AgentAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
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

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun compressToSemanticDOM(node: AccessibilityNodeInfo): String {
        val sb = java.lang.StringBuilder()
        DOMCache.nodeMap.clear()
        traverseNode(node, 0, sb)
        return sb.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, depth: Int, sb: java.lang.StringBuilder) {
        if (!node.isVisibleToUser) return

        val indent = "  ".repeat(depth)
        val text = node.text ?: node.contentDescription ?: ""
        val isClickable = if (node.isClickable) "[CLICKABLE]" else ""
        val isEditable = if (node.isEditable) "[EDITABLE]" else ""

        if (node.isClickable || node.isEditable) {
            val nodeId = node.hashCode()
            DOMCache.nodeMap[nodeId] = node
            sb.append("$indent- [$nodeId] '$text' $isClickable$isEditable\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, depth + 1, sb)
            }
        }
    }

    fun executeTap(nodeId: Int): Boolean {
        val node = DOMCache.nodeMap[nodeId]
        if (node != null && node.isClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d("SentinelA11y", "Executed tap on $nodeId: $result")
            return result
        }
        return false
    }

    fun executeType(nodeId: Int, text: String): Boolean {
        val node = DOMCache.nodeMap[nodeId]
        if (node != null && node.isEditable) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d("SentinelA11y", "Executed type on $nodeId: $result")
            return result
        }
        return false
    }
}

object DOMCache {
    var latestDOM: String = ""
    val nodeMap = ConcurrentHashMap<Int, AccessibilityNodeInfo>()
}
