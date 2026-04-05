package com.flowreader.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowreader.app.ui.screens.auth.AuthScreen
import com.flowreader.app.ui.screens.bookdetail.BookDetailScreen
import com.flowreader.app.ui.screens.library.LibraryScreen
import com.flowreader.app.ui.screens.reader.ReaderScreen
import com.flowreader.app.ui.screens.settings.ProfileScreen
import com.flowreader.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object BookDetail : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }
    object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: Long) = "reader/$bookId"
    }
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Auth : Screen("auth")
}

@Composable
fun FlowReaderNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route
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

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = { navController.popBackStack() }
            )
        }
    }
}
    object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: Long) = "reader/$bookId"
    }
    object Settings : Screen("settings")
}

@Composable
fun FlowReaderNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route
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
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
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
