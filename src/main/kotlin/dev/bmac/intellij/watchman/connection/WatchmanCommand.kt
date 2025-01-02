package dev.bmac.intellij.watchman.connection

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import dev.bmac.intellij.watchman.connection.model.Version
import dev.bmac.intellij.watchman.connection.model.WatchmanQuery
import kotlinx.serialization.json.Json

object WatchmanCommand {

    fun watchmanAvailable(): Boolean {
        try {
            ScriptRunnerUtil.getProcessOutput(
                GeneralCommandLine("watchman", "version")
            ).let {
                WatchmanQuery.json.decodeFromString<Version>(it)
                return true
            }
        } catch (e: Exception) {
            return false
        }
    }
}