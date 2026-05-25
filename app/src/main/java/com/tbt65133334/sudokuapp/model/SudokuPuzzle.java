package com.tbt65133334.sudokuapp.model;

public class SudokuPuzzle {
    private int[][] puzzle;    // bản đề
    private int[][] solution;  // đáp án hoàn chỉnh
    private int difficulty;    // 0=Dễ, 1=TB, 2=Khó

    public SudokuPuzzle(String puzzleStr, String solutionStr, int difficulty) {
        this.difficulty = difficulty;
        this.puzzle   = stringToGrid(puzzleStr);
        this.solution = stringToGrid(solutionStr);
    }

    private int[][] stringToGrid(String s) {
        int[][] grid = new int[9][9];
        for (int i = 0; i < 81; i++) {
            grid[i / 9][i % 9] = s.charAt(i) - '0';
        }
        return grid;
    }

    public int[][] getPuzzle()    { return puzzle; }
    public int[][] getSolution()  { return solution; }
    public int getDifficulty()    { return difficulty; }

    // Kiểm tra ô có phải ô đề 
    public boolean isGiven(int row, int col) {
        return puzzle[row][col] != 0;
    }
}