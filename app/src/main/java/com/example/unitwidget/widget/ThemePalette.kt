package com.example.unitwidget.widget

import androidx.compose.ui.graphics.Color

enum class Appearance { AUTO, LIGHT, DARK }
data class Palette(val name: String, val light: Color, val dark: Color)
object ThemePalette {
    val accents = listOf(
        Palette("Ocean", Color(0xFF1769AA), Color(0xFF72B7F2)),
        Palette("Teal", Color(0xFF087E78), Color(0xFF5AD6C8)),
        Palette("Indigo", Color(0xFF5146B8), Color(0xFFAAA1FF)),
        Palette("Emerald", Color(0xFF16784A), Color(0xFF66D69B)),
        Palette("Saffron", Color(0xFFB76B08), Color(0xFFFFC36B)),
        Palette("Rose", Color(0xFFB83F69), Color(0xFFFF8FB0)),
        Palette("Slate", Color(0xFF475569), Color(0xFFB5C2D4))
    )
}
