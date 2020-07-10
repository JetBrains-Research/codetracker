package org.jetbrains.research.ml.codetracker.ui.panes

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import javafx.collections.FXCollections
import javafx.embed.swing.JFXPanel
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import org.jetbrains.research.ml.codetracker.server.PluginServer
import org.jetbrains.research.ml.codetracker.ui.*
import java.lang.Thread.currentThread
import java.util.function.Consumer

/**
 * For panes with language combo box
 */
interface LanguageNotifier : Consumer<Int> {
    companion object {
        val LANGUAGE_TOPIC = Topic.create("pane language change", LanguageNotifier::class.java)
    }
}

open class LanguagePaneUiData : PaneUiData() {
    val language = ListedUiField(
        PluginServer.availableLanguages, 0,
        LanguageNotifier.LANGUAGE_TOPIC, false)

    override fun getData(): List<UiField<*>> = arrayListOf(language)
}

open class LanguagePaneController(project: Project, scale: Double, fxPanel: JFXPanel, id: Int) : PaneController(project, scale, fxPanel, id) {
    @FXML
    private lateinit var languageComboBox: ComboBox<String>
    protected val logger = Logger.getInstance(javaClass)
    protected open val paneUiData: LanguagePaneUiData = LanguagePaneUiData()

    override fun initialize() {
        println("${this::class.simpleName}:PCinitialize ${currentThread().name}")
        initLanguageComboBox()
    }

    override fun update() {
        paneUiData.updateUiData()
    }

    private fun initLanguageComboBox() {
        println("${this::class.simpleName}:initLanguageComboBox ${currentThread().name}")
        languageComboBox.items = FXCollections.observableList(paneUiData.language.dataList.map { it.key })
        languageComboBox.selectionModel.selectedItemProperty().addListener { _ ->
            paneUiData.language.uiValue = languageComboBox.selectionModel.selectedIndex
        }
        subscribe(LanguageNotifier.LANGUAGE_TOPIC, object : LanguageNotifier {
            override fun accept(newLanguageIndex: Int) {
                languageComboBox.selectionModel.select(newLanguageIndex)
            }
        })
    }
}