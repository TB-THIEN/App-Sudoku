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
    private static final int MAX_CHECKS       = 10;

    // ── Views ──
    private SudokuBoardView boardView;
    private Chronometer chronometer;
    private Button btnHint, btnAutoSolve, btnCheck, btnPause;
    private View btnHome, btnBack; // Thêm biến để quản lý trạng thái nút trên Toolbar
    private LinearLayout keyboardLayout;

    // ── Game state ──
    private SudokuPuzzle puzzle;
    private int[][] currentBoard;
    private int difficulty;
    private int hintsUsed;
    private int maxHints;
    private int checksUsed = 0;
    private boolean isPaused = false;
    private boolean autoSolved = false;
    private boolean isGameFinished = false; // Đánh dấu ván chơi đã kết thúc thành công/auto solve
    private long pauseOffset = 0;

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
        btnHome = v.findViewById(R.id.btn_home);
        btnBack = v.findViewById(R.id.btn_back);

        btnHome.setOnClickListener(b -> {
            clearCurrentPuzzleStateIfFinished();
            ((MainActivity) requireActivity()).goHome();
        });

        btnBack.setOnClickListener(b -> {
            clearCurrentPuzzleStateIfFinished();
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    // Nếu ván chơi đã hoàn thành hoặc đã bấm auto solve, xóa trạng thái để lần sau vào luôn ra đề mới
    private void clearCurrentPuzzleStateIfFinished() {
        if (isGameFinished || autoSolved) {
            puzzle = null;
        }
    }

    // ─── Tải đề bài (Chống trùng tuyệt đối) ────────────────────────────
    private void loadPuzzle() {
        String oldPuzzleStr = (puzzle != null) ? gridToString(puzzle.getPuzzle()) : "";

        String[] raw = null;
        int maxAttempts = 20;
        int attempts = 0;

        while (attempts < maxAttempts) {
            raw = db.getRandomPuzzle(difficulty);
            if (raw == null) break;

            if (oldPuzzleStr.isEmpty() || !raw[0].equals(oldPuzzleStr)) {
                break;
            }
            attempts++;
        }

        if (raw == null) {
            Toast.makeText(requireContext(), "Không tìm thấy đề bài phù hợp!", Toast.LENGTH_SHORT).show();
            return;
        }

        puzzle = new SudokuPuzzle(raw[0], raw[1], difficulty);
        currentBoard = copyGrid(puzzle.getPuzzle());

        boolean[][] given = new boolean[9][9];
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                given[r][c] = puzzle.isGiven(r, c);

        boardView.loadPuzzle(currentBoard, given);

        // Khôi phục lại trạng thái tương tác cho toàn bộ View khi vào ván mới
        setGameInteractionEnabled(true);

        hintsUsed = 0;
        checksUsed = 0;
        autoSolved = false;
        isGameFinished = false;
        updateHintButton();
        updateCheckButton();

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        isPaused = false;
        btnPause.setText("⏸");
    }

    // Hàm bật/tắt quyền tương tác của người chơi lên các nút tính năng điều khiển
    private void setGameInteractionEnabled(boolean enabled) {
        boardView.setEnabled(enabled);
        btnHint.setEnabled(enabled);
        btnCheck.setEnabled(enabled);
        btnAutoSolve.setEnabled(enabled);
        btnPause.setEnabled(enabled);
        if (!enabled) {
            keyboardLayout.setVisibility(View.GONE);
        }
    }

    // ─── Timer ─────────────────────────────────
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
        if (!isPaused && !isGameFinished && !autoSolved) pauseTimer();
    }

    // ─── Bàn phím ảo ───────────────────────────
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
            if (!puzzle.isGiven(row, col)) {
                keyboardLayout.setVisibility(View.VISIBLE);
            } else {
                keyboardLayout.setVisibility(View.GONE);
            }
        });
    }

    private void onNumberPressed(int num) {
        if (isPaused || autoSolved || isGameFinished) return;
        int row = boardView.getSelectedRow();
        int col = boardView.getSelectedCol();
        if (row < 0 || col < 0) return;
        if (puzzle.isGiven(row, col)) return;

        currentBoard[row][col] = num;
        boardView.setNumber(row, col, num);
        boardView.setError(row, col, false);

        checkWinCondition();
    }

    private void setupButtons() {
        btnPause.setOnClickListener(b -> pauseTimer());

        btnHint.setOnClickListener(b -> {
            if (isPaused || autoSolved || isGameFinished) return;
            if (hintsUsed >= maxHints) {
                Toast.makeText(requireContext(), "Hết lượt gợi ý!", Toast.LENGTH_SHORT).show();
                return;
            }
            giveHint();
        });

        btnAutoSolve.setOnClickListener(b -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Tự động giải")
                    .setMessage("Kết quả ván này sẽ không được tính điểm. Bạn vẫn muốn tiếp tục?")
                    .setPositiveButton("Đồng ý", (d, w) -> autoSolve())
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        btnCheck.setOnClickListener(b -> checkAllErrors());
    }

    private void giveHint() {
        java.util.List<int[]> empty = new java.util.ArrayList<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; r < 9; c++) // duyệt tìm ô trống
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

    // YÊU CẦU 1: Tự động giải hiển thị thông báo bên dưới bàn cờ để thấy được bàn cờ đã giải
    private void autoSolve() {
        autoSolved = true;
        chronometer.stop();

        // 1. Điền toàn bộ đáp án lên bàn cờ để người chơi nhìn thấy trực tiếp
        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                currentBoard[r][c] = sol[r][c];
                boardView.setNumber(r, c, sol[r][c]);
                boardView.setError(r, c, false);
            }
        }

        // 2. Ẩn bàn phím ảo và khóa các nút chức năng để lộ bàn cờ hoàn thiện rộng rãi
        setGameInteractionEnabled(false);

        // 3. Hiển thị hộp thoại tùy biến nằm sát cạnh dưới (Bottom) màn hình, không che bàn cờ
        AlertDialog bottomDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Đã tự động giải!")
                .setMessage("Hệ thống đã điền đáp án. Ván này không được tính điểm.")
                .setCancelable(false)
                .setPositiveButton("Chơi bài khác", (d, w) -> loadPuzzle()) // Nhấn vào bốc đề mới ngay
                .setNegativeButton("Đóng", (d, w) -> {
                    // Khi đóng xem bài, chỉ có nút Back và Home sáng, giữ nguyên giao diện đã khóa
                    Toast.makeText(requireContext(), "Bạn có thể nhấn Home hoặc Back trên thanh công cụ để thoát.", Toast.LENGTH_LONG).show();
                })
                .create();

        // Đặt vị trí Dialog nằm ở Bottom màn hình
        Window window = bottomDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.BOTTOM;
            wlp.y = 50; // Khoảng cách cách đáy màn hình một chút cho đẹp
            window.setAttributes(wlp);
        }

        bottomDialog.show();
    }

    private void checkAllErrors() {
        if (isPaused || autoSolved || isGameFinished) return;

        if (checksUsed >= MAX_CHECKS) {
            Toast.makeText(requireContext(), "Bạn đã dùng hết 10 lượt Kiểm tra!", Toast.LENGTH_SHORT).show();
            return;
        }

        checksUsed++;
        updateCheckButton();

        int remaining = MAX_CHECKS - checksUsed;
        if (remaining > 0) {
            Toast.makeText(requireContext(), "Đã kiểm tra! Bạn còn " + remaining + " lượt.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Đã kiểm tra! Đây là lượt cuối cùng của bạn.", Toast.LENGTH_SHORT).show();
        }

        int[][] sol = puzzle.getSolution();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (!puzzle.isGiven(r, c) && currentBoard[r][c] != 0) {
                    boolean isWrong = (currentBoard[r][c] != sol[r][c]);
                    boardView.setError(r, c, isWrong);
                }
            }
        }
    }

    private void checkWinCondition() {
        if (autoSolved || isGameFinished) return;
        int[][] sol = puzzle.getSolution();

        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (currentBoard[r][c] != sol[r][c]) return;

        // Đánh dấu người chơi đã tự giải xong ván đấu xuất sắc
        isGameFinished = true;
        chronometer.stop();

        long seconds = getElapsedSeconds();
        int baseScore = difficulty == 0 ? BASE_SCORE_EASY
                : difficulty == 1 ? BASE_SCORE_MEDIUM : BASE_SCORE_HARD;

        int score = (int) Math.max(0, baseScore - seconds * 2 - hintsUsed * 200 - checksUsed * 100);

        db.updateBestScore(difficulty, score, (int) seconds, hintsUsed);
        showWinDialog(score, seconds);
    }

    // YÊU CẦU 2 & 3: Bảng chiến thắng có nút X tắt đi, vô hiệu hóa bàn cờ, chỉ sáng Back và Home
    private void showWinDialog(int score, long seconds) {
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);

        AlertDialog winDialog = new AlertDialog.Builder(requireContext())
                .setTitle("🎉 Xuất sắc! Chiến thắng")
                .setMessage("Điểm số đạt được: " + score +
                        "\nThời gian hoàn thành: " + time +
                        "\nSố lần dùng gợi ý: " + hintsUsed + "/" + maxHints +
                        "\nSố lần kiểm tra lỗi: " + checksUsed + "/" + MAX_CHECKS)
                .setCancelable(false)
                .setPositiveButton("Chơi lại ván khác", (d, w) -> loadPuzzle()) // Click là đổi đề bài ngẫu nhiên khác
                // Thêm nút "X" (Đóng) ở vị trí NeutralButton để người chơi tắt thông báo
                .setNeutralButton("✕ Đóng", (d, w) -> {
                    // Thực hiện tắt thông báo và vô hiệu hóa tất cả các nút dưới, chỉ giữ lại Back/Home sáng
                    setGameInteractionEnabled(false);
                    Toast.makeText(requireContext(), "Bạn đã hoàn thành bài Sudoku này! Hãy bấm nút Back hoặc Home để tiếp tục.", Toast.LENGTH_LONG).show();
                })
                .create();

        winDialog.show();
    }

    private void updateHintButton() {
        btnHint.setText("Gợi ý: " + (maxHints - hintsUsed) + "/" + maxHints);
    }

    private void updateCheckButton() {
        int remaining = MAX_CHECKS - checksUsed;
        btnCheck.setText("Kiểm tra: " + remaining + "/" + MAX_CHECKS);
    }

    // ─── Utility Methods ──────────────────────────────
    private int[][] copyGrid(int[][] src) {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++)
            System.arraycopy(src[r], 0, copy[r], 0, 9);
        return copy;
    }

    private String gridToString(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                sb.append(grid[r][c]);
        return sb.toString();
    }
}