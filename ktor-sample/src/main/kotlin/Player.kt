class Player(val colour: String){
    //send a move back to the game
    fun sendMove(): MutableList<Array<Int>>{
        var test = false
        val moves = mutableListOf<String>()
        //loop while any board space named is invalid
        while (!test) {
            println("Piece to Move?")
            var current = readln().uppercase()
            moves.add(current)
            println("Where to Move First?")
            current = readln().uppercase()
            //any additional multi-jumps are listed here
            while (current != "") {
                moves.add(current)
                println("Additional Movements")
                current = readln().uppercase()
            }
            //test that every board space is valid
            test = true
            for (move in moves){
                if (move.length != 2){
                    test = false
                    break
                }
                if (move[0] !in 'A'..'H' || move[1] !in '1'..'8'){
                    test = false
                    break
                }
            }
            if (!test){
                println("Invalid move")
                moves.clear()
            }
        }
        //create a list of coordinates of the places
        val result = mutableListOf<Array<Int>>()
        for (move in moves){
            val x = move[1].digitToInt() - 1
            val y = move[0] - 'A'
            result.add(arrayOf(x, y))
        }
        return result
    }
}