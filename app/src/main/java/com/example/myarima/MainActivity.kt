package com.example.myarima

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SwapHoriz
import android.util.Log


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArimaaGame()
        }
    }
}

val initialBoardState: Array<Array<String?>> = arrayOf(
    arrayOf("silver_rabbit", "silver_rabbit", "silver_rabbit", "silver_rabbit", "silver_rabbit", "silver_rabbit", "silver_rabbit", "silver_rabbit"),
    arrayOf("silver_cat", "silver_dog", "silver_horse", "silver_camel", "silver_elephant", "silver_horse", "silver_dog", "silver_cat"),
    arrayOfNulls<String>(8),
    arrayOfNulls<String>(8),
    arrayOfNulls<String>(8),
    arrayOfNulls<String>(8),
    arrayOf("gold_cat", "gold_dog", "gold_horse", "gold_camel", "gold_elephant", "gold_horse", "gold_dog", "gold_cat"),
    arrayOf("gold_rabbit", "gold_rabbit", "gold_rabbit", "gold_rabbit", "gold_rabbit", "gold_rabbit", "gold_rabbit", "gold_rabbit")
)

@Composable
fun ArimaaGame() {
    var boardState by remember { mutableStateOf(deepCopy(initialBoardState)) }
    var turnStartState by remember { mutableStateOf(deepCopy(initialBoardState)) }
    var frozenState by remember { mutableStateOf(Array(8) { Array(8) { false } }) }
    var selectedPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val squareColors = remember {
        Array(8) { Array(8) { Color(0xFFD2B48C) } }
    }
    var currentPlayer by remember { mutableStateOf(1) }
    var moveCount by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Player 1's turn!") }
    var errorMessage by remember { mutableStateOf("") }
    var boardHistory = mutableListOf<Array<Array<String?>>>()
    var validMoves by remember { mutableStateOf(emptyList<Pair<Int, Int>>()) }
    var gameOver by remember { mutableStateOf(false) }
    var pullDialogState by remember { mutableStateOf<Pair<Pair<Int, Int>, Pair<Int, Int>>?>(null) }

    val trapSquares = listOf(2 to 2, 2 to 5, 5 to 2, 5 to 5)

    fun isTrapSquare(row: Int, col: Int): Boolean = trapSquares.contains(row to col)

    fun hasFriendlyPieceAdjacent(row: Int, col: Int): Boolean {
        val piece = boardState[row][col] ?: return false
        val neighbors = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        return neighbors.any { (dRow, dCol) ->
            val nRow = row + dRow
            val nCol = col + dCol
            if (nRow in 0..7 && nCol in 0..7) {
                val neighborPiece = boardState[nRow][nCol]
                neighborPiece != null && neighborPiece.startsWith(piece.take(4))
            } else false
        }
    }

    fun handleTrapSquares() {
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                if (isTrapSquare(row, col)) {
                    val piece = boardState[row][col]
                    if (piece != null && !hasFriendlyPieceAdjacent(row, col)) {
                        boardState[row][col] = null
                    }
                }
            }
        }
    }

    fun updateFrozenState() {
        frozenState = Array(8) { Array(8) { false } }
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = boardState[row][col] ?: continue
                val neighbors = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).map { (dRow, dCol) ->
                    row + dRow to col + dCol
                }.filter { (nRow, nCol) ->
                    nRow in 0..7 && nCol in 0..7
                }

                val isFrozen = neighbors.any { (nRow, nCol) ->
                    val neighborPiece = boardState[nRow][nCol]
                    neighborPiece != null && isStronger(neighborPiece, piece)
                } && neighbors.none { (nRow, nCol) ->
                    val neighborPiece = boardState[nRow][nCol]
                    neighborPiece != null && neighborPiece.startsWith(piece.take(4))
                }

                if (isFrozen) {
                    frozenState[row][col] = true
                }
            }
        }
    }

    fun checkForWinner(): String? {

        if (boardState[0].any { it == "gold_rabbit" }) {
            return "Player 1 (Gold) wins by getting the rabbit to the other side!"
        }


        if (boardState[7].any { it == "silver_rabbit" }) {
            return "Player 2 (Silver) wins by getting the rabbit to the other side!"
        }


        if (boardState.flatten().none { it?.contains("gold_rabbit") == true }) {
            return "Player 2 (Silver) wins by elimination!"
        }


        if (boardState.flatten().none { it?.contains("silver_rabbit") == true }) {
            return "Player 1 (Gold) wins by elimination!"
        }


        return null
    }


    fun handlePull(
        fromRow: Int, fromCol: Int,
        pullRow: Int, pullCol: Int,
        targetRow: Int, targetCol: Int
    ) {
        if (moveCount + 2 > 4) {
            errorMessage = "Maximum moves reached. End turn to continue."
            return
        }

        val pullingPiece = boardState[fromRow][fromCol]!!
        val pulledPiece = boardState[pullRow][pullCol]!!

        boardState[targetRow][targetCol] = pullingPiece
        boardState[fromRow][fromCol] = pulledPiece
        boardState[pullRow][pullCol] = null

        moveCount += 2
        validMoves = emptyList()
        selectedPosition = null
        pullDialogState = null

        handleTrapSquares()
        updateFrozenState()

        val winner = checkForWinner()
        if (winner != null) {
            message = winner
            gameOver = true
        } else if (moveCount >= 4) {
            errorMessage = "Maximum moves reached. End turn to continue."
        } else {
            message = "Move $moveCount for Player $currentPlayer."
        }
    }

    fun performSingleMove(selectedRow: Int, selectedCol: Int, row: Int, col: Int) {
        if (moveCount + 1 > 4) {
            errorMessage = "Maximum moves reached. End turn to continue."
            return
        }


        val selectedPiece = boardState[selectedRow][selectedCol]
        if (selectedPiece != null && isValidMove(boardState, selectedRow, selectedCol, row, col)) {


            val newBoardState = deepCopy(boardState)


            if (boardHistory.any { areBoardsEqual(it, newBoardState) }) {
                errorMessage = "This board state has been encountered before. Move rejected."
                return
            }


            boardState[row][col] = selectedPiece
            boardState[selectedRow][selectedCol] = null
            selectedPosition = null
            validMoves = emptyList()


            boardHistory.add(newBoardState)


            moveCount++


            handleTrapSquares()
            updateFrozenState()

            val winner = checkForWinner()
            if (winner != null) {
                message = winner
                gameOver = true
            } else if (moveCount >= 4) {
                errorMessage = "Maximum moves reached. End turn to continue."
            } else {
                message = "Move $moveCount for Player $currentPlayer."
            }
        } else {
            errorMessage = "Invalid move: Cannot move to this square."
            selectedPosition = null
            validMoves = emptyList()
        }
    }


    fun handlePush(
        fromRow: Int, fromCol: Int,
        pushRow: Int, pushCol: Int,
        targetRow: Int, targetCol: Int
    ) {
        if (moveCount + 2 > 4) {
            errorMessage = "Maximum moves reached. End turn to continue."
            return
        }

        val pushingPiece = boardState[fromRow][fromCol]!!
        val pushedPiece = boardState[pushRow][pushCol]!!


        boardState[targetRow][targetCol] = pushedPiece
        boardState[pushRow][pushCol] = null
        boardState[pushRow][pushCol] = pushingPiece
        boardState[fromRow][fromCol] = null

        moveCount += 2
        validMoves = emptyList()
        selectedPosition = null

        handleTrapSquares()
        updateFrozenState()

        val winner = checkForWinner()
        if (winner != null) {
            message = winner
            gameOver = true
        } else if (moveCount >= 4) {
            errorMessage = "Maximum moves reached. End turn to continue."
        } else {
            message = "Push performed."
        }
    }

    fun calculatePushMoves(
        boardState: Array<Array<String?>>,
        fromRow: Int,
        fromCol: Int
    ): List<Pair<Int, Int>> {
        val piece = boardState[fromRow][fromCol] ?: return emptyList()
        val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        val validPushMoves = mutableListOf<Pair<Int, Int>>()

        directions.forEach { (dRow, dCol) ->
            val toRow = fromRow + dRow
            val toCol = fromCol + dCol

            if (toRow in 0..7 && toCol in 0..7 && boardState[toRow][toCol] != null) {

                val adjacentPiece = boardState[toRow][toCol]!!
                if (adjacentPiece.startsWith(if (currentPlayer == 1) "silver" else "gold")) {
                    validPushMoves.add(toRow to toCol)
                }
            }
        }

        return validPushMoves
    }


    @Composable
    fun PullConfirmationDialog(
        onSingleMove: () -> Unit,
        onPullMove: () -> Unit,
        onCancel: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { onCancel() },
            title = { Text("Choose Your Move") },
            text = { Text("Do you want to move your piece only or pull the opponent's piece?") },
            confirmButton = {
                Button(onClick = { onPullMove() }) {
                    Text("Pull (Two Moves)")
                }
            },
            dismissButton = {
                Row {
                    Button(onClick = { onSingleMove() }) {
                        Text("Single Move")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onCancel() }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    pullDialogState?.let { (moveTarget, pullTarget) ->
        val (moveRow, moveCol) = moveTarget
        val (pullRow, pullCol) = pullTarget

        PullConfirmationDialog(
            onSingleMove = {
                val (selectedRow, selectedCol) = selectedPosition!!
                boardState[moveRow][moveCol] = boardState[selectedRow][selectedCol]
                boardState[selectedRow][selectedCol] = null
                pullDialogState = null
                validMoves = emptyList()
                selectedPosition = null
                moveCount++
                updateFrozenState()
                handleTrapSquares()
                val winner = checkForWinner()
                if (winner != null) {
                    message = winner
                    gameOver = true
                }
            },
            onPullMove = {
                val (selectedRow, selectedCol) = selectedPosition!!
                handlePull(selectedRow, selectedCol, pullRow, pullCol, moveRow, moveCol)
            },
            onCancel = {
                pullDialogState = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(if (currentPlayer == 1) Color(0xFFFFC0CB) else Color.Yellow),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (gameOver) "Game Over! $message" else message,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }


        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        ArimaaBoard(
            boardState = boardState,
            frozenState = frozenState,
            selectedPosition = selectedPosition,
            squareColors = squareColors,
            validMoves = validMoves,
            trapSquares = trapSquares,
            onSquareClick = { row, col ->
                if (gameOver) return@ArimaaBoard

                if (selectedPosition == null) {

                    val piece = boardState[row][col]


                    if (piece != null && piece.startsWith(if (currentPlayer == 1) "gold" else "silver") && !frozenState[row][col]) {
                        selectedPosition = row to col


                        validMoves = calculateValidMoves(boardState, row, col)
                        errorMessage = ""
                    } else if (frozenState[row][col]) {
                        errorMessage = "This piece is frozen. You cannot move it."
                    } else {
                        errorMessage = "Invalid selection: Select your own piece."
                    }
                } else {
                    val (selectedRow, selectedCol) = selectedPosition!!


                    if (frozenState[selectedRow][selectedCol]) {
                        errorMessage = "This piece is frozen. You cannot move it."
                        selectedPosition = null
                        validMoves = emptyList()
                        return@ArimaaBoard
                    }


                    val piece = boardState[selectedRow][selectedCol]
                    if (piece == "gold_rabbit" && row >= selectedRow) {

                        errorMessage = "Rabbits cannot move backward."
                        selectedPosition = null
                        validMoves = emptyList()
                        return@ArimaaBoard
                    } else if (piece == "silver_rabbit" && row <= selectedRow) {

                        errorMessage = "Rabbits cannot move backward."
                        selectedPosition = null
                        validMoves = emptyList()
                        return@ArimaaBoard
                    }


                    val adjacentSquares = listOf(
                        row to col,
                        selectedRow + 1 to selectedCol,
                        selectedRow - 1 to selectedCol,
                        selectedRow to selectedCol + 1,
                        selectedRow to selectedCol - 1
                    )

                    val pullCandidates = adjacentSquares.filter { (pullRow, pullCol) ->
                        pullRow in 0..7 && pullCol in 0..7 &&
                                boardState[pullRow][pullCol]?.startsWith(if (currentPlayer == 1) "silver" else "gold") == true &&
                                isStronger(
                                    boardState[selectedRow][selectedCol]!!,
                                    boardState[pullRow][pullCol]!!
                                )
                    }


                    if (pullCandidates.isNotEmpty()) {
                        pullDialogState = (row to col) to pullCandidates.first()
                    } else if (validMoves.contains(row to col)) {

                        performSingleMove(selectedRow, selectedCol, row, col)
                    } else {
                        errorMessage = "Invalid move: Cannot move to this square."
                        selectedPosition = null
                        validMoves = emptyList()
                    }
                }
            }
        )




        Spacer(modifier = Modifier.height(16.dp))

        Row(

            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    boardState = deepCopy(turnStartState)
                    moveCount = 0
                    selectedPosition = null
                    validMoves = emptyList()
                    errorMessage = ""
                    message = "Move restarted. Player $currentPlayer's turn!"
                }) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restart Move",
                        tint = Color.Green
                    )
                }
                Text("Restart Move", fontSize = 12.sp, color = Color.Black)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    boardState = deepCopy(initialBoardState)
                    turnStartState = deepCopy(initialBoardState)
                    selectedPosition = null
                    currentPlayer = 1
                    moveCount = 0
                    validMoves = emptyList()
                    message = "Game reset. Player 1's turn!"
                    errorMessage = ""
                    gameOver = false
                    boardHistory.clear()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Game",
                        tint = Color.Red
                    )
                }
                Text("Reset Game", fontSize = 12.sp, color = Color.Black)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    if (moveCount > 0) {
                        currentPlayer = if (currentPlayer == 1) 2 else 1
                        turnStartState = deepCopy(boardState)
                        moveCount = 0
                        selectedPosition = null
                        validMoves = emptyList()
                        message = "Player $currentPlayer's turn!"
                        errorMessage = ""
                    } else {
                        errorMessage = "You must make at least one move before ending your turn!"
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "End Turn",
                        tint = Color.Magenta
                    )
                }
                Text("End Turn", fontSize = 12.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun ArimaaBoard(
    boardState: Array<Array<String?>>,
    frozenState: Array<Array<Boolean>>,
    selectedPosition: Pair<Int, Int>?,
    squareColors: Array<Array<Color>>,
    validMoves: List<Pair<Int, Int>>,
    trapSquares: List<Pair<Int, Int>>,
    onSquareClick: (row: Int, col: Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (selectedPosition != null) {
            Text(
                text = "Selected Position: (${selectedPosition.first}, ${selectedPosition.second})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFD2B48C)),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until 8) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 8) {
                            val isTrapSquare = trapSquares.contains(row to col)
                            val isSelected = selectedPosition == row to col
                            val piece = boardState[row][col]

                            val squareColor = when {
                                row to col in validMoves -> Color.Green
                                isTrapSquare -> Color.Red
                                else -> squareColors[row][col]
                            }

                            val circleColor = when {
                                piece?.startsWith("gold") == true -> Color(0xFFFFC0CB)
                                piece?.startsWith("silver") == true -> Color.Yellow
                                else -> null
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(1.dp, Color.Black)
                                    .background(if (isSelected) Color.Blue else squareColor)
                                    .clickable { onSquareClick(row, col) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(circleColor ?: Color.Transparent, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = getPieceInitial(piece),
                                            color = if (frozenState[row][col]) Color.Gray else Color.Black,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



fun calculateValidMoves(boardState: Array<Array<String?>>, fromRow: Int, fromCol: Int): List<Pair<Int, Int>> {
    val piece = boardState[fromRow][fromCol] ?: return emptyList()
    val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    val validMoves = mutableListOf<Pair<Int, Int>>()


    val isGoldRabbit = piece == "gold_rabbit"
    val isSilverRabbit = piece == "silver_rabbit"


    if (isGoldRabbit) {

        directions.filter { (dRow, _) -> dRow < 0 }
    } else if (isSilverRabbit) {

        directions.filter { (dRow, _) -> dRow > 0 }
    }


    directions.forEach { (dRow, dCol) ->
        val toRow = fromRow + dRow
        val toCol = fromCol + dCol

        if (toRow in 0..7 && toCol in 0..7 && boardState[toRow][toCol] == null) {
            validMoves.add(toRow to toCol)
        }
    }

    return validMoves
}


fun getPieceInitial(type: String): String {
    return when {
        type.contains("rabbit") -> "🐇"
        type.contains("cat") -> "🐱"
        type.contains("dog") -> "🐶"
        type.contains("horse") -> "🐴"
        type.contains("camel") -> "🐪"
        type.contains("elephant") -> "🐘"
        else -> ""
    }
}

fun isValidMove(boardState: Array<Array<String?>>, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
    val piece = boardState[fromRow][fromCol] ?: return false
    val isOneStep = kotlin.math.abs(fromRow - toRow) + kotlin.math.abs(fromCol - toCol) == 1

    if (!isOneStep) return false


    if (boardState[toRow][toCol] != null) return false


    return true
}


fun isStronger(piece1: String, piece2: String): Boolean {
    val strengthOrder = listOf("rabbit", "cat", "dog", "horse", "camel", "elephant")
    return strengthOrder.indexOf(piece1.substringAfter('_')) > strengthOrder.indexOf(piece2.substringAfter('_'))
}

fun areBoardsEqual(board1: Array<Array<String?>>, board2: Array<Array<String?>>): Boolean {
    for (i in board1.indices) {
        for (j in board1[i].indices) {
            if (board1[i][j] != board2[i][j]) {
                Log.d("BoardMismatch", "Mismatch at ($i, $j): ${board1[i][j]} != ${board2[i][j]}")
                return false
            }
        }
    }
    return true
}


fun deepCopy(boardState: Array<Array<String?>>): Array<Array<String?>> {
    return Array(boardState.size) { row ->
        Array(boardState[row].size) { col ->
            boardState[row][col]
        }
    }
}