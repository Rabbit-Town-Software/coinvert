package com.rabbittownsoftware.coinvert

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.rabbittownsoftware.coinvert.ui.theme.CurrencyConverterTheme
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * MainActivity is the entry point of the CoinVert app.
 * It initializes the CurrencyRepository, sets up the ViewModel using a factory,
 * and displays either a custom animated splash screen or the main UI depending on state.
 */
class MainActivity : ComponentActivity()
{
    /**
     * Called when the activity is first created.
     *
     * Sets up the CurrencyRepository, binds the ViewModel using ViewModelProvider,
     * and renders the UI with Jetpack Compose.
     *
     * Also displays an animated splash screen before loading the main content.
     *
     * @param savedInstanceState Optional saved state.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val repository = CurrencyRepository(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            CurrencyViewModelFactory(repository)
        )[CurrencyViewModel::class.java]

        setContent()
        {
            var splashVisible by remember { mutableStateOf(true) }
            var showMain by remember { mutableStateOf(false) }

            // Splash screen timing and animation control
            LaunchedEffect(Unit)
            {
                delay(500)   // fade-in buffer
                splashVisible = true
                delay(1000)  // splash visible duration
                splashVisible = false
                delay(500)   // fade-out duration
                showMain = true
            }

            CurrencyConverterTheme()
            {
                if (!showMain)
                {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    )
                    {
                        AnimatedVisibility(
                            visible = splashVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        )
                        {
                            Image(
                                painter = painterResource(id = R.drawable.splash_logo),
                                contentDescription = "Splash Logo",
                                modifier = Modifier.size(280.dp)
                            )
                        }
                    }
                }
                else
                {
                    CurrencyConverterScreen(viewModel)
                }
            }
        }
    }
}
