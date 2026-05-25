package com.tbt65133334.sudokuapp.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;
import com.tbt65133334.sudokuapp.database.SudokuDatabase;
import com.tbt65133334.sudokuapp.model.SudokuPuzzle;
import com.tbt65133334.sudokuapp.view.SudokuBoardView;

public class GameFragment extends Fragment {

    // ── Constants ──
    private static final int MAX_HINTS_EASY   = 5;
    private static final int MAX_HINTS_MEDIUM = 3;
    private static final int MAX_HINTS_HARD   = 2;
    private static final int BASE_SCORE_EASY  = 1000;
    private static final int BASE_SCORE_MEDIUM= 2000;
    private static final int BASE_SCORE_HARD  = 3000;

    // ── Views ──
    private SudokuBoardView boardView;
    private Chronometer chronometer;
    private Button btnHint, btnAutoSolve, btnCheck, btnPause;
    private LinearLayout keyboardLayout;

    // ── Game state ──
    private SudokuPuzzle puzzle;
    private int[][] currentBoard;  // bản sao trạng thái người chơi đang nhập
    private int difficulty;
    private int hintsUsed;
    private int maxHints;
    private boolean isPaused = false;
    private boolean autoSolved = false;
    private long pauseOffset = 0;  // để resume Chronometer đúng

