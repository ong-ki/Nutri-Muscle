package com.nutrimuscle.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nutrimuscle.app.R
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.ui.addfood.AddFoodBottomSheet
import com.nutrimuscle.app.ui.addfood.AddFoodViewModel
import com.nutrimuscle.app.ui.components.FloatingNavBar
import com.nutrimuscle.app.ui.components.FloatingNavItem
import com.nutrimuscle.app.ui.history.HistoryScreen
import com.nutrimuscle.app.ui.profile.ProfileScreen
import com.nutrimuscle.app.ui.today.TodayScreen

sealed class BottomTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Today : BottomTab("today", R.string.nav_today, Icons.Outlined.Home)
    data object History : BottomTab("history", R.string.nav_history, Icons.Outlined.CalendarMonth)
    data object Profile : BottomTab("profile", R.string.nav_profile, Icons.Outlined.Person)
}

private val bottomTabs = listOf(
    BottomTab.Today,
    BottomTab.History,
    BottomTab.Profile
)

@Composable
fun NutriMuscleAppRoot(
    pendingReminderMealType: MealType? = null,
    onReminderConsumed: () -> Unit = {},
    addFoodViewModel: AddFoodViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var showAddFoodSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pendingReminderMealType) {
        if (pendingReminderMealType != null) {
            navController.navigate(BottomTab.Today.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            addFoodViewModel.startNewMeal(initialMealType = pendingReminderMealType)
            showAddFoodSheet = true
            onReminderConsumed()
        }
    }

    val selectedRoute = bottomTabs.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }?.route ?: BottomTab.Today.route

    val navItems = bottomTabs.map { tab ->
        FloatingNavItem(
            key = tab.route,
            icon = tab.icon,
            contentDescription = stringResource(tab.labelRes)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        NavHost(
            navController = navController,
            startDestination = BottomTab.Today.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomTab.Today.route) {
                TodayScreen(
                    onOpenAddFood = { mealType ->
                        addFoodViewModel.startNewMeal(initialMealType = mealType)
                        showAddFoodSheet = true
                    },
                    onEditMeal = { mealId ->
                        addFoodViewModel.loadMealForEditing(mealId)
                        showAddFoodSheet = true
                    }
                )
            }
            composable(BottomTab.History.route) {
                HistoryScreen(
                    onEditMeal = { mealId ->
                        addFoodViewModel.loadMealForEditing(mealId)
                        showAddFoodSheet = true
                    },
                    onOpenAddFood = { dateIso ->
                        addFoodViewModel.startNewMeal(dateIso = dateIso)
                        showAddFoodSheet = true
                    }
                )
            }
            composable(BottomTab.Profile.route) {
                ProfileScreen()
            }
        }

        FloatingNavBar(
            items = navItems,
            selected = selectedRoute,
            onSelect = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }

    if (showAddFoodSheet) {
        AddFoodBottomSheet(
            onDismiss = { showAddFoodSheet = false },
            viewModel = addFoodViewModel
        )
    }
}
