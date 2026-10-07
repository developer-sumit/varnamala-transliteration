package com.sumit.varnamala.data

sealed class Screen(val route: String) {
    data object ResourceSetup : Screen("resourceSetup")
    data object OnboardingLanguage : Screen("onboardingLanguage")
    data object Home : Screen("home")
    data object Gallery : Screen("gallery")
    data object Settings : Screen("settings")
    data object Language : Screen("language")
    data object OcrLanguage : Screen("ocrLanguage")
    data object TransliterationLanguage : Screen("transliterationLanguage/{role}") {
        const val ROLE_SOURCE = "source"
        const val ROLE_TARGET = "target"
        fun create(role: String) = "transliterationLanguage/$role"
    }
    data object TextInput : Screen("textInput")
    data object History : Screen("history")
    data object Voice : Screen("voice")
    data object Models : Screen("models")
    data object Help : Screen("help")
    data object SharedImage : Screen("sharedImage")
    data object ImageDetail : Screen("imageDetail/{id}") {
        fun create(id: Long) = "imageDetail/$id"
    }
}
