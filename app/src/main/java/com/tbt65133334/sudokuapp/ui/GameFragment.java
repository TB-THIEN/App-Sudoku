package com.tbt65133334.sudokuapp.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
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

    private static final String TAG = "GameFragment";

    private static final int MAX_HINTS_EASY    = 5;
    private static final int MAX_HINTS_MEDIUM  = 3;
    private static final int MAX_HINTS_HARD    = 2;
    private static final int BASE_SCORE_EASY   = 1000;
    private static final int BASE_SCORE_MEDIUM = 2000;
    private static final int BASE_SCORE_HARD   = 3000;
    private static final int MAX_CHECKS        = 10;
    private static final int TOTAL_PUZZLES     = 10;

    private SudokuBoardView boardView;
    private Chronometer     chronometer;
    private ImageButton     btnPause;           // ImageButton trong XML
    private TextView        tvHint;             // TextView bên trong layout_hint
    private TextView        tvAutoSolve;        // TextView bên trong layout_auto_solve
    private TextView        tvCheck;            // TextView bên trong layout_check
    private LinearLayout    layoutHint;         // LinearLayout clickable
    private LinearLayout    layoutAutoSolve;
    private LinearLayout    layoutCheck;
    private View            btnHome, btnBack;
    private LinearLayout    keyboardLayout;

    private SudokuPuzzle puzzle;
    private int[][]      currentBoard;
    private int          difficulty;
    private int          hintsUsed;
    private int          maxHints;
    private int          checksUsed    = 0;
    private boolean      isPaused      = false;
    private boolean      autoSolved    = false;
    private boolean      isGameFinished = false;
    private long         pauseOffset   = 0;

    private final List<String> remainingKeys = new ArrayList<>();
    private String currentPuzzleKey = null;

    private SudokuDatabase db;
    private DatabaseReference puzzlesRef;
    private String username;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_game, container, false);

        difficulty = getArguments() != null ? getArguments().getInt("difficulty", 0) : 0;
        maxHints   = difficulty == 0 ? MAX_HINTS_EASY
                : difficulty == 1 ? MAX_HINTS_MEDIUM : MAX_HINTS_HARD;

        // Lấy username từ MainActivity
        username   = ((MainActivity) requireActivity()).getCurrentUsername();

        db         = new SudokuDatabase(requireContext());
        puzzlesRef = FirebaseDatabase.getInstance().getReference("puzzles")
                .child(difficultyKey(difficulty));

        // ── Ánh xạ view đúng kiểu ──────────────────────────────────────────
        boardView      = v.findViewById(R.id.sudoku_board);
        chronometer    = v.findViewById(R.id.chronometer);
        btnPause       = v.findViewById(R.id.btn_pause);       // ImageButton ✓
        keyboardLayout = v.findViewById(R.id.keyboard_layout);

        // Các nút hành động: lấy LinearLayout (click) + TextView (hiển thị text)
        layoutHint      = v.findViewById(R.id.layout_hint);
        layoutAutoSolve = v.findViewById(R.id.layout_auto_solve);
        layoutCheck     = v.findViewById(R.id.layout_check);
        tvHint          = v.findViewById(R.id.btn_hint);       // TextView trong layout_hint
        tvAutoSolve     = v.findViewById(R.id.btn_auto_solve);
        tvCheck         = v.findViewById(R.id.btn_check);

        setupToolbar(v);
        setupKeyboard();
        setupBoardListener();
        setupButtons();

        resetRemainingKeys();
        loadNextPuzzle();

        return v;
    }

    // ── Difficulty key ────────────────────────────────────────────────────────

    private String difficultyKey(int diff) {
        if (diff == 0) return "easy";
        if (diff == 1) return "medium";
        return "hard";
    }

    // ── Quản lý danh sách đề ─────────────────────────────────────────────────

    private void resetRemainingKeys() {
        remainingKeys.clear();
        for (int i = 1; i <= TOTAL_PUZZLES; i++) {
            remainingKeys.add(String.valueOf(i));
        }
    }

    private String pickNextKey() {
        if (remainingKeys.isEmpty()) resetRemainingKeys();

        List<String> candidates = new ArrayList<>(remainingKeys);
        if (candidates.size() > 1 && currentPuzzleKey != null) {
            candidates.remove(currentPuzzleKey);
        }

        String chosen = candidates.get(new Random().nextInt(candidates.size()));
        remainingKeys.remove(chosen);
        return chosen;
    }

    // ── Load đề từ Firebase ───────────────────────────────────────────────────

    private void loadNextPuzzle() {
        currentPuzzleKey = pickNextKey();
        Log.d(TAG, "Loading puzzle key=" + currentPuzzleKey
                + " difficulty=" + difficultyKey(difficulty)
                + " remaining=" + remainingKeys.size());

        puzzlesRef.child(currentPuzzleKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(requireContext(),
                                    "Không tìm thấy đề bài trên Firebase!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String puzzleStr   = snapshot.child("puzzle").getValue(String.class);
                        String solutionStr = snapshot.child("solution").getValue(String.class);

                        if (puzzleStr == null || solutionStr == null) {
                            Toast.makeText(requireContext(),
                                    "Dữ liệu đề bài bị lỗi!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        applyPuzzle(puzzleStr, solutionStr);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase error: " + error.getMessage());
                        Toast.makeText(requireContext(),
                                "Lỗi tải đề: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyPuzzle(String puzzleStr, String solutionStr) {
        puzzle       = new SudokuPuzzle(puzzleStr, solutionStr, difficulty);
        currentBoard = copyGrid(puzzle.getPuzzle());

        boolean[][] given = new boolean[9][9];
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                given[r][c] = puzzle.isGiven(r, c);

        boardView.loadPuzzle(currentBoard, given);
        setGameInteractionEnabled(true);

        hintsUsed      = 0;
        checksUsed     = 0;
        autoSolved     = false;
        isGameFinished = false;

        updateHintButton();
        updateCheckButton();

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        isPaused = false;
        btnPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void setupToolbar(View v) {
        btnHome = v.findViewById(R.id.btn_home);
        btnBack = v.findViewById(R.id.btn_back);
        btnHome.setOnClickListener(b -> ((MainActivity) requireActivity()).goHome());
        btnBack.setOnClickListener(b ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    // ── Bật/tắt tương tác ────────────────────────────────────────────────────

    private void setGameInteractionEnabled(boolean enabled) {
        boardView.setEnabled(enabled);
        layoutHint.setEnabled(enabled);
        layoutHint.setClickable(enabled);
        layoutCheck.setEnabled(enabled);
        layoutCheck.setClickable(enabled);
        layoutAutoSolve.setEnabled(enabled);
        layoutAutoSolve.setClickable(enabled);
        btnPause.setEnabled(enabled);
        if (!enabled) keyboardLayout.setVisibility(View.GONE);
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void pauseTimer() {
        if (!isPaused) {
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            chronometer.stop();
            isPaused = true;
            btnPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isPaused = false;
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
        if (!isPaused && !isGameFinished && !autoSolved) pauseTimer();
    }

    // ── Bàn phím ảo ──────────────────────────────────────────────────────────

    private void setupKeyboard() {
        keyboardLayout.removeAllViews();
        for (int num = 1; num <= 9; num++) {
            Button btn = new Button(requireContext());
            btn.setText(String.valueOf(num));
            btn.setTextSize(18f);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(2, 2, 2, 2);
            btn.setLayoutParams(params);
            final int n = num;
            btn.setOnClickListener(b -> onNumberPressed(n));
            keyboardLayout.addView(btn);
        }
        Button btnDel = new Button(requireContext());
        btnDel.setText("✕");
        btnDel.setTextSize(18f);
        LinearLayout.LayoutParams delParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnDel.setLayoutParams(delParams);
        btnDel.setOnClickListener(b -> onNumberPressed(0));
        keyboardLayout.addView(btnDel);
    }

    private void setupBoardListener() {
        boardView.setOnCellSelectedListener((row, col) -> {
            if (isPaused || autoSolved || isGameFinished) return;
            keyboardLayout.setVisibility(puzzle.isGiven(row, col) ? View.GONE : View.VISIBLE);
        });
    }

    private void onNumberPressed(int num) {
        if (isPaused || autoSolved || isGameFinished) return;
        int row = boardView.getSelectedRow();
        int col = boardView.getSelectedCol();
        if (row < 0 || col < 0 || puzzle.isGiven(row, col)) return;

        currentBoard[row][col] = num;
        boardView.setNumber(row, col, num);
        boardView.setError(row, col, false);
        checkWinCondition();
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        btnPause.setOnClickListener(b -> pauseTimer());

        layoutHint.setOnClickListener(b -> {
            if (isPaused || autoSolved || isGameFinished) return;
            if (hintsUsed >= maxHints) {
                Toast.makeText(requireContext(), "Hết lượt gợi ý!", Toast.LENGTH_SHORT).show();
                return;
            }
            giveHint();
        });

        layoutAutoSolve.setOnClickListener(b ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Tự động giải")
                        .setMessage("Kết quả ván này sẽ không được tính điểm.\nBạn vẫn muốn tiếp tục?")
                        .setPositiveButton("Đồng ý", (d, w) -> autoSolve())
                        .setNegativeButton("Hủy", null)
                        .show());

        layoutCheck.setOnClickListener(b -> checkAllErrors());
    }

    // ── Gợi ý ─────────────────────────────────────────────────────────────────

    private void giveHint() {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!puzzle.isGiven(r, c) && currentBoard[r][c] == 0)
                    empty.add(new int[]{r, c});

        if (empty.isEmpty()) return;

        int[] cell  = empty.get(new Random().nextInt(empty.size()));
        int   row   = cell[0], col = cell[1];
        int   answer = puzzle.getSolution()[row][col];

        currentBoard[row][col] = answer;
        boardView.setNumber(row, col, answer);
        boardView.setHint(row, col);
        boardView.setError(row, col, false);

        hintsUsed++;
        updateHintButton();
        checkWinCondition();
    }

    // ── Tự động giải ──────────────────────────────────────────────────────────

    private void autoSolve() {
        autoSolved = true;
        chronometer.stop();

        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {
                currentBoard[r][c] = sol[r][c];
                boardView.setNumber(r, c, sol[r][c]);
                boardView.setError(r, c, false);
            }

        setGameInteractionEnabled(false);

        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setTitle("Đã tự động giải!")
                .setMessage("Hệ thống đã điền đáp án.\nVán này không được tính điểm.")
                .setCancelable(false)
                .setPositiveButton("Đề tiếp theo", (d, w) -> loadNextPuzzle())
                .setNegativeButton("Đóng", (d, w) ->
                        Toast.makeText(requireContext(),
                                "Nhấn Home hoặc Back để thoát.", Toast.LENGTH_LONG).show())
                .create();

        Window window = dlg.getWindow();
        if (window != null) {
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.BOTTOM;
            wlp.y = 50;
            window.setAttributes(wlp);
        }
        dlg.show();
    }

    // ── Kiểm tra lỗi ─────────────────────────────────────────────────────────

    private void checkAllErrors() {
        if (isPaused || autoSolved || isGameFinished) return;
        if (checksUsed >= MAX_CHECKS) {
            Toast.makeText(requireContext(), "Đã dùng hết 10 lượt kiểm tra!", Toast.LENGTH_SHORT).show();
            return;
        }
        checksUsed++;
        updateCheckButton();

        int remaining = MAX_CHECKS - checksUsed;
        Toast.makeText(requireContext(),
                remaining > 0 ? "Đã kiểm tra! Còn " + remaining + " lượt."
                        : "Đây là lượt kiểm tra cuối cùng!",
                Toast.LENGTH_SHORT).show();

        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!puzzle.isGiven(r, c) && currentBoard[r][c] != 0)
                    boardView.setError(r, c, currentBoard[r][c] != sol[r][c]);
    }

    // ── Kiểm tra chiến thắng ──────────────────────────────────────────────────

    private void checkWinCondition() {
        if (autoSolved || isGameFinished) return;
        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (currentBoard[r][c] != sol[r][c]) return;

        isGameFinished = true;
        chronometer.stop();

        long seconds   = getElapsedSeconds();
        int  baseScore = difficulty == 0 ? BASE_SCORE_EASY
                : difficulty == 1 ? BASE_SCORE_MEDIUM : BASE_SCORE_HARD;
        int  score     = (int) Math.max(0, baseScore - seconds * 2 - hintsUsed * 200);

        // Lưu thành tích theo username
        if (username != null && !username.isEmpty()) {
            db.updateBestScore(username, difficulty, score, (int) seconds, hintsUsed);
        }

        showWinDialog(score, seconds);
    }

    private void showWinDialog(int score, long seconds) {
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);

        new AlertDialog.Builder(requireContext())
                .setTitle("🎉 Xuất sắc! Chiến thắng")
                .setMessage("Điểm số: " + score
                        + "\nThời gian: " + time
                        + "\nGợi ý đã dùng: " + hintsUsed + "/" + maxHints
                        + "\nKiểm tra đã dùng: " + checksUsed + "/" + MAX_CHECKS)
                .setCancelable(false)
                .setPositiveButton("Đề tiếp theo", (d, w) -> loadNextPuzzle())
                .setNeutralButton("✕ Đóng", (d, w) -> {
                    setGameInteractionEnabled(false);
                    Toast.makeText(requireContext(),
                            "Nhấn Back hoặc Home để tiếp tục.", Toast.LENGTH_LONG).show();
                })
                .show();
    }


    private void updateHintButton() {
        tvHint.setText("Gợi ý: " + (maxHints - hintsUsed) + "/" + maxHints);
    }

    private void updateCheckButton() {
        tvCheck.setText("Kiểm tra:\n" + (MAX_CHECKS - checksUsed) + "/" + MAX_CHECKS);
    }

    private int[][] copyGrid(int[][] src) {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++) System.arraycopy(src[r], 0, copy[r], 0, 9);
        return copy;
    }
}