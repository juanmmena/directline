package com.directline.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.directline.core.whatsapp.CountryInfo

@Composable
fun ReadableWidthContainer(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = DirectlineSpacing.maxReadableWidth),
    ) {
      content()
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    if (actionLabel != null && onActionClick != null) {
      TextButton(onClick = onActionClick) {
        Text(text = actionLabel)
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(DirectlineSpacing.lg),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(DirectlineSpacing.sm))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(modifier = Modifier.height(DirectlineSpacing.xs))
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun StatusBanner(
  message: String,
  isError: Boolean,
  modifier: Modifier = Modifier,
  onDismiss: (() -> Unit)? = null,
) {
  val container = if (isError) {
    MaterialTheme.colorScheme.errorContainer
  } else {
    MaterialTheme.colorScheme.primaryContainer
  }
  val content = if (isError) {
    MaterialTheme.colorScheme.onErrorContainer
  } else {
    MaterialTheme.colorScheme.onPrimaryContainer
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .semantics { liveRegion = LiveRegionMode.Polite },
    colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isError) Icons.Default.Warning else Icons.Default.Info,
        contentDescription = null,
      )
      Spacer(modifier = Modifier.width(DirectlineSpacing.sm))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f),
      )
      if (onDismiss != null) {
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss notice",
          )
        }
      }
    }
  }
}

@Composable
fun CountryPickerDialog(
  countries: List<CountryInfo>,
  selectedIso: String?,
  allowClear: Boolean = false,
  onSelect: (CountryInfo?) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered = remember(query, countries) {
    val q = query.trim()
    if (q.isEmpty()) {
      countries
    } else {
      val digits = q.removePrefix("+")
      countries.filter {
        it.name.contains(q, ignoreCase = true) ||
          it.isoCode.equals(q, ignoreCase = true) ||
          it.callingCode.toString().startsWith(digits)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Select Country") },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Search country or code (+57)") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(DirectlineSpacing.sm))
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
          if (allowClear) {
            item(key = "clear_country") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .clickable {
                    onSelect(null)
                    onDismiss()
                  }
                  .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("None (always require selection)", style = MaterialTheme.typography.bodyMedium)
              }
              HorizontalDivider()
            }
          }
          items(filtered, key = { it.isoCode }) { country ->
            val isSelected = country.isoCode.equals(selectedIso, ignoreCase = true)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable {
                  onSelect(country)
                  onDismiss()
                }
                .padding(vertical = 12.dp, horizontal = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${country.name} (${country.isoCode})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = country.formattedCallingCode,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            HorizontalDivider()
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}
