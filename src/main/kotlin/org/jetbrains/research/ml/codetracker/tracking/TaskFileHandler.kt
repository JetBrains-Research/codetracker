package org.jetbrains.research.ml.codetracker.tracking

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.research.ml.codetracker.Plugin
import org.jetbrains.research.ml.codetracker.models.Language
import org.jetbrains.research.ml.codetracker.models.Task
import java.io.File
import java.lang.IllegalArgumentException

private class TaskDocumentListener : DocumentListener {
    private val logger: Logger = Logger.getInstance(javaClass)

    init {
        logger.info("${Plugin.PLUGIN_ID}: init documents listener")
    }

    // Tracking documents changes before to be consistent with activity-tracker plugin
    override fun beforeDocumentChange(event: DocumentEvent) {
        if (isValidChange(event)) DocumentLogger.log(event.document)
    }

    // To avoid completion events with IntellijIdeaRulezzz sign
    // Todo: add tests for it
    private fun isValidChange(event: DocumentEvent): Boolean {
        return EditorFactory.getInstance().getEditors(event.document).isNotEmpty()
                && FileDocumentManager.getInstance().getFile(event.document) != null
    }
}

object TaskFileHandler {
    private const val PLUGIN_FOLDER = "codetracker"

    private val logger: Logger = Logger.getInstance(javaClass)
    private val documentToTask: HashMap<Document, Task> = HashMap()

    private val listener by lazy {
        TaskDocumentListener()
    }

    /**
     * Creates file and adds [TaskDocumentListener] to it if it doesn't exist, opens it and makes it writable.
     */
    fun createAndOpenFile(project: Project, task: Task, language: Language = Language.PYTHON): VirtualFile? {
        val virtualFile = getOrCreateFile(project, task, language)
        addDocumentToTask(virtualFile, task)
        openFile(project, virtualFile)
        return virtualFile
    }

    private fun getOrCreateFile(project: Project, task: Task, language: Language): VirtualFile? {
        val file = File("${project.basePath}/$PLUGIN_FOLDER/${task.key}${language.extension.ext}")
        FileUtil.createIfDoesntExist(file)
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    }

    /**
     *  If [documentToTask] doesn't have this [task] then we didn't track the document
     */
    private fun addDocumentToTask(virtualFile: VirtualFile?, task: Task): Document? {
        val document = virtualFile?.let { FileDocumentManager.getInstance().getDocument(it) }
        val oldTask: Task? = documentToTask[document]
        if (oldTask == null) {
            document?.let { documentToTask[it] = task; it.addDocumentListener(listener) }
        } else {
            // If the old task is not equal to the new task, we should raise an error
            if (oldTask != task) {
                val message = "${Plugin.PLUGIN_ID}: an attempt to assign another task to the document ${document}. " +
                        "The old task is ${oldTask.key}, the new task is ${task.key}"
                logger.error(message)
                throw IllegalArgumentException(message)
            }
        }
        return document
    }

    /**
     * Opens file and makes it writable
     */
    private fun openFile(project: Project, virtualFile: VirtualFile?) {
        virtualFile?.let {
            // Line below makes file writable if it's not
            ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(listOf(virtualFile))
            FileEditorManager.getInstance(project).openFile(it, true, true)
        }
    }

    /**
     * Gets file by [task] and [language] and closes it, making it read-only
     */
    fun closeFile(project: Project, task: Task, language: Language = Language.PYTHON) {
        val virtualFile = getOrCreateFile(project, task, language)
        closeFile(project, virtualFile)
    }

    /**
     * Makes file read-only and closes it
     */
    private fun closeFile(project: Project, virtualFile: VirtualFile?) {
        virtualFile?.let {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            document?.setReadOnly(true)
//            document?.fireReadOnlyModificationAttempt()
            FileEditorManager.getInstance(project).closeFile(virtualFile)
        }
    }

    fun stopTracking() {
        documentToTask.forEach { it.key.removeDocumentListener(listener) }
    }

}