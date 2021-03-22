package com.example.mymemory

import android.animation.ArgbEvaluator
import android.app.Activity
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
import com.example.mymemory.models.UserImageList
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.example.mymemory.utils.EXTRA_GAME_NAME
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "Main Activity"
        private const val CREATE_REQUEST_CODE = 248
    }
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var boardSize:BoardSize = BoardSize.EASY
    private var customGameImages:List<String>? = null

    private val db = Firebase.firestore
    private var gameName:String? = null

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null){
                Log.e(TAG,"Got null custom Game name from create Activity")
            }
            downloadGame(customGameName!!)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get()
                .addOnSuccessListener {document ->
                   val userImageList = document.toObject(UserImageList::class.java)
                    if(userImageList?.images == null){
                        Log.e(TAG,"Invalid custom game data from Firestore")
                        Snackbar.make(
                                clRoot,
                                "Sorry we couldnot find any such game,'$customGameName'",
                                Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    //we have found the images but just need to reset the game with these photos
                     val numCards:Int = userImageList!!.images!!.size * 2
                    boardSize = BoardSize.getByValue(numCards)
                    gameName = customGameName
                    customGameImages = userImageList.images
                    setupBoard()
                }
                .addOnFailureListener{Exception ->
                    Log.e(TAG,"Exception when retrieving game",Exception)
                }
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
            gameName = null
            customGameImages = null
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
        supportActionBar?.title = gameName ?: getString(R.string.app_name)

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

        memoryGame = MemoryGame(boardSize,customGameImages)

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