    private SudokuDatabase db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_game, container, false);

        // Nhận độ khó
        difficulty = getArguments() != null ? getArguments().getInt("difficulty", 0) : 0;
        maxHints = difficulty == 0 ? MAX_HINTS_EASY
                : difficulty == 1 ? MAX_HINTS_MEDIUM : MAX_HINTS_HARD;

        db = new SudokuDatabase(requireContext());

        // Ánh xạ view
        boardView    = v.findViewById(R.id.sudoku_board);
        chronometer  = v.findViewById(R.id.chronometer);
        btnHint      = v.findViewById(R.id.btn_hint);
        btnAutoSolve = v.findViewById(R.id.btn_auto_solve);
        btnCheck     = v.findViewById(R.id.btn_check);
        btnPause     = v.findViewById(R.id.btn_pause);
        keyboardLayout = v.findViewById(R.id.keyboard_layout);

        setupToolbar(v);
        setupKeyboard();
        loadPuzzle();
        setupBoardListener();
        setupButtons();

        return v;
    }

    // ─── Toolbar ───────────────────────────────
    private void setupToolbar(View v) {
        v.findViewById(R.id.btn_home).setOnClickListener(b ->
                ((MainActivity) requireActivity()).goHome());
        v.findViewById(R.id.btn_back).setOnClickListener(b ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    // ─── Tải đề bài ────────────────────────────
    private void loadPuzzle() {
        String[] raw = db.getRandomPuzzle(difficulty);
        if (raw == null) {
            Toast.makeText(requireContext(), "Không tìm thấy đề!", Toast.LENGTH_SHORT).show();
            return;
        }
        puzzle = new SudokuPuzzle(raw[0], raw[1], difficulty);
        currentBoard = copyGrid(puzzle.getPuzzle());

        // Tạo mảng isGiven
        boolean[][] given = new boolean[9][9];
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                given[r][c] = puzzle.isGiven(r, c);

        boardView.loadPuzzle(currentBoard, given);

        // Bắt đầu đếm giờ
        hintsUsed = 0;
        updateHintButton();
        startTimer();
    }

    // ─── Timer ─────────────────────────────────
    private void startTimer() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void pauseTimer() {
        if (!isPaused) {
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            chronometer.stop();
            isPaused = true;
            btnPause.setText("▶");
        } else {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isPaused = false;
            btnPause.setText("⏸");
        }
    }

    private long getElapsedSeconds() {
        return (isPaused ? pauseOffset
                : SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isPaused) pauseTimer();  // tự pause khi thoát app
    }

    // ─── Bàn phím ảo ───────────────────────────
    private void setupKeyboard() {
        keyboardLayout.removeAllViews();

        // Nút 1-9
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

        // Nút Xóa
        Button btnDel = new Button(requireContext());
        btnDel.setText("✕");
        btnDel.setTextSize(18f);
        LinearLayout.LayoutParams delParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnDel.setLayoutParams(delParams);
        btnDel.setOnClickListener(b -> onNumberPressed(0));
        keyboardLayout.addView(btnDel);
    }

    // ─── Listener khi nhấn ô ──────────────────
    private void setupBoardListener() {
        boardView.setOnCellSelectedListener((row, col) -> {
            if (isPaused || autoSolved) return;
            if (!puzzle.isGiven(row, col)) {
                // Hiện bàn phím
                keyboardLayout.setVisibility(View.VISIBLE);
            } else {
                keyboardLayout.setVisibility(View.GONE);
            }
        });
    }

    // ─── Nhấn số / xóa ────────────────────────
    private void onNumberPressed(int num) {
        if (isPaused || autoSolved) return;
        int row = boardView.getSelectedRow();
        int col = boardView.getSelectedCol();
        if (row < 0 || col < 0) return;
        if (puzzle.isGiven(row, col)) return;  // ô đề không sửa

        currentBoard[row][col] = num;
        boardView.setNumber(row, col, num);

        // Kiểm tra lỗi tức thì
        if (num != 0) {
            boolean correct = (puzzle.getSolution()[row][col] == num);
            boardView.setError(row, col, !correct);
        } else {
            boardView.setError(row, col, false);
        }

        // Kiểm tra win sau mỗi lần nhập
        checkWinCondition();
    }

    // ─── Các nút chức năng ────────────────────
    private void setupButtons() {
        btnPause.setOnClickListener(b -> pauseTimer());

        btnHint.setOnClickListener(b -> {
            if (isPaused || autoSolved) return;
            if (hintsUsed >= maxHints) {
                Toast.makeText(requireContext(), "Hết lượt gợi ý!", Toast.LENGTH_SHORT).show();
                return;
            }
            giveHint();
        });

        btnAutoSolve.setOnClickListener(b -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Tự động giải")
                    .setMessage("Kết quả sẽ không được ghi nhận. Tiếp tục?")
                    .setPositiveButton("Đồng ý", (d, w) -> autoSolve())
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        btnCheck.setOnClickListener(b -> checkAllErrors());
    }

    private void giveHint() {
        // Tìm 1 ô trống ngẫu nhiên và điền đáp án
        java.util.List<int[]> empty = new java.util.ArrayList<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!puzzle.isGiven(r, c) && currentBoard[r][c] == 0)
                    empty.add(new int[]{r, c});

        if (empty.isEmpty()) return;

        int[] cell = empty.get(new java.util.Random().nextInt(empty.size()));
        int row = cell[0], col = cell[1];
        int answer = puzzle.getSolution()[row][col];

        currentBoard[row][col] = answer;
        boardView.setNumber(row, col, answer);
        boardView.setHint(row, col);
        boardView.setError(row, col, false);

        hintsUsed++;
        updateHintButton();
        checkWinCondition();
    }

    private void autoSolve() {
        autoSolved = true;
        chronometer.stop();

        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                currentBoard[r][c] = sol[r][c];
                boardView.setNumber(r, c, sol[r][c]);
                boardView.setError(r, c, false);
            }
        }

        keyboardLayout.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Đã tự động giải!", Toast.LENGTH_SHORT).show();
    }

    private void checkAllErrors() {
        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!puzzle.isGiven(r, c) && currentBoard[r][c] != 0)
                    boardView.setError(r, c, currentBoard[r][c] != sol[r][c]);
    }

    // ─── Kiểm tra thắng ───────────────────────
    private void checkWinCondition() {
        if (autoSolved) return;
        int[][] sol = puzzle.getSolution();

        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (currentBoard[r][c] != sol[r][c]) return;  // chưa xong

        // Thắng!
        chronometer.stop();
        long seconds = getElapsedSeconds();
        int baseScore = difficulty == 0 ? BASE_SCORE_EASY
                : difficulty == 1 ? BASE_SCORE_MEDIUM : BASE_SCORE_HARD;
        int score = (int) Math.max(0, baseScore - seconds * 2 - hintsUsed * 200);

        db.updateBestScore(difficulty, score, (int) seconds, hintsUsed);
        showWinDialog(score, seconds);
    }

    private void showWinDialog(int score, long seconds) {
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
        new AlertDialog.Builder(requireContext())
                .setTitle("🎉 Chúc mừng!")
                .setMessage("Điểm: " + score + "\nThời gian: " + time +
                        "\nGợi ý đã dùng: " + hintsUsed)
                .setCancelable(false)
                .setPositiveButton("Về trang chủ", (d, w) ->
                        ((MainActivity) requireActivity()).goHome())
                .setNegativeButton("Chơi lại", (d, w) -> loadPuzzle())
                .show();
    }

    private void updateHintButton() {
        btnHint.setText("Gợi ý: " + (maxHints - hintsUsed) + "/" + maxHints);
    }

    // ─── Utility ──────────────────────────────
    private int[][] copyGrid(int[][] src) {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++)
            System.arraycopy(src[r], 0, copy[r], 0, 9);
        return copy;
    }
}