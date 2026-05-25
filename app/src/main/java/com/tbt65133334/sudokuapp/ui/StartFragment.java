package com.tbt65133334.sudokuapp.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;
import com.tbt65133334.sudokuapp.database.SudokuDatabase;

public class StartFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_start, container, false);

        v.findViewById(R.id.btn_home).setOnClickListener(b ->
                ((MainActivity) requireActivity()).goHome());
        v.findViewById(R.id.btn_back).setOnClickListener(b ->
                requireActivity().getSupportFragmentManager().popBackStack());

        SudokuDatabase db = new SudokuDatabase(requireContext());
        int[][] stats = new int[3][];
        for (int i = 0; i < 3; i++) stats[i] = db.getStats(i);

        // Điền dữ liệu vào bảng
        String[] scoreIds = {String.valueOf(stats[0][0]), String.valueOf(stats[1][0]), String.valueOf(stats[2][0])};
        ((TextView) v.findViewById(R.id.tv_easy_score)).setText(scoreIds[0]);
        ((TextView) v.findViewById(R.id.tv_med_score)).setText(scoreIds[1]);
        ((TextView) v.findViewById(R.id.tv_hard_score)).setText(scoreIds[2]);

        for (int i = 0; i < 3; i++) {
            int s = stats[i][1]; // giây
            String time = s == 0 ? "–" : String.format("%02d:%02d", s / 60, s % 60);
            int tvId = i == 0 ? R.id.tv_easy_time : i == 1 ? R.id.tv_med_time : R.id.tv_hard_time;
            ((TextView) v.findViewById(tvId)).setText(time);
        }

        ((TextView) v.findViewById(R.id.tv_easy_hints)).setText(String.valueOf(stats[0][2]));
        ((TextView) v.findViewById(R.id.tv_med_hints)).setText(String.valueOf(stats[1][2]));
        ((TextView) v.findViewById(R.id.tv_hard_hints)).setText(String.valueOf(stats[2][2]));

        return v;
    }
}