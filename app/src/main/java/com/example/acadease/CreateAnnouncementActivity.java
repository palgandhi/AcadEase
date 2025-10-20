package com.example.acadease;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.example.acadease.data.AdminRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class CreateAnnouncementActivity extends AppCompatActivity {

    private static final String TAG = "CreateAnnounceActivity";

    private EditText titleEt, bodyEt, imgUrlEt;
    private AutoCompleteTextView categoryAcTv;
    private LinearLayout targetRoleContainer; // Maps the new Checkbox container
    private Button postButton;

    private AdminRepository adminRepository;

    private final String[] categories = {"academic", "club", "sports", "admin"};
    private final String[] roles = {"all", "student", "faculty", "admin"}; // All possible roles

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_announcement);

        adminRepository = new AdminRepository();

        // 1. Map UI components
        titleEt = findViewById(R.id.announcement_title_edit_text);
        bodyEt = findViewById(R.id.announcement_body_edit_text);
        imgUrlEt = findViewById(R.id.announcement_img_url_edit_text);
        categoryAcTv = findViewById(R.id.announcement_category_dropdown);
        targetRoleContainer = findViewById(R.id.announcement_target_role_container); // Map container
        postButton = findViewById(R.id.btn_post_announcement);

        // 2. Setup Category Dropdown
        setupDropdown(categoryAcTv, categories);

        // 3. Setup Target Role Checkboxes (Multi-Select)
        setupRoleCheckboxes();

        // 4. Set Listener
        postButton.setOnClickListener(v -> handlePostAnnouncement());
    }

    private void setupDropdown(AutoCompleteTextView view, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                items
        );
        view.setAdapter(adapter);
        view.setText(items[0], false);
    }

    private void setupRoleCheckboxes() {
        for (String role : roles) {
            CheckBox cb = new CheckBox(this);
            cb.setText(role.toUpperCase());
            cb.setTag(role); // Store the actual schema string

            // Set layout parameters to space them out
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            cb.setLayoutParams(params);
            targetRoleContainer.addView(cb);
        }
    }

    // CRITICAL: Collects the checked values into a List<String>
    private List<String> getSelectedRoles() {
        List<String> selectedRoles = new ArrayList<>();
        for (int i = 0; i < targetRoleContainer.getChildCount(); i++) {
            View child = targetRoleContainer.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                if (cb.isChecked()) {
                    selectedRoles.add(cb.getTag().toString());
                }
            }
        }
        return selectedRoles;
    }

    private void handlePostAnnouncement() {
        // 1. Capture Data
        String title = titleEt.getText().toString().trim();
        String body = bodyEt.getText().toString().trim();
        String imgUrl = imgUrlEt.getText().toString().trim();
        String category = categoryAcTv.getText().toString();
        List<String> targetRoles = getSelectedRoles();

        // Ensure Admin is logged in to get UID
        String postedByUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "ADMIN_TEST_UID"; // Fallback, though user should be logged in

        // 2. Validation
        if (title.isEmpty() || body.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Title, body, and category are required.", Toast.LENGTH_LONG).show();
            return;
        }
        if (targetRoles.isEmpty()) {
            Toast.makeText(this, "Please select at least one Target Audience.", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Call Repository
        adminRepository.createAnnouncement(title, body, imgUrl, targetRoles, category, postedByUid, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(CreateAnnouncementActivity.this, message, Toast.LENGTH_LONG).show();
                finish(); // Close this Activity on success
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CreateAnnouncementActivity.this, "Post Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Post Failed:", e);
            }
        });
    }
}