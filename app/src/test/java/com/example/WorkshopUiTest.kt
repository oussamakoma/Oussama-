package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
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
        repository = WorkshopRepository(db.transactionDao, db.personalDebtDao)
        viewModel = WorkshopViewModel(repository, com.example.data.repository.SettingsManager(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppLaunchAndInteractions() {
        composeTestRule.setContent {
            MyApplicationTheme {
                WorkshopApp(
                    viewModel = viewModel,
                    onExportBackup = {},
                    onImportBackup = {}
                )
            }
        }

        // Wait for UI to load and be idle
        composeTestRule.waitForIdle()

        // Capture initial dashboard
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/dashboard_initial.png")

        // Click Add Transaction FAB to open dialog
        composeTestRule.onNodeWithTag("add_transaction_fab").performClick()
        composeTestRule.waitForIdle()

        // Capture add dialog
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/add_dialog.png")

        // Fill dialog fields
        composeTestRule.onNodeWithTag("title_field").performTextInput("صيانة شاشة Samsung A52")
        composeTestRule.onNodeWithTag("cost_field").performTextInput("2200")
        composeTestRule.onNodeWithTag("selling_field").performTextInput("4500")
        composeTestRule.waitForIdle()

        // Click Save
        composeTestRule.onNodeWithTag("save_button").performClick()
        composeTestRule.waitForIdle()

        // Verify transaction is captured and displayed on dashboard after save
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/dashboard_after_save.png")
    }
}
