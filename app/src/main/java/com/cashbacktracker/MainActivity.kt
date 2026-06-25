package com.cashbacktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.cashbacktracker.ui.CashbackTrackerApp
import com.cashbacktracker.ui.theme.CashbackTrackerTheme
import com.cashbacktracker.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel> {
        MainViewModel.Factory((application as CashbackApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CashbackTrackerTheme {
                CashbackTrackerApp(viewModel = viewModel)
            }
        }
    }
}
