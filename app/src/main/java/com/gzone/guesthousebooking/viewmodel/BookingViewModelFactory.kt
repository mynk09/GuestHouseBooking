package com.gzone.guesthousebooking.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gzone.guesthousebooking.data.repository.BookingRepository
import com.gzone.guesthousebooking.data.repository.BookingRepositoryProvider

class BookingViewModelFactory(
    private val repository: BookingRepository
) : ViewModelProvider.Factory {
    constructor(context: Context) : this(BookingRepositoryProvider.create(context))

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
