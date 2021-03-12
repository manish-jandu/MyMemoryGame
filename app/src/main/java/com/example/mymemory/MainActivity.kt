package com.example.mymemory

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryGame
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "Main Activity"
    }
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var boardSize:BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        memoryGame = MemoryGame(boardSize)

        adapter =MemoryBoardAdapter(this,boardSize,memoryGame.cards,object:MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
               updateGameWithFlip(position)
            }
        })
        rvBoard.adapter =adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {
        //Error checking
        if(memoryGame.haveWonGame()){
            //Alert the user of an invalid game
                Snackbar.make(clRoot,"You already won!",Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){
            Snackbar.make(clRoot,"Invalid Move",Snackbar.LENGTH_LONG).show()
            //Alert the user of an invalid game
            return
        }
        //Actually flipping over the card
       if( memoryGame.flipCard(position)){
           Log.i(TAG,"Found a match! Num match found ${memoryGame.numPairsFound}")
       }
        adapter.notifyDataSetChanged()
    }
}