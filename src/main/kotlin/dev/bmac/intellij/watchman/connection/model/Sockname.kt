package dev.bmac.intellij.watchman.connection.model

import kotlinx.serialization.Serializable

@Serializable
data class Sockname(val sockname: String, val unix_domain: String?) {
    fun getUnixSocket() = unix_domain ?: sockname
}
