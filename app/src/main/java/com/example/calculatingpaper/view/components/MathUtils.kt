package com.example.calculatingpaper.view.components

import java.math.BigDecimal
import ch.obermuhlner.math.big.BigDecimalMath
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.max

private val PI = BigDecimalMath.pi(MathContext(34, RoundingMode.HALF_UP))
private val DEGREES_TO_RADIANS = PI.divide(BigDecimal(180), MathContext(34, RoundingMode.HALF_UP))
private val RADIANS_TO_DEGREES = BigDecimal(180).divide(PI, MathContext(34, RoundingMode.HALF_UP))


fun getMathContext(desiredScale: Int): MathContext {
    val internalPrecision = max(desiredScale + 16, 34)
    return MathContext(internalPrecision, RoundingMode.HALF_UP)
}

fun toRadians(degrees: BigDecimal, mathContext: MathContext): BigDecimal {
    return degrees.multiply(DEGREES_TO_RADIANS, mathContext)
}

fun toDegrees(radians: BigDecimal, mathContext: MathContext): BigDecimal {
    return radians.multiply(RADIANS_TO_DEGREES, mathContext)
}

fun infixToPostfix(expression: String): List<String> {
    val parts = expression.split("#", limit = 2)
    val equation = parts[0].trim()

    val functions = setOf("sin", "cos", "tan", "cot", "sec", "csc",
        "arcsin", "arccos", "arctan", "log₁₀", "log₂", "ln", "√", "exp", "e^", "abs")

    val precedence = mutableMapOf<String, Int>(
        "u-" to 5,
        "+" to 1,
        "-" to 1,
        "*" to 2,
        "/" to 2,
        "%" to 2,
        "^" to 3
    )
    for (func in functions) {
        precedence[func] = 4
        precedence["u-$func"] = 4
    }

    val output = mutableListOf<String>()
    val operators = mutableListOf<String>()

    val tokenRegex = Regex("\\d+(\\.\\d+)?|log₁₀|log₂|ln|π|√|abs|e\\^|[a-zA-Z_]+\\d*|[+\\-*/%^()]")
    val tokensList = tokenRegex.findAll(equation).map { it.value }.toList()

    val processedTokens = mutableListOf<String>()
    var i = 0
    while (i < tokensList.size) {
        val token = tokensList[i]
        if (token == "-") {
            if (i == 0 || tokensList[i - 1] in setOf("+", "-", "*", "/", "%", "^", "(")) {
                if (i + 1 < tokensList.size && tokensList[i + 1] in functions) {
                    processedTokens.add("u-" + tokensList[i + 1])
                    i += 2
                    continue
                } else {
                    processedTokens.add("u-")
                    i++
                    continue
                }
            }
        }
        processedTokens.add(token)
        i++
    }

    for (token in processedTokens) {
        when {
            token.matches(Regex("\\d+(\\.\\d+)?")) ||
                    (token.matches(Regex("[a-zA-Z_]+\\d*")) && token !in precedence) ||
                    token == "π" -> output.add(token)
            token in precedence -> {
                while (operators.isNotEmpty() &&
                    operators.last() != "(" &&
                    (precedence[operators.last()] ?: 0) >= (precedence[token] ?: 0)) {
                    output.add(operators.removeAt(operators.size - 1))
                }
                operators.add(token)
            }
            token == "(" -> operators.add(token)
            token == ")" -> {
                while (operators.isNotEmpty() && operators.last() != "(") {
                    output.add(operators.removeAt(operators.size - 1))
                }
                if (operators.isNotEmpty() && operators.last() == "(") {
                    operators.removeAt(operators.size - 1)
                } else {
                    throw IllegalArgumentException("Mismatched parentheses. Please ensure all parentheses are properly closed.")
                }
            }
        }
    }

    while (operators.isNotEmpty()) {
        val op = operators.removeAt(operators.size - 1)
        if (op == "(" || op == ")") {
            throw IllegalArgumentException("Mismatched parentheses in the expression.")
        }
        output.add(op)
    }

    return output
}



