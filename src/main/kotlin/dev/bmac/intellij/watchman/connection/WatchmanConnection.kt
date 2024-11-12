package dev.bmac.intellij.watchman.connection

import dev.bmac.intellij.watchman.connection.model.Event
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

abstract class WatchmanConnection {
    val notificationChannel = Channel<Event>()
    protected val sendChannel = ByteChannel(true)
    private val module = SerializersModule {
        polymorphic(Event::class) {
            subclass(Event.ErrorEvent::class)
            subclass(Event.SubscribeResult::class)
            subclass(Event.WatcherEvent::class)
            subclass(Event.SubscribedEvent::class)
        }
    }
    protected val json = Json {
        ignoreUnknownKeys = true
        serializersModule = module
    }

    abstract suspend fun startup()

    suspend fun send(payload: String) {
        sendChannel.writeStringUtf8("$payload \n")
    }

    protected suspend fun onReceive(result: String) {
        try {
            json.decodeFromString<Event>(result).apply {
                notificationChannel.send(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}