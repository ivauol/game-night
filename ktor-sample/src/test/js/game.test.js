require("./game.js");

beforeEach(() => {
    document.body.innerHTML = `
        <table class="checkers-board">
            <tr>
                <td></td>
                ${Array(8).fill('<td></td>').join("")}
            </tr>
            ${Array(8).fill(`
                <tr>
                    <td></td>
                    ${Array(8).fill('<td class="square"></td>').join("")}
                </tr>
            `).join("")}
        </table>

        <div id="movePreview"></div>

        <button id="submitMoveBtn"></button>
        <button id="clearMoveBtn"></button>

        <p id="errorBox"></p>

        <div id="blackId"></div>
        <div id="whiteId"></div>
    `;

    window.squares = [];
    window.player = "white";
    window.current = "white";
    window.winner = "";
    window.gameId = "test";
    global.fetch = jest.fn();
});

describe("buttonVisible", () => {
    test("Show buttons when on active game on player's turn", () =>{
        window.current = "white";
        window.player = "white";
        window.winner = "";

        buttonVisible();
        expect(document.getElementById("submitMoveBtn").style.display).toBe("inline-block");
        expect(document.getElementById("clearMoveBtn").style.display).toBe("inline-block");
    });
    test("Don't show buttons when on ended game", () =>{
        window.current = "white";
        window.player = "white";
        window.winner = "draw";

        buttonVisible();
        expect(document.getElementById("submitMoveBtn").style.display).toBe("none");
        expect(document.getElementById("clearMoveBtn").style.display).toBe("none");
    });
    test("Don't show buttons when not player's turn", () =>{
        window.current = "white";
        window.player = "black";
        window.winner = "";

        buttonVisible();
        expect(document.getElementById("submitMoveBtn").style.display).toBe("none");
        expect(document.getElementById("clearMoveBtn").style.display).toBe("none");
    });
});

describe("previewMove", () => {
    test("No squares selected leads to no move displayed", () => {
        window.squares = [];
        
        previewMove();
        expect(document.getElementById("movePreview").innerText).toBe("");
    });
    test("Squares selected leads to correct move display", () => {
        window.squares = [11, 33, 55];
        
        previewMove();
        expect(document.getElementById("movePreview").innerText).toBe("A1-C3-E5");
    });
});

describe("parseBoardString", () => {
    test("returns a correct board", () => {
        const testBoard = ".b..........W..........B......................w.................b";
        const tested = parseBoardString(testBoard);
        
        expect(tested[0][1]).toBe("b");
        expect(tested[5][6]).toBe("w");
        expect(tested[2][7]).toBe("B");
        expect(tested[1][4]).toBe("W");
    });
});

describe("updateBoardHTML", () => {
    test("returns a correct board", () => {
        const testBoard = ".b..........W..........B......................w.................b";
        const tested = parseBoardString(testBoard);
        updateBoardHTML(tested);
        const table = document.querySelector(".checkers-board");
        
        expect(table.rows[1].cells[1].innerHTML).toBe('');
        expect(table.rows[1].cells[2].innerHTML).toBe('<div class="piece black-piece"></div>');
        expect(table.rows[6].cells[7].innerHTML).toBe('<div class="piece white-piece"></div>');
        expect(table.rows[3].cells[8].innerHTML).toBe('<div class="piece black-piece king"></div>');
        expect(table.rows[2].cells[5].innerHTML).toBe('<div class="piece white-piece king"></div>');
    });
});

describe("currentPlayer", () => {
    test("highlights black on black turn", () => {
        currentPlayer("black");

        expect(document.getElementById("blackId").style.fontWeight).toBe("bold");
        expect(document.getElementById("whiteId").style.fontWeight).toBe("normal");
    });
    test("highlights white on white turn", () => {
        currentPlayer("white");

        expect(document.getElementById("whiteId").style.fontWeight).toBe("bold");
        expect(document.getElementById("blackId").style.fontWeight).toBe("normal");
    });
});

