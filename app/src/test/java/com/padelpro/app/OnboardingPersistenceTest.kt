package com.padelpro.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.SystemTimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.domain.model.MatchFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingPersistenceTest {

    private lateinit var context: Context
    private lateinit var db: PadelProDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var playerRepo: PlayerRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PadelProDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        playerRepo = PlayerRepositoryImpl(db, settings, SystemTimeProvider())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `creating local profile persists to Room and DataStore and reset clears it`() = runTest {
        assertNull(playerRepo.observeUserProfile().first())

        val created = playerRepo.createLocalProfile(
            displayName = "Alex",
            avatarId = 5,
            preferredFormat = MatchFormat.DOUBLES
        )

        assertEquals("Alex", created.displayName)
        assertEquals(5, created.avatarId)
        assertTrue(created.isUser)

        val storedUser = playerRepo.observeUserProfile().first()
        assertNotNull(storedUser)
        assertEquals("Alex", storedUser?.displayName)

        val progress = playerRepo.observeUserProgress().first()
        assertNotNull(progress)
        assertEquals(0, progress?.totalXp)

        val appSettings = settings.settings.first()
        assertTrue(appSettings.onboardingCompleted)
        assertEquals(MatchFormat.DOUBLES, appSettings.defaultFormat)

        playerRepo.resetAllData()
        assertNull(playerRepo.observeUserProfile().first())
        assertEquals(false, settings.settings.first().onboardingCompleted)
    }
}
