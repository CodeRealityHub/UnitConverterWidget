package com.example.unitwidget.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.math.pow

enum class SystemMode { INDIA, WORLD }
data class UnitDef(val label: String, val factor: Double, val offset: Double = 0.0)
data class Category(val id: String, val title: String, val emoji: String,
                    val india: List<UnitDef>, val world: List<UnitDef> = india,
                    val temperature: Boolean = false)

object ConverterEngine {
    private fun u(label: String, factor: Double) = UnitDef(label, factor)
    private val lengthIndia = listOf(u("mm", .001), u("cm", .01), u("m", 1.0), u("km", 1000.0))
    private val lengthWorld = lengthIndia + listOf(u("in", .0254), u("ft", .3048), u("yd", .9144), u("mi", 1609.344))
    private val weightIndia = listOf(u("mg", .000001), u("g", .001), u("kg", 1.0), u("qtl", 100.0), u("t", 1000.0), u("tola", .0116638))
    private val weightWorld = listOf(u("g", .001), u("kg", 1.0), u("oz", .0283495), u("lb", .453592), u("st", 6.35029), u("t", 1000.0))
    private val volumeIndia = listOf(u("mL", .001), u("tsp", .005), u("tbsp", .015), u("cup", .25), u("L", 1.0), u("kL", 1000.0))
    private val volumeWorld = volumeIndia + listOf(u("fl oz", .0295735), u("gal", 3.78541))
    private val areaIndia = listOf(u("m²", 1.0), u("sq ft", .092903), u("gaj", .836127), u("guntha", 101.171), u("acre", 4046.856), u("ha", 10000.0), u("km²", 1_000_000.0))
    private val areaWorld = listOf(u("m²", 1.0), u("sq ft", .092903), u("sq yd", .836127), u("acre", 4046.856), u("ha", 10000.0), u("km²", 1_000_000.0), u("sq mi", 2_589_988.11))
    private val speedIndia = listOf(u("m/s", 1.0), u("km/h", 1/3.6))
    private val speedWorld = speedIndia + listOf(u("mph", .44704), u("kn", .514444))
    private val data = listOf(u("B", 1.0), u("KB", 1024.0), u("MB", 1024.0.pow(2)), u("GB", 1024.0.pow(3)), u("TB", 1024.0.pow(4)))
    private val temperature = listOf(u("°C", 1.0), u("°F", 1.0), u("K", 1.0))
    private val currency = listOf("USD", "EUR", "GBP", "AED", "SGD", "JPY", "AUD", "CAD").map { u(it, 1.0) }
    private val rupee = listOf(u("paise", .01), u("₹", 1.0), u("thousand", 1000.0), u("lakh", 100000.0), u("crore", 10000000.0))

    val categories = listOf(
        Category("length", "Length", "📏", lengthIndia, lengthWorld),
        Category("weight", "Weight", "⚖️", weightIndia, weightWorld),
        Category("temp", "Temperature", "🌡️", temperature, temperature, true),
        Category("volume", "Volume", "🥤", volumeIndia, volumeWorld),
        Category("area", "Area", "🏞️", areaIndia, areaWorld),
        Category("speed", "Speed", "🚗", speedIndia, speedWorld),
        Category("data", "Data", "💾", data),
        Category("currency", "Currency → INR", "💱", currency),
        Category("rupee", "Rupee", "₹", rupee)
    )
    /** Full forms of every unit label, shown in the widget under the short label. */
    private val fullNames = mapOf(
        // length
        "mm" to "Millimetre", "cm" to "Centimetre", "m" to "Metre", "km" to "Kilometre",
        "in" to "Inch", "ft" to "Foot", "yd" to "Yard", "mi" to "Mile",
        // weight
        "mg" to "Milligram", "g" to "Gram", "kg" to "Kilogram", "qtl" to "Quintal (100 kg)",
        "t" to "Tonne (1,000 kg)", "tola" to "Tola (≈ 11.66 g)", "oz" to "Ounce", "lb" to "Pound", "st" to "Stone",
        // volume
        "mL" to "Millilitre", "tsp" to "Teaspoon", "tbsp" to "Tablespoon", "cup" to "Cup",
        "L" to "Litre", "kL" to "Kilolitre", "fl oz" to "Fluid ounce", "gal" to "Gallon",
        // area
        "m²" to "Square metre", "sq ft" to "Square foot", "gaj" to "Gaj (square yard)", "guntha" to "Guntha",
        "acre" to "Acre", "ha" to "Hectare", "km²" to "Square kilometre", "sq yd" to "Square yard", "sq mi" to "Square mile",
        // speed
        "m/s" to "Metres per second", "km/h" to "Kilometres per hour", "mph" to "Miles per hour", "kn" to "Knot",
        // data
        "B" to "Byte", "KB" to "Kilobyte", "MB" to "Megabyte", "GB" to "Gigabyte", "TB" to "Terabyte",
        // temperature
        "°C" to "Degree Celsius", "°F" to "Degree Fahrenheit", "K" to "Kelvin",
        // currency
        "USD" to "US dollar", "EUR" to "Euro", "GBP" to "British pound", "AED" to "UAE dirham",
        "SGD" to "Singapore dollar", "JPY" to "Japanese yen", "AUD" to "Australian dollar",
        "CAD" to "Canadian dollar", "INR ₹" to "Indian rupee",
        // rupee
        "paise" to "Paise (1/100 rupee)", "₹" to "Indian rupee", "thousand" to "Thousand (1,000)",
        "lakh" to "Lakh (1,00,000)", "crore" to "Crore (1,00,00,000)"
    )
    fun fullName(label: String): String = fullNames[label] ?: label

