package com.tbt65133334.sudokuapp.model;

public class GameStats {
    private String username;
    private int    difficulty;   // 0=Dễ, 1=TB, 2=Khó
    private int    bestScore;
    private int    bestTime;     // giây
    private int    bestHints;    // số gợi ý dùng khi đạt best

    public GameStats() {}

    public GameStats(String username, int difficulty, int bestScore, int bestTime, int bestHints) {
        this.username   = username;
        this.difficulty = difficulty;
        this.bestScore  = bestScore;
        this.bestTime   = bestTime;
        this.bestHints  = bestHints;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getUsername()   { return username; }
    public int    getDifficulty() { return difficulty; }
    public int    getBestScore()  { return bestScore; }
    public int    getBestTime()   { return bestTime; }
    public int    getBestHints()  { return bestHints; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setUsername(String username)     { this.username   = username; }
    public void setDifficulty(int difficulty)    { this.difficulty = difficulty; }
    public void setBestScore(int bestScore)      { this.bestScore  = bestScore; }
    public void setBestTime(int bestTime)        { this.bestTime   = bestTime; }
    public void setBestHints(int bestHints)      { this.bestHints  = bestHints; }

    // ── Helper: tên độ khó dạng chuỗi ────────────────────────────────────────
    public String getDifficultyLabel() {
        switch (difficulty) {
            case 0:  return "Dễ";
            case 1:  return "Trung bình";
            case 2:  return "Khó";
            default: return "Không xác định";
        }
    }

    // ── Helper: thời gian dạng mm:ss ─────────────────────────────────────────
    public String getBestTimeFormatted() {
        if (bestTime == 0) return "–";
        return String.format("%02d:%02d", bestTime / 60, bestTime % 60);
    }
}