package com.example

class Game(val black_id: Int, val white_id: Int, var current: String = "black", var boardState: String, var history: MutableMap<String, Int> = mutableMapOf<String, Int>()){
    var board: Array<Array<Piece?>>
    init {
        board = Array(8){ Array<Piece?>(8) {null} }
        this.createBoard()
    }

    //turn the board string into a board
    fun createBoard(){
        for (x in 0..7){
            for (y in 0..7){
                if (this.boardState[8*x + y] == 'B'){
                    this.board[x][y] = Piece("black", king = true)
                }
                if (this.boardState[8*x + y] == 'W'){
                    this.board[x][y] = Piece("white", king = true)
                }
                if (this.boardState[8*x + y] == 'b'){
                    this.board[x][y] = Piece("black")
                }
                if (this.boardState[8*x + y] == 'w'){
                    this.board[x][y] = Piece("white")
                }
            }
        }
    }
    //convert the board into a string to track threefold repetition
    fun boardString(){
        var string = ""
        for (x in 0..7){
            for (y in 0..7){
                val piece = this.board[x][y]
                if (piece == null){
                    string += "."
                }
                else if (piece.colour == "white"){
                    if (piece.king){
                        string += "W"
                    }
                    else{
                        string += "w"
                    }
                }
                else{
                    if (piece.king){
                        string += "B"
                    }
                    else{
                        string += "b"
                    }
                }
            }
        }
        string += this.current[0]
        this.boardState = string
    }

    //display the current board
    fun printBoard(){
        println("  A B C D E F G H")
        for (x in 0..7){
            print("${x+1} ")
            for (y in 0..7){
                val piece = this.board[x][y]
                if (piece == null){
                    print(".")
                }
                else{
                    piece.printPiece()
                }
                print(" ")
            }
            println()
        }
    }

    //check every valid move a piece has
    fun moveCheck(x: Int, y: Int, board: Array<Array<Piece?>>, player: String, captures: Boolean = false): MutableList<Array<Int>>{
        val piece = board[x][y]?: return mutableListOf<Array<Int>>()
        //determine what diagonal directions a piece should be allowed to move in
        val xDirections = if (piece.king) arrayOf(-1, 1)
        else if (piece.colour == "black") arrayOf(1)
        else arrayOf(-1)
        val yDirections = arrayOf(-1, 1)
        val validMoves = mutableListOf<Array<Int>>()
        //go through each possible diagonal square
        for (dx in xDirections) {
            for (dy in yDirections) {
                var current = arrayOf(x + dx, y + dy)
                //if that diagonal square would be off the board
                if (current[0] !in 0..7 || current[1] !in 0..7) {
                    continue
                }
                //if there's a non-capture move available
                if (board[current[0]][current[1]] == null && !captures) {
                    validMoves.add(current)
                }
                //if there's a capture move available
                if (board[current[0]][current[1]] != null && board[current[0]][current[1]]?.colour != player) {
                    current = arrayOf(current[0] + dx, current[1] + dy)
                    if (current[0] !in 0..7 || current[1] !in 0..7) {
                        continue
                    }
                    if (board[current[0]][current[1]] == null) {
                        validMoves.add(current)
                    }
                }
            }
        }
        return validMoves
    }

    //determine if the player has a capture move they can play on their turn
    fun forcedCaptures(player: String): Boolean {
        for (x in 0..7) {
            for (y in 0..7) {
                val piece = this.board[x][y] ?: continue
                if (piece.colour != player) {
                    continue
                }
                if (this.moveCheck(x, y, this.board, player, captures = true).isNotEmpty()){
                    return true
                }
            }
        }
        return false
    }

    //check that the move that was sent is allowed to be played
    fun validateMove(move: MutableList<Array<Int>>, player: String): Boolean{
        val tempBoard = this.board.map { it.clone() }.toTypedArray()
        //check that the current players owns the piece on the start square
        val x = move[0][0]
        val y = move[0][1]
        val piece = tempBoard[x][y]?: run {
            println("Invalid Piece")
            return false
        }
        if (piece.colour != player){
            println("Invalid Piece")
            return false
        }
        val forceTest = this.forcedCaptures(player) && move.size == 2
        var capture = false
        //run over each transition in the sequence
        for (i in 0..move.size-2){
            val validMoves = this.moveCheck(move[i][0], move[i][1], tempBoard, player, captures = forceTest)
            //if the current transition provided isn't a valid move
            if (!validMoves.any{it.contentEquals(move[i+1])}) {
                println("Move ${i+1} is invalid")
                return false
            }
            //if a capture has occurred
            if (move[i][0] - move[i+1][0] == 2 || move[i][0] - move[i+1][0] == -2){
                tempBoard[(move[i][0]+move[i+1][0])/2][(move[i][1]+move[i+1][1])/2] = null
                capture = true
            }
            //move the piece
            tempBoard[move[i+1][0]][move[i+1][1]] = tempBoard[move[i][0]][move[i][1]]
            tempBoard[move[i][0]][move[i][1]] = null
            //if the piece became a king
            if (player == "black" && move[i+1][0] == 7){
                tempBoard[move[i+1][0]][move[i+1][1]]?.king = true
            }
            if (player == "white" && move[i+1][0] == 0){
                tempBoard[move[i+1][0]][move[i+1][1]]?.king = true
            }
        }
        //if there was a forced capture the player could've taken
        if (capture == true) {
            val end = move.last()
            return this.moveCheck(end[0], end[1], tempBoard, player, captures = true).isEmpty()
        }
        return true
    }

    //to receive moves until a valid one is sent, and then update the board
    fun makeMove(move: MutableList<Array<Int>>){
        val player = this.current
        var next: String
        //decide who's turn it is
        if (this.current == "black"){
            next = "white"
        }
        else{
            next = "black"
        }

        //update the board and determine if either a kinging or capture has occurred
        var historyReset = false
        for (i in 0..move.size-2){
            //if a capture has occurred
            if (move[i][0] - move[i+1][0] == 2 || move[i][0] - move[i+1][0] == -2){
                this.board[(move[i][0]+move[i+1][0])/2][(move[i][1]+move[i+1][1])/2] = null
                historyReset = true
            }
            //move the piece
            this.board[move[i+1][0]][move[i+1][1]] = this.board[move[i][0]][move[i][1]]
            this.board[move[i][0]][move[i][1]] = null
            //if a kinging has occurred
            if (player == "black" && move[i+1][0] == 7){
                this.board[move[i+1][0]][move[i+1][1]]?.king = true
                historyReset = true
            }
            if (player == "white" && move[i+1][0] == 0){
                this.board[move[i+1][0]][move[i+1][1]]?.king = true
                historyReset = true
            }
        }
        this.current = next
        boardString()
        if (historyReset){
            this.history.clear()
        }
        this.history[this.boardState] = this.history.getOrDefault(this.boardState, 0) + 1
    }

    //determine if someone has won or if there is a stalemate
    fun winCheck(player: String): String{
        var blackPossible = false
        var whitePossible = false
        for (x in 0..7) {
            for (y in 0..7) {
                val piece = this.board[x][y] ?: continue
                if (piece.colour == "black" && this.moveCheck(x, y, this.board, "black").isNotEmpty()) {
                    blackPossible = true
                }
                if (piece.colour == "white" && this.moveCheck(x, y, this.board, "white").isNotEmpty()) {
                    whitePossible = true
                }
            }
        }
        if (!whitePossible && player == "white") {
            return "black"
        }
        if (!blackPossible && player == "black") {
            return "white"
        }
        return ""
    }
}