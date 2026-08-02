package com.flowreader.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.flowreader.app.core.designsystem.component.BookCover
import com.flowreader.app.core.designsystem.component.BookShelfSkeleton
import com.flowreader.app.core.designsystem.theme.FlowTheme
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual regression gate (v56): golden images committed under
 * `app/src/test/snapshots/`; CI runs `verifyRoborazziDebug`. A new golden is
 * recorded with `./gradlew recordRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class LibraryShelfScreenshotTest {

    @Test
    fun libraryShelfLight() {
        captureRoboImage(filePath = "src/test/snapshots/library_shelf_light.png") {
            FlowTheme(themeMode = AppThemeMode.LIGHT, colorSource = ColorSource.BRAND) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(FlowSpacing.lg)) {
                            Text("继续阅读", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(FlowSpacing.md))
                            BookCover(
                                title = "三体",
                                author = "刘慈欣",
                                coverPath = null,
                                modifier = Modifier.height(140.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun librarySkeletonDark() {
        captureRoboImage(filePath = "src/test/snapshots/library_skeleton_dark.png") {
            // BookShelfSkeleton only freezes its shimmer in inspection mode; without this the
            // capture would land on a random shimmer frame and the golden would never be stable.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FlowTheme(themeMode = AppThemeMode.DARK, colorSource = ColorSource.BRAND) {
                    MaterialTheme {
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                            BookShelfSkeleton()
                        }
                    }
                }
            }
        }
    }
}
