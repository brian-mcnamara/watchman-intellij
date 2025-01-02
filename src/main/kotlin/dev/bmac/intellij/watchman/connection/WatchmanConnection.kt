package dev.bmac.intellij.watchman.connection

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.openapi.Disposable
import dev.bmac.intellij.watchman.connection.model.Event
import dev.bmac.intellij.watchman.connection.model.Sockname
import dev.bmac.intellij.watchman.connection.model.WatchmanQuery
import io.ktor.utils.io.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.kotlinFunction

abstract class WatchmanConnection: Disposable {
    val notificationChannel = Channel<Event>()
    // Hack because there is a conflict with IJ ktor version
    protected val sendChannel = ByteChannel::class.primaryConstructor!!.call(true)
    val writeString = this::class.java.classLoader.loadClass("io.ktor.utils.io.ByteWriteChannelOperationsKt")
        .getMethod("writeString",
            ByteWriteChannel::class.java, String::class.java, Continuation::class.java).kotlinFunction

    abstract suspend fun startup()
    abstract fun shutdown()

    suspend fun send(payload: String) {
        writeString!!.callSuspend(sendChannel, "${payload.replace("\\", "\\\\")} \n")
    }

    protected suspend fun onReceive(result: String) {
        try {
            WatchmanQuery.json.decodeFromString<Event>(result).apply {
                notificationChannel.send(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dispose() {
//        sendChannel.close()
    }
}