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

    fun chatConversation(conversationId: Long) = "chat/$conversationId"
}
