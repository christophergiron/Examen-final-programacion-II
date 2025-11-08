package com.example.examenfinalprogramacionii

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TaskScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enviar tarea", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Seleccionar imagen")
        }

        imageUri?.let {
            AsyncImage(
                model = it,
                contentDescription = "Vista previa",
                modifier = Modifier
                    .size(150.dp)
                    .padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val uri = imageUri
            if (uri == null) {
                uploadMessage = "Selecciona una imagen antes de enviar."
                return@Button
            }

            if (user == null) {
                uploadMessage = "Debes iniciar sesión antes de enviar la tarea."
                return@Button
            }

            uploadMessage = " Procesando imagen..."

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) {
                    uploadMessage = "Error al leer la imagen."
                    return@Button
                }

                val base64Image =
                    android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

                val taskData = hashMapOf(
                    "description" to description,
                    "imageBase64" to base64Image,
                    "userId" to user.uid,
                    "timestamp" to Timestamp.now()
                )

                firestore.collection("tasks")
                    .add(taskData)
                    .addOnSuccessListener {
                        uploadMessage = " Tarea enviada correctamente"
                        description = ""
                        imageUri = null
                    }
                    .addOnFailureListener { e ->
                        uploadMessage = "Error al guardar: ${e.message}"
                    }

            } catch (e: Exception) {
                uploadMessage = "Error: ${e.message}"
            }
        }) {
            Text("Enviar tarea")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(uploadMessage)

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = { navController.navigate("task_list") }) {
            Text(" Volver a mis tareas")
        }
    }
}