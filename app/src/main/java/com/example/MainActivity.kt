package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppMakerRepository
import com.example.ui.AppMakerContent
import com.example.ui.AppMakerViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: AppMakerRepository
    private lateinit var viewModel: AppMakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic edge-to-edge layout safe area execution
        enableEdgeToEdge()

        // Persistent database bootstrapper
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "free_app_maker_database"
        ).fallbackToDestructiveMigration() // safeguard against schema evolution crashes during development
         .build()

        repository = AppMakerRepository(database.dao())

        // Custom ViewModelFactory provider instantiation
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AppMakerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AppMakerViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        viewModel = ViewModelProvider(this, factory)[AppMakerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppMakerContent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
