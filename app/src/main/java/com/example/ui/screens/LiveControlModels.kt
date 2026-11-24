package com.example.ui.screens


data class RemoteAppConfig(
    val developerPin: String = "7777",
    val updaterEnabled: Boolean = true,
    val instructorAccessEnabled: Boolean = true,
    val principalAccessEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val maintenanceBanner: String = "",
    val examMarksEnabled: Boolean = true,
    val shopFloorEnabled: Boolean = true,
    val undoEnabled: Boolean = true,
    val alertRepliesEnabled: Boolean = true
) {
    companion object {
        fun fromMap(map: Map<String, String>): RemoteAppConfig = RemoteAppConfig(
            developerPin = map["developerPin"]?.ifBlank { "7777" } ?: "7777",
            updaterEnabled = map["updaterEnabled"]?.lowercase() != "false",
            instructorAccessEnabled = map["instructorAccessEnabled"]?.lowercase() != "false",
            principalAccessEnabled = map["principalAccessEnabled"]?.lowercase() != "false",
            maintenanceMode = map["maintenanceMode"]?.lowercase() == "true",
            maintenanceBanner = map["maintenanceBanner"] ?: "",
            examMarksEnabled = map["examMarksEnabled"]?.lowercase() != "false",
            shopFloorEnabled = map["shopFloorEnabled"]?.lowercase() != "false",
            undoEnabled = map["undoEnabled"]?.lowercase() != "false",
            alertRepliesEnabled = map["alertRepliesEnabled"]?.lowercase() != "false"
        )
    }
}

data class RemoteUiConfig(
    val appTitle: String = "Govt. ITI Ranni - Electronics Mechanic (2025-27)",
    val traineeTabTitle: String = "Trainee Portal",
    val staffTabTitle: String = "Staff Console",
    val examMarksTabTitle: String = "Exam Marks (Theory 50 / Lab 100)",
    val shopFloorTabTitle: String = "Shop Floor",
    val showReportBugButton: Boolean = true,
    val showUndoButton: Boolean = true,
    val announcementBanner: String = "",
    val accentPreset: String = "default",
    val rawOverrides: Map<String, String> = emptyMap()
) {
    fun text(key: String, fallback: String): String =
        rawOverrides[key]?.takeIf { it.isNotBlank() } ?: fallback

    fun visible(key: String, defaultValue: Boolean = true): Boolean =
        rawOverrides[key]?.lowercase()?.let { it != "false" } ?: defaultValue

    companion object {
        fun fromMap(map: Map<String, String>): RemoteUiConfig = RemoteUiConfig(
            appTitle = map["appTitle"]?.ifBlank { "Govt. ITI Ranni - Electronics Mechanic (2025-27)" }
                ?: "Govt. ITI Ranni - Electronics Mechanic (2025-27)",
            traineeTabTitle = map["traineeTabTitle"]?.ifBlank { "Trainee Portal" } ?: "Trainee Portal",
            staffTabTitle = map["staffTabTitle"]?.ifBlank { "Staff Console" } ?: "Staff Console",
            examMarksTabTitle = map["examMarksTabTitle"]?.ifBlank { "Exam Marks (Theory 50 / Lab 100)" }
                ?: "Exam Marks (Theory 50 / Lab 100)",
            shopFloorTabTitle = map["shopFloorTabTitle"]?.ifBlank { "Shop Floor" } ?: "Shop Floor",
            showReportBugButton = map["showReportBugButton"]?.lowercase() != "false",
            showUndoButton = map["showUndoButton"]?.lowercase() != "false",
            announcementBanner = map["announcementBanner"] ?: "",
            accentPreset = map["accentPreset"]?.ifBlank { "default" } ?: "default",
            rawOverrides = map
        )
    }
}

data class ExamMarkRow(
    val roll: Int,
    val name: String,
    val theoryMarks: String = "",
    val labMarks: String = "",
    val remarks: String = "",
    val published: Boolean = false
) {
    val theoryInt: Int get() = theoryMarks.toIntOrNull()?.coerceIn(0, 50) ?: 0
    val labInt: Int get() = labMarks.toIntOrNull()?.coerceIn(0, 100) ?: 0
    val totalOutOf150: Int get() = theoryInt + labInt
}
