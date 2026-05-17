package com.metashield.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object MetadataViewer : Screen("viewer/{encodedUri}") {
        fun createRoute(encodedUri: String) = "viewer/$encodedUri"
    }

    object MetadataEditor : Screen("editor/{encodedUri}") {
        fun createRoute(encodedUri: String) = "editor/$encodedUri"
    }

    object RemovalConfig : Screen("removal/{encodedUri}") {
        fun createRoute(encodedUri: String) = "removal/$encodedUri"
    }

    object BatchQueue : Screen("batch")

    object OutputPreview : Screen("preview/{encodedInputUri}/{encodedOutputUri}") {
        fun createRoute(encodedInput: String, encodedOutput: String) =
            "preview/$encodedInput/$encodedOutput"
    }

    object Templates : Screen("templates")
    object History   : Screen("history")
    object Settings  : Screen("settings")
    object Tools     : Screen("tools")
    object Vault     : Screen("vault")
    object Stego     : Screen("stego")
    object BugReport : Screen("bug_report")
    
    object MetadataComparison : Screen("comparison/{historyId}") {
        fun createRoute(historyId: Long) = "comparison/$historyId"
    }

    object IdentityMasking : Screen("masking/{encodedUri}") {
        fun createRoute(encodedUri: String) = "masking/$encodedUri"
    }


}
