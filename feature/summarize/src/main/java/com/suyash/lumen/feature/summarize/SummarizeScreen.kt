package com.suyash.lumen.feature.summarize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suyash.lumen.core.ai.SummaryMode
import com.suyash.lumen.core.design.LumenTheme

@Composable
fun SummarizeScreen(
    initialText: String? = null,
    viewModel: SummarizeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialText) {
        if (!initialText.isNullOrBlank()) viewModel.onInputChanged(initialText)
    }

    LumenTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Lumen", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Paste text or share it from another app. Summarization runs on-device when supported.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::onInputChanged,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text("Input text") },
                placeholder = { Text("Paste an article, an email, a chapter…") },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMode.values().forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { viewModel.onModeChanged(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::onSummarizeClicked,
                    enabled = !state.isRunning && state.inputText.isNotBlank(),
                ) { Text(if (state.isRunning) "Summarizing…" else "Summarize") }

                if (state.isRunning) {
                    OutlinedButton(onClick = viewModel::onCancelClicked) { Text("Cancel") }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (state.error != null) {
                Text(
                    "Error: ${state.error}",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                )
            }

            if (state.output.isNotEmpty()) {
                Text("Summary", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(state.output, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
