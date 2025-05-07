package com.example.currency_converter

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * CurrencyViewModel manages UI state and business logic for the currency converter.
 * It coordinates currency selection, conversion logic, rate loading, and historical data tracking.
 *
 * @param repository The data source handling API requests and caching.
 */
class CurrencyViewModel(private val repository: CurrencyRepository) : ViewModel()
{
    // --- StateFlows exposed to UI ---

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies

    private val _historicalRates = MutableStateFlow<List<Pair<String, Double>>?>(null)
    val historicalRates: StateFlow<List<Pair<String, Double>>?> = _historicalRates

    private val _fromCurrency = MutableStateFlow("USD")
    val fromCurrency: StateFlow<String> = _fromCurrency

    private val _toCurrency = MutableStateFlow("EUR")
    val toCurrency: StateFlow<String> = _toCurrency

    private val _inputAmount = MutableStateFlow("")
    val inputAmount: StateFlow<String> = _inputAmount

    private val _conversionResult = MutableStateFlow("")
    val conversionResult: StateFlow<String> = _conversionResult

    private val _latestRates = MutableStateFlow<Map<String, Double>>(emptyMap())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init
    {
        // Load latest rates and populate initial state
        loadLatestRates()

        // Immediately populate with cached currency list (avoids empty dropdowns)
        _availableCurrencies.value = repository.loadCachedCurrencyList().sortedBy { it.uppercase() }

        // Optionally refresh again if needed
        loadLatestRates()
    }

    /** Sets the base currency. */
    fun setFromCurrency(currency: String)
    {
        _fromCurrency.value = currency
    }

    /** Sets the target currency. */
    fun setToCurrency(currency: String)
    {
        _toCurrency.value = currency
    }

    /** Sets the amount to convert. */
    fun setInputAmount(amount: String)
    {
        _inputAmount.value = amount
    }

    /**
     * Performs conversion using the latest fetched rate.
     * Does nothing if input or rate is invalid.
     */
    fun convert()
    {
        val amount = _inputAmount.value.toDoubleOrNull() ?: return
        val rate = _latestRates.value[toCurrency.value.lowercase()] ?: return
        val result = amount * rate
        _conversionResult.value = String.format("%.2f", result)
    }

    /**
     * Loads the latest exchange rates from the repository and updates available currencies.
     * Will include the base currency even if it's missing from the API response.
     */
    fun loadLatestRates()
    {
        viewModelScope.launch()
        {
            try
            {
                val rates = repository.getLatestRates(fromCurrency.value)
                if (rates != null)
                {
                    _latestRates.value = rates

                    // Ensure base currency is included in dropdown even if not returned by API
                    val currencyList = rates.keys.toMutableSet()
                    currencyList.add(fromCurrency.value.lowercase())
                    _availableCurrencies.value = currencyList.sortedBy { it.uppercase() }
                }
                else
                {
                    Log.e("CurrencyViewModel", "Latest rates missing or invalid.")
                }
            }
            catch (e: Exception)
            {
                Log.e("CurrencyViewModel", "Exception loading latest rates: ${e.message}")
            }
        }
    }

    /**
     * Loads historical exchange rates for the current base and target currencies over the past [daysBack] days.
     * Shows loading spinner while fetching.
     *
     * @param daysBack How many days in the past to retrieve data for.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadHistoricalRates(daysBack: Long)
    {
        viewModelScope.launch()
        {
            _isLoading.value = true
            try
            {
                val result = repository.getHistoricalRates(fromCurrency.value, toCurrency.value, daysBack.toInt())
                _historicalRates.value = result
            }
            catch (e: Exception)
            {
                Log.e("CurrencyViewModel", "Exception loading historical rates: ${e.message}")
            }
            finally
            {
                _isLoading.value = false
            }
        }
    }
}
