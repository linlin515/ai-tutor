package com.aitutor.app.data.tool.impl

import com.aitutor.app.data.tool.engine.Tool
import com.aitutor.app.domain.model.ToolResult
import java.util.Locale
import java.util.Stack

/**
 * Calculator tool that evaluates mathematical expressions and performs unit conversions.
 * Supports +, -, *, / operators with parentheses, and basic unit conversions.
 */
class CalculatorTool : Tool {

    override val name: String = "calculator"

    override val description: String =
        "Evaluate mathematical expressions or perform unit conversions. " +
            "Supports +, -, *, /, parentheses, and unit conversion between common units."

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "expression" to mapOf(
                "type" to "string",
                "description" to "The mathematical expression to evaluate (e.g., '(23 + 45) * 12')"
            ),
            "type" to mapOf(
                "type" to "string",
                "description" to "Operation type: 'calculate' or 'unit_conversion'",
                "enum" to listOf("calculate", "unit_conversion")
            ),
            "value" to mapOf(
                "type" to "number",
                "description" to "The numeric value to convert (for unit_conversion)"
            ),
            "from" to mapOf(
                "type" to "string",
                "description" to "Source unit (for unit_conversion)"
            ),
            "to" to mapOf(
                "type" to "string",
                "description" to "Target unit (for unit_conversion)"
            )
        ),
        "required" to listOf("expression")
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val startTime = System.currentTimeMillis()
        val queryStr = args.toString()
        return try {
            val operationType = args["type"]?.toString() ?: "calculate"
            val result = when (operationType) {
                "unit_conversion" -> {
                    val value = (args["value"] as? Number)?.toDouble()
                        ?: return ToolResult(name, queryStr, "", 0, true)
                    val fromUnit = args["from"]?.toString() ?: return ToolResult(name, queryStr, "", 0, true)
                    val toUnit = args["to"]?.toString() ?: return ToolResult(name, queryStr, "", 0, true)
                    val converted = convertUnits(value, fromUnit.lowercase(Locale.ROOT), toUnit.lowercase(Locale.ROOT))
                    formatNumber(value) + " $fromUnit = " + formatNumber(converted) + " $toUnit"
                }
                else -> {
                    val expression = args["expression"]?.toString()
                        ?: return ToolResult(name, queryStr, "", 0, true)
                    val calculated = calculate(expression)
                    "$expression = ${formatNumber(calculated)}"
                }
            }
            ToolResult(name, queryStr, result, System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, queryStr, "", System.currentTimeMillis() - startTime, true)
        }
    }

    fun calculate(expression: String): Double {
        val sanitized = expression.replace(" ", "")
        if (sanitized.isEmpty()) throw IllegalArgumentException("Empty expression")
        return evaluateExpression(sanitized)
    }

    // ---- Shunting-yard expression evaluator ----
    private fun evaluateExpression(expr: String): Double {
        val operators = Stack<Char>()
        val values = Stack<Double>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i]); i++
                    }
                    values.push(sb.toString().toDouble())
                    continue
                }
                c == '(' -> operators.push(c)
                c == ')' -> {
                    while (operators.isNotEmpty() && operators.peek() != '(')
                        values.push(applyOp(operators.pop(), values.pop(), values.pop()))
                    if (operators.isNotEmpty()) operators.pop()
                }
                c in "+-*/" -> {
                    while (operators.isNotEmpty() && hasPrecedence(c, operators.peek()))
                        values.push(applyOp(operators.pop(), values.pop(), values.pop()))
                    operators.push(c)
                }
            }
            i++
        }
        while (operators.isNotEmpty())
            values.push(applyOp(operators.pop(), values.pop(), values.pop()))
        return values.pop()
    }

    private fun hasPrecedence(op1: Char, op2: Char): Boolean {
        if (op2 == '(' || op2 == ')') return false
        return !((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-'))
    }

    private fun applyOp(op: Char, b: Double, a: Double): Double = when (op) {
        '+' -> a + b; '-' -> a - b; '*' -> a * b
        '/' -> if (b == 0.0) throw ArithmeticException("Division by zero") else a / b
        else -> throw IllegalArgumentException("Unknown operator: $op")
    }

    // ---- Unit Conversion ----
    private fun convertUnits(value: Double, from: String, to: String): Double {
        val tempUnits = setOf("celsius", "c", "fahrenheit", "f", "kelvin", "k")
        if (from in tempUnits && to in tempUnits) return convertTemperature(value, from, to)

        val factors = mapOf(
            "meter" to 1.0, "meters" to 1.0, "m" to 1.0,
            "kilometer" to 1000.0, "kilometers" to 1000.0, "km" to 1000.0,
            "centimeter" to 0.01, "centimeters" to 0.01, "cm" to 0.01,
            "millimeter" to 0.001, "millimeters" to 0.001, "mm" to 0.001,
            "inch" to 0.0254, "inches" to 0.0254, "in" to 0.0254,
            "foot" to 0.3048, "feet" to 0.3048, "ft" to 0.3048,
            "yard" to 0.9144, "yards" to 0.9144, "yd" to 0.9144,
            "mile" to 1609.344, "miles" to 1609.344, "mi" to 1609.344,
            "kilogram" to 1.0, "kilograms" to 1.0, "kg" to 1.0,
            "gram" to 0.001, "grams" to 0.001, "g" to 0.001,
            "milligram" to 1.0e-6, "milligrams" to 1.0e-6, "mg" to 1.0e-6,
            "pound" to 0.453592, "pounds" to 0.453592, "lb" to 0.453592, "lbs" to 0.453592,
            "ounce" to 0.0283495, "ounces" to 0.0283495, "oz" to 0.0283495,
            "liter" to 1.0, "liters" to 1.0, "l" to 1.0,
            "milliliter" to 0.001, "milliliters" to 0.001, "ml" to 0.001,
            "gallon" to 3.78541, "gallons" to 3.78541, "gal" to 3.78541,
            "quart" to 0.946353, "quarts" to 0.946353, "qt" to 0.946353,
            "second" to 1.0, "seconds" to 1.0, "s" to 1.0,
            "minute" to 60.0, "minutes" to 60.0, "min" to 60.0,
            "hour" to 3600.0, "hours" to 3600.0, "hr" to 3600.0,
            "day" to 86400.0, "days" to 86400.0
        )
        val fromFactor = factors[from] ?: throw IllegalArgumentException("Unknown unit: $from")
        val toFactor = factors[to] ?: throw IllegalArgumentException("Unknown unit: $to")
        return value * fromFactor / toFactor
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val celsius = when {
            from.startsWith("c") -> value
            from.startsWith("f") -> (value - 32.0) * 5.0 / 9.0
            from.startsWith("k") -> value - 273.15
            else -> throw IllegalArgumentException("Unknown temp unit: $from")
        }
        return when {
            to.startsWith("c") -> celsius
            to.startsWith("f") -> celsius * 9.0 / 5.0 + 32.0
            to.startsWith("k") -> celsius + 273.15
            else -> throw IllegalArgumentException("Unknown temp unit: $to")
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.10g", value).trimEnd('0').trimEnd('.')
        }
    }
}
