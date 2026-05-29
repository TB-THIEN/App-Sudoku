package com.tbt65133334.sudokuapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tbt65133334.sudokuapp.model.GameStats;

import java.util.ArrayList;
import java.util.List;

public class SudokuDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "sudoku.db";
    private static final int    DB_VERSION = 2;          // tăng lên 2 để trigger onUpgrade

    // ── Bảng đề bài ──────────────────────────────────────────────────────────
    public static final String TABLE_PUZZLES  = "puzzles";
    public static final String COL_ID         = "id";
    public static final String COL_DIFFICULTY = "difficulty";
    public static final String COL_PUZZLE     = "puzzle";
    public static final String COL_SOLUTION   = "solution";

    // ── Bảng thành tích theo username ─────────────────────────────────────────
    // Primary key: (username, difficulty)  → mỗi user có 3 hàng (dễ/tb/khó)
    public static final String TABLE_STATS    = "stats";
    public static final String COL_USERNAME   = "username";
    public static final String COL_DIFF_KEY   = "difficulty";
    public static final String COL_BEST_SCORE = "best_score";
    public static final String COL_BEST_TIME  = "best_time";
    public static final String COL_BEST_HINTS = "best_hints";

    public SudokuDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Bảng đề bài
        db.execSQL("CREATE TABLE " + TABLE_PUZZLES + " (" +
                COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DIFFICULTY + " INTEGER, " +
                COL_PUZZLE     + " TEXT, " +
                COL_SOLUTION   + " TEXT)");

        // Bảng thành tích: khóa chính ghép (username, difficulty)
        db.execSQL("CREATE TABLE " + TABLE_STATS + " (" +
                COL_USERNAME   + " TEXT NOT NULL, " +
                COL_DIFF_KEY   + " INTEGER NOT NULL, " +
                COL_BEST_SCORE + " INTEGER DEFAULT 0, " +
                COL_BEST_TIME  + " INTEGER DEFAULT 0, " +
                COL_BEST_HINTS + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COL_USERNAME + ", " + COL_DIFF_KEY + "))");

        preloadPuzzles(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PUZZLES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS);
        onCreate(db);
    }

    // ── Đề bài ───────────────────────────────────────────────────────────────

    public String[] getRandomPuzzle(int difficulty) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PUZZLES,
                new String[]{COL_PUZZLE, COL_SOLUTION},
                COL_DIFFICULTY + "=?", new String[]{String.valueOf(difficulty)},
                null, null, "RANDOM()", "1");
        String[] result = null;
        if (cursor.moveToFirst()) {
            result = new String[]{cursor.getString(0), cursor.getString(1)};
        }
        cursor.close();
        return result;
    }

    public void insertPuzzle(int difficulty, String puzzle, String solution) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DIFFICULTY, difficulty);
        cv.put(COL_PUZZLE, puzzle);
        cv.put(COL_SOLUTION, solution);
        db.insert(TABLE_PUZZLES, null, cv);
    }

    public int getPuzzleCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PUZZLES, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // ── Thành tích theo username ──────────────────────────────────────────────

    /**
     * Cập nhật điểm cao nếu score mới > điểm hiện tại.
     * Nếu chưa có hàng cho (username, difficulty) thì tự động tạo mới.
     */
    public void updateBestScore(String username, int difficulty,
                                int score, int timeSeconds, int hints) {
        SQLiteDatabase db = getWritableDatabase();

        // Đọc điểm hiện tại
        Cursor cursor = db.query(TABLE_STATS,
                new String[]{COL_BEST_SCORE},
                COL_USERNAME + "=? AND " + COL_DIFF_KEY + "=?",
                new String[]{username, String.valueOf(difficulty)},
                null, null, null);

        if (!cursor.moveToFirst()) {
            // Chưa có hàng → INSERT
            cursor.close();
            ContentValues cv = new ContentValues();
            cv.put(COL_USERNAME,   username);
            cv.put(COL_DIFF_KEY,   difficulty);
            cv.put(COL_BEST_SCORE, score);
            cv.put(COL_BEST_TIME,  timeSeconds);
            cv.put(COL_BEST_HINTS, hints);
            db.insert(TABLE_STATS, null, cv);
        } else {
            int currentBest = cursor.getInt(0);
            cursor.close();
            if (score > currentBest) {
                ContentValues cv = new ContentValues();
                cv.put(COL_BEST_SCORE, score);
                cv.put(COL_BEST_TIME,  timeSeconds);
                cv.put(COL_BEST_HINTS, hints);
                db.update(TABLE_STATS, cv,
                        COL_USERNAME + "=? AND " + COL_DIFF_KEY + "=?",
                        new String[]{username, String.valueOf(difficulty)});
            }
        }
    }

    /**
     * Trả về GameStats của một user + độ khó.
     * Nếu chưa có thành tích → trả về object với tất cả = 0.
     */
    public GameStats getStats(String username, int difficulty) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATS,
                new String[]{COL_BEST_SCORE, COL_BEST_TIME, COL_BEST_HINTS},
                COL_USERNAME + "=? AND " + COL_DIFF_KEY + "=?",
                new String[]{username, String.valueOf(difficulty)},
                null, null, null);

        GameStats stats = new GameStats(username, difficulty, 0, 0, 0);
        if (cursor.moveToFirst()) {
            stats.setBestScore(cursor.getInt(0));
            stats.setBestTime(cursor.getInt(1));
            stats.setBestHints(cursor.getInt(2));
        }
        cursor.close();
        return stats;
    }

    /**
     * Trả về thành tích của user cho cả 3 độ khó (index 0/1/2).
     */
    public List<GameStats> getAllStats(String username) {
        List<GameStats> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(getStats(username, i));
        }
        return list;
    }

    // ── Preload đề bài ────────────────────────────────────────────────────────

    private void preloadPuzzles(SQLiteDatabase db) {
        String[][] easy = {
                {
                        "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
                        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
                },
        };
        for (String[] e : easy) insertPuzzleRaw(db, 0, e[0], e[1]);

        String[][] medium = {
                {
                        "200080300060070084030500209000105408000000000402706000301007040720040060004010003",
                        "214986357967273184538541279673125498849397612452768931321657849785439526194812763"
                },
        };
        for (String[] m : medium) insertPuzzleRaw(db, 1, m[0], m[1]);

        String[][] hard = {
                {
                        "800000000003600000070090200060005030004002000030073000002000050000800400005674000",
                        "812753649943682175675491283168945732794236518235817964329164857486578329557829416"
                },
        };
        for (String[] h : hard) insertPuzzleRaw(db, 2, h[0], h[1]);
    }

    private void insertPuzzleRaw(SQLiteDatabase db, int difficulty, String puzzle, String solution) {
        ContentValues cv = new ContentValues();
        cv.put(COL_DIFFICULTY, difficulty);
        cv.put(COL_PUZZLE, puzzle);
        cv.put(COL_SOLUTION, solution);
        db.insert(TABLE_PUZZLES, null, cv);
    }
}