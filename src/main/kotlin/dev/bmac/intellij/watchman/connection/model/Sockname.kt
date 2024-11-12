package dev.bmac.intellij.watchman.connection.model

import kotlinx.serialization.Serializable

@Serializable
data class Sockname(val sockname: String)
