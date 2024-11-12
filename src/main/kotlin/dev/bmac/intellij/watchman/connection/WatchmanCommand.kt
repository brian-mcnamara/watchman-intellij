package dev.bmac.intellij.watchman.connection

import com.intellij.execution.CommandLineUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import dev.bmac.intellij.watchman.connection.model.Version
import kotlinx.serialization.json.Json

object WatchmanCommand {

    fun watchmanAvailable(): Boolean {
        try {
            ScriptRunnerUtil.getProcessOutput(
                GeneralCommandLine("watchman", "version")
            ).let {
                val version = Json.decodeFromString<Version>(it)
                // TODO parse the version
                return version.version.startsWith("4")
            }
        } catch (e: Exception) {
            return false
        }
    }
}