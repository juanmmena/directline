package com.directline.core.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.directline.feature.contacts.ContactsScreen
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
  CONTACTS("contacts", "Contacts", Icons.Default.Contacts),
  TEMPLATES("templates", "Templates", Icons.Default.Description),
  HISTORY("history", "History", Icons.Default.History),
  SETTINGS("settings", "Settings", Icons.Default.Settings),
}

@Composable
fun DirectlineApp(
  incomingSharedText: String?,
  onConsumeSharedText: () -> Unit,
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

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail {
          DirectlineTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationRailItem(
              selected = selected,
              onClick = { navigateToTab(tab) },
              icon = { Icon(tab.icon, contentDescription = tab.label) },
              label = { Text(tab.label) },
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavigationRail) {
            NavigationBar {
              DirectlineTab.entries.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(tab) },
                  icon = { Icon(tab.icon, contentDescription = tab.label) },
                  label = { Text(tab.label) },
                )
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
              onNavigateToContacts = { navigateToTab(DirectlineTab.CONTACTS) },
              onShowSnackbar = showSnackbar,
            )
          }
          composable(DirectlineTab.CONTACTS.route) {
            ContactsScreen(onShowSnackbar = showSnackbar)
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
            HistoryScreen(onShowSnackbar = showSnackbar)
          }
          composable(DirectlineTab.SETTINGS.route) {
            SettingsScreen(onShowSnackbar = showSnackbar)
          }
        }
      }
    }
  }
}
