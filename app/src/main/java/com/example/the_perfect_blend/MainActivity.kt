package com.example.the_perfect_blend

import com.example.the_perfect_blend.utils.Cache
import android.os.Bundle
// Firebase imports
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import android.graphics.Color
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.round
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.widget.Toast
import com.example.the_perfect_blend.data.database.AppDatabase
import com.example.the_perfect_blend.data.database.ColorEntity
import androidx.room.Room


data class LabColor(val l: Double, val a: Double, val b: Double)

class MainActivity : AppCompatActivity() {

    private lateinit var targetColorView: TextView
    private lateinit var mixedColorView: TextView
    private lateinit var percentageView: TextView
    private lateinit var resetButton: Button
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var profileButton: Button
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var allColors: List<ColorEntity>
    private lateinit var paletteButtons: Map<String, Pair<Button, Button>>
    private lateinit var db: AppDatabase

    private var targetColor: String = ""
    private var targetColorName: String = ""
    private var mixedColor: String = ""
    private var targetColors: MutableList<String> = mutableListOf()
    private var targetColorIndex: Int = 0
    private var savedColors: MutableList<String> = mutableListOf()
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

        // Initialize firebase stuff
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://the-perfect-blend-b4608-default-rtdb.asia-southeast1.firebasedatabase.app")

        // Is user currently signed in?
        val currentUser = auth.currentUser
        if (currentUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        // Initialize Room database
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "color-database"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

        // Initialize UI elements
        targetColorView = findViewById(R.id.targetColorView)
        mixedColorView = findViewById(R.id.mixedColorView)
        percentageView = findViewById(R.id.percentageView)
        resetButton = findViewById(R.id.resetButton)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        profileButton = findViewById(R.id.profileButton)

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        paletteButtons = mapOf(
            "#FFFFFF" to Pair(findViewById(R.id.buttonWhiteIncrement), findViewById(R.id.buttonWhiteDecrement)),
            "#000000" to Pair(findViewById(R.id.buttonBlackIncrement), findViewById(R.id.buttonBlackDecrement)),
            "#FF0000" to Pair(findViewById(R.id.buttonRedIncrement), findViewById(R.id.buttonRedDecrement)),
            "#00FF00" to Pair(findViewById(R.id.buttonGreenIncrement), findViewById(R.id.buttonGreenDecrement)),
            "#0000FF" to Pair(findViewById(R.id.buttonBlueIncrement), findViewById(R.id.buttonBlueDecrement)),
            "#00FFFF" to Pair(findViewById(R.id.buttonCyanIncrement), findViewById(R.id.buttonCyanDecrement)),
            "#FF00FF" to Pair(findViewById(R.id.buttonMagentaIncrement), findViewById(R.id.buttonMagentaDecrement)),
            "#FFFF00" to Pair(findViewById(R.id.buttonYellowIncrement), findViewById(R.id.buttonYellowDecrement))
        )

        // Fetch colors from Firestore and initialize target colors
        fetchColorsFromFirestore {
            repeat(20) { targetColors.add(generateRandomColor()) }
            targetColor = targetColors[targetColorIndex]
            updateTargetColorView()
        }

        // Set button listeners
        resetButton.setOnClickListener { resetPaletteWeights() }
        previousButton.setOnClickListener { navigateToPreviousColor() }
        nextButton.setOnClickListener { navigateToNextColor() }

        paletteButtons.forEach { (color, buttons) ->
            buttons.first.setOnClickListener { changePaletteWeight(color, 0.1) }
            buttons.second.setOnClickListener { changePaletteWeight(color, -0.1) }
        }
    }

