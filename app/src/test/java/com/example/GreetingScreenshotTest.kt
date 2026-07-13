package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.api.ShayariResponse
import com.example.ShayariSunehriCard
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
    val sampleShayari = ShayariResponse(
        shayari = "ज़िंदगी की राहों में मुस्कुराते चलो,\nहर पल को अपना बनाते चलो।",
        transliteration = "Zindagi ki raahon mein muskuraate chalo,\nHar pal ko apna banaate chalo.",
        translation = "Walk smiling through the paths of life,\nKeep making every moment your own.",
        mood = "Philosophical"
    )
    composeTestRule.setContent {
        MyApplicationTheme {
            ShayariSunehriCard(
                shayari = sampleShayari,
                isSaved = true,
                onSaveToggle = {},
                onCopy = {},
                onShare = {},
                onSpeak = {}
            )
        }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
