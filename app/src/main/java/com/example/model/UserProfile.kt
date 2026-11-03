package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val googlePhotoUrl: String = "",
  val customAvatarUrl: String = "",
  val avatarStoragePath: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun effectiveAvatarUrl(): String? {
    if (customAvatarUrl.isNotBlank()) return customAvatarUrl
    if (googlePhotoUrl.isNotBlank()) return googlePhotoUrl
    return null
  }
}
