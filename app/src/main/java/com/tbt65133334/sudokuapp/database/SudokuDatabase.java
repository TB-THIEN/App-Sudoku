package com.tbt65133334.sudokuapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SudokuDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "sudoku.db";
    private static final int DB_VERSION = 1;

    // Bảng lưu đề bài
    public static final String TABLE_PUZZLES = "puzzles";
    public static final String COL_ID        = "id";
    public static final String COL_DIFFICULTY= "difficulty";  // 0=Dễ, 1=TB, 2=Khó
    public static final String COL_PUZZLE    = "puzzle";      // chuỗi 81 ký tự
    public static final String COL_SOLUTION  = "solution";    // chuỗi 81 ký tự đáp án

    // Bảng lưu điểm cao nhất
    public static final String TABLE_STATS   = "stats";
    public static final String COL_DIFF_KEY  = "difficulty";
    public static final String COL_BEST_SCORE= "best_score";
    public static final String COL_BEST_TIME = "best_time";   // giây
    public static final String COL_BEST_HINTS= "best_hints";  // số gợi ý dùng khi đạt best

    public SudokuDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng đề bài
        db.execSQL("CREATE TABLE " + TABLE_PUZZLES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DIFFICULTY + " INTEGER, " +
                COL_PUZZLE + " TEXT, " +
                COL_SOLUTION + " TEXT)");

        // Tạo bảng thống kê
        db.execSQL("CREATE TABLE " + TABLE_STATS + " (" +
                COL_DIFF_KEY + " INTEGER PRIMARY KEY, " +
                COL_BEST_SCORE + " INTEGER DEFAULT 0, " +
                COL_BEST_TIME + " INTEGER DEFAULT 0, " +
                COL_BEST_HINTS + " INTEGER DEFAULT 0)");

        // Khởi tạo hàng mặc định cho 3 chế độ
        for (int i = 0; i < 3; i++) {
            ContentValues cv = new ContentValues();
            cv.put(COL_DIFF_KEY, i);
            cv.put(COL_BEST_SCORE, 0);
            cv.put(COL_BEST_TIME, 0);
            cv.put(COL_BEST_HINTS, 0);
            db.insert(TABLE_STATS, null, cv);
        }

        // Chèn sẵn đề bài vào DB
        preloadPuzzles(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PUZZLES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS);
        onCreate(db);
    }

    public String[] getRandomPuzzle(int difficulty) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PUZZLES, new String[]{COL_PUZZLE, COL_SOLUTION},
                COL_DIFFICULTY + "=?", new String[]{String.valueOf(difficulty)},
                null, null, "RANDOM()", "1");
        String[] result = null;
        if (cursor.moveToFirst()) {
            result = new String[]{cursor.getString(0), cursor.getString(1)};
        }
        cursor.close();
        return result;
    }

    public void updateBestScore(int difficulty, int score, int timeSeconds, int hints) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_STATS, new String[]{COL_BEST_SCORE},
                COL_DIFF_KEY + "=?", new String[]{String.valueOf(difficulty)},
                null, null, null);
        int currentBest = 0;
        if (cursor.moveToFirst()) currentBest = cursor.getInt(0);
        cursor.close();

        if (score > currentBest) {
            ContentValues cv = new ContentValues();
            cv.put(COL_BEST_SCORE, score);
            cv.put(COL_BEST_TIME, timeSeconds);
            cv.put(COL_BEST_HINTS, hints);
            db.update(TABLE_STATS, cv, COL_DIFF_KEY + "=?",
                    new String[]{String.valueOf(difficulty)});
        }
    }

    public int[] getStats(int difficulty) {
        // trả về [bestScore, bestTime, bestHints]
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATS,
                new String[]{COL_BEST_SCORE, COL_BEST_TIME, COL_BEST_HINTS},
                COL_DIFF_KEY + "=?", new String[]{String.valueOf(difficulty)},
                null, null, null);
        int[] result = {0, 0, 0};
        if (cursor.moveToFirst()) {
            result[0] = cursor.getInt(0);
            result[1] = cursor.getInt(1);
            result[2] = cursor.getInt(2);
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

    private void preloadPuzzles(SQLiteDatabase db) {

        // ─── DỄ nhiều ô cho sẵn ───
        String[][] easy = {
                {
                        "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
                        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
                },
                //thêm đề
        };
        for (String[] e : easy) insertPuzzleRaw(db, 0, e[0], e[1]);

        // ─── TRUNG BÌNH ───
        String[][] medium = {
                {
                        "200080300060070084030500209000105408000000000402706000301007040720040060004010003",
                        "214986357967273184538541279673125498849397612452768931321657849785439526194812763"
                },
        };
        for (String[] m : medium) insertPuzzleRaw(db, 1, m[0], m[1]);

        // ─── KHÓ ít ô cho sẵn ───
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
    // Thêm vào trước dấu } cuối cùng của class
    public int getPuzzleCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PUZZLES, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }
}
