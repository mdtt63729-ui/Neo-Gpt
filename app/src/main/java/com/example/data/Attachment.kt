package com.example.data

import android.net.Uri

/** A user-selected attachment that lives in the composer until sent or removed. */
data class Attachment(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val sizeBytes: Long,
    val type: AttachmentType
)

enum class AttachmentType { IMAGE, FILE }
