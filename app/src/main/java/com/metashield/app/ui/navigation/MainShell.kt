package com.metashield.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metashield.app.ui.components.NavItem
import com.metashield.app.ui.components.PremiumNavigationBar

// ─────────────────────────────────────────────────────────────────────────────
//  Tab definition
// ─────────────────────────────────────────────────────────────────────────────
sealed class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home     : BottomNavTab("tab_home",     "Home",     Icons.Filled.Home,     Icons.Outlined.Home)
    object Tools    : BottomNavTab("tab_tools",    "Tools",    Icons.Filled.Build,    Icons.Outlined.Build)
    object History  : BottomNavTab("tab_history",  "History",  Icons.Filled.History,  Icons.Outlined.History)
    object Settings : BottomNavTab("tab_settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val ALL_TABS = listOf(
    BottomNavTab.Home,
    BottomNavTab.Tools,
    BottomNavTab.History,
    BottomNavTab.Settings
)

// ─────────────────────────────────────────────────────────────────────────────
//  MainShell
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MainShell(
    onStripFile: (Uri) -> Unit,
    onEditMetadata: (Uri) -> Unit,
    onBatchProcess: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToTemplateSelection: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToStego: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToBugReport: () -> Unit,
    onNavigateToIdentityMasking: (String) -> Unit,
    onFileSelected: (Uri) -> Unit,
    onCompare: (Long) -> Unit,
    sharedUris: List<Uri> = emptyList(),
    batchViewModel: com.metashield.app.ui.batch.BatchQueueViewModel,
    initialTab: BottomNavTab = BottomNavTab.Home
) {
    var selectedTab   by remember { mutableStateOf<BottomNavTab>(initialTab) }
    val selectedIndex  = ALL_TABS.indexOf(selectedTab)

    val navItems = ALL_TABS.map { NavItem(it.label, it.selectedIcon, it.unselectedIcon) }

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // ── Custom sliding-pill navigation bar ────────────────────────
            PremiumNavigationBar(
                items         = navItems,
                selectedIndex = selectedIndex,
                onItemSelected = { idx -> selectedTab = ALL_TABS[idx] }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            // ── Crossfade + micro-slide between tabs ──────────────────────
            AnimatedContent(
                targetState  = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(280, delayMillis = 40)) +
                     slideInVertically(tween(280, delayMillis = 40)) { (it * 0.04f).toInt() })
                        .togetherWith(fadeOut(tween(180)))
                },
                label    = "tabContent",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    BottomNavTab.Home    -> com.metashield.app.ui.home.HomeScreen(
                        onStripFile                   = onStripFile,
                        onEditMetadata                = onEditMetadata,
                        onBatchProcess                = onBatchProcess,
                        onNavigateToHistory           = { selectedTab = BottomNavTab.History },
                        onNavigateToSettings          = { selectedTab = BottomNavTab.Settings },
                        onNavigateToTemplates         = onNavigateToTemplates,
                        onNavigateToTemplateSelection = onNavigateToTemplateSelection,
                        onNavigateToVault             = onNavigateToVault,
                        onNavigateToStego             = onNavigateToStego,
                        onNavigateToScanner           = onNavigateToScanner,
                        onNavigateToTools             = { selectedTab = BottomNavTab.Tools },
                        sharedUris                    = sharedUris,
                        batchViewModel                = batchViewModel
                    )

                    BottomNavTab.Tools   -> com.metashield.app.ui.tools.ToolsScreen(
                        onStripFile                   = onStripFile,
                        onEditMetadata                = onEditMetadata,
                        onBatchProcess                = onBatchProcess,
                        onNavigateToTemplates         = onNavigateToTemplates,
                        onNavigateToTemplateSelection = onNavigateToTemplateSelection,
                        onNavigateToVault             = onNavigateToVault,
                        onNavigateToStego             = onNavigateToStego,
                        onNavigateToIdentityMasking   = onNavigateToIdentityMasking,
                        batchViewModel                = batchViewModel
                    )

                    BottomNavTab.History -> com.metashield.app.ui.history.HistoryScreen(
                        onNavigateUp   = { selectedTab = BottomNavTab.Home },
                        onFileSelected = onFileSelected,
                        onCompare      = onCompare
                    )

                    BottomNavTab.Settings -> com.metashield.app.ui.settings.SettingsScreen(
                        onNavigateUp          = { selectedTab = BottomNavTab.Home },
                        onNavigateToBugReport = onNavigateToBugReport
                    )
                }
            }
        }
    }
}
