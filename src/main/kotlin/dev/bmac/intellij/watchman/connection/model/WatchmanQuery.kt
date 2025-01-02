package dev.bmac.intellij.watchman.connection.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

object WatchmanQuery {
    private val module = SerializersModule {
        polymorphic(Event::class) {
            subclass(Event.ErrorEvent::class)
            subclass(Event.SubscribeResult::class)
            subclass(Event.WatcherEvent::class)
            subclass(Event.SubscribedEvent::class)
        }
    }
    val json = Json {
        ignoreUnknownKeys = true
        serializersModule = module
    }
    fun subscribe(path: String, subscriptionName: String): String {
        return """
            [ "subscribe", "$path", "$subscriptionName", {
                   "expression": ["true"],
                   "fields": ["name", "exists", "new"]
                }
            ]
        """.trim()
    }

    fun subscribeFile(path: String, subscriptionName: String): String {
        Path.of(path).let {
            return """
                [ "subscribe", "${it.parent.pathString}", "$subscriptionName", {
                       "expression": ["name", "${it.name}"],
                       "fields": ["name", "exists", "new"]
                    }
                ]
            """.trim()
        }

    }

    fun watch(path: String): String {
        return """
            [ "watch", "$path" ]
        """.trim()
    }

    fun unwatch(path: String): String {
        return "[ \"watch-del\", \"$path\" ]"
    }

    fun unsubscribe(path: String, subscriptionName: String): String {
        return "[ \"unsubscribe\", \"$path\", \"$subscriptionName\" ]"
    }

    private fun String.trim() = this.trimIndent().replace("\n", "")
}
