package dev.bmac.intellij.watchman.connection.model

import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

object WatchmanQuery {
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
