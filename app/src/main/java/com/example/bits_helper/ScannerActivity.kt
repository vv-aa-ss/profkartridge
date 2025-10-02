package com.example.bits_helper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import android.widget.FrameLayout
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.content.Intent
import android.view.ViewGroup
import android.widget.RelativeLayout

class ScannerActivity : ComponentActivity() {
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Создаем контейнер для камеры и оверлея
        val container = RelativeLayout(this)
        container.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        
        // Создаем PreviewView для камеры
        val preview = PreviewView(this)
        preview.layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        container.addView(preview)
        
        // Создаем оверлей с прицелом
        val overlay = ScannerOverlayView(this)
        overlay.layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        container.addView(overlay)
        
        setContentView(container)
        this.previewView = preview
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private lateinit var previewView: PreviewView

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analyzer = ImageAnalysis.Builder().build().also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
                    processImage(image)
                }
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(image: ImageProxy) {
        val mediaImage = image.image ?: return image.close()
        val rotation = image.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)
        val scanner = BarcodeScanning.getClient()
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.rawValue
                if (!value.isNullOrBlank()) {
                    val data = Intent().putExtra("qr_value", value)
                    setResult(RESULT_OK, data)
                    finish()
                }
            }
            .addOnCompleteListener { image.close() }
    }
}


