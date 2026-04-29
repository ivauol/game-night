package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameTest: DescribeSpec({
    describe("createBoard"){
        it("Stores an empty board if given an empty board"){
            val board = "................................................................b"
            val game = Game(1, 2, "black", board)
            for (x in 0..7){
                for (y in 0..7){
                    game.board[x][y] shouldBe null
                }
            }
        }

        it("Stores pieces in the correct place"){
            val board = "....W.........b...w...........................B.................b"
            val game = Game(1, 2, "black", board)
            game.board[0][4] shouldBe Piece("white", king = true)
            game.board[1][6] shouldBe Piece("black")
            game.board[2][2] shouldBe Piece("white")
            game.board[5][6] shouldBe Piece("black", king = true)
        }
    }

    describe("moveCheck"){
        val game = Game(1, 2, "black", "................................................................b")
        it("shows a surrounded piece cant move"){
            val board = Array(8){ Array<Piece?>(8){ null } }
            board[3][3] = Piece("white")
            board[4][2] = Piece("white")
            board[4][4] = Piece("white")
            board[2][2] = Piece("white")
            board[2][4] = Piece("white")

            game.moveCheck(3, 3, board, "white").isEmpty() shouldBe true
        }
        it("shows a normal piece can only move diagonally forward"){
            val board = Array(8){ Array<Piece?>(8){ null } }
            board[2][2] = Piece("black")
            board[6][6] = Piece("white")

            val bmoves = game.moveCheck(2, 2, board, "black")
            bmoves.any{ it.contentEquals(arrayOf(3, 1)) } shouldBe true
            bmoves.any{ it.contentEquals(arrayOf(3, 3)) } shouldBe true
            bmoves.any{ it.contentEquals(arrayOf(1, 3)) } shouldBe false
            bmoves.any{ it.contentEquals(arrayOf(1, 3)) } shouldBe false
            val wmoves = game.moveCheck(6, 6, board, "white")
            wmoves.any{ it.contentEquals(arrayOf(5, 5)) } shouldBe true
            wmoves.any{ it.contentEquals(arrayOf(5, 7)) } shouldBe true
            wmoves.any{ it.contentEquals(arrayOf(7, 5)) } shouldBe false
            wmoves.any{ it.contentEquals(arrayOf(7, 7)) } shouldBe false
        }
        it("shows a king piece can move in any diagonal direction"){
            val board = Array(8){ Array<Piece?>(8){ null } }
            board[2][2] = Piece("black", king = true)
            board[6][6] = Piece("white", king = true)

            val bmoves = game.moveCheck(2, 2, board, "black")
            bmoves.any{ it.contentEquals(arrayOf(3, 1)) } shouldBe true
            bmoves.any{ it.contentEquals(arrayOf(3, 3)) } shouldBe true
            bmoves.any{ it.contentEquals(arrayOf(1, 3)) } shouldBe true
            bmoves.any{ it.contentEquals(arrayOf(1, 3)) } shouldBe true
            val wmoves = game.moveCheck(6, 6, board, "white")
            wmoves.any{ it.contentEquals(arrayOf(5, 5)) } shouldBe true
            wmoves.any{ it.contentEquals(arrayOf(5, 7)) } shouldBe true
            wmoves.any{ it.contentEquals(arrayOf(7, 5)) } shouldBe true
            wmoves.any{ it.contentEquals(arrayOf(7, 7)) } shouldBe true
        }
        it("handles captures for a normal piece"){
            val board = Array(8) { Array<Piece?>(8) { null } }
            board[2][2] = Piece("black")
            board[3][3] = Piece("white")

            game.moveCheck(2, 2, board, "black").any{ it.contentEquals(arrayOf(4, 4)) } shouldBe true
        }
        it("handles captures for a king piece"){
            val board = Array(8) { Array<Piece?>(8) { null } }
            board[3][3] = Piece("black", king = true)
            board[2][2] = Piece("white")

            game.moveCheck(3, 3, board, "black").any{ it.contentEquals(arrayOf(1, 1)) } shouldBe true
        }
        it("takes into account if a piece is protecting another"){
            val board = Array(8) { Array<Piece?>(8) { null } }
            board[2][2] = Piece("black")
            board[3][3] = Piece("white")
            board[4][4] = Piece("white")

            game.moveCheck(2, 2, board, "black").any{ it.contentEquals(arrayOf(4, 4)) } shouldBe false
        }
    }

    describe("forcedCaptures"){
        it("returns false if no captures present"){
            val board = ".....................b..........................................b"
            val game = Game(1, 2, "black", board)
            game.forcedCaptures("black") shouldBe false
        }
        it("returns true if capture present"){
            val board = "..................b........w..........b.........................b"
            val game = Game(1, 2, "black", board)
            game.forcedCaptures("black") shouldBe true
        }
        it("no interruption by enemy piece") {
            val board = "..................w........b..........w.........................b"
            val game = Game(1, 2, "black", board)
            game.forcedCaptures("black") shouldBe false
}
    }

    describe("validateMove"){
        it("allows a piece to move forward"){
            val board = "............................b...................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 4), arrayOf(4, 3))
            game.validateMove(move, "black") shouldBe true
        }
        it("doesn't allow a normal piece to move backwards"){
            val board = "............................b...................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 4), arrayOf(2, 3))
            game.validateMove(move, "black") shouldBe false
        }
        it("allows a king piece to move backwards"){
            val board = "............................B...................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 4), arrayOf(2, 3))
            game.validateMove(move, "black") shouldBe true
        }
        it("allows a piece to capture forwards"){
            val board = "...................b......w.....................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(2, 3), arrayOf(4, 1))
            game.validateMove(move, "black") shouldBe true
        }
        it("doesn't allow a normal piece to capture backwards"){
            val board = "...................w......b.....................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 2), arrayOf(1, 4))
            game.validateMove(move, "black") shouldBe false
        }
        it("allows a king to capture backwards"){
            val board = "...................w......B.....................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 2), arrayOf(1, 4))
            game.validateMove(move, "black") shouldBe true
        }
        it("doesn't allow non-diagonal moves"){
            val board = "............................b...................................b"
            val game = Game(1, 2, "black", board)
            val move1 = mutableListOf(arrayOf(3, 4), arrayOf(3, 5))
            game.validateMove(move1, "black") shouldBe false
            val move2 = mutableListOf(arrayOf(3, 4), arrayOf(4, 4))
            game.validateMove(move2, "black") shouldBe false
        }
        it("accounts for multi-jumps"){
            val board = "......b......w.............w....................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(0, 6), arrayOf(2, 4), arrayOf(4, 2))
            game.validateMove(move, "black") shouldBe true
        }
        it("requires only captures for multi-jumps"){
            val board = "......b.........................................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(0, 6), arrayOf(1, 5), arrayOf(2, 4))
            game.validateMove(move, "black") shouldBe false
        }
        it("fails if capture still available on forcedCapture"){
            val board = "......b......w.............w....................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(0, 6), arrayOf(2, 4))
            game.validateMove(move, "black") shouldBe false
        }
        it("accounts for a kinging during a multi-jump"){
            val board = "..........................................b........w.w..........b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(5, 2), arrayOf(7, 4), arrayOf(5, 6))
            game.validateMove(move, "black") shouldBe true
        }
    }

    describe("makeMove"){
        it("handles regular moves"){
            val board = "............................b...................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 4), arrayOf(4, 3))
            game.makeMove(move)
            game.board[3][4] shouldBe null
            game.board[4][3]?.colour shouldBe "black"
        }
        it("swaps player after making a move"){
            val board = "............................b...................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 4), arrayOf(4, 3))
            game.makeMove(move)
            game.current shouldBe "white"
        }
        it("handles moves with capture"){
            val board = "...................b......w.....................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(2, 3), arrayOf(4, 1))
            game.makeMove(move)
            game.board[2][3] shouldBe null
            game.board[3][2] shouldBe null
            game.board[4][1]?.colour shouldBe "black"
        }
        it("handles moves with a kinging"){
            val board = "................................................b...............b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(6, 0), arrayOf(7, 1))
            game.makeMove(move)
            game.board[6][1] shouldBe null
            game.board[7][1]?.colour shouldBe "black"
            game.board[7][1]?.king shouldBe true
        }
        it("updates history with new board state"){
            val board = "............................b...................................b"
            val game = Game(1, 2, "black", board)
            val move = mutableListOf(arrayOf(3, 4), arrayOf(4, 3))
            game.makeMove(move)
            game.history[game.boardState] shouldBe 1
        }
        it("resets history on a capture"){
            val board = "...................b......w.....................................b"
            val game = Game(1, 2, "black", board)
            game.history["............................b...................................b"] = 2
            val move = mutableListOf(arrayOf(2, 3), arrayOf(4, 1))
            game.makeMove(move)
            game.history.getOrDefault("............................b...................................b", 0) shouldBe 0
        }
        it("resets history on a kinging"){
            val board = "................................................b...............b"
            val game = Game(1, 2, "black", board)
            game.history["............................b...................................b"] = 2
            val move = mutableListOf(arrayOf(6, 0), arrayOf(7, 1))
            game.makeMove(move)
            game.history.getOrDefault("............................b...................................b", 0) shouldBe 0
        }
    }

    describe("winCheck"){
        it("Accurately says if a player wins"){
            val bboard = "....................................W......W....................b"
            val bgame = Game(1, 2, "black", bboard)
            bgame.winCheck("black") shouldBe "white"
            bgame.winCheck("white") shouldBe ""
            val wboard = "....................................B......B....................w"
            val wgame = Game(1, 2, "white", wboard)
            wgame.winCheck("white") shouldBe "black"
            wgame.winCheck("black") shouldBe ""
        }
        it("Determines if theres been threefold repetition"){
            val board = "................................................................b"
            val game = Game(1, 2, "black", board)
            game.history[board] = 3
            game.winCheck("black") shouldBe "draw"
        }
        it("Says if theres no winner yet"){
            val board = "....W......................................................B....b"
            val game = Game(1, 2, "black", board)
            game.winCheck("black") shouldBe ""
        }
    }
})