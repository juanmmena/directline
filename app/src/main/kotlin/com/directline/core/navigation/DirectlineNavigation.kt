package com.directline.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.directline.core.ads.AdMobBanner
import com.directline.core.designsystem.DirectlineLogoMark
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.feature.history.HistoryScreen
import com.directline.feature.home.HomeScreen
import com.directline.feature.settings.SettingsScreen
import com.directline.feature.templates.TemplatesScreen
import kotlinx.coroutines.launch

enum class DirectlineTab(
  val route: String,
  val label: String,
  val icon: ImageVector,
) {
  HOME("home", "Home", Icons.Default.Home),
  TEMPLATES("templates", "Templates", Icons.Default.Description),
  HISTORY("history", "History", Icons.Default.History),
  SETTINGS("settings", "Settings", Icons.Default.Settings),
}

@Composable
fun DirectlineApp(
  incomingSharedText: String?,
  onConsumeSharedText: () -> Unit,
  onBeforeWhatsAppLaunch: (proceed: () -> Unit) -> Unit,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var pendingTemplateMessage by remember { mutableStateOf<String?>(null) }

  fun navigateToTab(tab: DirectlineTab) {
    navController.navigate(tab.route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  val showSnackbar: (String) -> Unit = remember(snackbarHostState, scope) {
    { message ->
      scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
      }
    }
  }

  LaunchedEffect(incomingSharedText) {
    if (!incomingSharedText.isNullOrBlank()) {
      navigateToTab(DirectlineTab.HOME)
    }
  }

  val contentScaffold: @Composable (showBottomBar: Boolean, modifier: Modifier) -> Unit =
    { showBottomBar, modifier ->
      Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          Column(modifier = Modifier.fillMaxWidth()) {
            AdMobBanner(
              modifier = if (showBottomBar) Modifier else Modifier.navigationBarsPadding(),
            )
            if (showBottomBar) {
              NavigationBar {
                DirectlineTab.entries.forEach { tab ->
                  val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateToTab(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, maxLines = 1) },
                  )
                }
              }
            }
          }
        },
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = DirectlineTab.HOME.route,
          modifier = Modifier.padding(innerPadding),
        ) {
          composable(DirectlineTab.HOME.route) {
            HomeScreen(
              incomingSharedText = incomingSharedText,
              onConsumeSharedText = onConsumeSharedText,
              pendingTemplateMessage = pendingTemplateMessage,
              onConsumePendingTemplate = { pendingTemplateMessage = null },
              onNavigateToHistory = { navigateToTab(DirectlineTab.HISTORY) },
              onBeforeWhatsAppLaunch = onBeforeWhatsAppLaunch,
              onShowSnackbar = showSnackbar,
            )
          }
          composable(DirectlineTab.TEMPLATES.route) {
            TemplatesScreen(
              onUseTemplateInLauncher = { renderedMessage ->
                pendingTemplateMessage = renderedMessage
                navigateToTab(DirectlineTab.HOME)
              },
              onShowSnackbar = showSnackbar,
            )
          }
          composable(DirectlineTab.HISTORY.route) {
            HistoryScreen(
              onBeforeWhatsAppLaunch = onBeforeWhatsAppLaunch,
              onShowSnackbar = showSnackbar,
            )
          }
          composable(DirectlineTab.SETTINGS.route) {
            SettingsScreen(onShowSnackbar = showSnackbar)
          }
        }
      }
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    when {
      maxWidth >= 840.dp -> {
        PermanentNavigationDrawer(
          drawerContent = {
            PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
              Row(
                modifier = Modifier.padding(DirectlineSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                DirectlineLogoMark(size = 36.dp)
                Text("Directline", style = MaterialTheme.typography.titleLarge)
              }
              Spacer(modifier = Modifier.height(DirectlineSpacing.sm))
              DirectlineTab.entries.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                NavigationDrawerItem(
                  selected = selected,
                  onClick = { navigateToTab(tab) },
                  icon = { Icon(tab.icon, contentDescription = null) },
                  label = { Text(tab.label) },
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
              }
            }
          },
        ) {
          contentScaffold(false, Modifier.fillMaxSize())
        }
      }

      maxWidth >= 600.dp -> {
        Row(modifier = Modifier.fillMaxSize()) {
          NavigationRail(
            header = {
              DirectlineLogoMark(
                size = 36.dp,
                modifier = Modifier.padding(vertical = 8.dp),
              )
            },
          ) {
            DirectlineTab.entries.forEach { tab ->
              val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, maxLines = 1) },
              )
            }
          }
          contentScaffold(false, Modifier.weight(1f))
        }
      }

      else -> {
        contentScaffold(true, Modifier.fillMaxSize())
      }
    }
  }
}
