package com.tbt65133334.sudokuapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;
import com.tbt65133334.sudokuapp.MainActivity;
import com.tbt65133334.sudokuapp.R;

public class LoginFragment extends Fragment {

    private EditText etUsername, etPassword;
    private TextView tvError;
    private Button btnLogin;
    private ProgressBar progressBar;

    private DatabaseReference accountsRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        etUsername  = v.findViewById(R.id.et_username);
        etPassword  = v.findViewById(R.id.et_password);
        tvError     = v.findViewById(R.id.tv_error);
        btnLogin    = v.findViewById(R.id.btn_login);
        progressBar = v.findViewById(R.id.progress_bar);

        accountsRef = FirebaseDatabase.getInstance().getReference("accounts");

        btnLogin.setOnClickListener(b -> attemptLogin());

        return v;
    }

    //Đăng nhập
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validate(username, password)) return;

        setLoading(true);

        accountsRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (!snapshot.exists()) {
                    showError("Tài khoản không tồn tại!");
                    return;
                }
                String savedPwd = snapshot.child("password").getValue(String.class);
                if (password.equals(savedPwd)) {
                    ((MainActivity) requireActivity()).onLoginSuccess(username);
                } else {
                    showError("Mật khẩu không đúng!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                showError("Lỗi kết nối: " + error.getMessage());
            }
        });
    }

    private boolean validate(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            showError("Vui lòng nhập tài khoản!");
            return false;
        }
        if (username.contains(".") || username.contains("#") ||
                username.contains("$") || username.contains("[") || username.contains("]")) {
            showError("Tài khoản không được chứa ký tự đặc biệt: . # $ [ ]");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Vui lòng nhập mật khẩu!");
            return false;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự!");
            return false;
        }
        tvError.setVisibility(View.GONE);
        return true;
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}