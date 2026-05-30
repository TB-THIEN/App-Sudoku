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

public class RegisterFragment extends Fragment {

    private EditText    etUsername, etPassword, etConfirmPassword;
    private TextView    tvError;
    private Button      btnRegister;
    private ProgressBar progressBar;

    private DatabaseReference accountsRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register, container, false);

        etUsername        = v.findViewById(R.id.et_username);
        etPassword        = v.findViewById(R.id.et_password);
        etConfirmPassword = v.findViewById(R.id.et_confirm_password);
        tvError           = v.findViewById(R.id.tv_error);
        btnRegister       = v.findViewById(R.id.btn_register);
        progressBar       = v.findViewById(R.id.progress_bar);

        accountsRef = FirebaseDatabase.getInstance().getReference("accounts");

        btnRegister.setOnClickListener(b -> attemptRegister());

        v.findViewById(R.id.tv_go_login).setOnClickListener(b ->
                requireActivity().getSupportFragmentManager().popBackStack());

        return v;
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (!validate(username, password, confirm)) return;

        setLoading(true);

        // Kiểm tra username đã tồn tại chưa
        accountsRef.child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            setLoading(false);
                            showError("Tên đăng nhập đã được sử dụng!"); return;
                        }
                        saveAccount(username, password);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setLoading(false);
                        showError("Lỗi kết nối: " + error.getMessage());
                    }
                });
    }

    private void saveAccount(String username, String password) {
        accountsRef.child(username).child("password").setValue(password)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Đăng ký thành công! Chào " + username,
                            Toast.LENGTH_SHORT).show();
                    // Đăng nhập luôn sau khi đăng ký
                    ((MainActivity) requireActivity()).onLoginSuccess(username);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Đăng ký thất bại: " + e.getMessage());
                });
    }

    private boolean validate(String username, String password, String confirm) {
        if (TextUtils.isEmpty(username)) {
            showError("Vui lòng nhập tên đăng nhập!"); return false;
        }
        if (username.length() < 3) {
            showError("Tên đăng nhập phải có ít nhất 3 ký tự!"); return false;
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            showError("Tên đăng nhập chỉ được dùng chữ, số và dấu _"); return false;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Vui lòng nhập mật khẩu!"); return false;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự!"); return false;
        }
        if (!password.equals(confirm)) {
            showError("Mật khẩu xác nhận không khớp!"); return false;
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
        btnRegister.setEnabled(!loading);
    }
}