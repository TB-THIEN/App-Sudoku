package com.tbt65133334.sudokuapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.tbt65133334.sudokuapp.ui.HomeFragment;
import com.tbt65133334.sudokuapp.ui.LoginFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private String currentUsername = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            navigateTo(new LoginFragment(), false);
        }
    }

    public void onLoginSuccess(String username) {
        this.currentUsername = username;
        Log.d(TAG, "Đăng nhập thành công: " + username);
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        navigateTo(new HomeFragment(), false);
    }

    public void onLogout() {
        this.currentUsername = null;
        Log.d(TAG, "Đã đăng xuất");
        // Xóa toàn bộ back-stack, quay về màn hình đăng nhập
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        navigateTo(new LoginFragment(), false);
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        var tx = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }

    public void goHome() {
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        navigateTo(new HomeFragment(), false);
    }
}