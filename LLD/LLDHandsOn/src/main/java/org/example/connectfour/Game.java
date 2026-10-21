
package org.example.connectfour;

public class Game {

    private final Board board;
    private final Player player1;
    private final Player player2;

    private Player currentPlayer;
    private GameState state;
    private Player winner;

    public Game(Player player1, Player player2) {
        if (player1 == null || player2 == null || player1 == player2) {
            throw new IllegalArgumentException("Two distinct players are required");
        }

        if (player1.getColor() == null || player2.getColor() == null || player1.getColor() == player2.getColor()) {
            throw new IllegalArgumentException("Players must have different, non-null colors");
        }

        this.board = new Board();
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        this.state = GameState.IN_PROGRESS;
        this.winner = null;
    }

    public boolean makeMove(Player player, int column) {
        if (state != GameState.IN_PROGRESS) {
            return false;
        }

        if (player == null || player != currentPlayer) {
            return false;
        }

        int row = board.placeDisc(column, player.getColor());
        if (row == -1) {
            return false;
        }

        if (board.checkWin(row, column, player.getColor())) {
            state = GameState.WON;
            winner = player;
        } else if (board.isFull()) {
            state = GameState.DRAW;
        } else {
            currentPlayer = (currentPlayer == player1)
                    ? player2
                    : player1;
        }

        return true;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return state;
    }

    public Player getWinner() {
        return winner;
    }

    public Board getBoard() {
        return board;
    }
}
