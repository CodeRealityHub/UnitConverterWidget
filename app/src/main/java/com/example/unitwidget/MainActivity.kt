package com.example.unitwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
// import androidx.activity.setContent
import android.widget.TextView
import android.view.Gravity
import android.graphics.Typeface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this).apply {
            text = "Unit Converter Widget\n\nAdd the widget from your home-screen widget picker.\n\nSupports India / World units, quick conversions, INR currency estimates and appearance settings."
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            typeface = Typeface.DEFAULT
        }
        setContentView(view)
    }
}
