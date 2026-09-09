package com.altrex.mobile

import android.app.Application
import com.altrex.mobile.data.local.AppDatabase
import com.altrex.mobile.data.provider.ProviderService
import com.altrex.mobile.data.repository.ConversationRepository
import com.altrex.mobile.data.repository.ProviderRepository
import com.altrex.mobile.data.repository.SettingsRepository

class AltrexApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var providerService: ProviderService
        private set
    lateinit var providerRepository: ProviderRepository
        private set
    lateinit var conversationRepository: ConversationRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        providerService = ProviderService()
        providerRepository = ProviderRepository(database, this)
        conversationRepository = ConversationRepository(database)
        settingsRepository = SettingsRepository(database)
    }
}
