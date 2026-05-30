package com.tbt65133334.sudokuapp.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;
import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;
import com.tbt65133334.sudokuapp.database.SudokuDatabase;
import com.tbt65133334.sudokuapp.model.SudokuPuzzle;
import com.tbt65133334.sudokuapp.view.SudokuBoardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameFragment extends Fragment {

    private static final String TAG            = "GameFragment";
    private static final int    MAX_HINTS_EASY = 5;
    private static final int    MAX_HINTS_MED  = 3;
    private static final int    MAX_HINTS_HARD = 2;
    private static final int    BASE_EASY      = 1000;
    private static final int    BASE_MED       = 2000;
    private static final int    BASE_HARD      = 3000;
    private static final int    MAX_CHECKS     = 10;
    private static final int    TOTAL_PUZZLES  = 10;

    private SudokuBoardView boardView;
    private Chronometer     chronometer;
    private TextView        tvHint, tvCheck;
    private View            layoutCheck, layoutHint, layoutAutoSolve;
    private LinearLayout    keyboardLayout;
    private ImageButton     btnPause;


    private int     difficulty   = 0;
    private String  username     = "Guest";
    private int     hintsUsed    = 0;
    private int     maxHints     = MAX_HINTS_EASY;
    private int     checksUsed   = 0;
    private boolean isGameActive = false;
    private boolean isPaused     = false;
    private long    pauseOffset  = 0;

    private SudokuPuzzle      puzzle;
    private int[][]           currentBoard;
    private DatabaseReference puzzlesRef;
    private SudokuDatabase    db;

    private final List<String> remainingKeys  = new ArrayList<>();
    private String             currentPuzzleKey = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new SudokuDatabase(requireContext());

        // Đọc đúng kiểu int từ DifficultyFragment
        if (getArguments() != null) {
            difficulty = getArguments().getInt("difficulty", 0);
        }

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            username = activity.getCurrentUsername();
            if (username == null || username.isEmpty()) username = "Guest";
        }

        maxHints  = difficulty == 1 ? MAX_HINTS_MED
                : difficulty == 2 ? MAX_HINTS_HARD : MAX_HINTS_EASY;

        puzzlesRef = FirebaseDatabase.getInstance()
                .getReference("puzzles").child(difficultyKey());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_game, container, false);

        boardView      = v.findViewById(R.id.sudoku_board);
        chronometer    = v.findViewById(R.id.chronometer);
        tvHint         = v.findViewById(R.id.btn_hint);
        tvCheck        = v.findViewById(R.id.btn_check);
        layoutCheck    = v.findViewById(R.id.layout_check);
        layoutHint     = v.findViewById(R.id.layout_hint);
        layoutAutoSolve= v.findViewById(R.id.layout_auto_solve);
        keyboardLayout = v.findViewById(R.id.keyboard_layout);
        btnPause       = v.findViewById(R.id.btn_pause);

        View btnBack = v.findViewById(R.id.btn_back);
        View btnHome = v.findViewById(R.id.btn_home);
        if (btnBack != null) btnBack.setOnClickListener(view -> handleExitRequest(false));
        if (btnHome != null) btnHome.setOnClickListener(view -> handleExitRequest(true));
        if (btnPause != null) btnPause.setOnClickListener(view -> togglePause());

        if (layoutCheck     != null) layoutCheck.setOnClickListener(view -> handleCheckAction());
        if (layoutHint      != null) layoutHint.setOnClickListener(view -> handleHintAction());
        if (layoutAutoSolve != null) layoutAutoSolve.setOnClickListener(view -> handleAutoSolve());

        setupKeyboard();
        setupBoardListener();
        updateHintButton();
        updateCheckButton();

        resetRemainingKeys();
        loadNextPuzzle();

        return v;
    }

    // ── Difficulty

    private String difficultyKey() {
        if (difficulty == 1) return "medium";
        if (difficulty == 2) return "hard";
        return "easy";
    }

    private void resetRemainingKeys() {
        remainingKeys.clear();
        for (int i = 1; i <= TOTAL_PUZZLES; i++) remainingKeys.add(String.valueOf(i));
    }

    private String pickNextKey() {
        if (remainingKeys.isEmpty()) resetRemainingKeys();
        List<String> candidates = new ArrayList<>(remainingKeys);
        if (candidates.size() > 1 && currentPuzzleKey != null)
            candidates.remove(currentPuzzleKey);
        String chosen = candidates.get(new Random().nextInt(candidates.size()));
        remainingKeys.remove(chosen);
        return chosen;
    }

    private void loadNextPuzzle() {
        currentPuzzleKey = pickNextKey();
        Log.d(TAG, "Loading key=" + currentPuzzleKey + " diff=" + difficultyKey());

        puzzlesRef.child(currentPuzzleKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        if (!snapshot.exists()) {
                            // Fallback: lấy từ SQLite
                            loadFromSQLite();
                            return;
                        }
                        String puzzleStr   = snapshot.child("puzzle").getValue(String.class);
                        String solutionStr = snapshot.child("solution").getValue(String.class);
                        if (puzzleStr == null || solutionStr == null) {
                            loadFromSQLite();
                            return;
                        }
                        applyPuzzle(puzzleStr, solutionStr);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Firebase error: " + error.getMessage());
                        // Fallback: lấy từ SQLite
                        loadFromSQLite();
                    }
                });
    }

    private void loadFromSQLite() {
        String[] data = db.getRandomPuzzle(difficulty);
        if (data != null) {
            applyPuzzle(data[0], data[1]);
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy đề bài!", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyPuzzle(String puzzleStr, String solutionStr) {
        puzzle       = new SudokuPuzzle(puzzleStr, solutionStr, difficulty);
        currentBoard = copyGrid(puzzle.getPuzzle());

        boolean[][] given = new boolean[9][9];
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                given[r][c] = puzzle.isGiven(r, c);

        boardView.loadPuzzle(currentBoard, given);

        hintsUsed  = 0;
        checksUsed = 0;
        isGameActive = true;
        isPaused     = false;
        pauseOffset  = 0;

        updateHintButton();
        updateCheckButton();

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        if (btnPause != null)
            btnPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    // ── Timer

    private void togglePause() {
        if (!isGameActive) return;
        if (!isPaused) {
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            chronometer.stop();
            isPaused = true;
            if (btnPause != null)
                btnPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isPaused = false;
            if (btnPause != null)
                btnPause.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private long getElapsedSeconds() {
        return (isPaused ? pauseOffset
                : SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isGameActive && !isPaused) togglePause();
    }

    // ── Bàn phím

    private void setupKeyboard() {
        if (keyboardLayout == null) return;
        keyboardLayout.removeAllViews();
        for (int num = 1; num <= 9; num++) {
            Button btn = new Button(requireContext());
            btn.setText(String.valueOf(num));
            btn.setTextSize(18f);
            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            p.setMargins(2, 2, 2, 2);
            btn.setLayoutParams(p);
            final int n = num;
            btn.setOnClickListener(b -> onNumberPressed(n));
            keyboardLayout.addView(btn);
        }
        Button btnDel = new Button(requireContext());
        btnDel.setText("✕");
        btnDel.setTextSize(18f);
        LinearLayout.LayoutParams dp =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        dp.setMargins(2, 2, 2, 2);
        btnDel.setLayoutParams(dp);
        btnDel.setOnClickListener(b -> onNumberPressed(0));
        keyboardLayout.addView(btnDel);
    }

    private void setupBoardListener() {
        if (boardView == null) return;
        boardView.setOnCellSelectedListener((row, col) -> {
            if (!isGameActive || isPaused || puzzle == null) return;
            keyboardLayout.setVisibility(
                    puzzle.isGiven(row, col) ? View.GONE : View.VISIBLE);
        });
    }

    private void onNumberPressed(int num) {
        if (!isGameActive || isPaused || puzzle == null) return;
        int row = boardView.getSelectedRow();
        int col = boardView.getSelectedCol();
        if (row < 0 || col < 0 || puzzle.isGiven(row, col)) return;
        currentBoard[row][col] = num;
        boardView.setNumber(row, col, num);
        boardView.setError(row, col, false);
        checkWinCondition();
    }

    // ── Hành động

    private void handleCheckAction() {
        if (!isGameActive || isPaused || puzzle == null) return;
        if (checksUsed >= MAX_CHECKS) {
            Toast.makeText(requireContext(), "Đã dùng hết lượt kiểm tra!", Toast.LENGTH_SHORT).show();
            return;
        }
        checksUsed++;
        updateCheckButton();
        int remaining = MAX_CHECKS - checksUsed;
        Toast.makeText(requireContext(),
                remaining > 0 ? "Còn " + remaining + " lượt kiểm tra."
                        : "Đây là lượt kiểm tra cuối cùng!",
                Toast.LENGTH_SHORT).show();
        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!puzzle.isGiven(r, c) && currentBoard[r][c] != 0)
                    boardView.setError(r, c, currentBoard[r][c] != sol[r][c]);
    }

    private void handleHintAction() {
        if (!isGameActive || isPaused || puzzle == null) return;
        if (hintsUsed >= maxHints) {
            Toast.makeText(requireContext(), "Đã hết lượt gợi ý!", Toast.LENGTH_SHORT).show();
            return;
        }
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!puzzle.isGiven(r, c) && currentBoard[r][c] == 0)
                    empty.add(new int[]{r, c});
        if (empty.isEmpty()) return;
        int[] cell   = empty.get(new Random().nextInt(empty.size()));
        int   row    = cell[0], col = cell[1];
        int   answer = puzzle.getSolution()[row][col];
        currentBoard[row][col] = answer;
        boardView.setNumber(row, col, answer);
        boardView.setHint(row, col);
        boardView.setError(row, col, false);
        hintsUsed++;
        updateHintButton();
        checkWinCondition();
    }

    private void handleAutoSolve() {
        if (!isGameActive || puzzle == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Tự động giải")
                .setMessage("Kết quả ván này sẽ không được tính điểm.\nBạn vẫn muốn tiếp tục?")
                .setPositiveButton("Đồng ý", (d, w) -> {
                    isGameActive = false;
                    chronometer.stop();
                    int[][] sol = puzzle.getSolution();
                    for (int r = 0; r < 9; r++)
                        for (int c = 0; c < 9; c++) {
                            currentBoard[r][c] = sol[r][c];
                            boardView.setNumber(r, c, sol[r][c]);
                            boardView.setError(r, c, false);
                        }
                    keyboardLayout.setVisibility(View.GONE);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleExitRequest(boolean goHome) {
        if (isGameActive) {
            if (!isPaused) togglePause();
            showExitConfirmationDialog(() -> {
                chronometer.stop();
                MainActivity activity = (MainActivity) getActivity();
                if (activity == null) return;
                if (goHome) activity.goHome();
                else requireActivity().getSupportFragmentManager().popBackStack();
            });
        } else {
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) return;
            if (goHome) activity.goHome();
            else requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    // ── Chiến thắng

    private void checkWinCondition() {
        if (!isGameActive || puzzle == null) return;
        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (currentBoard[r][c] != sol[r][c]) return;

        isGameActive = false;
        chronometer.stop();

        long seconds = getElapsedSeconds();
        int  base    = difficulty == 1 ? BASE_MED : difficulty == 2 ? BASE_HARD : BASE_EASY;
        int  score   = Math.max(100, base - (int) seconds * 2 - hintsUsed * 50);

        if (db != null) {
            db.updateBestScore(username, difficulty, score, (int) seconds, hintsUsed);
        }

        showWinDialog(score, seconds);
    }

    // ── Dialogs

    private void showWinDialog(int score, long seconds) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_win);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }
        TextView tvScore  = dialog.findViewById(R.id.tv_win_score);
        TextView tvTime   = dialog.findViewById(R.id.tv_win_time);
        TextView tvHints  = dialog.findViewById(R.id.tv_win_hints);
        TextView tvChecks = dialog.findViewById(R.id.tv_win_checks);
        Button   btnClose = dialog.findViewById(R.id.btn_win_close);
        Button   btnNext  = dialog.findViewById(R.id.btn_win_next);

        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
        if (tvScore  != null) tvScore.setText(String.valueOf(score));
        if (tvTime   != null) tvTime.setText(time);
        if (tvHints  != null) tvHints.setText(hintsUsed + "/" + maxHints);
        if (tvChecks != null) tvChecks.setText(checksUsed + "/" + MAX_CHECKS);
        if (btnClose != null) btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            keyboardLayout.setVisibility(View.GONE);
        });
        if (btnNext  != null) btnNext.setOnClickListener(v -> {
            dialog.dismiss();
            resetRemainingKeys();
            loadNextPuzzle();
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showExitConfirmationDialog(Runnable onConfirm) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_exit_confirmation);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }
        Button btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        Button btnExit   = dialog.findViewById(R.id.btn_dialog_exit);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            // Resume timer sau khi hủy thoát
            if (isPaused) togglePause();
        });
        if (btnExit != null) btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    // ── Tiện ích

    private void updateHintButton() {
        if (tvHint != null) tvHint.setText("Gợi ý: " + (maxHints - hintsUsed) + "/" + maxHints);
    }

    private void updateCheckButton() {
        if (tvCheck != null) tvCheck.setText("Kiểm tra:\n" + (MAX_CHECKS - checksUsed) + "/" + MAX_CHECKS);
    }

    private int[][] copyGrid(int[][] src) {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++) System.arraycopy(src[r], 0, copy[r], 0, 9);
        return copy;
    }
}