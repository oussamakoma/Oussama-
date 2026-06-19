package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          modifier = androidx.compose.ui.Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
          ) {
            androidx.compose.material3.Text(
              text = "ورشتي الصيانة الذكية 📱",
              style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
              color = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            androidx.compose.material3.Text(
              text = "لوحة التحكم بالأرباح الفورية والمباشرة",
              style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
              color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
