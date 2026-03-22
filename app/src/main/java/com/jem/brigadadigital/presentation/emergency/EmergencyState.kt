package com.jem.brigadadigital.presentation.emergency

import com.jem.brigadadigital.domain.model.EmergencyEvent

sealed class EmergencyState {
    data object Idle : EmergencyState() // No active emergency
    data class Active(val emergency: EmergencyEvent) : EmergencyState() // Active emergency occurring
    data object Loading : EmergencyState()
    data class Error(val message: String) : EmergencyState()
}
