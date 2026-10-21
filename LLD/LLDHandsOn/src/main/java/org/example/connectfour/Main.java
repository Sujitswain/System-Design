
package org.example.connectfour;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Player player1 = new Player("Alice", DiscColor.RED);
        Player player2 = new Player("Bob", DiscColor.YELLOW);

        Game game = new Game(player1, player2);
        Scanner scanner = new Scanner(System.in);

        while (game.getGameState() == GameState.IN_PROGRESS) {
            Player current = game.getCurrentPlayer();

            game.getBoard().printBoard();
            System.out.println(current.getName() + ", choose a column (0-6):");

            int column = scanner.nextInt();
            boolean success = game.makeMove(current, column);

            if (!success) {
                System.out.println("Invalid move! Try again.");
                continue;
            }
            if (game.getGameState() == GameState.WON) {
                game.getBoard().printBoard();
                System.out.println("Winner: " + game.getWinner().getName());
            } else if (game.getGameState() == GameState.DRAW) {
                game.getBoard().printBoard();
                System.out.println("The game is a draw!");
            }
        }

        scanner.close();
    }
}
