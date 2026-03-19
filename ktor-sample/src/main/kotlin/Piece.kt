//to store piece data for the Game class
class Piece(val colour: String, var king: Boolean = false){
    fun printPiece() = if (king) print(colour[0].uppercaseChar()) else print(colour[0])
}