fun evaluateExpression(expression: String, variables: Map<String, BigDecimal>, isDegrees: Boolean, precision: Int): BigDecimal {
    val postfixTokens = try {
        infixToPostfix(expression)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid expression: ${e.message}")
    }
    val stack = mutableListOf<BigDecimal>()
    val mathContext = getMathContext(precision)

    for (token in postfixTokens) {
        when {
            variables.containsKey(token) -> stack.add(variables[token]!!)
            token.matches(Regex("\\d+(\\.\\d+)?")) -> stack.add(BigDecimal(token))
            token == "π" -> stack.add(BigDecimalMath.pi(mathContext))
            token.startsWith("u-") && token != "u-" -> {
                if (stack.isEmpty()) throw IllegalArgumentException("Invalid expression: Missing operand for operator '$token'")
                val b = stack.removeAt(stack.size - 1)
                val func = token.substring(2)
                val intermediate = when (func) {
                    "sin" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimalMath.sin(radians, mathContext)
                    }
                    "cos" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimalMath.cos(radians, mathContext)
                    }
                    "tan" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                         BigDecimalMath.tan(radians, mathContext)
                    }
                    "cot" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimal.ONE.divide(BigDecimalMath.tan(radians, mathContext), mathContext)
                    }
                    "sec" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimal.ONE.divide(BigDecimalMath.cos(radians, mathContext), mathContext)
                    }
                    "csc" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimal.ONE.divide(BigDecimalMath.sin(radians, mathContext), mathContext)
                    }
                    "arcsin" -> {
                        val radians = BigDecimalMath.asin(b, mathContext)
                        if (isDegrees) toDegrees(radians, mathContext) else radians
                    }
                    "arccos" -> {
                        val radians = BigDecimalMath.acos(b, mathContext)
                        if (isDegrees) toDegrees(radians, mathContext) else radians
                    }
                    "arctan" -> {
                        val radians = BigDecimalMath.atan(b, mathContext)
                        if (isDegrees) toDegrees(radians, mathContext) else radians
                    }
                    "log₁₀" -> BigDecimalMath.log10(b, mathContext)
                    "log₂" -> BigDecimalMath.log2(b, mathContext)
                    "ln" -> BigDecimalMath.log(b, mathContext)
                    "√" -> BigDecimalMath.sqrt(b, mathContext)
                    "exp", "e^" -> BigDecimalMath.exp(b, mathContext)
                    "abs" -> b.abs()
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
                stack.add(intermediate.negate())
            }
            token == "u-" -> {
                if (stack.isEmpty()) throw IllegalArgumentException("Invalid expression: Missing operand for unary minus")
                val value = stack.removeAt(stack.size - 1)
                stack.add(value.negate())
            }
            else -> {
                val b = if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                else throw IllegalArgumentException("Invalid expression: Missing operand for operator '$token'")
                val result = when (token) {
                    "+", "-", "*", "/", "%" -> {
                        val a = if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                        else throw IllegalArgumentException("Invalid expression: Missing operand for operator '$token'")
                        when (token) {
                            "+" -> a.add(b)
                            "-" -> a.subtract(b)
                            "*" -> a.multiply(b)
                            "/" -> {
                                if (b.compareTo(BigDecimal.ZERO) == 0) throw IllegalArgumentException("Division by zero is not allowed.")
                                a.divide(b, mathContext)
                            }
                            "%" -> {
                                if (b.compareTo(BigDecimal.ZERO) == 0) throw IllegalArgumentException("Modulo by zero is not allowed.")
                                a.remainder(b, mathContext)
                            }
                            else -> throw IllegalArgumentException("Unknown operator: $token")
                        }
                    }
                    "abs" -> b.abs()
                    "^" -> {
                        val a = if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                        else throw IllegalArgumentException("Invalid expression: Missing operand for operator '$token'")
                        val exponent = b.toLong()
                        BigDecimalMath.pow(a, exponent, mathContext)
                    }
                    "e^" -> BigDecimalMath.exp(b, mathContext)
                    "√" -> BigDecimalMath.sqrt(b, mathContext)
                    "sin" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimalMath.sin(radians, mathContext)
                    }
                    "cos" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimalMath.cos(radians, mathContext)
                    }
                    "tan" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimalMath.tan(radians, mathContext)
                    }
                    "cot" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimal.ONE.divide(BigDecimalMath.tan(radians, mathContext), mathContext)
                    }
                    "sec" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimal.ONE.divide(BigDecimalMath.cos(radians, mathContext), mathContext)
                    }
                    "csc" -> {
                        val radians = if (isDegrees) toRadians(b, mathContext) else b
                        BigDecimal.ONE.divide(BigDecimalMath.sin(radians, mathContext), mathContext)
                    }
                    "arcsin" -> {
                        val radians = BigDecimalMath.asin(b, mathContext)
                        if (isDegrees) toDegrees(radians, mathContext) else radians
                    }
                    "arccos" -> {
                        val radians = BigDecimalMath.acos(b, mathContext)
                        if (isDegrees) toDegrees(radians, mathContext) else radians
                    }
                    "arctan" -> {
                        val radians = BigDecimalMath.atan(b, mathContext)
                        if (isDegrees) toDegrees(radians, mathContext) else radians
                    }
                    "log₁₀" -> BigDecimalMath.log10(b, mathContext)
                    "log₂" -> BigDecimalMath.log2(b, mathContext)
                    "ln" -> BigDecimalMath.log(b, mathContext)
                    "exp" -> BigDecimalMath.exp(b, mathContext)
                    else -> throw IllegalArgumentException("Unknown operator or function: $token")
                }
                stack.add(result)
            }
        }
    }
    return stack.singleOrNull() ?: throw IllegalArgumentException("Invalid expression: The expression could not be evaluated")
}

fun formatResult(value: BigDecimal, precision: Int): String {
    val roundedValue = value.setScale(precision, RoundingMode.HALF_UP)

    return if (roundedValue.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
        roundedValue.toBigInteger().toString()
    } else {
        roundedValue.toPlainString()
    }
}