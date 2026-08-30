package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.feature.career.CareerScreen
import com.example.feature.game.GameScreen
import com.example.feature.film.FilmStudyScreen
import com.example.ui.theme.CrewChiefTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CrewChiefTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = "career"
        ) {
            composable(
                route = "career",
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(350))
                }
            ) {
                CareerScreen(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToGame = { navController.navigate("game") },
                    onNavigateToFilmStudy = { navController.navigate("film_study") }
                )
            }
            composable(
                route = "game",
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(400))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(350))
                }
            ) {
                GameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(
                route = "film_study",
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(350))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(350))
                }
            ) {
                FilmStudyScreen(
                    onNavigateBack = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
      }
    }
  }
}

