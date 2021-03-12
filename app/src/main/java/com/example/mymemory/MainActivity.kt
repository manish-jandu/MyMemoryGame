package com.example.mymemory

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryGame
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
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

        setupBoard()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh -> {
                //setup the game again
                if(memoryGame.getNumMoves() >0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit Your Current Game?",null,View.OnClickListener {
                        setupBoard()
                    })
                }else{
                    setupBoard()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAlertDialog(title:String,view:View?,positiveClickListener:View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Ok"){_,_ ->
                    positiveClickListener.onClick(null)
                }.show()
    }

    private fun setupBoard(){
        when(boardSize){
            BoardSize.EASY -> {
                tvNumPairs.text = "Easy: 4x2"
                tvNumMoves.text = "Pairs 0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumPairs.text = "Medium: 6x3"
                tvNumMoves.text = "Pairs 0/9"
            }
            BoardSize.HARD -> {
                tvNumPairs.text = "Hard: 6x4"
                tvNumMoves.text = "Pairs 0/12"
            }
        }

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
            Snackbar.make(clRoot,"Invalid Move",Snackbar.LENGTH_SHORT).show()
            //Alert the user of an invalid game
            return
        }
        //Actually flipping over the card
       if( memoryGame.flipCard(position)){
           Log.i(TAG,"Found a match! Num match found ${memoryGame.numPairsFound}")
           val color = ArgbEvaluator().evaluate(
                   (memoryGame.numPairsFound / boardSize.getNumPairs()).toFloat(),
                   ContextCompat.getColor(this,R.color.color_progress_none),
                   ContextCompat.getColor(this,R.color.color_progress_full)
           )as Int
            tvNumPairs.setTextColor(color)
           tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound}/${boardSize.getNumPairs()}"
           if (memoryGame.haveWonGame()){
               Snackbar.make(clRoot,"You won! Congratulations.",Snackbar.LENGTH_LONG).show()
           }
       }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}