package com.tbt65133334.sudokuapp.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;
import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;
import com.tbt65133334.sudokuapp.database.SudokuDatabase;
import com.tbt65133334.sudokuapp.model.GameStats;

import java.util.List;

public class StartFragment extends Fragment {

    private TextView tvEasyScore, tvMedScore, tvHardScore;
    private TextView tvEasyTime,  tvMedTime,  tvHardTime;
    private TextView tvEasyHints, tvMedHints, tvHardHints;

    private String         username;
    private SudokuDatabase db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_start, container, false);

        v.findViewById(R.id.btn_home).setOnClickListener(b ->
                ((MainActivity) requireActivity()).goHome());
        v.findViewById(R.id.btn_back).setOnClickListener(b ->
                requireActivity().getSupportFragmentManager().popBackStack());

        tvEasyScore = v.findViewById(R.id.tv_easy_score);
        tvMedScore  = v.findViewById(R.id.tv_med_score);
        tvHardScore = v.findViewById(R.id.tv_hard_score);
        tvEasyTime  = v.findViewById(R.id.tv_easy_time);
        tvMedTime   = v.findViewById(R.id.tv_med_time);
        tvHardTime  = v.findViewById(R.id.tv_hard_time);
        tvEasyHints = v.findViewById(R.id.tv_easy_hints);
        tvMedHints  = v.findViewById(R.id.tv_med_hints);
        tvHardHints = v.findViewById(R.id.tv_hard_hints);

        username = ((MainActivity) requireActivity()).getCurrentUsername();
        if (username == null) username = "";
        db = new SudokuDatabase(requireContext());

        // Hiển thị ngay từ SQLite
        loadFromSQLite();

        // Fetch Firebase cập nhật đè lên
        loadFromFirebase();

        return v;
    }

    // ── SQLite

    private void loadFromSQLite() {
        List<GameStats> list = db.getAllStats(username);
        GameStats easy   = getStats(list, 0);
        GameStats medium = getStats(list, 1);
        GameStats hard   = getStats(list, 2);
        applyToUI(easy, medium, hard);
    }

    // ── Firebase

    private void loadFromFirebase() {
        if (username.isEmpty()) return;

        DatabaseReference statsRef = FirebaseDatabase.getInstance()
                .getReference("stats").child(username);

        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                GameStats easy   = parseFirebase(snapshot, "easy",   0);
                GameStats medium = parseFirebase(snapshot, "medium", 1);
                GameStats hard   = parseFirebase(snapshot, "hard",   2);

                applyToUI(easy, medium, hard);

                // Đồng bộ về SQLite nếu Firebase có điểm cao hơn
                syncToSQLite(easy);
                syncToSQLite(medium);
                syncToSQLite(hard);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private GameStats parseFirebase(DataSnapshot root, String key, int difficulty) {
        DataSnapshot node = root.child(key);
        int score = 0, time = 0, hints = 0;
        if (node.exists()) {
            Long s = node.child("bestScore").getValue(Long.class);
            Long t = node.child("bestTime").getValue(Long.class);
            Long h = node.child("bestHints").getValue(Long.class);
            if (s != null) score = s.intValue();
            if (t != null) time  = t.intValue();
            if (h != null) hints = h.intValue();
        }
        return new GameStats(username, difficulty, score, time, hints);
    }

    private void syncToSQLite(GameStats stats) {
        if (stats.getBestScore() > 0) {
            db.updateBestScore(username, stats.getDifficulty(),
                    stats.getBestScore(), stats.getBestTime(), stats.getBestHints());
        }
    }

    // ── Hiển thị UI

    private void applyToUI(GameStats easy, GameStats medium, GameStats hard) {
        tvEasyScore.setText(String.valueOf(easy.getBestScore()));
        tvMedScore.setText(String.valueOf(medium.getBestScore()));
        tvHardScore.setText(String.valueOf(hard.getBestScore()));

        tvEasyTime.setText(easy.getBestTimeFormatted());
        tvMedTime.setText(medium.getBestTimeFormatted());
        tvHardTime.setText(hard.getBestTimeFormatted());

        tvEasyHints.setText(String.valueOf(easy.getBestHints()));
        tvMedHints.setText(String.valueOf(medium.getBestHints()));
        tvHardHints.setText(String.valueOf(hard.getBestHints()));
    }

    // ── Tiện ích
    private GameStats getStats(List<GameStats> list, int difficulty) {
        if (list != null) {
            for (GameStats s : list) {
                if (s.getDifficulty() == difficulty) return s;
            }
        }
        return new GameStats(username, difficulty, 0, 0, 0);
    }
}