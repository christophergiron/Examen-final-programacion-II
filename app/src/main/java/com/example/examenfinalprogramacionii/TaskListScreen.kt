package com.example.examenfinalprogramacionii

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
@Composable
fun TaskListScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var tasks by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        if (user != null) {
            firestore.collection("tasks")
                .whereEqualTo("userId", user.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firestore", "Error al obtener tareas", e)
                        return@addSnapshotListener
                    }

                    // Si snapshot es nulo o vacío → lista vacía
                    val newTasks = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.toMutableMap()?.apply {
                            this["id"] = doc.id
                        }
                    } ?: emptyList()

                    // Solo reemplazamos si hay cambios reales
                    if (newTasks != tasks) {
                        tasks = newTasks
                        Log.d("Firestore", "Lista actualizada: ${tasks.size} tareas")
                    }

                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("task")
            }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                "Mis Tareas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tienes tareas aún.")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = tasks,
                            key = { it["id"] as? String ?: it.hashCode().toString() }
                        ) { task ->
                            TaskItem(task)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Map<String, Any>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task["description"] as? String ?: "(Sin descripción)",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            val base64Image = task["imageBase64"] as? String
            if (!base64Image.isNullOrEmpty()) {
                val imageBitmap = remember(base64Image) {
                    try {
                        val cleanBase64 = base64Image.substringAfter("base64,", base64Image)
                        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        bitmap?.asImageBitmap()
                    } catch (e: Exception) {
                        Log.e("TaskItem", "Error decodificando imagen Base64", e)
                        null
                    }
                }

                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Imagen de tarea",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}