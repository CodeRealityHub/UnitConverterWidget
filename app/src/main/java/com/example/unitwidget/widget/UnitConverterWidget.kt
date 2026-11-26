package com.example.unitwidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.unitwidget.R
import com.example.unitwidget.core.*

/** Colours resolved once per render. AUTO follows the system day/night setting. */
private class Theme(
    val bg: ColorProvider,
    val surface: ColorProvider,
    val fg: ColorProvider,
    val accent: ColorProvider,
    val accentTint: ColorProvider
)

class UnitConverterWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val p = currentState<Preferences>()
        val mode = if ((p[WidgetState.system] ?: "INDIA") == "WORLD") SystemMode.WORLD else SystemMode.INDIA
        val screen = p[WidgetState.screen] ?: "home"
        val cat = ConverterEngine.categories.firstOrNull { it.id == (p[WidgetState.category] ?: "length") }
            ?: ConverterEngine.categories[0]
        val units = ConverterEngine.units(cat, mode)
        val input = (p[WidgetState.input] ?: "1").toDoubleOrNull() ?: 1.0
        val index = if (units.isEmpty()) 0 else (p[WidgetState.fromIndex] ?: 0).mod(units.size)
        val from = units.getOrNull(index) ?: UnitDef("-", 1.0)

        val appearance = p[WidgetState.appearance] ?: "AUTO"
        fun pick(light: Color, dark: Color): ColorProvider = when (appearance) {
            "DARK" -> ColorProvider(day = dark, night = dark)
            "LIGHT" -> ColorProvider(day = light, night = light)
            else -> ColorProvider(day = light, night = dark)   // AUTO follows system
        }
        val accents = ThemePalette.accents
        val accentDef = accents[(p[WidgetState.accent] ?: "0").toIntOrNull()?.mod(accents.size) ?: 0]
        val t = Theme(
            bg = pick(Color(0xFFF5F7FB), Color(0xFF101722)),
            surface = pick(Color.White, Color(0xFF1B2634)),
            fg = pick(Color(0xFF172033), Color(0xFFEAF0F8)),
            accent = pick(accentDef.light, accentDef.dark),
            accentTint = pick(accentDef.light.copy(alpha = .18f), accentDef.dark.copy(alpha = .18f))
        )

        Column(modifier = GlanceModifier.fillMaxSize().background(t.bg).padding(10.dp)) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unit Converter", style = ts(t.fg, 15.sp, FontWeight.Bold))
                Spacer(GlanceModifier.defaultWeight())
                if (screen == "home" || screen == "quick") {
                    ActionButton(
                        label = if (mode == SystemMode.INDIA) "India" else "World",
                        icon = R.drawable.ic_swap, action = "system", t = t
                    )
                }
                if (screen == "home") {
                    Spacer(GlanceModifier.width(10.dp))
                    ActionButton(icon = R.drawable.ic_hint, action = "quick", t = t)
                    Spacer(GlanceModifier.width(10.dp))
                    ActionButton(icon = R.drawable.ic_palette, action = "appearance", t = t)
                } else {
                    Spacer(GlanceModifier.width(10.dp))
                    ActionButton(icon = R.drawable.ic_home, action = "home", t = t)
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            when (screen) {
                "convert" -> ConvertScreen(cat, units, from, input, mode, t)
                "quick" -> QuickScreen(mode, t)
                "appearance" -> AppearanceScreen(p, accentDef.name, t)
                else -> HomeScreen(t)
            }
        }
    }

    /**
     * All converters as a tile grid that stretches to fill the remaining widget height.
     * Rows/columns are balanced (e.g. 9 converters -> 3x3) so no row is left half empty.
     */
    @Composable
    private fun ColumnScope.HomeScreen(t: Theme) {
        val cats = ConverterEngine.categories
        val rows = ((cats.size + 3) / 4).coerceAtLeast(1)
        val cols = ((cats.size + rows - 1) / rows).coerceAtLeast(1)
        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            cats.chunked(cols).forEach { row ->
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    row.forEach { category ->
                        CardBox(
                            radius = 14.dp, fill = t.surface,
                            onClick = go("convert", "category" to category.id, "input" to "1"),
                            outer = GlanceModifier.defaultWeight().fillMaxHeight().padding(6.dp),
                            inner = GlanceModifier.fillMaxSize().padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(category.emoji, style = ts(t.fg, 28.sp))
                                Spacer(GlanceModifier.height(2.dp))
                                Text(category.title, style = ts(t.fg, 14.sp, FontWeight.Bold), maxLines = 1)
                            }
                        }
                    }
                    // keep tile widths equal on a short last row
                    repeat(cols - row.size) { Spacer(GlanceModifier.defaultWeight()) }
                }
            }
        }
    }

    /**
     * Convert screen. Everything is weight-based, so the keypad and result rows share whatever
     * height the widget has instead of overflowing it. Left: input + results. Right: keypad.
     */
    @Composable
    private fun ColumnScope.ConvertScreen(
        cat: Category, units: List<UnitDef>, from: UnitDef, input: Double,
        mode: SystemMode, t: Theme
    ) {
        val india = mode == SystemMode.INDIA
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${cat.emoji} ${cat.title}",
                style = ts(t.fg, 14.sp, FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            ActionButton(label = from.label, icon = R.drawable.ic_unfold, action = "cycle", t = t)
        }
        Spacer(GlanceModifier.height(2.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            // ---- left: input + results ----
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(t.accentTint)
                        .cornerRadius(12.dp)
                        .padding(8.dp)
                ) {
                    val fromName = ConverterEngine.fullName(from.label)
                    Text(
                        "INPUT · " + if (fromName == from.label) from.label else "$fromName (${from.label})",
                        style = ts(t.fg, 11.sp),
                        maxLines = 1
                    )
                    Text(ConverterEngine.format(input, india), style = ts(t.fg, 24.sp, FontWeight.Bold), maxLines = 1)
                    Text("Tap a result to reverse", style = ts(t.fg, 10.sp), maxLines = 1)
                }
                Spacer(GlanceModifier.height(3.dp))

                val isCurrency = cat.id == "currency"
                val inr = UnitDef("INR ₹", 1.0)
                val targets = if (isCurrency) listOf(inr) else units.filterNot { it.label == from.label }
                targets.take(4).forEach { target ->
                    val value = ConverterEngine.convert(input, from, target, cat)
                    CardBox(
                        radius = 9.dp, fill = t.surface,
                        onClick = go("convert", "category" to cat.id, "input" to ConverterEngine.format(value, india)),
                        outer = GlanceModifier.fillMaxWidth().defaultWeight().padding(vertical = 3.dp),
                        inner = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(target.label, style = ts(t.fg, 13.sp, FontWeight.Bold), maxLines = 1)
                                Text(ConverterEngine.fullName(target.label), style = ts(t.fg, 10.sp), maxLines = 1)
                            }
                            Text(
                                if (isCurrency) "₹ ${ConverterEngine.format(value, true)}" else ConverterEngine.format(value, india),
                                style = ts(t.accent, 13.sp, FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }
                if (isCurrency) {
                    Text("Rates are indicative until first successful refresh.", style = ts(t.fg, 10.sp), maxLines = 2)
                }
            }
            Spacer(GlanceModifier.width(6.dp))
            // ---- right: keypad (5 equal rows) ----
            Column(modifier = GlanceModifier.width(150.dp).fillMaxHeight()) {
                listOf(
                    listOf("7", "8", "9"),
                    listOf("4", "5", "6"),
                    listOf("1", "2", "3"),
                    listOf("±", "0", "."),
                    listOf("C", "⌫")
                ).forEach { keys ->
                    Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        keys.forEach { key ->
                            when (key) {
                                "C" -> KeyButton("C", null, "clear", t)
                                "⌫" -> KeyButton(null, R.drawable.ic_backspace, "back", t)
                                "±" -> KeyButton("±", null, "sign", t)
                                "." -> KeyButton(".", null, "dot", t)
                                else -> KeyButton(key, null, "digit:$key", t)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.QuickScreen(mode: SystemMode, t: Theme) {
        Text("Quick conversions", style = ts(t.fg, 15.sp, FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.height(2.dp))
        ConverterEngine.presets
            .filter { it.category != "rupee" || mode == SystemMode.INDIA }
            .take(10)
            .forEach { preset ->
                CardBox(
                    radius = 9.dp, fill = t.surface,
                    onClick = go("convert", "category" to preset.category, "input" to preset.value.toString()),
                    outer = GlanceModifier.fillMaxWidth().defaultWeight().padding(vertical = 3.dp),
                    inner = GlanceModifier.fillMaxSize().padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            preset.label,
                            style = ts(t.fg, 13.sp),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text("›", style = ts(t.accent, 18.sp, FontWeight.Bold))
                    }
                }
            }
    }

    @Composable
    private fun AppearanceScreen(p: Preferences, accentName: String, t: Theme) {
        Text("Appearance", style = ts(t.fg, 16.sp, FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        Text("Mode: ${p[WidgetState.appearance] ?: "AUTO"}", style = ts(t.fg, 14.sp))
        ActionButton(label = "Auto / Light / Dark", icon = R.drawable.ic_theme, action = "mode", t = t)
        Spacer(GlanceModifier.height(10.dp))
        Text("Accent: $accentName", style = ts(t.fg, 14.sp))
        ActionButton(label = "Change accent", icon = R.drawable.ic_palette, action = "accent", t = t)
        Spacer(GlanceModifier.height(6.dp))
        Text("Seven professional palettes", style = ts(t.fg, 11.sp))
    }

    /** Flat rounded card. Spacing comes from `outer` (equal margin on every side). */
    @Composable
    private fun CardBox(
        radius: Dp,
        fill: ColorProvider,
        onClick: Action,
        outer: GlanceModifier = GlanceModifier,
        inner: GlanceModifier = GlanceModifier,
        contentAlignment: Alignment = Alignment.TopStart,
        content: @Composable () -> Unit
    ) {
        Box(modifier = outer) {
            Box(
                modifier = inner.background(fill).cornerRadius(radius).clickable(onClick),
                contentAlignment = contentAlignment
            ) { content() }
        }
    }

    /** Icon and/or label button. */
    @Composable
    private fun ActionButton(
        t: Theme, action: String, label: String? = null, icon: Int? = null
    ) {
        CardBox(
            radius = 10.dp, fill = t.surface, onClick = go(action),
            outer = GlanceModifier.padding(3.dp),
            inner = GlanceModifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Image(
                        provider = ImageProvider(icon),
                        contentDescription = label ?: action,
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(t.accent)
                    )
                }
                if (icon != null && label != null) Spacer(GlanceModifier.width(5.dp))
                if (label != null) Text(label, style = ts(t.accent, 13.sp, FontWeight.Bold), maxLines = 1)
            }
        }
    }

    @Composable
    private fun RowScope.KeyButton(label: String?, icon: Int?, action: String, t: Theme) {
        CardBox(
            radius = 8.dp, fill = t.surface, onClick = go(action),
            outer = GlanceModifier.defaultWeight().fillMaxHeight().padding(3.dp),
            inner = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = action,
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(t.fg)
                )
            } else if (label != null) {
                Text(label, style = ts(t.fg, 16.sp, FontWeight.Bold), maxLines = 1)
            }
        }
    }

    private fun ts(color: ColorProvider, size: TextUnit, weight: FontWeight = FontWeight.Normal) =
        TextStyle(color = color, fontSize = size, fontWeight = weight)

    private fun go(action: String, vararg values: Pair<String, String>): Action =
        actionRunCallback<WidgetAction>(params(action, *values))

    private fun params(action: String, vararg values: Pair<String, String>): ActionParameters {
        val pairs = mutableListOf<ActionParameters.Pair<out Any>>()
        pairs += ActionParameters.Key<String>("action") to action
        values.forEach { (key, value) -> pairs += ActionParameters.Key<String>(key) to value }
        return actionParametersOf(*pairs.toTypedArray())
    }
}