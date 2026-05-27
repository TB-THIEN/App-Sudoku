package com.tbt65133334.sudokuapp;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tbt65133334.sudokuapp.ui.HomeFragment;
import com.tbt65133334.sudokuapp.database.SudokuDatabase;
import com.tbt65133334.sudokuapp.until.SudokuGenerator; // Lưu ý: nếu tên thư mục là 'util' thì bạn sửa lại chữ 'until' này nhé

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatabase();

        testFirebase();

        if (savedInstanceState == null) {
            navigateTo(new HomeFragment(), false);
        }
    }

    private void testFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        myRef.setValue("Hello, World!");


        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void initDatabase() {
        SudokuDatabase db = new SudokuDatabase(this);
        if (db.getPuzzleCount() == 0) {
            new Thread(() -> {
                SQLiteDatabase writeableDb = db.getWritableDatabase();
                writeableDb.beginTransaction();
                try {
                    for (int difficulty = 0; difficulty < 3; difficulty++) {
                        for (int i = 0; i < 10; i++) {
                            int[][][] gen = SudokuGenerator.generate(difficulty);
                            String puzzle   = gridToString(gen[0]);
                            String solution = gridToString(gen[1]);

                            db.insertPuzzle(difficulty, puzzle, solution);
                        }
                    }
                    writeableDb.setTransactionSuccessful();
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing database", e);
                } finally {
                    writeableDb.endTransaction();
                }
            }).start();
        }
    }

    private String gridToString(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                sb.append(grid[r][c]);
        return sb.toString();
    }

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