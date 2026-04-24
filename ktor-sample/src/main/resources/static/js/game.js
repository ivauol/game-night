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

function previewMove(){
    const preview = document.getElementById("movePreview");

    if (window.squares.length === 0) {
        preview.innerText = "";
        return;
    }

    let parts = [];

    for (let square of window.squares) {
        const row = Math.floor(square / 10);
        const col = square % 10;

        const letter = String.fromCharCode("A".charCodeAt(0) + col);
        const number = row + 1;

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
function currentPlayer(turn) {
    const black = document.getElementById("blackId");
    const white = document.getElementById("whiteId");

    if (turn === "black") {
        black.style.fontWeight = "bold";
        white.style.fontWeight = "normal";
    } else {
        white.style.fontWeight = "bold";
        black.style.fontWeight = "normal";
    }
}

//check for win
function winCheck(player) {
    if (player !== "") {

        const winnerDiv = document.createElement("div");
        winnerDiv.style.textAlign = "center";
        winnerDiv.style.fontSize = "24px";
        winnerDiv.style.marginTop = "20px";
        winnerDiv.innerText = player + " wins!";
        document.body.appendChild(winnerDiv);
        return player;
    }
}

function handleSquareClick(row, col, cell) {
    // prevent duplicate last click
    const last = window.squares[window.squares.length - 1];
    if (last === row*10 + col) return;

    if (window.player != window.current || window.winner != "") return;

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

    window.squares.push(row*10 + col);

    // visual feedback
    cell.style.outline = "3px solid yellow";
    previewMove();

    console.log("Squares:", window.squares);
}

async function submitMove() {
    if (window.squares.length < 2) return;

    const formData = new FormData();
    formData.append("token", window.token);
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

function clearSelection() {
    window.squares = [];

    document.querySelectorAll(".square").forEach(cell => {
        cell.style.outline = "none";
    });
    previewMove();
}