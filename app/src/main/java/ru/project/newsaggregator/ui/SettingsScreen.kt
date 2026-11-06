package ru.project.newsaggregator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.project.newsaggregator.data.NewsSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
) {
    val sources by viewModel.sources.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var pendingRemove by remember { mutableStateOf<NewsSource?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Тема", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = viewModel.appTheme == theme,
                        onClick = { viewModel.selectTheme(theme) },
                        label = { Text(theme.title) },
                    )
                }
            }

            Text("Поиск", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Поиск по ленте") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )

            Text("Источники", style = MaterialTheme.typography.titleMedium)
            sources.forEach { source ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = source.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = source.enabled,
                        onCheckedChange = { viewModel.setSourceEnabled(source.id, it) },
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Удалить ${source.name}",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { pendingRemove = source },
                    )
                }
            }
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Добавить источник", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showAdd) {
        AddSourceDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, url ->
                val error = viewModel.addSource(name, url)
                if (error == null) showAdd = false
                error
            },
        )
    }
    pendingRemove?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Удалить источник") },
            text = { Text("«${source.name}» пропадёт из ленты вместе со своими новостями.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSource(source.id)
                    pendingRemove = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: suspend (name: String, url: String) -> String?,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Новый источник") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название") },
                    placeholder = { Text("Можно оставить пустым") },
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Адрес сайта или RSS") },
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        error = onAdd(name, url)
                        saving = false
                    }
                },
                enabled = !saving && url.isNotBlank(),
            ) {
                Text(if (saving) "Проверяю…" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена") }
        },
    )
}
