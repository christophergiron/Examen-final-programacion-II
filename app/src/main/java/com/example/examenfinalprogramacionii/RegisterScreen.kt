package com.example.examenfinalprogramacionii

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var carnet by remember { mutableStateOf("") }
    var carrera by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("estudiante") } // "estudiante" o "admin"
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Registro de usuario", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = carnet, onValueChange = { carnet = it }, label = { Text("Carnet") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = carrera, onValueChange = { carrera = it }, label = { Text("Carrera") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = rol == "estudiante", onClick = { rol = "estudiante" })
            Text("Estudiante", modifier = Modifier.padding(end = 12.dp))
            RadioButton(selected = rol == "admin", onClick = { rol = "admin" })
            Text("Administrador")
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (photoUri != null) {
                AsyncImage(model = photoUri, contentDescription = "Foto seleccionada", modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Text("No foto", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Seleccionar foto")
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            message = ""
            if (email.isBlank() || password.isBlank() || nombre.isBlank() || (rol=="estudiante" && (carnet.isBlank() || carrera.isBlank()))) {
                message = "Completa los campos requeridos"
                return@Button
            }

            loading = true

            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid == null) {
                        message = "Error: UID nulo"
                        loading = false
                        return@addOnSuccessListener
                    }

                    // Convertir foto a Base64 (si existe)
                    var photoBase64 = ""
                    try {
                        photoUri?.let { uri ->
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bytes = stream.readBytes()
                                photoBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Register", "Error convirtiendo foto", e)
                    }

                    val userData = hashMapOf(
                        "nombre" to nombre,
                        "carnet" to carnet,
                        "carrera" to carrera,
                        "rol" to rol,
                        "email" to email.trim(),
                        "fotoBase64" to photoBase64,
                        "createdAt" to Timestamp.now()
                    )

                    firestore.collection("usuarios").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            loading = false
                            message = "Registro completado"
                            // ir a la pantalla inicial dependiendo del rol
                            if (rol == "admin") {
                                navController.navigate("admin_panel") { popUpTo("register") { inclusive = true } }
                            } else {
                                navController.navigate("lista_prestamos") { popUpTo("register") { inclusive = true } }
                            }
                        }
                        .addOnFailureListener { e ->
                            loading = false
                            message = "Error al guardar usuario: ${e.message}"
                        }
                }
                .addOnFailureListener { e ->
                    loading = false
                    message = "Error en el registro: ${e.message}"
                }
        }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrando...")
            } else {
                Text("Registrar")
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { navController.navigate("login") }) { Text("¿Ya tienes cuenta? Inicia sesión") }

        Spacer(Modifier.height(8.dp))
        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
    }
}