describe("winCheck", () => {
    test("No win block made if winner is empty", () => {
        const result = winCheck("");

        expect(result).toBe("");
        expect(document.body.querySelector(".win-message")).toBeNull();
    });
    test("Appropriate draw message when draw", () => {
        const result = winCheck("draw");

        expect(result).toBe("draw");
        expect(document.body.querySelector(".win-message").innerText).toBe("Draw!");
    });
    test("Appropriate win message when win", () => {
        const result = winCheck("black");

        expect(result).toBe("black");
        expect(document.body.querySelector(".win-message").innerText).toBe("black wins!");
    });
});

describe("handleSquareClick", () => {
    test("Nothing happens if it isn't your turn", () => {
        window.player = "black";
        window.current = "white";
        window.winner = "";
        updateBoardHTML(parseBoardString(".b..........W..........B......................w.................b"));
        const table = document.querySelector(".checkers-board");

        handleSquareClick(0, 1, table.rows[1].cells[2]);
        expect(window.squares.length).toBe(0);
    });
    test("Nothing happens if the game has ended", () => {
        window.player = "black";
        window.current = "black";
        window.winner = "white";
        updateBoardHTML(parseBoardString(".b..........W..........B......................w.................b"));
        const table = document.querySelector(".checkers-board");

        handleSquareClick(0, 1, table.rows[1].cells[2]);
        expect(window.squares.length).toBe(0);
    });
    test("Must start by picking a piece which belongs to you", () => {
        window.player = "black";
        window.current = "black";
        window.winner = "";
        updateBoardHTML(parseBoardString(".b..........W..........B......................w.................b"));
        const table = document.querySelector(".checkers-board");

        handleSquareClick(0, 3, table.rows[1].cells[4]);
        expect(window.squares.length).toBe(0);
    });
    test("Valid squares are highlighted corrected", () => {
        window.player = "black";
        window.current = "black";
        window.winner = "";
        updateBoardHTML(parseBoardString(".b..........W..........B......................w.................b"));
        const table = document.querySelector(".checkers-board");

        handleSquareClick(0, 1, table.rows[1].cells[2]);
        expect(window.squares.length).toBe(1);
        expect(table.rows[1].cells[2].style.outline).toBe("3px solid yellow");
    });
    test("Can't add the same square twice in a row", () => {
        window.player = "black";
        window.current = "black";
        window.winner = "";
        window.squares = [12];
        updateBoardHTML(parseBoardString(".b..........W..........B......................w.................b"));
        const table = document.querySelector(".checkers-board");

        handleSquareClick(0, 1, table.rows[1].cells[2]);
        expect(window.squares.length).toBe(1);
    });
});

describe("submitMove", () => {
    test("does nothing if less than 2 squares selected", async () => {
        window.squares = [11];
        await submitMove();
        expect(fetch).not.toHaveBeenCalled();
    });
    test("clears error box on successful move", async () => {
        window.squares = [11, 22];
        fetch.mockResolvedValue({
            ok: true,
            text: async () => ""
        });
        await submitMove();
        expect(document.getElementById("errorBox").innerText).toBe("");
    });
    test("shows error message when request fails", async () => {
        window.squares = [11, 22];
        fetch.mockResolvedValue({
            ok: false,
            text: async () => "Invalid move"
        });
        await submitMove();
        expect(document.getElementById("errorBox").innerText).toBe("Invalid move");
    });
});

describe("clearSelection", () => {
    test("Clears squares correctly", () => {
        window.player = "black";
        window.current = "black";
        window.winner = "";
        updateBoardHTML(parseBoardString(".b..........W..........B......................w.................b"));
        const table = document.querySelector(".checkers-board");
        handleSquareClick(0, 1, table.rows[1].cells[2]);

        clearSelection();
        expect(window.squares.length).toBe(0);
        expect(table.rows[1].cells[2].style.outline).toBe("none");
        expect(document.getElementById("movePreview").innerText).toBe("");
    });
});