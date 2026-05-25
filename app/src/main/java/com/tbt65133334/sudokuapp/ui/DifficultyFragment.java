package com.tbt65133334.sudokuapp.ui;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;
import com.tbt65133334.sudokuapp.ui.GameFragment;

public class DifficultyFragment extends Fragment {

    public static final int EASY   = 0;
    public static final int MEDIUM = 1;
    public static final int HARD   = 2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_difficulty, container, false);

        v.findViewById(R.id.btn_easy).setOnClickListener(b -> startGame(EASY));
        v.findViewById(R.id.btn_medium).setOnClickListener(b -> startGame(MEDIUM));
        v.findViewById(R.id.btn_hard).setOnClickListener(b -> startGame(HARD));

        v.findViewById(R.id.btn_home).setOnClickListener(b ->
                ((MainActivity) requireActivity()).goHome());
        v.findViewById(R.id.btn_back).setOnClickListener(b ->
                requireActivity().getSupportFragmentManager().popBackStack());

        return v;
    }

    private void startGame(int difficulty) {
        Bundle args = new Bundle();
        args.putInt("difficulty", difficulty);
        GameFragment gameFragment = new GameFragment();
        gameFragment.setArguments(args);
        ((MainActivity) requireActivity()).navigateTo(gameFragment, true);
    }
}