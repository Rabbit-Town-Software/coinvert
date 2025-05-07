package com.example.currency_converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * CurrencyViewModelFactory is a custom ViewModelProvider.Factory implementation
 * that allows the CurrencyViewModel to be constructed with a CurrencyRepository dependency.
 *
 * @param repository The repository instance to inject into the ViewModel.
 */
class CurrencyViewModelFactory
    (private val repository: CurrencyRepository) : ViewModelProvider.Factory
{
    /**
     * Creates an instance of the given ViewModel class.
     * Throws an error if the class is not CurrencyViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T
    {
        if (modelClass.isAssignableFrom(CurrencyViewModel::class.java))
        {
            @Suppress("UNCHECKED_CAST")
            return CurrencyViewModel(repository) as T
        }

        // If we hit this, the ViewModel requested is not supported by this factory
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
