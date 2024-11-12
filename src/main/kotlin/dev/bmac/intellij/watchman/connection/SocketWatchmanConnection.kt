package dev.bmac.intellij.watchman.connection

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import dev.bmac.intellij.watchman.WatchmanService
import dev.bmac.intellij.watchman.connection.model.Sockname
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SocketWatchmanConnection: WatchmanConnection() {
    override suspend fun startup() {
        val socketPath = ScriptRunnerUtil.getProcessOutput(
            GeneralCommandLine("watchman", "get-sockname")
        ).let {
            json.decodeFromString<Sockname>(it).sockname
        }

        val selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect(UnixSocketAddress(socketPath))

        socket.attachForWriting(sendChannel)
        WatchmanService.getService().coroutineScope.launch(Dispatchers.IO) {
            val reader = socket.openReadChannel()
            while (isActive) {
                reader.readUTF8Line()?.also { line ->
                    onReceive(line)
                }
            }
        }
    }
}