package com.tbt65133334.sudokuapp.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;
import com.tbt65133334.sudokuapp.database.SudokuDatabase;
import com.tbt65133334.sudokuapp.model.GameStats;

import java.util.List;

public class StartFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_start, container, false);

        v.findViewById(R.id.btn_home).setOnClickListener(b ->
                ((MainActivity) requireActivity()).goHome());
        v.findViewById(R.id.btn_back).setOnClickListener(b ->
                requireActivity().getSupportFragmentManager().popBackStack());

        String username = ((MainActivity) requireActivity()).getCurrentUsername();
        SudokuDatabase db = new SudokuDatabase(requireContext());

        // Lấy thành tích cho cả 3 độ khó của user hiện tại
        List<GameStats> statsList = db.getAllStats(username != null ? username : "");

        GameStats easy   = statsList.get(0);
        GameStats medium = statsList.get(1);
        GameStats hard   = statsList.get(2);

        // Điểm cao nhất
        ((TextView) v.findViewById(R.id.tv_easy_score)).setText(String.valueOf(easy.getBestScore()));
        ((TextView) v.findViewById(R.id.tv_med_score)).setText(String.valueOf(medium.getBestScore()));
        ((TextView) v.findViewById(R.id.tv_hard_score)).setText(String.valueOf(hard.getBestScore()));

        // Thời gian tốt nhất
        ((TextView) v.findViewById(R.id.tv_easy_time)).setText(easy.getBestTimeFormatted());
        ((TextView) v.findViewById(R.id.tv_med_time)).setText(medium.getBestTimeFormatted());
        ((TextView) v.findViewById(R.id.tv_hard_time)).setText(hard.getBestTimeFormatted());

        // Số lần gợi ý khi đạt best
        ((TextView) v.findViewById(R.id.tv_easy_hints)).setText(String.valueOf(easy.getBestHints()));
        ((TextView) v.findViewById(R.id.tv_med_hints)).setText(String.valueOf(medium.getBestHints()));
        ((TextView) v.findViewById(R.id.tv_hard_hints)).setText(String.valueOf(hard.getBestHints()));

        return v;
    }
}