package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WorkshopDatabase
import com.example.data.repository.WorkshopRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WorkshopViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class WorkshopUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: WorkshopDatabase
    private lateinit var repository: WorkshopRepository
    private lateinit var viewModel: WorkshopViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WorkshopDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkshopRepository(
            db.transactionDao, 
            db.personalDebtDao, 
            db.installmentPaymentDao, 
            db.refurbishedDeviceDao, 
            db.maintenanceExpenseDao
        )
        viewModel = WorkshopViewModel(repository, com.example.data.repository.SettingsManager(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppLaunchAndInteractions() {
        // Pause auto-advancing clock to prevent infinite animation idling timeouts in Robolectric
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            MyApplicationTheme {
                WorkshopApp(
                    viewModel = viewModel,
                    onExportBackup = {},
                    onImportBackup = {}
                )
            }
        }

        // Advance frames deterministically for initial composition
        composeTestRule.mainClock.advanceTimeBy(500)

        // Capture initial dashboard
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/dashboard_initial.png")

        // Click Sections tab in bottom navigation bar
        composeTestRule.onNodeWithContentDescription("الأقسام").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

        // Capture sections screen
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/sections_screen.png")
    }
}
