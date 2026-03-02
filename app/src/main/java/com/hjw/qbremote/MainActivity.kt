package com.hjw.qbremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.QbRepository
import com.hjw.qbremote.ui.MainScreen
import com.hjw.qbremote.ui.MainViewModel
import com.hjw.qbremote.ui.theme.QBRemoteTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connectionStore = ConnectionStore(applicationContext)
        val repository = QbRepository()

        lifecycleScope.launch {
            connectionStore.settingsFlow
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collect { applyLanguage(it) }
        }

        setContent {
            QBRemoteTheme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModel.factory(connectionStore, repository)
                )
                MainScreen(viewModel = vm)
            }
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.ZH_CN -> LocaleListCompat.forLanguageTags("zh-CN")
            AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
