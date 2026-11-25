package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "msrs_settings")

data class AppSettings(
    val language: AppLanguage = AppLanguage.BANGLA,
    val githubRepo: String = "ferdausharfin/ms-rs-math-results",
    val githubToken: String = "",
    val githubProfileUrl: String = "https://github.com/ferdausharfin",
    val defaultInstitution: String = "",
    val defaultExamName: String = ""
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val GITHUB_PROFILE_URL = stringPreferencesKey("github_profile_url")
        val DEFAULT_INSTITUTION = stringPreferencesKey("default_institution")
        val DEFAULT_EXAM = stringPreferencesKey("default_exam")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.BANGLA.code
        val language = if (langCode == AppLanguage.ENGLISH.code) AppLanguage.ENGLISH else AppLanguage.BANGLA
        AppSettings(
            language = language,
            githubRepo = prefs[Keys.GITHUB_REPO] ?: "ferdausharfin/ms-rs-math-results",
            githubToken = prefs[Keys.GITHUB_TOKEN] ?: "",
            githubProfileUrl = prefs[Keys.GITHUB_PROFILE_URL] ?: "https://github.com/ferdausharfin",
            defaultInstitution = prefs[Keys.DEFAULT_INSTITUTION] ?: "",
            defaultExamName = prefs[Keys.DEFAULT_EXAM] ?: ""
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun updateGitHubConfig(repo: String, token: String, profileUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GITHUB_REPO] = repo.trim()
            prefs[Keys.GITHUB_TOKEN] = token.trim()
            prefs[Keys.GITHUB_PROFILE_URL] = profileUrl.trim()
        }
    }

    suspend fun updateResultDefaults(institution: String, examName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_INSTITUTION] = institution.trim()
            prefs[Keys.DEFAULT_EXAM] = examName.trim()
        }
    }
}
