package dev.bmac.intellij.watchman

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.system.OS
import dev.bmac.intellij.watchman.connection.SocketWatchmanConnection
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.APP)
class WatchmanService(val coroutineScope: CoroutineScope) {

    companion object {
        fun getService() = ApplicationManager.getApplication().getService(WatchmanService::class.java)
    }
}