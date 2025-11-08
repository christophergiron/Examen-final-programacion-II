package com.example.examenfinalprogramacionii

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") { LoginScreen(navController) }
                    composable("register") { RegisterScreen(navController) }
                    composable("lista_prestamos") { ListaPrestamosScreen(navController) }

                    composable("prestamos/nuevo") {
                        SolicitudPrestamoScreen(
                            navController,
                            equipoId = ""
                        )
                    }

                    composable(
                        route = "prestamos/{equipoId}",
                        arguments = listOf(navArgument("equipoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val equipoId = backStackEntry.arguments?.getString("equipoId") ?: ""
                        SolicitudPrestamoScreen(navController, equipoId)
                    }

                    composable("admin_panel") { /* AdminPanelScreen(navController) */ }
                }
            }
        }
    }
}