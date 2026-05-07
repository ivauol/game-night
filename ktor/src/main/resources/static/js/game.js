//show the submit move and clear buttons if its the players turn and the game isnt over
function buttonVisible(){
    const submit = document.getElementById("submitMoveBtn");
    const clear = document.getElementById("clearMoveBtn");

    if (window.winner === "" && window.player === window.current) {
        submit.style.display = "inline-block";
        clear.style.display = "inline-block";
    } else {
        submit.style.display = "none";
        clear.style.display = "none";
    }
}

//show the player what the current move they are attempting to make is
function previewMove(){
    const preview = document.getElementById("movePreview");

    if (window.squares.length === 0) {
        preview.innerText = "";
        return;
    }

    let parts = [];

    //convert all selected squares into proper notation
    for (let square of window.squares) {
        const row = Math.floor(square / 10);
        const col = (square % 10) - 1;

        const letter = String.fromCharCode("A".charCodeAt(0) + col);
        const number = row;

        parts.push(letter + number);
    }

    preview.innerText = parts.join("-");
}

//turn the board string into a 2D board
function parseBoardString(str) {
    const board = [];
    for (let i = 0; i < 8; i++) {
        const row = [];
        for (let j = 0; j < 8; j++) {
            row.push(str[i*8 + j]);
        }
        board.push(row);
    }
    return board;
}

//fill the board with pieces on squares
function updateBoardHTML(board) {
    const table = document.querySelector(".checkers-board");
    for (let i = 0; i < 8; i++) {
        for (let j = 0; j < 8; j++) {
            const cell = table.rows[i + 1].cells[j + 1];
            const piece = board[i][j];
            if (piece === "w") {
                cell.innerHTML = '<div class="piece white-piece"></div>';
            } else if (piece === "b") {
                cell.innerHTML = '<div class="piece black-piece"></div>';
            } else if (piece === "W") {
                cell.innerHTML = '<div class="piece white-piece king"></div>';
            } else if (piece === "B") {
                cell.innerHTML = '<div class="piece black-piece king"></div>';
            } else {
                cell.innerHTML = "";
            }
        }
    }
}

//highlight whos turn it is
function currentPlayer() {
    let turn = document.getElementById("turn");

    if (window.player == window.current){
        turn.innerText = "It is your turn";
    }
    else{
        turn.innerText = "It is opponent's turn";
    }
}

//check for win
function winCheck(winner) {
    if (winner !== "") {

        const winnerDiv = document.createElement("div");
        winnerDiv.className = "win-message";
        winnerDiv.style.textAlign = "center";
        winnerDiv.style.fontSize = "24px";
        winnerDiv.style.marginTop = "20px";
        if (winner == "draw"){
            winnerDiv.innerText = "Draw!";
        }
        else{
            winnerDiv.innerText = winner + " wins!";
        }
        document.body.appendChild(winnerDiv);

        return winner;
    }
    return "";
}

//if a square is clicked for a movement option
function handleSquareClick(row, col, cell) {
    //prevent duplicate last click
    const last = window.squares[window.squares.length - 1];
    if (last === row*10 + col + 11) return;

    //if it isnt the players turn to move or the game is over
    if (window.player != window.current || window.winner != "") return;

    //if the players first selected square isn't one of their pieces
    if (window.squares.length === 0){
        if (window.player === "white"){
            if (cell.innerHTML !== '<div class="piece white-piece"></div>' && cell.innerHTML !== '<div class="piece white-piece king"></div>'){
                return;
            }
        }
        else if (window.player === "black"){
            if (cell.innerHTML !== '<div class="piece black-piece"></div>' && cell.innerHTML !== '<div class="piece black-piece king"></div>'){
                return;
            }
        }
    }

    window.squares.push(row*10 + col + 11);

    cell.style.outline = "3px solid yellow";
    previewMove();
}

//submit the move
async function submitMove() {
    if (window.squares.length < 2) return;

    const formData = new FormData();
    formData.append("gameId", window.gameId);
    formData.append("move", window.squares.join(","));

    const response = await fetch("/move", {
        method: "POST",
        body: formData
    });
    clearSelection();
    const errorBox = document.getElementById("errorBox");

    if (!response.ok) {
        errorBox.innerText = await response.text();
    } else {
        errorBox.innerText = "";
    }
}

//clear the current move
function clearSelection() {
    window.squares = [];

    document.querySelectorAll(".square").forEach(cell => {
        cell.style.outline = "none";
    });
    previewMove();
}