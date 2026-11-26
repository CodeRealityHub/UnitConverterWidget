package com.example.unitwidget.core

object RateMemory {
    @Volatile var rates: Map<String, Double> = emptyMap()
    @Volatile var updatedAt: Long = 0L
}
