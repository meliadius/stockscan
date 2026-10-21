package com.example.stockscan.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.stockscan.StockViewModel
import com.example.stockscan.data.Product
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    vm: StockViewModel,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var lookupDone by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val paused = rememberUpdatedState(scannedCode != null)

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val providerRef = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    DisposableEffect(Unit) {
        onDispose {
            providerRef[0]?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(scannedCode) {
        lookupDone = false
        scannedProduct = scannedCode?.let { vm.find(it) }
        lookupDone = true
    }
    LaunchedEffect(torchOn, camera) { camera?.cameraControl?.enableTorch(torchOn) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        providerRef[0] = provider
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    analysisExecutor,
                                    BarcodeAnalyzer(isPaused = { paused.value }) { code ->
                                        if (scannedCode == null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            scannedCode = code
                                        }
                                    }
                                )
                            }
                        provider.unbindAll()
                        camera = provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
            // Cadre de visée
            Box(
                Modifier.align(Alignment.Center).size(280.dp, 170.dp)
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp))
            )
            Text(
                "Placez le code-barres ou le QR code dans le cadre",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                    .background(Color(0x99000000), RoundedCornerShape(8.dp)).padding(8.dp)
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("L'accès à la caméra est nécessaire pour scanner.", color = Color.White)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la caméra")
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color(0x99000000), CircleShape)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White) }
            IconButton(
                onClick = { torchOn = !torchOn },
                modifier = Modifier.background(Color(0x99000000), CircleShape)
            ) {
                Icon(
                    if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    "Lampe torche", tint = Color.White
                )
            }
        }
    }

    scannedCode?.let { code ->
        if (lookupDone) {
            ScanResultDialog(
                code = code,
                product = scannedProduct,
                onAdjust = { p, delta, note -> vm.adjust(p.id, delta, note); scannedCode = null },
                onCreate = { scannedCode = null; onCreate(code) },
                onEdit = { p -> scannedCode = null; onEdit(p.id) },
                onDismiss = { scannedCode = null }
            )
        }
    }
}

@Composable
private fun ScanResultDialog(
    code: String,
    product: Product?,
    onAdjust: (Product, Int, String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    if (product == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Produit inconnu") },
            text = { Text("Aucun produit avec le code :\n$code") },
            confirmButton = { Button(onClick = onCreate) { Text("Créer la fiche") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
        )
        return
    }

    var entry by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("1") }
    val n = amount.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Code : ${product.barcode}", style = MaterialTheme.typography.bodySmall)
                Text("Stock actuel : ${product.quantity}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = entry, onClick = { entry = true }, label = { Text("Entrée (+)") })
                    FilterChip(selected = !entry, onClick = { entry = false }, label = { Text("Sortie (−)") })
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = { Text("Quantité") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextButton(onClick = { onEdit(product) }) { Text("Modifier la fiche") }
            }
        },
        confirmButton = {
            Button(
                enabled = n > 0,
                onClick = {
                    onAdjust(product, if (entry) n else -n, if (entry) "Entrée (scan)" else "Sortie (scan)")
                }
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private class BarcodeAnalyzer(
    private val isPaused: () -> Boolean,
    private val onCode: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null || isPaused()) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { list ->
                list.firstNotNullOfOrNull { it.rawValue }?.let(onCode)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
