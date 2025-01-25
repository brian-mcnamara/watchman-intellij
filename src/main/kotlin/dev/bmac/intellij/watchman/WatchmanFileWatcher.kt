package dev.bmac.intellij.watchman

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.local.FileWatcherNotificationSink
import com.intellij.openapi.vfs.local.PluggableFileWatcher
import com.intellij.openapi.vfs.newvfs.ManagingFS
import com.intellij.util.SmartList
import dev.bmac.intellij.watchman.connection.SocketWatchmanConnection
import dev.bmac.intellij.watchman.connection.WatchmanCommand
import dev.bmac.intellij.watchman.connection.model.Event
import dev.bmac.intellij.watchman.connection.model.WatchmanQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.io.path.pathString
import kotlin.properties.Delegates

class WatchmanFileWatcher: PluggableFileWatcher() {
    private lateinit var notificationSink: FileWatcherNotificationSink
    private var hasWatchman by Delegates.notNull<Boolean>()

    private val watchmanConnection = SocketWatchmanConnection()
    private val watchedRoots = ArrayList<String>()
    private val rootsInProgress = AtomicInteger(0)
    @Volatile
    private var recursiveWatchedRoots: List<String> = Collections.emptyList()
    @Volatile
    private var flatWatchedRoots: List<String> = Collections.emptyList()
    @Volatile
    private var setRootsJob: Job? = null
    private val ignoredRoots = CopyOnWriteArrayList(SmartList<String>())
    private val isRunning = AtomicBoolean(false)
    private val log = Logger.getInstance(WatchmanFileWatcher::class.java)


    override fun initialize(managingFS: ManagingFS, notificationSink: FileWatcherNotificationSink) {
        hasWatchman = WatchmanCommand.watchmanAvailable()
        if (hasWatchman) {
            log.info("Using Watchman file watcher")
            //Disable the built-in filewatcher
            System.setProperty("idea.filewatcher.disabled", "true")
            this.notificationSink = notificationSink
            runBlocking {
                watchmanConnection.startup()
                isRunning.set(true)
            }

            WatchmanService.getService().coroutineScope.launch(Dispatchers.IO) {
                watchmanConnection.notificationChannel.consumeEach { event ->
                    when (event) {
                        is Event.SubscribeResult -> {
                            log.debug("Watchman notification fired")
                            for (file in event.files) {
                                if (!file.new && file.exists) {
                                    notificationSink.notifyDirtyPath(Path.of(event.root, file.name).pathString)
                                } else {
                                    notificationSink.notifyPathCreatedOrDeleted(
                                        Path.of(
                                            event.root,
                                            file.name
                                        ).pathString
                                    )
                                }
                            }
                        }

                        is Event.SubscribedEvent -> {
                            rootsInProgress.decrementAndGet()
                        }

                        is Event.ErrorEvent -> {
                            log.debug("Unable to watch path: ${event.getRoot()}")
                            event.getRoot()?.apply {
                                rootsInProgress.decrementAndGet()
                                ignoredRoots.add(this)
                                notificationSink.notifyManualWatchRoots(this@WatchmanFileWatcher, ignoredRoots)
                            }
                        }

                        else -> {
                            log.debug("Unhandled watchman event")
                        }
                    }
                }
            }
        }
    }

    override fun dispose() {
        watchmanConnection.dispose()
    }

    override fun isOperational(): Boolean {
        return hasWatchman && isRunning.get()
    }

    override fun isSettingRoots(): Boolean {
        return rootsInProgress.get() > 0
    }

    override fun setWatchRoots(recursive: MutableList<String>, flat: MutableList<String>, shuttingDown: Boolean) {
        if (!shuttingDown && isRunning.get()) {
            if (recursiveWatchedRoots == recursive && flatWatchedRoots == flat) {
                notificationSink.notifyManualWatchRoots(this, ignoredRoots)
            } else {
                setRootsJob?.cancel()
                recursiveWatchedRoots = recursive
                flatWatchedRoots = flat
                setRootsJob = WatchmanService.getService().coroutineScope.launch {
                    unregister()
                    for (path in recursive) {
                        rootsInProgress.incrementAndGet()
                        watchmanConnection.send(WatchmanQuery.watch(path))
                        watchedRoots.add(path)
                        watchmanConnection.send(WatchmanQuery.subscribe(path, (watchedRoots.size - 1).subscription()))
                    }
                    for (path in flat) {
                        rootsInProgress.incrementAndGet()
                        watchmanConnection.send(WatchmanQuery.watch(Path.of(path).parent.pathString))
                        watchedRoots.add(path)
                        watchmanConnection.send(
                            WatchmanQuery.subscribeFile(
                                path,
                                (watchedRoots.size - 1).subscription()
                            )
                        )
                    }
                }
            }
        } else {
            log.debug("Attempted to set roots when watcher not running")
        }
    }

    override fun startup() {
        "".toString()
    }

    override fun shutdown() {
        isRunning.set(false)
        runBlocking {
            unregister()
        }
        watchmanConnection.shutdown()
    }

    private suspend fun unregister() {
        watchedRoots.forEachIndexed { index, path ->
            watchmanConnection.send(WatchmanQuery.unsubscribe(path, index.subscription()))
            watchmanConnection.send(WatchmanQuery.unwatch(path))
        }
        watchedRoots.clear()
        ignoredRoots.clear()
    }

    private fun Int.subscription() = "intellij-$this"
}