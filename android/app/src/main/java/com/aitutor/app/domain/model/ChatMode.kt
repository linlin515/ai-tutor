package com.aitutor.app.domain.model

/**
 * ChatMode represents the current interaction mode of the AI tutor.
 *
 * ASSISTANT - Normal Q&A mode, direct answers.
 * TUTOR - Socratic teaching mode, guides student through questions.
 * QUIZ - Quiz mode, tests student knowledge with questions.
 */
enum class ChatMode {
    ASSISTANT,
    TUTOR,
    QUIZ;

    val displayName: String
        get() = when (this) {
            ASSISTANT -> "普通问答"
            TUTOR -> "苏格拉底式教学"
            QUIZ -> "测验模式"
        }

    val description: String
        get() = when (this) {
            ASSISTANT -> "直接回答问题，提供详细解答"
            TUTOR -> "通过提问引导你思考，逐步发现答案"
            QUIZ -> "通过测验检验你的知识掌握程度"
        }
}
