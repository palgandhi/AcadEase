package com.example.acadease;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.acadease.fragments.FacultyAnnouncementFragment;
import com.example.acadease.fragments.FacultyAssignmentFragment;
import com.example.acadease.fragments.FacultyAttendanceFragment;
import com.example.acadease.fragments.FacultyResultsFragment;
import com.example.acadease.fragments.ScheduleFragment; // NEW
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;

public class FacultyDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        bottomNav = findViewById(R.id.bottom_navigation); // Map the BottomNavigationView

        // Set the listener for navigation clicks
        bottomNav.setOnItemSelectedListener(this::onNavigationItemSelected);

        // Wire profile icon clicks from any fragment header
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(FragmentManager fm, Fragment f, android.view.View v, android.os.Bundle savedInstanceState) {
                android.view.View profileIcon = v.findViewById(R.id.profile_icon);
                if (profileIcon != null) {
                    Log.d("FacultyDashboard", "Profile icon found in fragment: " + f.getClass().getSimpleName());
                    if (profileIcon instanceof android.widget.ImageView) {
                        UserDashboardImageHelper.ensureProfileIcon((android.widget.ImageView) profileIcon);
                    }
                    profileIcon.setOnClickListener(view -> {
                        Log.d("FacultyDashboard", "Profile icon clicked! Opening ProfileActivity");
                        startActivity(new Intent(FacultyDashboardActivity.this, ProfileActivity.class));
                    });
                } else {
                    Log.d("FacultyDashboard", "Profile icon NOT found in fragment: " + f.getClass().getSimpleName());
                }
            }
        }, true);

        // Load the initial fragment (Announcements/Home)
        if (savedInstanceState == null) {
            loadFragment(new FacultyAnnouncementFragment());
        }
    }

    /**
     * Handles the selection of a tab item on the Bottom Navigation Bar.
     */
    private boolean onNavigationItemSelected(MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        // Map the IDs from faculty_bottom_nav_menu.xml
        if (itemId == R.id.nav_home) {
            // Loads Announcements Feed
            selectedFragment = new FacultyAnnouncementFragment();
        } else if (itemId == R.id.nav_schedule) {
            selectedFragment = new ScheduleFragment();
        } else if (itemId == R.id.nav_attendance) {
            selectedFragment = new FacultyAttendanceFragment();
        }
        else if (itemId == R.id.nav_assignments) {
            selectedFragment = new FacultyAssignmentFragment();
        }
        else if (itemId == R.id.nav_results) {
            selectedFragment = new FacultyResultsFragment();
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        } else {
            Toast.makeText(this, "Feature not yet implemented.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    /**
     * Swaps the current fragment in the FrameLayout container.
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // R.id.fragment_container is the ID from activity_faculty_dashboard.xml
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}