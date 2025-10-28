package com.example.acadease;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;

import android.view.MenuItem;

import com.example.acadease.fragments.student.StudentHomeFragment;
import com.example.acadease.fragments.ScheduleFragment;
import com.example.acadease.fragments.student.StudentAttendanceFragment;
import com.example.acadease.fragments.student.StudentResultsFragment;
import com.example.acadease.fragments.student.StudentAssignmentsFragment;

public class StudentDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(this::onNavigationItemSelected);

        // Wire profile icon across fragments
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(FragmentManager fm, Fragment f, android.view.View v, android.os.Bundle savedInstanceState) {
                android.view.View profileIcon = v.findViewById(R.id.profile_icon);
                if (profileIcon != null) {
                    Log.d("StudentDashboard", "Profile icon found in fragment: " + f.getClass().getSimpleName());
                    if (profileIcon instanceof android.widget.ImageView) {
                        UserDashboardImageHelper.ensureProfileIcon((android.widget.ImageView) profileIcon);
                    }
                    profileIcon.setOnClickListener(view -> {
                        Log.d("StudentDashboard", "Profile icon clicked! Opening ProfileActivity");
                        startActivity(new Intent(StudentDashboardActivity.this, ProfileActivity.class));
                    });
                } else {
                    Log.d("StudentDashboard", "Profile icon NOT found in fragment: " + f.getClass().getSimpleName());
                }
            }
        }, true);

        if (savedInstanceState == null) {
            loadFragment(new StudentHomeFragment());
        }
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        Fragment selected = null;
        int id = item.getItemId();
        if (id == R.id.nav_home) selected = new StudentHomeFragment();
        else if (id == R.id.nav_schedule) {
            ScheduleFragment sf = new ScheduleFragment();
            Bundle args = new Bundle();
            args.putString(ScheduleFragment.ARG_USER_ROLE, "student");
            sf.setArguments(args);
            selected = sf;
        }
        else if (id == R.id.nav_attendance) selected = new StudentAttendanceFragment();
        else if (id == R.id.nav_results) selected = new StudentResultsFragment();
        else if (id == R.id.nav_assignments) selected = new StudentAssignmentsFragment();
        if (selected != null) {
            loadFragment(selected);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_container, fragment);
        tx.commit();
    }

    public void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}