    private fun fetchColorsFromFirestore(onComplete: () -> Unit) {
        firestore.collection("colors").get()
            .addOnSuccessListener { documents ->
                val colorList = documents.map { document ->
                    ColorEntity(
                        hex = document.getString("hex") ?: "",
                        name = document.getString("name") ?: "",
                        seen = 0,
                        matched = 0
                    ).also { Cache.putColor(it)}
                }
                lifecycleScope.launch {
                    db.colorDao().insertAll(colorList)
                    allColors = db.colorDao().getAllColors()
                    onComplete()
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreTest", "Error", exception )
            }
    }

    private fun generateRandomColor(): String {
        val random = Random
        return rgbToHex(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }

    private fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        if (hex.isEmpty()){
            return Triple(0,0,0)
        }

        val color = hex.removePrefix("#").toInt(16)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return Triple(r, g, b)
    }

    private fun rgbToHex(r: Int, g: Int, b: Int): String {
        return String.format("#%02x%02x%02x", r, g, b)
    }

    private fun changePaletteWeight(color: String, amount: Double) {
        paletteWeights[color] = (paletteWeights[color] ?: 0.0) + amount
        if (paletteWeights[color]!! < 0) paletteWeights[color] = 0.0
        updateMixedColorAndPercentage()
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

    private fun findClosestColor(color: String): ColorEntity {
        val targetRgb = hexToRgb(color)
        var closestColor = allColors[0]
        var minDistance = Double.MAX_VALUE

        for (colorEntity in allColors) {
            val currentRgb = hexToRgb(colorEntity.hex)
            val distance = calculateDistance(targetRgb, currentRgb)
            if (distance < minDistance) {
                minDistance = distance
                closestColor = colorEntity
            }
        }
        return closestColor
    }

    private fun calculateDistance(rgb1: Triple<Int, Int, Int>, rgb2: Triple<Int, Int, Int>): Double {
        return sqrt((rgb1.first - rgb2.first).toDouble().pow(2) +
                (rgb1.second - rgb2.second).toDouble().pow(2) +
                (rgb1.third - rgb2.third).toDouble().pow(2))
    }

    private fun updateTargetColorView() {
        lifecycleScope.launch {
            val closestColor = findClosestColor(targetColor)
            targetColorName = closestColor.name
            targetColorView.text = targetColorName
            targetColorView.setBackgroundColor(Color.parseColor(targetColor))
            incrementColorSeen(closestColor)
        }
    }

    private suspend fun incrementColorSeen(colorEntity: ColorEntity) {
        if (colorEntity.seen == 0) {
            colorEntity.seen += 1
            db.colorDao().insert(colorEntity)
            val currentUser = auth.currentUser ?: return
            val userStatsRef = database.reference.child("user_stats").child(currentUser.uid)
            userStatsRef.child("colors_seen").get().addOnSuccessListener { snapshot ->
                val colorsSeen = snapshot.getValue(Int::class.java) ?: 0
                userStatsRef.child("colors_seen").setValue(colorsSeen + 1)
            }
        }
    }

    private fun updateMixedColorAndPercentage() {
        mixedColor = produceMixture(paletteWeights)
        mixedColorView.setBackgroundColor(Color.parseColor(mixedColor))
        val matchPercentage = calculatePercentage(targetColor, mixedColor)
        percentageView.text = "Match Percentage: $matchPercentage%"

        if (matchPercentage > 85) {
            saveMatchedColor(targetColor)
            incrementColorMatched()
            Toast.makeText(this, "Color Matched!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementColorMatched() {
        val currentUser = auth.currentUser ?: return
        val userStatsRef = database.reference.child("user_stats").child(currentUser.uid)
        userStatsRef.child("colors_matched").get().addOnSuccessListener { snapshot ->
            val colorsMatched = snapshot.getValue(Int::class.java) ?: 0
            userStatsRef.child("colors_matched").setValue(colorsMatched + 1)
        }
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
        return sqrt(
            (lab1.l - lab2.l).pow(2.0) +
                    (lab1.a - lab2.a).pow(2.0) +
                    (lab1.b - lab2.b).pow(2.0)
        )
    }

    private fun saveMatchedColor(color: String) {
        savedColors.add(color)
    }

    private fun resetPaletteWeights() {
        paletteWeights = mutableMapOf(
            "#FFFFFF" to 0.0,
            "#000000" to 0.0,
            "#FF0000" to 0.0,
            "#00FF00" to 0.0,
            "#0000FF" to 0.0,
            "#00FFFF" to 0.0,
            "#FF00FF" to 0.0,
            "#FFFF00" to 0.0
        )
        updateMixedColorAndPercentage()
    }

    private fun navigateToPreviousColor() {
        if (targetColorIndex > 0) {
            targetColorIndex--
            targetColor = targetColors[targetColorIndex]
            updateTargetColorView()
            resetPaletteWeights()
        }
    }

    private fun navigateToNextColor() {
        if (targetColorIndex < targetColors.size - 1) {
            targetColorIndex++
            targetColor = targetColors[targetColorIndex]
            updateTargetColorView()
            resetPaletteWeights()
        }
    }
}

