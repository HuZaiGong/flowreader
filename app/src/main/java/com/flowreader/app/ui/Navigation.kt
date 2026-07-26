package com.flowreader.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowreader.app.core.designsystem.theme.FlowTheme
import com.flowreader.app.core.designsystem.token.FlowMotion
import com.flowreader.app.ui.screens.bookdetail.BookDetailScreen
import com.flowreader.app.ui.screens.library.LibraryScreen
import com.flowreader.app.ui.screens.reader.ReaderScreen
import com.flowreader.app.ui.screens.settings.SettingsScreen
import com.flowreader.app.ui.screens.stats.StatsScreen
import com.flowreader.app.ui.screens.wheel.WheelScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Library : Screen("library", "书架", Icons.AutoMirrored.Filled.LibraryBooks)
    object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)

    /** Secondary destination since v52 — the wheel is a side tool, not a reading surface. */
    object Wheel : Screen("wheel", "决策转盘")

    object BookDetail : Screen("book_detail/{bookId}", "书籍详情") {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }

    object Reader : Screen("reader/{bookId}?chapterIndex={chapterIndex}", "阅读") {
        fun createRoute(bookId: Long, chapterIndex: Int = -1) =
            if (chapterIndex >= 0) "reader/$bookId?chapterIndex=$chapterIndex" else "reader/$bookId"
    }
}

private val bottomNavItems = listOf(Screen.Library, Screen.Stats, Screen.Settings)

@Composable
fun FlowReaderNavHost(viewModel: AppShellViewModel = hiltViewModel()) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    FlowTheme(themeMode = appSettings.themeMode, colorSource = appSettings.colorSource) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                modifier = Modifier.padding(paddingValues),
                enterTransition = {
                    fadeIn(animationSpec = tween(FlowMotion.STANDARD_MS, easing = FlowMotion.standard)) +
                        slideInHorizontally(
                            animationSpec = tween(FlowMotion.STANDARD_MS, easing = FlowMotion.standard),
                            initialOffsetX = { it / 24 }
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(FlowMotion.STANDARD_MS, easing = FlowMotion.standard)) +
                        slideOutHorizontally(
                            animationSpec = tween(FlowMotion.STANDARD_MS, easing = FlowMotion.standard),
                            targetOffsetX = { -it / 24 }
                        )
                }
            ) {
                composable(Screen.Library.route) {
                    LibraryScreen(
                        onBookClick = { bookId ->
                            navController.navigate(Screen.BookDetail.createRoute(bookId))
                        },
                        onContinueReading = { bookId ->
                            navController.navigate(Screen.Reader.createRoute(bookId))
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onWheelClick = {
                            navController.navigate(Screen.Wheel.route)
                        }
                    )
                }

                composable(Screen.Stats.route) {
                    StatsScreen()
                }

                composable(Screen.Wheel.route) {
                    WheelScreen(onBackClick = { navController.popBackStack() })
                }

                composable(
                    route = Screen.BookDetail.route,
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                    BookDetailScreen(
                        bookId = bookId,
                        onBackClick = { navController.popBackStack() },
                        onReadClick = { id, chapterIndex ->
                            navController.navigate(Screen.Reader.createRoute(id, chapterIndex))
                        }
                    )
                }

                composable(
                    route = Screen.Reader.route,
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.LongType },
                        navArgument("chapterIndex") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) {
                    ReaderScreen(onBackClick = { navController.popBackStack() })
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }
}
