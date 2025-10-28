package com.example.acadease;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;

import com.example.acadease.data.UserRepository;
import com.example.acadease.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.example.acadease.util.ImageLoader;
import com.example.acadease.data.StorageRepository;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTv, emailTv, roleTv, customIdTv, semesterTv;
    private Button logoutBtn;
    private Button changeImageBtn;
    private ImageView avatarIv;

    private UserRepository userRepository;
    private StorageRepository storageRepository;

    // Image picker
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleImagePicked(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userRepository = new UserRepository();
        storageRepository = new StorageRepository();

        nameTv = findViewById(R.id.profile_name);
        emailTv = findViewById(R.id.profile_email);
        roleTv = findViewById(R.id.profile_role);
        customIdTv = findViewById(R.id.profile_custom_id);
        semesterTv = findViewById(R.id.profile_semester);
        logoutBtn = findViewById(R.id.btn_logout);
        changeImageBtn = findViewById(R.id.btn_change_image);
        avatarIv = findViewById(R.id.profile_avatar);

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
                    // Load avatar
                    ImageLoader.load(avatarIv, user.getProfileImageUrl(), R.drawable.person);
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

        changeImageBtn.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void handleImagePicked(Uri uri) {
        if (userRepository.getCurrentFirebaseUser() == null) return;
        String uid = userRepository.getCurrentFirebaseUser().getUid();
        // Upload to the same path pattern as Admin flow: profiles/{uid}/profile.jpg
        storageRepository.uploadProfileImage(uri, uid, new StorageRepository.UploadCallback() {
            @Override public void onSuccess(String downloadUrl) {
                userRepository.updateProfileImageUrl(uid, downloadUrl, new UserRepository.UpdateCallback() {
                    @Override public void onSuccess() {
                        ImageLoader.load(avatarIv, downloadUrl, R.drawable.person);
                        UserDashboardImageHelper.invalidate();
                    }
                    @Override public void onFailure(Exception e) { /* ignore for now */ }
                });
            }
            @Override public void onFailure(Exception e) {
                // ignore for now or add toast (not adding UI text per instruction)
            }
        });
    }
}
