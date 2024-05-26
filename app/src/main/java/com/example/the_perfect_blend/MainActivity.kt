package com.example.the_perfect_blend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.the_perfect_blend.ui.theme.ThePerfectBlendTheme
// Firebase imports
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

import android.graphics.Color
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.round

data class LabColor(val l: Double, val a: Double, val b: Double)

class MainActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var targetColorView: TextView
    private lateinit var mixButton: Button
    private lateinit var seekBarRed: SeekBar
    private lateinit var seekBarGreen: SeekBar
    private lateinit var seekBarBlue: SeekBar

    private var targetColor: String = ""
    private var mixedColor: String = ""
    private var paletteWeights: MutableMap<String, Double> = mutableMapOf(
        "#FFFFFF" to 0.0,
        "#000000" to 0.0,
        "#FF0000" to 0.0,
        "#00FF00" to 0.0,
        "#0000FF" to 0.0,
        "#00FFFF" to 0.0,
        "#FF00FF" to 0.0,
        "#FFFF00" to 0.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        targetColorView = findViewById(R.id.targetColorView)
        mixButton = findViewById(R.id.mixButton)
        seekBarRed = findViewById(R.id.seekBarRed)
        seekBarGreen = findViewById(R.id.seekBarGreen)
        seekBarBlue = findViewById(R.id.seekBarBlue)

        targetColor = generateRandomColor()
        targetColorView.setBackgroundColor(Color.parseColor(targetColor))

        mixButton.setOnClickListener {
            mixedColor = produceMixture(paletteWeights)
            val matchPercentage = calculatePercentage(targetColor, mixedColor)
            // Handle the result (e.g., display it to the players, update the database, etc.)
        }
    }

    private fun generateRandomColor(): String {
        val random = Random
        return rgbToHex(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }

    private fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        val color = hex.removePrefix("#").toInt(16)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return Triple(r, g, b)
    }

    private fun rgbToHex(r: Int, g: Int, b: Int): String {
        return String.format("#%02x%02x%02x", r, g, b)
    }

    private fun produceMixture(paletteWeights: Map<String, Double>): String {
        val palette = paletteWeights.keys.toList()
        val weights = paletteWeights.values.toList()
        val sumWeights = weights.sum()

        if (sumWeights == 0.0) {
            return rgbToHex(0, 0, 0)
        }

        val normWeights = weights.map { it / sumWeights }
        val paletteRGBs = palette.map { hexToRgb(it) }

        val mixedColor = Triple(
            paletteRGBs.map { it.first * normWeights[paletteRGBs.indexOf(it)] }.sum(),
            paletteRGBs.map { it.second * normWeights[paletteRGBs.indexOf(it)] }.sum(),
            paletteRGBs.map { it.third * normWeights[paletteRGBs.indexOf(it)] }.sum()
        )

        return rgbToHex(mixedColor.first.toInt(), mixedColor.second.toInt(), mixedColor.third.toInt())
    }

    private fun rgbToXyz(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        var r = r / 255.0
        var g = g / 255.0
        var b = b / 255.0

        r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else r / 12.92
        g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else g / 12.92
        b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else b / 12.92

        val x = r * 0.4124 + g * 0.3576 + b * 0.1805
        val y = r * 0.2126 + g * 0.7152 + b * 0.0722
        val z = r * 0.0193 + g * 0.1192 + b * 0.9505

        return Triple(x, y, z)
    }

    private fun xyzToLab(x: Double, y: Double, z: Double): LabColor {
        val xRef = 95.047
        val yRef = 100.0
        val zRef = 108.883

        val xtemp = x / xRef
        val ytemp = y / yRef
        val ztemp = z / zRef

        val x = if (xtemp > 0.008856) xtemp.pow(1 / 3.0) else 7.787 * xtemp + 16 / 116.0
        val y = if (ytemp > 0.008856) ytemp.pow(1 / 3.0) else 7.787 * ytemp + 16 / 116.0
        val z = if (ztemp > 0.008856) ztemp.pow(1 / 3.0) else 7.787 * ztemp + 16 / 116.0

        val l = 116 * y - 16
        val a = 500 * (x - y)
        val b = 200 * (y - z)

        return LabColor(l, a, b)
    }

    private fun deltaE(lab1: LabColor, lab2: LabColor): Double {
        return Math.sqrt(
            Math.pow(lab1.l - lab2.l, 2.0) +
                    Math.pow(lab1.a - lab2.a, 2.0) +
                    Math.pow(lab1.b - lab2.b, 2.0)
        )
    }

    private fun calculatePercentage(hex1: String, hex2: String): Double {
        val rgb1 = hexToRgb(hex1)
        val rgb2 = hexToRgb(hex2)
        val xyz1 = rgbToXyz(rgb1.first, rgb1.second, rgb1.third)
        val xyz2 = rgbToXyz(rgb2.first, rgb2.second, rgb2.third)

        val labColor1 = xyzToLab(xyz1.first, xyz1.second, xyz1.third)
        val labColor2 = xyzToLab(xyz2.first, xyz2.second, xyz2.third)

        val difference = deltaE(labColor1, labColor2)
        return round(100 * exp(-difference / 10) * 10) / 10.0
    }
}