package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class LiveTechNewsItem(
    val id: String,
    val title: String,
    val source: String,
    val publishedAt: String,
    val summary: String
)

@Composable
fun TraineeTechNewsCard(
    items: List<LiveTechNewsItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Live Technical News", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onRefresh, enabled = !isLoading) {
                    Text(if (isLoading) "Loading..." else "Refresh")
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (!isLoading && items.isEmpty()) {
                Text("No technical news updates available right now.", style = MaterialTheme.typography.bodySmall)
            } else {
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text("${item.source} • ${item.publishedAt}", style = MaterialTheme.typography.labelSmall)
                        Text(item.summary, style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
