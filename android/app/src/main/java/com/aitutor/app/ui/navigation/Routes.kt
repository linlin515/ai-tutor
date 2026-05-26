package com.aitutor.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val CHAT = "chat"
    const val CHAT_CONVERSATION = "chat/{conversationId}"
    const val SOLVE = "solve"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val SUBSCRIPTION = "subscription"
    const val DASHBOARD = "dashboard"
    const val QUIZ = "quiz"
    const val REVIEW = "review"
    const val REPORT = "report"
    const val REPORT_EXPORT = "report_export"
    const val FLASHCARD = "flashcard"
    const val PRIVACY_POLICY = "privacy_policy"
    const val USER_AGREEMENT = "user_agreement"
    const val CRASH_LOG = "crash_log"
    const val ONBOARDING = "onboarding"
    const val FAVORITES = "favorites"
    const val WRONG_ANSWERS = "wrong_answers"

    fun chatConversation(conversationId: Long) = "chat/$conversationId"
}
