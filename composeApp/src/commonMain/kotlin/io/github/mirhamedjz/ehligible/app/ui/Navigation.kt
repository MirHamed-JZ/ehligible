package io.github.mirhamedjz.ehligible.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.mirhamedjz.ehligible.app.ui.checklist.ChecklistScreen
import io.github.mirhamedjz.ehligible.app.ui.home.HomeScreen
import io.github.mirhamedjz.ehligible.app.ui.journal.JournalScreen
import io.github.mirhamedjz.ehligible.app.ui.journal.TripEditorScreen
import io.github.mirhamedjz.ehligible.app.ui.review.ReviewScreen
import io.github.mirhamedjz.ehligible.app.ui.settings.SettingsScreen
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object TripsRoute

/** Edit an entered trip ([tripId]), annotate a detected one ([detectedDeparture], ISO date), or add one (neither). */
@Serializable
data class TripEditorRoute(val tripId: String? = null, val detectedDeparture: String? = null)

@Serializable
data object ReviewRoute

@Serializable
data object ChecklistRoute

@Serializable
data object SettingsRoute

@Composable
fun MainNavigation(viewModel: AppViewModel, initial: AppUiState.Ready) {
    val nav = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? AppUiState.Ready ?: initial
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(
        nav,
        startDestination = HomeRoute,
        enterTransition = { ForwardEnter },
        exitTransition = { ForwardExit },
        popEnterTransition = { BackEnter },
        popExitTransition = { BackExit },
        // The back gesture scrubs through the same animation as the back arrow (by default it shrinks the screen
        // into a card instead, so going back looked different depending on how you did it).
        predictivePopEnterTransition = { BackEnter },
        predictivePopExitTransition = { BackExit },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                ready = ready,
                onOpenTrips = { nav.navigate(TripsRoute) },
                onOpenReview = { nav.navigate(ReviewRoute) },
                onOpenChecklist = { nav.navigate(ChecklistRoute) },
                onOpenSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<TripsRoute> {
            JournalScreen(
                ready = ready,
                onBack = back,
                onAddTrip = { nav.navigate(TripEditorRoute()) },
                onOpenManualTrip = { id -> nav.navigate(TripEditorRoute(tripId = id)) },
                onOpenDetectedTrip = { departure ->
                    nav.navigate(TripEditorRoute(detectedDeparture = departure.toString()))
                },
            )
        }
        composable<TripEditorRoute> { entry ->
            val route = entry.toRoute<TripEditorRoute>()
            TripEditorScreen(
                viewModel = viewModel,
                ready = ready,
                tripId = route.tripId,
                detectedDeparture = route.detectedDeparture?.let(LocalDate::parse),
                onDone = back,
            )
        }
        composable<ReviewRoute> { ReviewScreen(viewModel, ready, onBack = back) }
        composable<ChecklistRoute> { ChecklistScreen(viewModel, ready, onBack = back) }
        composable<SettingsRoute> { SettingsScreen(viewModel, ready, onBack = back) }
    }
}

/**
 * Every screen change is the same short slide and fade (Material's shared X axis): forward moves left, back moves
 * right, whether by the back arrow or the back gesture. Screens slide a fifth of their width: a hint of direction,
 * not a full push.
 */
private const val NAV_MILLIS = 300

private fun <T> navSpec() = tween<T>(NAV_MILLIS, easing = FastOutSlowInEasing)

private val ForwardEnter: EnterTransition = slideInHorizontally(navSpec()) { it / 5 } + fadeIn(navSpec())
private val ForwardExit: ExitTransition = slideOutHorizontally(navSpec()) { -it / 5 } + fadeOut(navSpec())
private val BackEnter: EnterTransition = slideInHorizontally(navSpec()) { -it / 5 } + fadeIn(navSpec())
private val BackExit: ExitTransition = slideOutHorizontally(navSpec()) { it / 5 } + fadeOut(navSpec())
