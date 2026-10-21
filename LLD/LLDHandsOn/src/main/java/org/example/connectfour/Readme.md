Requirements:
    1. Two players take turns dropping discs into a
        7-column, 6-row board
    2. A disc falls to the lowest available row in the chosen column
    3. The game ends when:
        - A player gets four discs in a row
            (vertical, horizontal, or diagonal). They win.
        - The board is full. It's a draw.
    4. Invalid moves should be rejected clearly:
        - Dropping in a full column.
        - Moving out of turn.
        - Moving after the game is over.
    Out of scope:
        - UI support
        - Concurrent games
        - Move history
        - Undo
        - Board size configuration

Core Entities & Relationships
    1. Game
        - The orchestrator. Holds the Board, tracks which Player's turn it is,
          manages game state (in progress, won, draw), and enforces turn-based rules.
        - When a player makes a move, Game validates it, tells Board to place
          the disc, checks if that move won, and switches turns.
    2. Board
        - The 7x6 grid where discs live. Owns the grid state and handles disc placement.
          Knows how to check if a column is full, where a disc should fall, and whether four discs are connected. Doesn't care about whose turn it is or who's winning.
    3. Player	Represents a person in the game. Simple data holder with a name and disc color. No game logic here.