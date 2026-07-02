package com.workoutelite.platform

expect fun uuidString(): String

fun stableSeed(value: String): Int = value.fold(0) { hash, char ->
    hash * 31 + char.code
}
