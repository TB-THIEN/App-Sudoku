package com.tbt65133334.sudokuapp.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        v.findViewById(R.id.btn_play).setOnClickListener(btn ->
                ((MainActivity) requireActivity()).navigateTo(new DifficultyFragment(), true));

        v.findViewById(R.id.btn_start).setOnClickListener(btn ->
                ((MainActivity) requireActivity()).navigateTo(new StartFragment(), true));

        v.findViewById(R.id.btn_help).setOnClickListener(btn -> showHelp());

        v.findViewById(R.id.btn_logout).setOnClickListener(btn -> showLogoutConfirm());

        return v;
    }

    private void showHelp() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hướng dẫn")
                .setMessage("Điền số từ 1-9 vào lưới 9×9 sao cho:\n" +
                        "• Mỗi hàng không có số trùng\n" +
                        "• Mỗi cột không có số trùng\n" +
                        "• Mỗi vùng 3×3 không có số trùng\n\n" +
                        "Nhấn vào ô → chọn số từ bàn phím bên dưới.\n" +
                        "Gợi ý: tự động điền 1 ô đúng.\n" +
                        "Tự động giải: điền toàn bộ đáp án.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void showLogoutConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (d, w) ->
                        ((MainActivity) requireActivity()).onLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }
}