package com.booknest.app.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_preferences")

        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        private val FIRST_TIME_LAUNCH = booleanPreferencesKey("first_time_launch")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ID] ?: ""
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    val userEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL] ?: ""
    }

    val userProfileImage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_PROFILE_IMAGE] ?: ""
    }

    val isFirstTimeLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_TIME_LAUNCH] ?: true
    }

    suspend fun saveLoginState(
        isLoggedIn: Boolean,
        userId: String = "",
        userName: String = "",
        userEmail: String = "",
        profileImage: String = ""
    ) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
            preferences[USER_ID] = userId
            preferences[USER_NAME] = userName
            preferences[USER_EMAIL] = userEmail
            preferences[USER_PROFILE_IMAGE] = profileImage
        }
    }

    suspend fun updateUserProfile(
        userName: String,
        userEmail: String,
        profileImage: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = userName
            preferences[USER_EMAIL] = userEmail
            preferences[USER_PROFILE_IMAGE] = profileImage
        }
    }

    suspend fun setFirstTimeLaunch(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_TIME_LAUNCH] = isFirstTime
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
