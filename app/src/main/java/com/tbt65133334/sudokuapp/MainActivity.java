package com.tbt65133334.sudokuapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

// ← Giữ nguyên import cũ của bạn
import com.tbt65133334.sudokuapp.ui.HomeFragment;

// ← Thêm 2 import mới
import com.tbt65133334.sudokuapp.database.SudokuDatabase;
import com.tbt65133334.sudokuapp.until.SudokuGenerator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ← Thêm dòng này
        initDatabase();

        if (savedInstanceState == null) {
            navigateTo(new HomeFragment(), false);
        }
    }

    // ← Thêm hàm này
    private void initDatabase() {
        SudokuDatabase db = new SudokuDatabase(this);
        if (db.getPuzzleCount() == 0) {
            new Thread(() -> {
                for (int difficulty = 0; difficulty < 3; difficulty++) {
                    for (int i = 0; i < 5; i++) {
                        int[][][] gen = SudokuGenerator.generate(difficulty);
                        String puzzle   = gridToString(gen[0]);
                        String solution = gridToString(gen[1]);
                        db.insertPuzzle(difficulty, puzzle, solution);
                    }
                }
            }).start();
        }
    }

    // ← Thêm hàm này
    private String gridToString(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                sb.append(grid[r][c]);
        return sb.toString();
    }

    // ← Giữ nguyên 2 hàm cũ, không đổi gì
    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        var tx = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }

    public void goHome() {
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        navigateTo(new HomeFragment(), false);
    }

}