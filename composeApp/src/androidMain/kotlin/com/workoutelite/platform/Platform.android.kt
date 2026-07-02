package com.workoutelite.platform

import java.util.UUID

actual fun uuidString(): String = UUID.randomUUID().toString()
