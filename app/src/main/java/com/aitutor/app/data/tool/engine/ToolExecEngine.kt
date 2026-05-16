package com.aitutor.app.data.tool.engine

import com.aitutor.app.domain.model.ToolResult

/**
 * v2.0 Agent: 工具接口 — 所有工具必须实现此接口
 */
interface Tool {
    /** 工具名称（如 web_search, calculator, datetime） */
    val name: String

    /** 工具描述（用于 LLM 理解工具用途） */
    val description: String

    /** OpenAI JSON Schema 格式的参数定义 */
    val parameters: Map<String, Any>

    /**
     * 执行工具并返回结果
     * @param args 工具参数（从 LLM tool_call 解析而来）
     * @return ToolResult 包含执行结果或错误信息
     */
    suspend fun execute(args: Map<String, Any>): ToolResult
}
