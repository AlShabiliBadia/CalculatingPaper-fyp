package com.example.calculatingpaper.data

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val notes: List<Note>,
    val folders: List<Folder>,
    val appSettings: AppSettings
)

@Serializable
data class AppSettings(
    val decimalPrecision: Int
)