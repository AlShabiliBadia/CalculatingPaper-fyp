package com.example.calculatingpaper.view.components
import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.*
import java.math.BigDecimal
private const val TAG = "MpLineGraph"
private val PI_FLOAT = PI.toFloat()
@Composable
fun MpLineGraph(
    equation: String,
    variables: Map<String, BigDecimal>,
    isDegreesMode: Boolean,
    xMinText: String,
    xMaxText: String,
    onXMinTextChange: (String) -> Unit,
    onXMaxTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var xMin by remember { mutableStateOf(xMinText.toFloatOrNull() ?: -2f * PI_FLOAT) }
    var xMax by remember { mutableStateOf(xMaxText.toFloatOrNull() ?: 2f * PI_FLOAT) }
    val numPoints = 1000
    LaunchedEffect(xMinText) {
        xMinText.toFloatOrNull()?.let { if (it < xMax) xMin = it }
    }
    LaunchedEffect(xMaxText) {
        xMaxText.toFloatOrNull()?.let { if (it > xMin) xMax = it }
    }
    val (xVariable, yVariable) = remember(equation, variables) {
        val cleanEquation = equation.trim()
        val parts = cleanEquation.split("=", limit = 2)

        var determinedXVar = "x"
        val determinedYVar: String

        if (parts.size == 2) {
            determinedYVar = parts[0].trim()
            val rightSide = parts[1].trim()

            val variableRegex = "[a-zA-Z][a-zA-Z0-9_]*".toRegex()
            val varsInEquation = variableRegex.findAll(rightSide)
                .map { it.value }
                .filter { !isFunction(it) && !isConstant(it) }
                .toSet()

            val definedVars = variables.keys

            val undefinedVars = varsInEquation - definedVars

            when (undefinedVars.size) {
                1 -> {
                    determinedXVar = undefinedVars.first()
                }
                0 -> {
                    determinedXVar = "x"
                }
                else -> {
                    Log.e(TAG, "Graphing error: Ambiguous equation with multiple undefined variables: $undefinedVars")
                    determinedXVar = ""
                }
            }
        } else {
            determinedYVar = "y"
            val variableRegex = "[a-zA-Z][a-zA-Z0-9_]*".toRegex()
            val varsInEquation = variableRegex.findAll(cleanEquation).map { it.value }.filter { !isFunction(it) && !isConstant(it) }.toSet()
            val definedVars = variables.keys
            val undefinedVars = varsInEquation - definedVars
            when (undefinedVars.size) {
                1 -> determinedXVar = undefinedVars.first()
                0 -> determinedXVar = "x"
                else -> {
                    Log.e(TAG, "Graphing error: Ambiguous equation with multiple undefined variables: $undefinedVars")
                    determinedXVar = ""
                }
            }
        }
        Pair(determinedXVar, determinedYVar)
    }
    Log.d(TAG, "Graphing: yVariable='${yVariable}', xVariable='${xVariable}' for equation='${equation}'")
    val points = produceState(initialValue = emptyList<Entry>(), equation, variables, isDegreesMode, xMin, xMax, xVariable) {
        if (xMin >= xMax) {
            Log.w(TAG, "xMin ($xMin) must be less than xMax ($xMax). Setting to empty list.")
            value = emptyList()
            return@produceState
        }
        try {
            val mathContext = getMathContext(10)
            val stepSize = BigDecimal((xMax - xMin).toString()).divide(BigDecimal(numPoints.toString()), mathContext)
            val xValues = (0..numPoints).map { i ->
                BigDecimal(xMin.toString()).add(stepSize.multiply(BigDecimal(i.toString())))
            }
            Log.d(TAG, "Generating points for equation: $equation")
            Log.d(TAG, "X variable: $xVariable")
            Log.d(TAG, "X range: $xMin to $xMax, Step size: $stepSize")
            Log.d(TAG, "Degrees mode: $isDegreesMode")
            Log.d(TAG, "Global variables: $variables")
            val expressionToEvaluate = if (equation.contains("=")) {
                equation.split("=", limit = 2).getOrElse(1) { "" }.trim()
            } else {
                equation.trim()
            }
            if (expressionToEvaluate.isBlank()){
                Log.e(TAG, "Expression to evaluate is blank. Original equation: '$equation'")
                value = emptyList()
                return@produceState
            }
            val yValues = xValues.map { currentX ->
                try {
                    val calculationVariables = mutableMapOf<String, BigDecimal>()
                    variables.forEach { (key, value) -> calculationVariables[key] = value }
                    calculationVariables[xVariable] = currentX
                    evaluateExpression(expressionToEvaluate, calculationVariables, isDegreesMode, 10)
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating y for x=$currentX in expression '$expressionToEvaluate': ${e.message}")
                    null
                }
            }
            value = xValues.zip(yValues)
                .filter { (_, y) -> y != null && y.toFloat().isFinite() }
                .map { (x, y) -> Entry(x.toFloat(), y!!.toFloat()) }
            if (value.isNotEmpty()) {
                Log.d(TAG, "Generated ${value.size} valid points")
                val samplePointsLog = value.take(5).joinToString { "(${it.x}, ${it.y})" }
                Log.d(TAG, "Sample points: $samplePointsLog")
            } else {
                Log.w(TAG, "No valid points generated for expression: $expressionToEvaluate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating points: ${e.message}")
            value = emptyList()
        }
    }.value
    val (xAxisFormatter, yAxisFormatter) = remember(points, xMin, xMax) {
        val xDataMin = points.minOfOrNull { it.x } ?: xMin
        val xDataMax = points.maxOfOrNull { it.x } ?: xMax
        val yDataMin = points.minOfOrNull { it.y } ?: -10f
        val yDataMax = points.maxOfOrNull { it.y } ?: 10f
        createAxisFormatter(xDataMax - xDataMin) to createAxisFormatter(yDataMax - yDataMin)
    }
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        factory = { context: Context ->
            LineChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(AndroidColor.WHITE)
                description.isEnabled = true
                description.text = "$yVariable vs $xVariable: $equation"
                description.textSize = 14f
                description.textColor = AndroidColor.BLACK
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                axisRight.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    textColor = AndroidColor.BLACK
                    textSize = 12f
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    textColor = AndroidColor.BLACK
                    textSize = 12f
                }
                legend.isEnabled = true
                legend.textSize = 12f
                legend.textColor = AndroidColor.BLACK
                legend.form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
            }
        },
        update = { chart ->
            chart.description.text = "$yVariable vs $xVariable: $equation"
            chart.xAxis.valueFormatter = xAxisFormatter
            chart.axisLeft.valueFormatter = yAxisFormatter
            if (points.isNotEmpty()) {
                val yDataMin = points.minOfOrNull { it.y } ?: -10f
                val yDataMax = points.maxOfOrNull { it.y } ?: 10f
                val yPadding = abs(yDataMax - yDataMin) * 0.1f
                var finalYMin = yDataMin - yPadding
                var finalYMax = yDataMax + yPadding
                if (finalYMin == finalYMax) {
                    finalYMin -= 1f
                    finalYMax += 1f
                }
                if (yDataMin == 0f && yDataMax == 0f && yPadding == 0f) {
                    finalYMin = -1f
                    finalYMax = 1f
                }
                chart.axisLeft.apply{
                    axisMinimum = finalYMin
                    axisMaximum = finalYMax
                    granularity = calculateGranularity(axisMinimum, axisMaximum)
                    setLabelCount(7, true)
                }
                chart.xAxis.apply {
                    axisMinimum = xMin
                    axisMaximum = xMax
                    granularity = calculateGranularity(axisMinimum, axisMaximum)
                    setLabelCount(7, true)
                }
                val dataSet = LineDataSet(points, equation).apply {
                    color = AndroidColor.BLUE
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                    mode = LineDataSet.Mode.LINEAR
                    axisDependency = YAxis.AxisDependency.LEFT
                }
                chart.data = LineData(dataSet)
            } else {
                chart.clear()
                chart.setNoDataText("No valid data points for equation: $equation")
                chart.setNoDataTextColor(AndroidColor.RED)
            }
            chart.invalidate()
        }
    )
}
private fun isFunction(name: String): Boolean {
    val functions = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc",
        "arcsin", "arccos", "arctan", "log₁₀", "log₂", "ln",
        "sqrt", "exp", "abs"
    )
    return functions.contains(name.lowercase())
}
private fun isConstant(name: String): Boolean {
    val constants = setOf("π", "pi", "e")
    return constants.contains(name.lowercase())
}
private fun calculateGranularity(min: Float, max: Float): Float {
    val range = abs(max - min)
    if (range == 0f) return 1f
    if (range < 1e-6f) return 0.1f * range
    val magnitude = log10(range).toInt()
    val factor = 10f.pow(magnitude)
    return when {
        range / factor < 2 -> factor / 5f
        range / factor < 5 -> factor / 2f
        else -> factor
    }.coerceAtLeast(1e-5f)
}
private fun createAxisFormatter(range: Float): ValueFormatter {
    return object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val absValue = abs(value)
            val absRange = abs(range)
            return when {
                absRange > 1000 || (absValue > 1000 && absValue != 0f) -> String.format("%.1e", value.toDouble())
                absRange < 0.01 && absValue != 0f -> String.format("%.4f", value.toDouble())
                absRange < 0.1 && absValue != 0f -> String.format("%.3f", value.toDouble())
                absRange < 1 || (absValue < 1 && absValue != 0f) -> String.format("%.2f", value.toDouble())
                else -> String.format("%.1f", value.toDouble())
            }.removeSuffix(".0")
        }
    }
}