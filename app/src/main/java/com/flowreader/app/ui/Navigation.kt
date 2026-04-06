package com.flowreader.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowreader.app.ui.screens.bookdetail.BookDetailScreen
import com.flowreader.app.ui.screens.library.LibraryScreen
import com.flowreader.app.ui.screens.reader.ReaderScreen
import com.flowreader.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Library : Screen("library", "书架", Icons.Default.LibraryBooks)
    object BookDetail : Screen("book_detail/{bookId}", "书籍详情") {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }
    object Reader : Screen("reader/{bookId}", "阅读") {
        fun createRoute(bookId: Long) = "reader/$bookId"
    }
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

private val bottomNavItems = listOf(Screen.Library, Screen.Settings)

@Composable
fun FlowReaderNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
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
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
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
                    onReadClick = { id ->
                        navController.navigate(Screen.Reader.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.Reader.route,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                ReaderScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
