package ru.project.newsaggregator

import android.app.Application
import ru.project.newsaggregator.data.NewsRepository

class NewsApplication : Application() {
    val repository: NewsRepository by lazy { NewsRepository(this) }
}
