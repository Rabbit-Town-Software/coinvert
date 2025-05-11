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

class MainActivity : ComponentActivity()
{
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val repository = CurrencyRepository(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            CurrencyViewModelFactory(repository)
        )[CurrencyViewModel::class.java]

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(2000) // 2 seconds splash duration
                showSplash = false
            }

            CurrencyConverterTheme {
                if (showSplash)
                {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = showSplash,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
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
