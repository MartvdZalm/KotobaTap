package com.example.kotobatap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.kotobatap.ui.navigation.AppNavHost
import com.example.kotobatap.ui.theme.KotobaTapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotobaTapTheme {
                val navController = rememberNavController()
                AppNavHost(navController)
            }
        }
    }
}


