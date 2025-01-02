package dev.bmac.intellij.watchman.connection.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(Event.EventSerializer::class)
sealed interface Event {
    object EventSerializer: JsonContentPolymorphicSerializer<Event>(Event::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Event> {
            val jsonObject = element.jsonObject
            return when {
                jsonObject.containsKey("error") -> ErrorEvent.serializer()
                jsonObject.containsKey("subscribe") -> SubscribedEvent.serializer()
                jsonObject.containsKey("watch") -> WatcherEvent.serializer()
                jsonObject.containsKey("subscription") -> SubscribeResult.serializer()
                jsonObject.containsKey("unsubscribe") -> UnsubscribeEvent.serializer()
                jsonObject.containsKey("watch-del") -> WatchDelEvent.serializer()
                else -> serializer()
            }
        }

    }
    @Serializable
    data class ErrorEvent(val error: String): Event {
        fun getRoot(): String? {
            if (error.startsWith(unresolveableError)) {
                return error.substring(unresolveableError.length, error.indexOf(':'))
            }
            return null
        }

        companion object {
            private const val unresolveableError = "unable to resolve root "
        }
    }

    @Serializable
    data class FileEvent(val name: String, val new: Boolean, val exists: Boolean): Event

    @Serializable
    data class SubscribedEvent(val subscribe: String, val clock: String): Event

    @Serializable
    data class SubscribeResult(val files: List<FileEvent>, val subscription: String, val root: String): Event

    @Serializable
    data class WatcherEvent(val watcher: String, val watch: String): Event

    @Serializable
    data class UnsubscribeEvent(val unsubscribe: String, val deleted: Boolean): Event

    @Serializable
    data class WatchDelEvent(val root: String, val `watch-del`: Boolean): Event
}