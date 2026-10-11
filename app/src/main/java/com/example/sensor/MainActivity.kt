package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private var magSensor: Sensor? = null

    // State Sensor (Mulai dengan nilai 100 agar awal buka langsung Light Mode / Terang)
    private var lightValue by mutableStateOf(100f)
    private var azimuthDegree by mutableStateOf(0f)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val compassOffset = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        setContent {
            // AMBANG BATAS DIUBAH: < 5f agar tidak gampang gelap (hanya jika benar-benar ditutup/gelap gulita)
            val isDark = lightValue < 5f

            val bgColor by animateColorAsState(
                targetValue = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF1F5F9),
                label = "bgColor"
            )
            val compassRingColor by animateColorAsState(
                targetValue = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
                label = "ringColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isDark) Color.White else Color(0xFF0F172A),
                label = "textColor"
            )
            val accentColor by animateColorAsState(
                targetValue = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                label = "accentColor"
            )

            val animatedRotation by animateFloatAsState(
                targetValue = -azimuthDegree,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 150f),
                label = "compassRotation"
            )

            val directionText = getCompassDirection(azimuthDegree)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = bgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Atas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compass & Light Hub",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFCBD5E1)
                        ) {
                            Text(
                                text = if (isDark) "🌙 Dark Mode" else "☀️ Light Mode",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }

                    // Indikator Utama Arah Kompas
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$directionText${azimuthDegree.roundToInt()}°",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    }

                    // Piringan Kompas Utama
                    Box(
                        modifier = Modifier
                            .size(290.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(compassRingColor),
                        contentAlignment = Alignment.Center
                    ) {
                        // Segitiga Penunjuk Arah Atas
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = 4.dp)
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(path, color = accentColor)
                            }
                        }

                        // Piringan yang Berputar
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .graphicsLayer(rotationZ = animatedRotation),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val radius = size.minDimension / 2f
                                val center = center

                                for (i in 0 until 360 step 10) {
                                    val angleRad = Math.toRadians(i.toDouble())
                                    val isMajor = i % 30 == 0
                                    val lineLength = if (isMajor) 15f else 8f

                                    val startX = center.x + (radius - 20f) * kotlin.math.sin(angleRad).toFloat()
                                    val startY = center.y - (radius - 20f) * kotlin.math.cos(angleRad).toFloat()
                                    val endX = center.x + (radius - 20f - lineLength) * kotlin.math.sin(angleRad).toFloat()
                                    val endY = center.y - (radius - 20f - lineLength) * kotlin.math.cos(angleRad).toFloat()

                                    drawLine(
                                        color = if (isMajor) textColor else textColor.copy(alpha = 0.4f),
                                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                                        strokeWidth = if (isMajor) 3f else 1.5f
                                    )
                                }
                            }

                            // Huruf Mata Angin
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text("N", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
                                Text("E", color = textColor, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
                                Text("S", color = textColor, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))
                                Text("W", color = textColor, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp))
                            }
                        }
                    }

                    // Informasi Nilai Sensor Bawah
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "LIGHT SENSOR", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${lightValue.toInt()} Lux", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "AZIMUTH", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${azimuthDegree.roundToInt()}°", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_LIGHT -> lightValue = it.values[0]
                Sensor.TYPE_ACCELEROMETER -> System.arraycopy(it.values, 0, gravity, 0, it.values.size)
                Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(it.values, 0, geomagnetic, 0, it.values.size)
            }

            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                var azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                azimuthInDegrees = (azimuthInDegrees + compassOffset + 360) % 360
                azimuthDegree = azimuthInDegrees
            }
        }
    }

    private fun getCompassDirection(degrees: Float): String {
        return when {
            degrees >= 337.5 || degrees < 22.5 -> "N"
            degrees >= 22.5 && degrees < 67.5 -> "NE"
            degrees >= 67.5 && degrees < 112.5 -> "E"
            degrees >= 112.5 && degrees < 157.5 -> "SE"
            degrees >= 157.5 && degrees < 202.5 -> "S"
            degrees >= 202.5 && degrees < 247.5 -> "SW"
            degrees >= 247.5 && degrees < 292.5 -> "W"
            degrees >= 292.5 && degrees < 337.5 -> "NW"
            else -> "N"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}