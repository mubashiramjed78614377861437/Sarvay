package com.roadsurvey.manager.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roadsurvey.manager.R
import com.roadsurvey.manager.RoadSurveyApp
import com.roadsurvey.manager.data.Repository
import kotlinx.coroutines.launch
import kotlin.math.pow

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private lateinit var history: TextView
    private lateinit var repo: Repository
    private var current = "0"
    private var operand: Double? = null
    private var pendingOp: String? = null
    private var resetOnNext = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Calculator"

        repo = (application as RoadSurveyApp).repository
        display = findViewById(R.id.txtDisplay)
        history = findViewById(R.id.txtHistory)

        val ids = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnPlus, R.id.btnMinus, R.id.btnMul, R.id.btnDiv,
            R.id.btnEq, R.id.btnC, R.id.btnBack, R.id.btnPct
        )
        ids.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onKey((it as Button).text.toString()) }
        }

        findViewById<Button>(R.id.btnMtoKm).setOnClickListener { convert(1.0 / 1000, "m→km") }
        findViewById<Button>(R.id.btnKmtoM).setOnClickListener { convert(1000.0, "km→m") }
        findViewById<Button>(R.id.btnMtoFt).setOnClickListener { convert(3.28084, "m→ft") }
        findViewById<Button>(R.id.btnFttoM).setOnClickListener { convert(1.0 / 3.28084, "ft→m") }
        findViewById<Button>(R.id.btnSqMtoSqFt).setOnClickListener { convert(10.7639, "m²→ft²") }
        findViewById<Button>(R.id.btnSqFttoSqM).setOnClickListener { convert(1.0 / 10.7639, "ft²→m²") }
    }

    private fun onKey(key: String) {
        when (key) {
            "C" -> {
                current = "0"
                operand = null
                pendingOp = null
                resetOnNext = false
            }
            "⌫" -> {
                current = if (current.length <= 1) "0" else current.dropLast(1)
            }
            "+", "−", "×", "÷" -> {
                val v = current.toDoubleOrNull() ?: return
                if (operand != null && pendingOp != null && !resetOnNext) {
                    current = compute(operand!!, v, pendingOp!!).toDisplay()
                }
                operand = current.toDoubleOrNull()
                pendingOp = key
                resetOnNext = true
            }
            "=" -> {
                val v = current.toDoubleOrNull() ?: return
                if (operand != null && pendingOp != null) {
                    val result = compute(operand!!, v, pendingOp!!)
                    val expr = "${operand.toDisplay()} $pendingOp ${v.toDisplay()}"
                    current = result.toDisplay()
                    lifecycleScope.launch { repo.addCalc(expr, current) }
                    history.text = "$expr = $current"
                    operand = null
                    pendingOp = null
                    resetOnNext = true
                }
            }
            "%" -> {
                val v = current.toDoubleOrNull() ?: return
                current = (v / 100.0).toDisplay()
            }
            "." -> {
                if (resetOnNext) {
                    current = "0."
                    resetOnNext = false
                } else if (!current.contains(".")) {
                    current += "."
                }
            }
            else -> { // digit
                if (resetOnNext || current == "0") {
                    current = key
                    resetOnNext = false
                } else {
                    current += key
                }
            }
        }
        display.text = current
    }

    private fun convert(factor: Double, label: String) {
        val v = current.toDoubleOrNull() ?: return
        val result = v * factor
        lifecycleScope.launch { repo.addCalc("$v $label", result.toDisplay()) }
        history.text = "$v $label = ${result.toDisplay()}"
        current = result.toDisplay()
        display.text = current
        resetOnNext = true
    }

    private fun compute(a: Double, b: Double, op: String): Double = when (op) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b == 0.0) Double.NaN else a / b
        else -> b
    }

    private fun Double.toDisplay(): String {
        if (isNaN()) return "Error"
        return if (this == toLong().toDouble()) toLong().toString()
        else String.format("%.6f", this).trimEnd('0').trimEnd('.')
    }

    private fun Double?.toDisplay() = this?.toDisplay() ?: "0"

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
