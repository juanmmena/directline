package com.directline.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.core.designsystem.EmptyStateCard
import com.directline.core.designsystem.StatusBanner
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
  viewModel: HistoryViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var showClearConfirm by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(DirectlineSpacing.md),
    verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Interaction History", style = MaterialTheme.typography.headlineSmall)
        if (state.items.isNotEmpty()) {
          TextButton(onClick = { showClearConfirm = true }) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear all")
          }
        }
      }
    }

    item {
      StatusBanner(
        message = "History records represent WhatsApp launch attempts from Directline — not message delivery or read confirmation.",
        isError = false,
      )
    }

    state.feedbackMessage?.let { msg ->
      item {
        StatusBanner(
          message = msg,
          isError = state.isFeedbackError,
          onDismiss = viewModel::dismissFeedback,
        )
      }
    }

    if (state.items.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Default.History,
          title = "No history records",
          description = "WhatsApp conversations initiated from Directline will be logged locally here.",
        )
      }
    } else {
      items(state.items, key = { it.id }) { item ->
        val formattedTime = remember(item.createdAt) {
          DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(item.createdAt))
        }
        Card(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.contactNameSnapshot ?: item.phoneNumberSnapshot,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              if (item.contactNameSnapshot != null) {
                Text(
                  text = item.phoneNumberSnapshot,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              item.messagePreview?.let { preview ->
                Text(
                  text = preview,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "Launch attempt • $formattedTime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(onClick = { viewModel.reopenDestination(item) }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Reopen WhatsApp destination",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(onClick = { viewModel.deleteItem(item.id) }) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete record",
              )
            }
          }
        }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear all history?") },
      text = { Text("This will remove all local launch attempt records.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearHistory()
            showClearConfirm = false
          },
        ) {
          Text("Clear")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }
}
