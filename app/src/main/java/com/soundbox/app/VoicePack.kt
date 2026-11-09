package com.azmaynu.soundalert.model // (Apne project ka package name yahan check kar lena)

data class VoicePack(
    val id: Int,
    val title: String,
    val subtitle: String,
    val language: String,
    var isSelected: Boolean = false
)
