package ru.project.newsaggregator.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import ru.project.newsaggregator.data.Article
import ru.project.newsaggregator.data.formatRelative

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    article: Article?,
    saved: Boolean,
    onBack: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(article?.sourceName ?: "Материал") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (article != null) {
                        IconButton(onClick = onToggleSaved) {
                            Icon(
                                imageVector = if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (saved) "Убрать из сохранённых" else "Сохранить",
                                tint = if (saved) Color(0xFFC9891A) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (article == null) {
            Text(
                text = "Материал не найден",
                modifier = Modifier.padding(padding).padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(text = article.title, style = MaterialTheme.typography.headlineSmall)
            val whenText = formatRelative(article.publishedAt)
            if (whenText.isNotBlank()) {
                Text(
                    text = whenText,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            if (article.summary.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = article.summary,
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, article.link.toUri()))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Открыть источник")
            }
        }
    }
}
