package com.example.smartattendancesystem

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartattendancesystem.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private val allowedLatitude = 31.5000
//    private val allowedLongitude = 76.2800
//    private val allowedWifiSSID = "\"IIITUnaWiFi\"" // Quotes are part of SSID
//    private val locationTolerance = 100 // meters

    private val allowedLatitude = 22.6156895
    private val allowedLongitude = 75.8094764
    private val allowedWifiSSID =  "\"AirFiber-Vi5dah\"" // Quotes are part of SSID
    private val locationTolerance = 100 // meters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Redirect to login if not authenticated
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            startCamera()
        }

        binding.btnCapture.setOnClickListener {
            isValidLocationAndWifi { isValid ->
                if (isValid) {
                    takePhotoAndDetectFace()
                } else {
                    Toast.makeText(this, "❌ Not at allowed location or Wi-Fi", Toast.LENGTH_SHORT).show()
                }
            }
        }


        binding.btnViewAttendance.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }



        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndDetectFace() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val mediaImage = imageProxy.image
                    val rotation = imageProxy.imageInfo.rotationDegrees

                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, rotation)
                        detectFace(image)
                    }

                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun detectFace(image: InputImage) {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.size == 1) {
                    val face = faces[0]
                    val smileProb = face.smilingProbability ?: -1f

                    if (smileProb > 0.5) {
                        markAttendance()
                    } else {
                        Toast.makeText(this, "😐 Face detected, but not smiling.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "❌ No face or multiple faces detected", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Face detection failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markAttendance() {
        val userId = firebaseAuth.currentUser?.uid ?: "unknown"
        val email = firebaseAuth.currentUser?.email ?: "no-email"
        val timestamp = System.currentTimeMillis()

        val data = hashMapOf(
            "userId" to userId,
            "email" to email,
            "timestamp" to timestamp,
            "status" to "Present"
        )

        firestore.collection("attendance")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Attendance marked!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to mark attendance", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun isValidLocationAndWifi(onResult: (Boolean) -> Unit) {
        // Check Wi-Fi SSID
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val currentSsid = wifiManager.connectionInfo.ssid
        val isCorrectWifi = currentSsid == allowedWifiSSID

        // Get current location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 102)
            onResult(false)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val result = FloatArray(1)
                Location.distanceBetween(
                    allowedLatitude,
                    allowedLongitude,
                    location.latitude,
                    location.longitude,
                    result
                )
                val distance = result[0]
                val isNearby = distance <= locationTolerance

                onResult(isNearby && isCorrectWifi)
            } else {
                onResult(false)
            }
        }.addOnFailureListener {
            onResult(false)
        }

    }

}
