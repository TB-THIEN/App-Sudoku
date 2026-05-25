package com.tbt65133334.sudokuapp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SudokuBoardView extends View {

    // Màu sắc
    private static final int COLOR_BACKGROUND      = Color.WHITE;
    private static final int COLOR_CELL_GIVEN      = Color.parseColor("#E8EEF7");  // ô đề bài
    private static final int COLOR_SELECTED        = Color.parseColor("#B3D4FF");  // ô đang chọn
    private static final int COLOR_HIGHLIGHT       = Color.parseColor("#DCE8FF");  // hàng/cột liên quan
    private static final int COLOR_HINT_BORDER     = Color.parseColor("#FF9800");  // viền gợi ý
    private static final int COLOR_ERROR_TEXT      = Color.RED;
    private static final int COLOR_GIVEN_TEXT      = Color.parseColor("#1A237E");
    private static final int COLOR_USER_TEXT       = Color.parseColor("#0D47A1");
    private static final int COLOR_GRID_THIN       = Color.parseColor("#BDBDBD");
    private static final int COLOR_GRID_BOLD       = Color.parseColor("#37474F");
    private static final int COLOR_BORDER_SELECTED = Color.parseColor("#1976D2");

    private Paint backgroundPaint, cellPaint, boldLinePaint, thinLinePaint,
            textPaint, givenTextPaint, errorTextPaint, selectedBorderPaint,
            hintBorderPaint;

    private float cellSize;
    private int selectedRow = -1, selectedCol = -1;

    // Dữ liệu bàn cờ
    private int[][] board;       // giá trị hiện tại (0 = trống)
    private boolean[][] isGiven; // true = ô đề (không sửa)
    private boolean[][] isError; // true = ô nhập sai
    private boolean[][] isHint;  // true = ô được gợi ý

    private OnCellSelectedListener listener;

    public interface OnCellSelectedListener {
        void onCellSelected(int row, int col);
    }

    public SudokuBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        board   = new int[9][9];
        isGiven = new boolean[9][9];
        isError = new boolean[9][9];
        isHint  = new boolean[9][9];

        backgroundPaint = makePaint(COLOR_BACKGROUND, Paint.Style.FILL, 0);
        cellPaint       = makePaint(COLOR_CELL_GIVEN, Paint.Style.FILL, 0);

        thinLinePaint = new Paint();
        thinLinePaint.setColor(COLOR_GRID_THIN);
        thinLinePaint.setStrokeWidth(1f);

        boldLinePaint = new Paint();
        boldLinePaint.setColor(COLOR_GRID_BOLD);
        boldLinePaint.setStrokeWidth(4f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(COLOR_USER_TEXT);
        textPaint.setTextAlign(Paint.Align.CENTER);

        givenTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        givenTextPaint.setColor(COLOR_GIVEN_TEXT);
        givenTextPaint.setFakeBoldText(true);
        givenTextPaint.setTextAlign(Paint.Align.CENTER);

        errorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        errorTextPaint.setColor(COLOR_ERROR_TEXT);
        errorTextPaint.setTextAlign(Paint.Align.CENTER);

        selectedBorderPaint = makePaint(COLOR_BORDER_SELECTED, Paint.Style.STROKE, 6f);
        hintBorderPaint     = makePaint(COLOR_HINT_BORDER, Paint.Style.STROKE, 6f);
    }

    private Paint makePaint(int color, Paint.Style style, float strokeWidth) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(style);
        if (strokeWidth > 0) p.setStrokeWidth(strokeWidth);
        return p;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Bàn cờ luôn là hình vuông
        int size = Math.min(MeasureSpec.getSize(widthSpec), MeasureSpec.getSize(heightSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cellSize = (float) w / 9;
        float textSize = cellSize * 0.55f;
        textPaint.setTextSize(textSize);
        givenTextPaint.setTextSize(textSize);
        errorTextPaint.setTextSize(textSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();

        // 1. Nền trắng
        canvas.drawRect(0, 0, width, width, backgroundPaint);

        // 2. Tô màu ô
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                float left = c * cellSize, top = r * cellSize;
                float right = left + cellSize, bottom = top + cellSize;

                if (r == selectedRow && c == selectedCol) {
                    cellPaint.setColor(COLOR_SELECTED);
                } else if (r == selectedRow || c == selectedCol) {
                    cellPaint.setColor(COLOR_HIGHLIGHT);
                } else if (isGiven[r][c]) {
                    cellPaint.setColor(COLOR_CELL_GIVEN);
                } else {
                    cellPaint.setColor(COLOR_BACKGROUND);
                }
                canvas.drawRect(left, top, right, bottom, cellPaint);
            }
        }

        // 3. Vẽ số
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) continue;
                float cx = c * cellSize + cellSize / 2f;
                float cy = r * cellSize + cellSize / 2f;

                // Căn giữa theo chiều dọc
                Rect bounds = new Rect();
                textPaint.getTextBounds(String.valueOf(board[r][c]), 0, 1, bounds);
                float textY = cy + bounds.height() / 2f - bounds.bottom;

                Paint p = isGiven[r][c] ? givenTextPaint
                        : (isError[r][c] ? errorTextPaint : textPaint);
                canvas.drawText(String.valueOf(board[r][c]), cx, textY, p);
            }
        }

        // 4. Vẽ đường kẻ mỏng (ô đơn)
        for (int i = 1; i < 9; i++) {
            if (i % 3 != 0) {
                canvas.drawLine(i * cellSize, 0, i * cellSize, width, thinLinePaint);
                canvas.drawLine(0, i * cellSize, width, i * cellSize, thinLinePaint);
            }
        }

        // 5. Vẽ đường kẻ đậm (vùng 3x3)
        for (int i = 0; i <= 9; i += 3) {
            canvas.drawLine(i * cellSize, 0, i * cellSize, width, boldLinePaint);
            canvas.drawLine(0, i * cellSize, width, i * cellSize, boldLinePaint);
        }

        // 6. Viền ô gợi ý & ô được chọn
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                float l = c * cellSize + 2, t = r * cellSize + 2;
                float ri = l + cellSize - 4, bo = t + cellSize - 4;
                if (isHint[r][c]) {
                    canvas.drawRect(l, t, ri, bo, hintBorderPaint);
                }
                if (r == selectedRow && c == selectedCol) {
                    canvas.drawRect(l, t, ri, bo, selectedBorderPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int col = (int) (event.getX() / cellSize);
            int row = (int) (event.getY() / cellSize);
            if (row >= 0 && row < 9 && col >= 0 && col < 9) {
                selectedRow = row;
                selectedCol = col;
                invalidate();
                if (listener != null) listener.onCellSelected(row, col);
            }
        }
        return true;
    }

    public void setOnCellSelectedListener(OnCellSelectedListener l) { this.listener = l; }
    public int getSelectedRow() { return selectedRow; }
    public int getSelectedCol() { return selectedCol; }

    public void setNumber(int row, int col, int num) {
        board[row][col] = num;
        invalidate();
    }

    public void setError(int row, int col, boolean error) {
        isError[row][col] = error;
        invalidate();
    }

    public void setHint(int row, int col) {
        isHint[row][col] = true;
        invalidate();
    }

    public void loadPuzzle(int[][] puzzle, boolean[][] given) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {
                board[r][c]   = puzzle[r][c];
                isGiven[r][c] = given[r][c];
                isError[r][c] = false;
                isHint[r][c]  = false;
            }
        selectedRow = -1;
        selectedCol = -1;
        invalidate();
    }

    public int[][] getBoard() { return board; }

    public void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        invalidate();
    }
}