package com.example.examenfinalprogramacionii

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudPrestamoScreen(navController: NavController, equipoId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var fechaDevolucion by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var equipoData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Si equipoId está vacío → cargamos lista de equipos para elegir
    var equiposList by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var equipoSeleccionadoId by remember { mutableStateOf<String?>(if (equipoId.isBlank()) null else equipoId) }

    LaunchedEffect(equipoId, user, equipoSeleccionadoId) {
        isLoading = true
        if (user == null) {
            mensaje = "Debes iniciar sesión."
            isLoading = false
            return@LaunchedEffect
        }

        try {
            // cargar datos del usuario
            val usuarioDoc = firestore.collection("usuarios").document(user.uid).get().await()
            userData = usuarioDoc.data

            if (equipoSeleccionadoId != null) {
                // cargar equipo seleccionado
                val equipoDoc = firestore.collection("equipos").document(equipoSeleccionadoId!!).get().await()
                equipoData = equipoDoc.data
            } else {
                // cargar todos los equipos (lista)
                val snap = firestore.collection("equipos").get().await()
                equiposList = snap.documents.mapNotNull { d ->
                    d.data?.toMutableMap()?.apply { this["id"] = d.id }?.let { Pair(d.id, it) }
                }
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("SolicitudPrestamo", "Error al cargar datos", e)
            mensaje = "Error al cargar datos: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Solicitud de préstamo") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
        })
    }) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp)) {

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (equipoSeleccionadoId == null) {
                // Mostrar lista de equipos para elegir
                Text("Selecciona un equipo", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                if (equiposList.isEmpty()) {
                    Text("No hay equipos disponibles.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(equiposList) { pair ->
                            val id = pair.first
                            val data = pair.second
                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    equipoSeleccionadoId = id
                                }) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(text = data["nombre"] as? String ?: "(sin nombre)")
                                    Spacer(Modifier.height(6.dp))
                                    Text(text = data["descripcion"] as? String ?: "")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { navController.navigate("lista_prestamos") }) { Text("Volver") }
                if (mensaje.isNotEmpty()) Text(mensaje, color = MaterialTheme.colorScheme.error)
                return@Column
            }

            // Ahora tenemos equipoSeleccionadoId o equipoId original
            // aseguramos que equipoData esté cargado
            if (equipoData == null) {
                Text("⚠️ Error al cargar el equipo.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { equipoSeleccionadoId = null }) { Text("Elegir otro equipo") }
                return@Column
            }

            val equipoNombre = equipoData?.get("nombre") as? String ?: "(Sin nombre)"
            val base64Image = equipoData?.get("imagenBase64") as? String

            Text("Equipo: $equipoNombre", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            base64Image?.let {
                val imageBitmap = remember(it) {
                    try {
                        val clean = it.substringAfter("base64,", it)
                        val decoded = Base64.decode(clean, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
                    } catch (e: Exception) {
                        Log.e("SolicitudPrestamo", "Error decodificando imagen", e)
                        null
                    }
                }
                imageBitmap?.let { img ->
                    Image(bitmap = img, contentDescription = "Imagen equipo", modifier = Modifier.size(160.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = fechaDevolucion, onValueChange = { fechaDevolucion = it }, label = { Text("Fecha de devolución (DD/MM/AAAA)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                // Validaciones
                if (user == null) { mensaje = "Debes iniciar sesión."; return@Button }
                if (fechaDevolucion.isBlank()) { mensaje = "Ingresa fecha de devolución."; return@Button }

                val prestamoData = hashMapOf(
                    "equipoId" to equipoSeleccionadoId,
                    "equipoNombre" to equipoData?.get("nombre"),
                    "usuarioId" to user.uid,
                    "fechaSolicitud" to Timestamp.now(),
                    "fechaDevolucion" to fechaDevolucion,
                    "estado" to "pendiente",
                    "usuarioNombre" to (userData?.get("nombre") ?: user.email),
                    "usuarioCarrera" to (userData?.get("carrera") ?: ""),
                    "usuarioImagen" to (userData?.get("fotoBase64") ?: "")
                )

                firestore.collection("prestamos")
                    .add(prestamoData)
                    .addOnSuccessListener {
                        mensaje = "Solicitud enviada correctamente"
                        fechaDevolucion = ""
                        navController.navigate("lista_prestamos") { popUpTo("lista_prestamos") { inclusive = true } }
                    }
                    .addOnFailureListener { e ->
                        mensaje = "Error al enviar solicitud: ${e.message}"
                    }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Enviar solicitud")
            }

            Spacer(Modifier.height(8.dp))
            if (mensaje.isNotEmpty()) Text(mensaje, color = MaterialTheme.colorScheme.error)
        }
    }
}