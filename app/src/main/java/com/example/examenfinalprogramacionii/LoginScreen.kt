package com.example.examenfinalprogramacionii

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun LoginScreen(navController: NavController) {
    val auth = Firebase.auth
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        navController.navigate("task")
                    }
                    .addOnFailureListener {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                navController.navigate("task")
                            }
                            .addOnFailureListener { e ->
                                message = e.message ?: "Error al registrar"
                            }
                    }
            }
        }) {
            Text("Entrar / Registrar")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}