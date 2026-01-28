package com.example.mygame

class Percentage(
    var current: Double,
    var total: Double,
) {
    fun percentage(): Double {
        return (current / total) * 100.0
    }
}