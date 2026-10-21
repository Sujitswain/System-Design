
package org.example.connectfour;

public class Board {

    private final int rows;
    private final int cols;
    private final DiscColor[][] grid;

    private final int[][] directions = {
            {0, 1},   // Horizontal
            {1, 0},   // Vertical
            {1, 1},   // Diagonal down-right
            {-1, 1}   // Diagonal up-right
    };

    public Board() {
        this.rows = 6;
        this.cols = 7;
        this.grid = new DiscColor[rows][cols];
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean canPlace(int column) {
        if (column < 0 || column >= cols) {
            return false;
        }

        return grid[0][column] == null;
    }

    public int placeDisc(int column, DiscColor color) {
        if (!canPlace(column)) {
            return -1;
        }

        // Start at the bottom and move upward.
        for (int row = rows - 1; row >= 0; row--) {
            if (grid[row][column] == null) {
                grid[row][column] = color;
                return row;
            }
        }

        return -1;
    }

    public boolean isFull() {
        for (int column = 0; column < cols; column++) {
            if (canPlace(column)) {
                return false;
            }
        }

        return true;
    }

    public boolean checkWin(int row, int column, DiscColor color) {
        if (!inBounds(row, column))
            return false;

        for (int[] dir : directions) {
            int dr = dir[0];
            int dc = dir[1];

            int count = 1;
            count += countInDirection(row, column, dr, dc, color);
            count += countInDirection(row, column, -dr, -dc, color);

            if (count >= 4) {
                return true;
            }
        }
        return false;
    }

    private int countInDirection(int row, int column, int dr, int dc, DiscColor color) {

        int count = 0;
        int r = row + dr;
        int c = column + dc;
        while (inBounds(r, c) && grid[r][c] == color) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    private boolean inBounds(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < cols;
    }

    public void printBoard() {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < cols; column++) {
                DiscColor color = grid[row][column];

                String cell = color == null
                        ? " . "
                        : color == DiscColor.RED ? " R " : " Y ";

                System.out.print(cell);
            }

            System.out.println();
        }

        System.out.println(" 0  1  2  3  4  5  6");
    }
}
