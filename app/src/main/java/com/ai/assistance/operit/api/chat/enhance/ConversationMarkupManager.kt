package com.ai.assistance.operit.api.chat.enhance

import android.content.Context
import com.ai.assistance.operit.R
import com.ai.assistance.operit.core.tools.ToolExecutionLimits
import com.ai.assistance.operit.util.ChatMarkupRegex
import com.ai.assistance.operit.data.model.ToolResult
import com.ai.assistance.operit.core.application.OperitApplication

class ConversationMarkupManager {
    companion object {
        private val toolResultTruncationSuffix: String
            get() = try {
                OperitApplication.instance.getString(R.string.tool_result_truncated)
            } catch (e: Exception) {
                "\n[Tool result too long, truncated]"
            }

        fun createToolErrorStatus(toolName: String, errorMessage: String): String {
            return createToolResultXml(toolName, "error", "<content><error>${errorMessage}</error></content>")
        }

        fun createWarningStatus(warningMessage: String): String {
            return "<status type=\"warning\">$warningMessage</status>"
        }

        fun formatToolResultForMessage(result: ToolResult): String {
            return if (result.success) {
                createBoundedToolResultXml(result.toolName, "success", result.result.toString()) { payload ->
                    "<content>$payload</content>"
                }
            } else {
                val errorPayload = buildString {
                    val message = result.error.orEmpty().trim()
                    val detail = result.result.toString().trim()
                    append(message)
                    if (detail.isNotEmpty()) {
                        if (message.isNotEmpty()) append("\n\n")
                        append(detail)
                    }
                }
                createBoundedToolResultXml(result.toolName, "error", errorPayload) { payload ->
                    "<content><error>$payload</error></content>"
                }
            }
        }

        fun buildBoundedToolResultMessage(results: List<ToolResult>): String {
            if (results.isEmpty()) return ""
            val maxChars = ToolExecutionLimits.MAX_FINAL_TOOL_RESULT_MESSAGE_CHARS
            val separator = "\n"
            val builder = StringBuilder()
            for (result in results) {
                val formatted = formatToolResultForMessage(result)
                if (builder.length + formatted.length + (if (builder.isEmpty()) 0 else separator.length) > maxChars) break
                if (builder.isNotEmpty()) builder.append(separator)
                builder.append(formatted)
            }
            return builder.toString()
        }

        fun createMultipleToolsWarning(context: Context, toolName: String): String {
            return createWarningStatus(context.getString(R.string.conversation_markup_multiple_tools_warning, toolName))
        }

        fun createToolNotAvailableError(toolName: String, details: String? = null): String {
            val errorMessage = details ?: "The tool \`$toolName\` is not available."
            return createToolErrorStatus(toolName, errorMessage)
        }

        private fun createToolResultXml(toolName: String, status: String, content: String): String {
            val tagName = ChatMarkupRegex.generateRandomToolResultTagName()
            return "<$tagName name=\"$toolName\" status=\"$status\">$content</$tagName>"
        }

        private fun createBoundedToolResultXml(toolName: String, status: String, rawPayload: String, bodyBuilder: (String) -> String): String {
            val emptyXml = createToolResultXml(toolName, status, bodyBuilder(""))
            val maxPayloadChars = (ToolExecutionLimits.MAX_FINAL_TOOL_RESULT_MESSAGE_CHARS - emptyXml.length).coerceAtLeast(0)
            val boundedPayload = truncatePayload(rawPayload, maxPayloadChars)
            return createToolResultXml(toolName, status, bodyBuilder(boundedPayload))
        }

        private fun truncatePayload(payload: String, maxChars: Int): String {
            if (payload.length <= maxChars) return payload
            if (maxChars <= 0) return ""
            val suffix = toolResultTruncationSuffix
            if (suffix.length >= maxChars) return suffix.take(maxChars)
            return payload.take(maxChars - suffix.length).trimEnd() + suffix
        }
    }
}
