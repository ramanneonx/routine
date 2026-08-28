package com.neonroutine.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import com.neonroutine.ui.theme.ThemePreset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neonroutine.data.model.Task
import com.neonroutine.ui.camera.CameraOverlayScreen
import com.neonroutine.ui.grid.GridViewScreen
import com.neonroutine.ui.home.HomeScreen
import com.neonroutine.ui.memory.MemoryScreen
import com.neonroutine.ui.month.MonthScreen
import com.neonroutine.ui.settings.SettingsScreen
import com.neonroutine.ui.stats.StatsScreen
import com.neonroutine.ui.task.AddTaskScreen
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.timer.TimerScreen
import com.neonroutine.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val taskViewModel: TaskViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        }
    ) {
        composable("dashboard") {
            DashboardScreen(navController = navController, taskViewModel = taskViewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = taskViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddTask.route) {
            AddTaskScreen(
                onBack = { navController.popBackStack() },
                onSave = { task -> taskViewModel.addTask(task) }
            )
        }
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            var existingTask by remember { mutableStateOf<Task?>(null) }
            LaunchedEffect(taskId) {
                existingTask = taskViewModel.getTaskById(taskId)
            }
            existingTask?.let { task ->
                AddTaskScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { updatedTask -> taskViewModel.updateTask(updatedTask) },
                    existingTask = task
                )
            }
        }
        composable(
            route = Screen.Timer.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType },
                navArgument("timerIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            val timerIndex = backStackEntry.arguments?.getInt("timerIndex") ?: 0
            TimerScreen(taskId = taskId, timerIndex = timerIndex, viewModel = taskViewModel, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Camera.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            val ctx = LocalContext.current
            val today = LocalDate.now()
            val file = com.neonroutine.util.PhotoStorageUtil.createMemoryPhotoFile(ctx, taskId, today)
            
            val entries by taskViewModel.entriesInRange.collectAsState()
            val lastPhoto = remember(entries, taskId) {
                entries
                    .filter { it.taskId == taskId && !it.photoPath.isNullOrBlank() && File(it.photoPath).exists() }
                    .maxByOrNull { it.date }?.photoPath
            }

            CameraOverlayScreen(
                outputFile = file,
                lastPhotoPath = lastPhoto,
                onPhotoTaken = { absPath ->
                    taskViewModel.savePhotoToEntry(taskId, today, absPath)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, taskViewModel: TaskViewModel) {
    var selectedTabIndex by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val designStyle = LocalDesignStyle.current
    val appShapes = LocalAppShapes.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentScreen = bottomNavItems[selectedTabIndex.coerceIn(0, bottomNavItems.size - 1)]

    // Collect one-shot UI events from the ViewModel and show as snackbars
    LaunchedEffect(Unit) {
        taskViewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = false
            )
        }
    }

    val saveableStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

    // Brutal Minimal uses a top-tab style — we put it above the content
    if (designStyle == DesignStyle.BRUTAL_MINIMAL) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                // Brutal top tab row
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.outline)
                        .padding(0.dp)
                ) {
                    bottomNavItems.forEachIndexed { index, screen ->
                        val selected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 14.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            if (screen.icon != null) {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.label,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AllTabsLayout(selectedTabIndex, navController, taskViewModel, onNavigateHome = { selectedTabIndex = 0 })
                }
                // Settings icon bottom strip
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.outline)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            currentScreen.label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            }
        }
        return
    }

    // Shared background for Glassmorphism to prevent laggy multi-screen overdraw
    val bgGradient = if (designStyle == DesignStyle.GLASSMORPHISM) {
        val background = MaterialTheme.colorScheme.background
        remember {
            Brush.verticalGradient(
                colors = listOf(
                    background,
                    background.copy(alpha = 0.8f),
                    background.copy(alpha = 0.9f)
                )
            )
        }
    } else null

    // Standard scaffold for NEON_GLOW, SOFT_PASTEL and GLASSMORPHISM
    Box(modifier = Modifier.fillMaxSize().then(if (bgGradient != null) Modifier.background(bgGradient) else Modifier)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            currentScreen.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (designStyle == DesignStyle.SOFT_PASTEL) FontWeight.SemiBold else FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (designStyle == DesignStyle.NEON_GLOW)
                            MaterialTheme.colorScheme.background
                        else
                            MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = if (designStyle == DesignStyle.NEON_GLOW)
                        Modifier.clip(appShapes.bottomNav)
                    else Modifier,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = if (designStyle == DesignStyle.SOFT_PASTEL) 2.dp else 0.dp
                ) {
                    bottomNavItems.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                AllTabsLayout(selectedTabIndex, navController, taskViewModel, onNavigateHome = { selectedTabIndex = 0 })
            }
        }
    }
}

/**
 * Keeps ALL tab composables alive simultaneously for 0ms tab switching.
 * Inactive tabs are visually hidden and moved 10,000 pixels off-screen 
 * so they cannot intercept any touch events, preserving 120 FPS scrolling.
 */
@Composable
private fun AllTabsLayout(
    selectedIndex: Int,
    navController: NavController,
    taskViewModel: TaskViewModel,
    onNavigateHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        bottomNavItems.forEachIndexed { index, screen ->
            val isActive = selectedIndex == index
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { 
                        alpha = if (isActive) 1f else 0f
                        translationX = if (isActive) 0f else 10000f // Move off-screen to kill touch intercept
                    }
            ) {
                when (screen) {
                    Screen.Home -> HomeScreen(
                        viewModel = taskViewModel,
                        onAddTask = { navController.navigate(Screen.AddTask.route) },
                        onEditTask = { taskId -> navController.navigate(Screen.EditTask.createRoute(taskId)) },
                        onNavigateToTimer = { taskId, idx -> navController.navigate(Screen.Timer.createRoute(taskId, idx)) },
                        onNavigateToCamera = { taskId -> navController.navigate(Screen.Camera.createRoute(taskId)) }
                    )
                    Screen.Grid   -> GridViewScreen(viewModel = taskViewModel)
                    Screen.Month  -> MonthScreen(viewModel = taskViewModel)
                    Screen.Stats  -> StatsScreen(viewModel = taskViewModel)
                    Screen.Memory -> MemoryScreen(viewModel = taskViewModel, onBack = onNavigateHome)
                    else -> {}
                }
            }
        }
    }
}

