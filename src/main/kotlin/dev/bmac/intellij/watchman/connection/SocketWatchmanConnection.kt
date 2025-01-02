package dev.bmac.intellij.watchman.connection

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.util.asSafely
import dev.bmac.intellij.watchman.WatchmanService
import dev.bmac.intellij.watchman.connection.model.Sockname
import dev.bmac.intellij.watchman.connection.model.WatchmanQuery
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

class SocketWatchmanConnection: WatchmanConnection() {

    // TODO: This lovely hack is brought to you by the major version differences between 2.x (which is bundled in IJ)
    //  And 3.x which this plugin uses due to a fix added for Windows support...
    val readUtf8Line = this::class.java.classLoader.loadClass("io.ktor.utils.io.ByteReadChannelOperationsKt")
        .getMethod("readUTF8Line", ByteReadChannel::class.java, Int::class.java, Continuation::class.java)
        .kotlinFunction

    private lateinit var selectorManager: SelectorManager

    override suspend fun startup() {
        val socketPath = ScriptRunnerUtil.getProcessOutput(
            GeneralCommandLine("watchman", "get-sockname")
        ).let {
            WatchmanQuery.json.decodeFromString<Sockname>(it).getUnixSocket()
        }

        selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect(UnixSocketAddress(socketPath))


        WatchmanService.getService().coroutineScope.launch(Dispatchers.IO) {
            socket.attachForWriting(sendChannel)
            val reader = socket.openReadChannel()
            while (isActive) {
                readUtf8Line!!.callSuspend(reader, Int.MAX_VALUE)?.also { line ->
                    onReceive(line as String)
                }
            }
        }
    }

    override fun shutdown() {
        selectorManager.close()
    }
}