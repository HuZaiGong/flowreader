package com.flowreader.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import com.flowreader.app.core.designsystem.component.FlowStateHost
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression gate for issue #6 (「每个界面的顶部UI都很宽，导致内容被截断」).
 *
 * [FlowShellScaffold] nests a per-screen `Scaffold` + `TopAppBar` inside the shell's `Scaffold`.
 * Both read the same [WindowInsets], so unless the shell consumes what it spends, the status-bar
 * inset is applied twice — an extra blank band above every title, and content truncated by the
 * same doubling on the navigation bar.
 *
 * Insets are invisible to Robolectric by default (`statusBars.top` is 0), which is why neither the
 * unit suite nor the Roborazzi gate caught this in the first place. [injectSystemBars] dispatches
 * them onto the `ComposeView` directly; dispatching to the decor view alone does *not* reach
 * Compose under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class ShellWindowInsetsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tabScreenAppliesEachInsetExactlyOnce() {
        composeRule.setContent {
            MaterialTheme {
                FlowShellScaffold(
                    bottomBar = {
                        NavigationBar(modifier = Modifier.testTag(TAG_NAV_BAR)) {
                            NavigationBarItem(
                                icon = {},
                                label = { Text("tab") },
                                selected = true,
                                onClick = {}
                            )
                        }
                    }
                ) {
                    FakeScreen()
                }
            }
        }
        composeRule.injectSystemBars()

        // statusBar (24dp) + TopAppBar (64dp). The bug put it at 112dp: 24dp of status-bar inset
        // from the shell, then the whole inset again from the screen's own TopAppBar.
        composeRule.onNodeWithTag(TAG_SCREEN_CONTENT)
            .assertTopPositionInRootIsEqualTo(STATUS_BAR_DP + TOP_APP_BAR_DP)

        // The NavigationBar height already contains the navigation-bar inset, so the screen must
        // not add it a second time: its content ends exactly where the bar begins.
        val contentBottom = composeRule.onNodeWithTag(TAG_SCREEN_CONTENT).getBoundsInRoot().bottom
        val navBarTop = composeRule.onNodeWithTag(TAG_NAV_BAR).getBoundsInRoot().top
        assertEquals(navBarTop.value, contentBottom.value, 0.5f)
    }

    @Test
    fun fullBleedScreenStillSeesInsetsWhenShellHasNoBottomBar() {
        // The reader route hides the bottom bar and insets itself (ReaderControls uses
        // windowInsetsPadding). The shell must pass the insets straight through so that path
        // stays full-bleed and is applied once, not zeroed and not doubled.
        composeRule.setContent {
            MaterialTheme {
                FlowShellScaffold(bottomBar = {}) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .fillMaxSize()
                                .testTag(TAG_SCREEN_CONTENT)
                        )
                    }
                }
            }
        }
        composeRule.injectSystemBars()

        composeRule.onNodeWithTag(TAG_SCREEN_CONTENT)
            .assertTopPositionInRootIsEqualTo(STATUS_BAR_DP)
    }

    /**
     * The shape Library / Stats / BookDetail actually use: the inset padding is handed to
     * [FlowStateHost] as its `modifier` rather than applied to a `Box` directly.
     *
     * [tabScreenAppliesEachInsetExactlyOnce] passed all along while those three screens were
     * visibly broken, because [FakeScreen] applies the padding itself and never routes through
     * `FlowStateHost` — whose success branch used to drop the modifier on the floor.
     */
    @Test
    fun stateHostSuccessContentClearsTheTopBar() {
        composeRule.setContent {
            MaterialTheme {
                FlowShellScaffold(bottomBar = {}) {
                    FakeStateHostScreen(isLoading = false)
                }
            }
        }
        composeRule.injectSystemBars()

        composeRule.onNodeWithTag(TAG_SCREEN_CONTENT)
            .assertTopPositionInRootIsEqualTo(STATUS_BAR_DP + TOP_APP_BAR_DP)
    }

    /**
     * The loading state was always positioned correctly, which is why the bug looked like "content
     * is fine until the page has data". Pin both states to the same offset so a future change
     * cannot fix one and regress the other.
     */
    @Test
    fun stateHostLoadingAndSuccessShareTheSameContentTop() {
        composeRule.setContent {
            MaterialTheme {
                FlowShellScaffold(bottomBar = {}) {
                    FakeStateHostScreen(isLoading = true)
                }
            }
        }
        composeRule.injectSystemBars()

        composeRule.onNodeWithTag(TAG_LOADING_CONTENT)
            .assertTopPositionInRootIsEqualTo(STATUS_BAR_DP + TOP_APP_BAR_DP)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FakeScreen() {
        // Same shape every screen in the app uses: its own Scaffold with an M3 TopAppBar.
        Scaffold(
            topBar = { TopAppBar(title = { Text("title") }) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .testTag(TAG_SCREEN_CONTENT)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FakeStateHostScreen(isLoading: Boolean) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("title") }) }
        ) { padding ->
            FlowStateHost(
                isLoading = isLoading,
                isEmpty = false,
                error = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                loadingContent = {
                    Box(modifier = Modifier.fillMaxSize().testTag(TAG_LOADING_CONTENT))
                }
            ) {
                Box(modifier = Modifier.fillMaxSize().testTag(TAG_SCREEN_CONTENT))
            }
        }
    }

    private fun ComposeContentTestRule.injectSystemBars() {
        waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            val insets = WindowInsetsCompat.Builder()
                .setInsets(
                    WindowInsetsCompat.Type.statusBars(),
                    Insets.of(0, STATUS_BAR_PX, 0, 0)
                )
                .setInsets(
                    WindowInsetsCompat.Type.navigationBars(),
                    Insets.of(0, 0, 0, NAV_BAR_PX)
                )
                .build()
            val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            ViewCompat.dispatchApplyWindowInsets(content.getChildAt(0), insets)
        }
        waitForIdle()
    }

    private companion object {
        const val TAG_SCREEN_CONTENT = "screenContent"
        const val TAG_LOADING_CONTENT = "loadingContent"
        const val TAG_NAV_BAR = "shellNavBar"

        /** 420dpi qualifier gives density 2.625, so these land on whole dp values. */
        const val STATUS_BAR_PX = 63
        const val NAV_BAR_PX = 126
        val STATUS_BAR_DP = 24.dp
        val TOP_APP_BAR_DP = 64.dp
    }
}
