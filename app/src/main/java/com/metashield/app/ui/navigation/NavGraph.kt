package com.metashield.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.metashield.app.ui.batch.BatchQueueScreen
import com.metashield.app.ui.editor.MetadataEditorScreen
import com.metashield.app.ui.preview.OutputPreviewScreen
import com.metashield.app.ui.removal.RemovalConfigScreen
import com.metashield.app.ui.settings.BugReportScreen
import com.metashield.app.ui.update.UpdateChecker
import com.metashield.app.ui.templates.TemplatesScreen
import com.metashield.app.ui.viewer.MetadataViewerScreen
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(initialSharedUris: List<Uri> = emptyList()) {
    val navController = rememberNavController()



    // Shared BatchViewModel scoped to NavGraph
    val batchViewModel: com.metashield.app.ui.batch.BatchQueueViewModel = hiltViewModel()

    // ── Background update check — shows dialog automatically if newer version found ──
    UpdateChecker()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ── Main Shell (Bottom Nav Host) ──────────────────────────────────────
        composable(Screen.Home.route) {
            MainShell(
                onStripFile = { uri ->
                    val enc = java.net.URLEncoder.encode(uri.toString(), "UTF-8")
                    navController.navigate(Screen.MetadataViewer.createRoute(enc))
                },
                onEditMetadata = { uri ->
                    val enc = java.net.URLEncoder.encode(uri.toString(), "UTF-8")
                    navController.navigate(Screen.MetadataViewer.createRoute(enc))
                },
                onBatchProcess      = { navController.navigate(Screen.BatchQueue.route) },
                onNavigateToTemplates = { navController.navigate(Screen.Templates.route) },
                onNavigateToTemplateSelection = { navController.navigate("template_selection") },
                onNavigateToVault   = { navController.navigate(Screen.Vault.route) },
                onNavigateToStego   = { navController.navigate(Screen.Stego.route) },
                onNavigateToIdentityMasking = { enc -> navController.navigate(Screen.IdentityMasking.createRoute(enc)) },
                onNavigateToScanner = { navController.navigate("privacy_scanner") },
                onNavigateToBugReport = { navController.navigate(Screen.BugReport.route) },
                onFileSelected = { uri ->
                    val enc = java.net.URLEncoder.encode(uri.toString(), "UTF-8")
                    navController.navigate(Screen.MetadataViewer.createRoute(enc))
                },
                onCompare = { id -> navController.navigate(Screen.MetadataComparison.createRoute(id)) },
                sharedUris = initialSharedUris,
                batchViewModel = batchViewModel
            )
        }

        // ── Privacy Scanner ───────────────────────────────────────────────────
        composable("privacy_scanner") {
            com.metashield.app.ui.scanner.PrivacyScannerScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // ── Metadata Viewer ───────────────────────────────────────────────────
        composable(
            route = Screen.MetadataViewer.route,
            arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
        ) { back ->
            val enc = back.arguments?.getString("encodedUri") ?: return@composable
            val uri = Uri.parse(enc)
            MetadataViewerScreen(
                fileUri            = uri,
                onNavigateUp       = { navController.popBackStack() },
                onNavigateToRemoval = {
                    val encoded = URLEncoder.encode(enc, "UTF-8")
                    navController.navigate(Screen.RemovalConfig.createRoute(encoded))
                },
                onNavigateToEditor = {
                    val encoded = URLEncoder.encode(enc, "UTF-8")
                    navController.navigate(Screen.MetadataEditor.createRoute(encoded))
                }
            )
        }

        // ── Metadata Editor ───────────────────────────────────────────────────
        composable(
            route = Screen.MetadataEditor.route,
            arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
        ) { back ->
            val enc = back.arguments?.getString("encodedUri") ?: return@composable
            val uri = Uri.parse(enc)
            MetadataEditorScreen(
                fileUri      = uri,
                onNavigateUp = { navController.popBackStack() },
                onSaved      = { inEnc, outEnc ->
                    navController.navigate(Screen.OutputPreview.createRoute(inEnc, outEnc)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // ── Removal Config ────────────────────────────────────────────────────
        composable(
            route = Screen.RemovalConfig.route,
            arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
        ) { back ->
            val enc = back.arguments?.getString("encodedUri") ?: return@composable
            val uri = Uri.parse(enc)
            RemovalConfigScreen(
                fileUri      = uri,
                onNavigateUp = { navController.popBackStack() },
                onProcessed  = { inEnc, outEnc ->
                    navController.navigate(Screen.OutputPreview.createRoute(inEnc, outEnc)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // ── Batch Queue ───────────────────────────────────────────────────────
        composable(Screen.BatchQueue.route) {
            BatchQueueScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToResults = { 
                    navController.navigate("batch_results") {
                        popUpTo(Screen.BatchQueue.route) { inclusive = true }
                    }
                },
                viewModel = batchViewModel
            )
        }

        // ── Template Selection ──────────────────────────────────────────────
        composable("template_selection") {
            com.metashield.app.ui.templates.TemplateSelectionScreen(
                onNavigateUp = { navController.popBackStack() },
                onTemplateSelected = { template ->
                    batchViewModel.setMode(com.metashield.app.ui.batch.BatchProcessingMode.APPLY_TEMPLATE, template)
                    navController.navigate(Screen.BatchQueue.route)
                }
            )
        }

        // ── Batch Results ──────────────────────────────────────────────────────
        composable("batch_results") {
            com.metashield.app.ui.batch.BatchResultsScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToViewer = { uri ->
                    val enc = URLEncoder.encode(uri.toString(), "UTF-8")
                    navController.navigate(Screen.MetadataViewer.createRoute(enc))
                },
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                viewModel = batchViewModel
            )
        }

        // ── Output Preview ────────────────────────────────────────────────────
        composable(
            route = Screen.OutputPreview.route,
            arguments = listOf(
                navArgument("encodedInputUri")  { type = NavType.StringType },
                navArgument("encodedOutputUri") { type = NavType.StringType }
            )
        ) { back ->
            val encIn  = back.arguments?.getString("encodedInputUri")  ?: return@composable
            val encOut = back.arguments?.getString("encodedOutputUri") ?: return@composable
            OutputPreviewScreen(
                inputUri  = Uri.parse(encIn),
                outputUri = Uri.parse(encOut),
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // ── Templates ─────────────────────────────────────────────────────────
        composable(Screen.Templates.route) {
            TemplatesScreen(
                onNavigateUp = { navController.popBackStack() },
                onApplyTemplate = { navController.navigate(Screen.BatchQueue.route) }
            )
        }


        // ── Bug Report ───────────────────────────────────────────────────────
        composable(Screen.BugReport.route) {
            BugReportScreen(onNavigateUp = { navController.popBackStack() })
        }

        // ── Vault ─────────────────────────────────────────────────────────────
        composable(Screen.Vault.route) {
            com.metashield.app.ui.vault.VaultScreen(onNavigateUp = { navController.popBackStack() })
        }

        // ── Steganography (Pixel Armor) ───────────────────────────────────────
        composable(Screen.Stego.route) {
            com.metashield.app.ui.stego.SteganographyScreen(onNavigateUp = { navController.popBackStack() })
        }

        // ── Metadata Comparison (Time Machine) ────────────────────────────────
        composable(
            route = Screen.MetadataComparison.route,
            arguments = listOf(navArgument("historyId") { type = NavType.LongType })
        ) { back ->
            val historyId = back.arguments?.getLong("historyId") ?: 0L
            com.metashield.app.ui.history.MetadataComparisonScreen(
                historyId = historyId,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // ── Identity Masking (AI Anonymization) ───────────────────────────────
        composable(
            route = Screen.IdentityMasking.route,
            arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
        ) { back ->
            val enc = back.arguments?.getString("encodedUri") ?: return@composable
            val uri = Uri.parse(java.net.URLDecoder.decode(enc, "UTF-8"))
            com.metashield.app.ui.tools.AnonymizeScreen(
                fileUri = uri,
                onNavigateBack = { navController.popBackStack() },
                onProcessed = { outUri ->
                    val encOut = java.net.URLEncoder.encode(outUri.toString(), "UTF-8")
                    navController.navigate(Screen.MetadataViewer.createRoute(encOut)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }


    }
}
