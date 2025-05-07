package com.example.currency_converter

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.example.currency_converter.ui.theme.CurrencyConverterTheme

/**
 * MainActivity is the entry point of CoinVert app.
 * It sets up the ViewModel and loads the main UI content using Jetpack Compose.
 */
class MainActivity : ComponentActivity()
{
    /**
     * Initializes the application when the activity is created.
     * Sets up the ViewModel using a custom factory and applies the app theme.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // Initialize the repository with application context
        val repository = CurrencyRepository(applicationContext)

        // Provide the CurrencyViewModel using the factory and the repository
        val viewModel = ViewModelProvider(
            this,
            CurrencyViewModelFactory(repository))[CurrencyViewModel::class.java]

        // Set the Jetpack Compose UI content
        setContent()
        {
            CurrencyConverterTheme()
            {
                CurrencyConverterScreen(viewModel)
            }
        }
    }
}