    fun units(category: Category, mode: SystemMode) =
        if (mode == SystemMode.INDIA) category.india else category.world.filterNot { category.id == "rupee" }
    fun convert(value: Double, from: UnitDef, to: UnitDef, category: Category): Double {
        if (category.id == "currency") return value * currencyToInr(from.label)
        if (category.temperature) {
            val c = when (from.label) { "°F" -> (value - 32) * 5 / 9; "K" -> value - 273.15; else -> value }
            return when (to.label) { "°F" -> c * 9 / 5 + 32; "K" -> c + 273.15; else -> c }
        }
        return value * from.factor / to.factor
    }
    // Fallback values are estimates only. A successful sync replaces these values.
    private val fallbackInr = mapOf("USD" to 83.5, "EUR" to 90.5, "GBP" to 106.0, "AED" to 22.75,
        "SGD" to 62.0, "JPY" to .56, "AUD" to 54.5, "CAD" to 61.0)
    fun currencyToInr(code: String): Double = RateMemory.rates[code] ?: fallbackInr[code] ?: 1.0
    fun fallbackRates() = fallbackInr
    fun format(value: Double, india: Boolean): String {
        if (!value.isFinite()) return "—"
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,##0.####", symbols)
        val raw = df.format(value)
        if (!india) return raw
        val negative = raw.startsWith("-")
        val s = raw.removePrefix("-")
        val parts = s.split(".")
        val integer = parts[0]
        val grouped = if (integer.length <= 3) integer else {
            val last = integer.takeLast(3)
            var rest = integer.dropLast(3)
            val chunks = mutableListOf<String>()
            while (rest.length > 2) { chunks.add(0, rest.takeLast(2)); rest = rest.dropLast(2) }
            if (rest.isNotEmpty()) chunks.add(0, rest)
            chunks.joinToString(",") + "," + last
        }
        return (if (negative) "-" else "") + grouped + if (parts.size > 1) ".${parts[1]}" else ""
    }
    data class Preset(val label: String, val category: String, val value: Double, val from: String, val to: String)
    val presets = listOf(
        Preset("1 kg → g", "weight", 1.0, "kg", "g"),
        Preset("1 qtl → kg", "weight", 1.0, "qtl", "kg"),
        Preset("10 tola → g", "weight", 10.0, "tola", "g"),
        Preset("1 km → m", "length", 1.0, "km", "m"),
        Preset("1 L → mL", "volume", 1.0, "L", "mL"),
        Preset("60 km/h → m/s", "speed", 60.0, "km/h", "m/s"),
        Preset("1000 sq ft → m²", "area", 1000.0, "sq ft", "m²"),
        Preset("1 acre → ha", "area", 1.0, "acre", "ha"),
        Preset("100 USD → ₹", "currency", 100.0, "USD", "INR"),
        Preset("100 EUR → ₹", "currency", 100.0, "EUR", "INR"),
        Preset("250 paise → ₹", "rupee", 250.0, "paise", "₹"),
        Preset("1 lakh → ₹", "rupee", 1.0, "lakh", "₹")
    )
}