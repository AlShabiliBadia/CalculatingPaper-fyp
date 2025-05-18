package com.example.calculatingpaper.viewmodel

sealed class SyncCheckState {
    object Idle : SyncCheckState()
    object RequiresDownloadConfirmation : SyncCheckState()
    object RequiresUploadConfirmation : SyncCheckState()
    object CanEnableDirectly : SyncCheckState()
    data class Error(val message: String) : SyncCheckState()
}

sealed class SyncActivationState {
    object Idle : SyncActivationState()
    data class Running(val message: String) : SyncActivationState()
    object Success : SyncActivationState()
    data class Error(val message: String) : SyncActivationState()
}