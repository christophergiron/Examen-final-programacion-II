package com.example.examenfinalprogramacionii

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaPrestamosScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var prestamos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        if (user != null) {
            firestore.collection("prestamos")
                .whereEqualTo("usuarioId", user.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firestore", "Error al obtener préstamos", e)
                        errorMessage = "Error al cargar los datos."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val nuevos = snapshot.documents.mapNotNull { doc ->
                            doc.data?.toMutableMap()?.apply {
                                this["id"] = doc.id
                            }
                        }

                        prestamos = nuevos
                        Log.d("Firestore", "Lista actualizada: ${prestamos.size} préstamos")
                    } else {
                        prestamos = emptyList()
                    }

                    isLoading = false
                }
        } else {
            isLoading = false
            errorMessage = "Usuario no autenticado."
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("prestamos/nuevo") }
            ) {
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
                text = "Lista de Préstamos",
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

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(errorMessage ?: "Error desconocido")
                    }
                }

                prestamos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay préstamos registrados aún.")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = prestamos,
                            key = { it["id"] as? String ?: it.hashCode().toString() }
                        ) { prestamo ->
                            PrestamoItem(
                                prestamo = prestamo,
                                onClick = {
                                    val id = prestamo["id"] as? String ?: "sin_id"
                                    navController.navigate("prestamos/$id")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrestamoItem(
    prestamo: Map<String, Any>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = prestamo["descripcion"] as? String ?: "(Sin descripción)",
                style = MaterialTheme.typography.bodyLarge
            )

            val monto = prestamo["monto"] as? String ?: "(Monto no especificado)"
            Text(
                text = "Monto: Q$monto",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            val base64Image = prestamo["imagenBase64"] as? String
            if (!base64Image.isNullOrEmpty()) {
                val imageBitmap = remember(base64Image) {
                    try {
                        val cleanBase64 = base64Image.substringAfter("base64,", base64Image)
                        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        bitmap?.asImageBitmap()
                    } catch (e: Exception) {
                        Log.e("PrestamoItem", "Error decodificando imagen Base64", e)
                        null
                    }
                }

                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Imagen del préstamo",
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