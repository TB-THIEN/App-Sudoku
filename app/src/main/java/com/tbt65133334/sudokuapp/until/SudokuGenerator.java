package com.tbt65133334.sudokuapp.until;

import java.util.Random;
public class SudokuGenerator {

    private static final Random RND = new Random();
    public static int[][][] generate(int difficulty) {
        int[][] solution = new int[9][9];
        fillGrid(solution);

        int[][] puzzle = copyGrid(solution);
        int removals = difficulty == 0 ? 35 : difficulty == 1 ? 45 : 55;
        removeNumbers(puzzle, removals);

        return new int[][][]{puzzle, solution};
    }

    // Điền lưới hoàn chỉnh bằng backtracking + random
    private static boolean fillGrid(int[][] grid) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (grid[r][c] == 0) {
                    int[] nums = shuffled();
                    for (int num : nums) {
                        if (isValid(grid, r, c, num)) {
                            grid[r][c] = num;
                            if (fillGrid(grid)) return true;
                            grid[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    // Xóa ngẫu nhiên các ô
    private static void removeNumbers(int[][] grid, int count) {
        while (count > 0) {
            int r = RND.nextInt(9), c = RND.nextInt(9);
            if (grid[r][c] != 0) {
                grid[r][c] = 0;
                count--;
            }
        }
    }

    public static boolean isValid(int[][] grid, int row, int col, int num) {
        // Kiểm tra hàng
        for (int c = 0; c < 9; c++)
            if (c != col && grid[row][c] == num) return false;
        // Kiểm tra cột
        for (int r = 0; r < 9; r++)
            if (r != row && grid[r][col] == num) return false;
        // Kiểm tra vùng 3x3
        int br = (row / 3) * 3, bc = (col / 3) * 3;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if ((r != row || c != col) && grid[r][c] == num) return false;
        return true;
    }

    private static int[] shuffled() {
        int[] arr = {1,2,3,4,5,6,7,8,9};
        for (int i = 8; i > 0; i--) {
            int j = RND.nextInt(i + 1);
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return arr;
    }

    private static int[][] copyGrid(int[][] src) {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++) System.arraycopy(src[r], 0, copy[r], 0, 9);
        return copy;
    }
}