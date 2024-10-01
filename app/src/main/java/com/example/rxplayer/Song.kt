package com.example.rxplayer

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String,
    val duration: Long,
    val albumArt: String?
)