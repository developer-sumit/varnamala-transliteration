package com.sumit.varnamala.data

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
