package com.example.mymemory

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryCard
import com.example.mymemory.utils.DEFAULT_ICONS
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var boardSize:BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val chosenImages:List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.numCards)
        val randomizedImages:List<Int> = (chosenImages + chosenImages).shuffled()
        val memoryCards:List<MemoryCard> = randomizedImages.map { MemoryCard(it) }

        rvBoard.adapter = MemoryBoardAdapter(this,boardSize,memoryCards)
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }
}