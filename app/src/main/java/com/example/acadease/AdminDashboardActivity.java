package com.example.acadease;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.acadease.fragments.HomeFragment;
import com.example.acadease.fragments.UserManagementFragment;
import com.example.acadease.fragments.ScheduleManagementFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;

public class AdminDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Uses the activity_admin_dashboard.xml layout
        setContentView(R.layout.activity_admin_dashboard);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Set the listener for when a navigation item is selected
        bottomNav.setOnItemSelectedListener(this::onNavigationItemSelected);

        // Wire profile icon clicks from any fragment header
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(FragmentManager fm, Fragment f, android.view.View v, android.os.Bundle savedInstanceState) {
                android.view.View profileIcon = v.findViewById(R.id.profile_icon);
                if (profileIcon != null) {
                    Log.d("AdminDashboard", "Profile icon found in fragment: " + f.getClass().getSimpleName());
                    if (profileIcon instanceof android.widget.ImageView) {
                        UserDashboardImageHelper.ensureProfileIcon((android.widget.ImageView) profileIcon);
                    }
                    profileIcon.setOnClickListener(view -> {
                        Log.d("AdminDashboard", "Profile icon clicked! Opening ProfileActivity");
                        startActivity(new Intent(AdminDashboardActivity.this, ProfileActivity.class));
                    });
                } else {
                    Log.d("AdminDashboard", "Profile icon NOT found in fragment: " + f.getClass().getSimpleName());
                }
            }
        }, true);

        // Load the initial fragment (HomeFragment/Announcements) when the Activity starts
        if (savedInstanceState == null) {
            HomeFragment adminHome = new HomeFragment();
            android.os.Bundle args = new android.os.Bundle();
            args.putBoolean("CAN_DELETE", true);
            adminHome.setArguments(args);
            loadFragment(adminHome);
        }
    }

    /**
     * Handles the selection of a tab item on the Bottom Navigation Bar.
     */
    private boolean onNavigationItemSelected(MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        // Use the IDs defined in res/menu/admin_bottom_nav_menu.xml
        if (itemId == R.id.nav_home) {
            HomeFragment adminHome = new HomeFragment();
            android.os.Bundle args = new android.os.Bundle();
            args.putBoolean("CAN_DELETE", true);
            adminHome.setArguments(args);
            selectedFragment = adminHome;
        } else if (itemId == R.id.nav_users) {
            selectedFragment = new UserManagementFragment();
        } else if (itemId == R.id.nav_schedule) {
            selectedFragment = new ScheduleManagementFragment();
        }

        // Logout feature is handled separately, but often included in an overflow menu.
        // For simplicity, if we detect an unhandled ID, we just toast.
        if (selectedFragment == null) {
            Toast.makeText(this, "Navigation Error.", Toast.LENGTH_SHORT).show();
            return false;
        }

        loadFragment(selectedFragment);
        return true;
    }

    /**
     * Replaces the content of the FrameLayout container with the chosen fragment.
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // R.id.fragment_container is the ID from activity_admin_dashboard.xml
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    // NOTE: You need to implement a mechanism to log out,
    // likely via a button or menu option in one of the fragments or the toolbar.
    public void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        // Return user to the login screen and clear all history
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
    }
}