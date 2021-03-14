package com.example.mymemory

import android.animation.ArgbEvaluator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryGame
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "Main Activity"
        private const val CREATE_REQUEST_CODE = 248
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
                    showAlertDialog("Quit Your Current Game?", null, View.OnClickListener {setupBoard()
                    })
                }else{
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size ->{
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCreationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroupSize)
        showAlertDialog("Create your own Memory Board",boardSizeView,View.OnClickListener {
            //set a new value of board size
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //Navigate to New Activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroupSize)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size",boardSizeView,View.OnClickListener {
            //set a new value of board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
           setupBoard()
        })
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