package com.example.mymemory.models

data class MemoryCard(
        val identifier:Int,
        var ifFaceUp:Boolean = false,
        var isMatched:Boolean = false
)