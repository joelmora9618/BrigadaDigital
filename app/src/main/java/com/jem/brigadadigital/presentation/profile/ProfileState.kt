package com.jem.brigadadigital.presentation.profile

import com.jem.brigadadigital.domain.model.UserProfile

sealed class ProfileState {
    data object Idle : ProfileState()
    data object Loading : ProfileState()
    data class Loaded(val profile: UserProfile) : ProfileState()
    data object NotFound : ProfileState() // Profile does not exist yet
    data class Saved(val uid: String) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
