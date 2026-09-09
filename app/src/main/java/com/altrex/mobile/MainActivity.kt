package com.altrex.mobile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altrex.mobile.ui.screens.MainScreen
import com.altrex.mobile.ui.theme.AltrexTheme
import com.altrex.mobile.viewmodel.AltrexViewModel
import com.altrex.mobile.viewmodel.AltrexViewModelFactory

class MainActivity : ComponentActivity() {

    // The native splash animation is intentionally allowed to complete its main
    // visual choreography before the real UI takes over. This is not a fake
    // splash screen or a second Activity; AndroidX still owns the launch surface.
    private companion object {
        const val SPLASH_VISUAL_MIN_MS = 1900L
    }

    private var splashExitHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be installed before super.onCreate() so Android owns the launch splash.
        val splashStartTime = SystemClock.elapsedRealtime()
        val splashScreen = installSplashScreen()

        // Let the logo complete its distinctive motion when the app starts very
        // quickly. If startup itself takes longer, this condition naturally ends
        // without adding any extra wait. Reduced-motion users are never held here.
        splashScreen.setKeepOnScreenCondition {
            ValueAnimator.areAnimatorsEnabled() &&
                SystemClock.elapsedRealtime() - splashStartTime < SPLASH_VISUAL_MIN_MS
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the real application UI underneath the native splash. It is revealed
        // during the short exit transition instead of using a second/fake splash.
        window.decorView.findViewById<View>(android.R.id.content)?.alpha = 0f

        configureSplashExit(splashScreen)

        setContent {
            AltrexTheme {
                val viewModel: AltrexViewModel =
                    viewModel(factory = AltrexViewModelFactory(application as AltrexApplication))
                val state = viewModel.uiState.collectAsState()

                MainScreen(
                    state = state.value,
                    onSendClick = { viewModel.sendMessage() },
                    onStopClick = { viewModel.cancelRequest() },
                    onNewChat = { viewModel.newConversation() },
                    onSelectConversation = { viewModel.selectConversation(it) },
                    onModeChange = { viewModel.setMode(it) },
                    onModelChange = { viewModel.setModelSelection(it) },
                    onInputChange = { viewModel.updateInput(it) },
                    onToggleSidebar = { viewModel.toggleSidebar() },
                    onShowProviderDialog = { viewModel.showProviderDialog() },
                    onShowSettingsDialog = { viewModel.showSettingsDialog() },
                    onShowCommandPalette = { viewModel.showCommandPalette() },
                    onDismissDialog = { viewModel.dismissDialog() },
                    onSuggestionClick = { viewModel.sendSuggestion(it) },
                    onConnectProvider = { input -> viewModel.connectProvider(input) },
                    onDisconnectProvider = { id -> viewModel.disconnectProvider(id) },
                    onTestProvider = { input -> viewModel.testProvider(input) },
                    onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                    onJumpToLatest = { viewModel.jumpToLatest() },
                    onMultiAiRevise = { text -> viewModel.multiAiRevise(text) },
                    onMultiAiCancel = { viewModel.multiAiCancel() },
                    onMultiAiRestart = { viewModel.multiAiRestart() },
                    onCopyMessage = { content -> viewModel.copyToClipboard(content) },
                )
            }
        }
    }

    private fun configureSplashExit(splashScreen: SplashScreen) {
        splashScreen.setOnExitAnimationListener { provider ->
            if (splashExitHandled) {
                provider.remove()
                return@setOnExitAnimationListener
            }
            splashExitHandled = true

            val splashView = provider.view
            val iconView = provider.iconView
            val contentView = window.decorView.findViewById<View>(android.R.id.content)

            // Android's animation-reduction setting is represented by disabled
            // property animators. In that mode, use a short non-motion reveal.
            if (!ValueAnimator.areAnimatorsEnabled()) {
                contentView?.alpha = 1f
                splashView.alpha = 0f
                provider.remove()
                return@setOnExitAnimationListener
            }

            val duration = 360L
            val interpolator = DecelerateInterpolator(1.6f)

            // Final logo hand-off: tiny scale-down + fade, never a second splash.
            iconView.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()

            splashView.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        provider.remove()
                    }
                })
                .start()

            // Reveal the already-created main UI at the same time.
            contentView?.animate()
                ?.alpha(1f)
                ?.setDuration(400L)
                ?.setInterpolator(interpolator)
                ?.start()
        }
    }
}
