package com.example.acadease;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.acadease.data.UserRepository;
import com.example.acadease.model.User;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTv, emailTv, roleTv, customIdTv, semesterTv;
    private Button logoutBtn;

    private UserRepository userRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userRepository = new UserRepository();

        nameTv = findViewById(R.id.profile_name);
        emailTv = findViewById(R.id.profile_email);
        roleTv = findViewById(R.id.profile_role);
        customIdTv = findViewById(R.id.profile_custom_id);
        semesterTv = findViewById(R.id.profile_semester);
        logoutBtn = findViewById(R.id.btn_logout);

        if (userRepository.getCurrentFirebaseUser() != null) {
            String uid = userRepository.getCurrentFirebaseUser().getUid();
            userRepository.fetchUserProfile(uid, new UserRepository.LoginCallback() {
                @Override
                public void onSuccess(User user) {
                    nameTv.setText(user.getName() != null ? user.getName() : "");
                    emailTv.setText(user.getEmail() != null ? user.getEmail() : "");
                    roleTv.setText(user.getRole() != null ? user.getRole() : "");
                    String customId = "admin".equalsIgnoreCase(user.getRole()) || "faculty".equalsIgnoreCase(user.getRole())
                            ? user.getFacultyId()
                            : user.getStudentId();
                    customIdTv.setText(customId != null ? customId : "");
                    if ("student".equalsIgnoreCase(user.getRole())) {
                        semesterTv.setText(String.valueOf(user.getCurrentSemester()));
                    } else {
                        semesterTv.setText("-");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // leave defaults
                }
            });
        }

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
