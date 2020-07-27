package org.jetbrains.research.ml.codetracker.models

import kotlinx.serialization.Serializable

@Serializable
data class SurveyPaneText(
    val age: String,
    val gender: String,
    val experience: String,
    val country: String,
    val years: String,
    val months: String,
    val startSession: String
)

@Serializable
data class FinalPaneText(
    val praise: String,
    val backToSurvey: String,
    val finalMessage: String,
    val backToTasks: String
)

@Serializable
data class TaskChoosingPaneText(
    val chooseTask: String,
    val finishSession: String,
    val startSolving: String
)

@Serializable
data class TaskSolvingPaneText(
    val inputData: String,
    val outputData: String,
    val submit: String,
    val backToTasks: String
)

@Serializable
data class PaneText(
    val surveyPane: Map<PaneLanguage, SurveyPaneText>,
    val taskChoosingPane: Map<PaneLanguage, TaskChoosingPaneText>,
    val taskSolvingPane: Map<PaneLanguage, TaskSolvingPaneText>,
    val finalPane: Map<PaneLanguage, FinalPaneText>
)