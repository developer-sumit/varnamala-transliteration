package com.sumit.varnamala.data

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Gallery : Screen("gallery")
    data object Settings : Screen("settings")
    data object Language : Screen("language")
    data object ImageDetail : Screen("imageDetail/{id}") {
        fun create(id: Long) = "imageDetail/$id"
    }
}
