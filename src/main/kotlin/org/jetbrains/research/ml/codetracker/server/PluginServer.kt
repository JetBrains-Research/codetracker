package org.jetbrains.research.ml.codetracker.server

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.messages.Topic
import org.jetbrains.research.ml.codetracker.Plugin
import org.jetbrains.research.ml.codetracker.models.*
import java.util.function.Consumer
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.Task.Backgroundable

enum class ServerConnectionResult {
    UNINITIALIZED,
    LOADING,
    SUCCESS,
    FAIL
}

interface ServerConnectionNotifier : Consumer<ServerConnectionResult> {
    companion object {
        val SERVER_CONNECTION_TOPIC = Topic.create("server connection result", ServerConnectionNotifier::class.java)
    }
}

object PluginServer {

    var paneText: PaneText? = null
        private set
    var availableLanguages: List<PaneLanguage> = emptyList()
        private set
    var tasks: List<Task> = emptyList()
        private set
    var genders: List<Gender> = emptyList()
        private set
    var countries: List<Country> = emptyList()
        private set
    var serverConnectionResult: ServerConnectionResult = ServerConnectionResult.UNINITIALIZED
        private set

    private val logger: Logger = Logger.getInstance(javaClass)


    /**
     * Finds all data in background task and sends results about finding
     */
    fun reconnect(project: Project) {
        ProgressManager.getInstance().run(object : Backgroundable(project, "Getting data from server") {
            override fun run(indicator: ProgressIndicator) {
                safeFind { findData() }
            }
        })

    }

    /**
     * Tries to call 'find' and sends the result of it to all subscribers
     */
    private fun safeFind(find: () -> Unit) {
        val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(ServerConnectionNotifier.SERVER_CONNECTION_TOPIC)
        serverConnectionResult = ServerConnectionResult.LOADING
        publisher.accept(serverConnectionResult)

        serverConnectionResult = try {
            find()
            ServerConnectionResult.SUCCESS
        } catch (e: java.lang.IllegalStateException) {
            ServerConnectionResult.FAIL
        }
        publisher.accept(serverConnectionResult)

    }

    private fun findData() {
        paneText = findPaneText()
        availableLanguages = findAvailableLanguages()
        tasks = findTasks()
        genders = findGenders()
        countries = findCountries()
    }

    private fun findAvailableLanguages(): List<PaneLanguage> {
        logger.info("${Plugin.PLUGIN_ID}: Getting available languages from server, current thread is ${Thread.currentThread().name}")
        return CollectionsQueryExecutor.getCollection("language/all", PaneLanguage.serializer())
    }

    private fun findTasks(): List<Task> {
        logger.info("${Plugin.PLUGIN_ID}: Getting tasks from server, current thread is ${Thread.currentThread().name}")
        return CollectionsQueryExecutor.getCollection("task/all", Task.serializer())
    }

    private fun findGenders(): List<Gender> {
        logger.info("${Plugin.PLUGIN_ID}: Getting genders from server, current thread is ${Thread.currentThread().name}")
        return CollectionsQueryExecutor.getCollection("gender/all", Gender.serializer())
    }

    private fun findCountries(): List<Country> {
        logger.info("${Plugin.PLUGIN_ID}: Getting countries from server, current thread is ${Thread.currentThread().name}")
        return CollectionsQueryExecutor.getCollection("country/all", Country.serializer())
    }

    private fun findPaneText(): PaneText {
        logger.info("${Plugin.PLUGIN_ID}: Getting pane text from server, current thread is ${Thread.currentThread().name}")
        val paneTextList = CollectionsQueryExecutor.getCollection("settings", PaneText.serializer())
        if (paneTextList.size == 1) {
            return paneTextList[0]
        }
        throw IllegalStateException("Got incorrect data from server")
    }

}
