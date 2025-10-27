package com.example.acadease.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acadease.R;
import com.example.acadease.adapters.AnnouncementAdapter;
import com.example.acadease.data.AnnouncementRepository;
import com.example.acadease.data.AdminRepository; // Needed for the delete method call
import com.example.acadease.model.Announcement;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;
import com.example.acadease.ProfileActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// CRITICAL: Implements the adapter interface for deletion handling (even if denied by rules)
public class FacultyAnnouncementFragment extends Fragment implements AnnouncementAdapter.OnAnnouncementActionListener {

    private RecyclerView recyclerView;
    private TextView greetingTextView;

    // UI elements for Filters
    private Button btnFilterAcademic, btnFilterSports, btnFilterAll, btnFilterEvents;
    private String currentFilterCategory = "All"; // Default filter state

    // Repositories
    private AnnouncementRepository announcementRepository;
    private AdminRepository adminRepository;
    private AnnouncementAdapter adapter;


    public FacultyAnnouncementFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Reuses the layout structure (fragment_home.xml)
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Repositories
        announcementRepository = new AnnouncementRepository();
        adminRepository = new AdminRepository();

        // 1. Map Core UI components
        recyclerView = view.findViewById(R.id.announcements_recycler_view);
        // We must hide the FAB since Faculty should not be able to create announcements here
        view.findViewById(R.id.fab_create_announcement).setVisibility(View.GONE);
        greetingTextView = view.findViewById(R.id.greeting_text);

        // 2. Map Filter Buttons
        btnFilterAcademic = view.findViewById(R.id.btn_filter_academic);
        btnFilterSports = view.findViewById(R.id.btn_filter_sports);
        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterEvents = view.findViewById(R.id.btn_filter_events);

        // Profile icon
        View profileIcon = view.findViewById(R.id.profile_icon);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProfileActivity.class)));
        }

        // Setup Header Greeting
        setupGreeting();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Set Filter Listeners
        View.OnClickListener filterListener = v -> {
            Button clickedButton = (Button) v;
            String category = clickedButton.getText().toString();
            applyFilter(category);
        };

        btnFilterAcademic.setOnClickListener(filterListener);
        btnFilterSports.setOnClickListener(filterListener);
        btnFilterAll.setOnClickListener(filterListener);
        btnFilterEvents.setOnClickListener(filterListener);

        // Initial Load and style the 'All' button as active
        loadAnnouncements();
        applyFilter(currentFilterCategory);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAnnouncements(); // Refresh feed when returning
    }

    private void setupGreeting() {
        String userName = "Dr. Smith";
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        greetingTextView.setText(String.format("%s, %s", greeting, userName));
    }

    /**
     * Applies the selected category filter and updates button aesthetics.
     */
    private void applyFilter(String category) {
        currentFilterCategory = category;
        resetFilterButtonStyles();

        int primaryDarkGrey = requireContext().getColor(R.color.primary_dark_grey);
        int whiteTextColor = requireContext().getColor(R.color.text_light);

        Button clickedButton = null;
        if ("Academic".equals(category)) clickedButton = btnFilterAcademic;
        else if ("Sports".equals(category)) clickedButton = btnFilterSports;
        else if ("All".equals(category)) clickedButton = btnFilterAll;
        else if ("Events".equals(category)) clickedButton = btnFilterEvents;

        if (clickedButton != null) {
            clickedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryDarkGrey));
            clickedButton.setTextColor(whiteTextColor);
        }

        loadAnnouncements();
    }

    private void resetFilterButtonStyles() {
        int defaultBgColor = requireContext().getColor(R.color.white);
        int defaultTextColor = requireContext().getColor(R.color.text_dark);
        android.content.res.ColorStateList defaultTintList = android.content.res.ColorStateList.valueOf(defaultBgColor);

        // Apply default style to all buttons
        btnFilterAcademic.setBackgroundTintList(defaultTintList);
        btnFilterSports.setBackgroundTintList(defaultTintList);
        btnFilterAll.setBackgroundTintList(defaultTintList);
        btnFilterEvents.setBackgroundTintList(defaultTintList);

        btnFilterAcademic.setTextColor(defaultTextColor);
        btnFilterSports.setTextColor(defaultTextColor);
        btnFilterAll.setTextColor(defaultTextColor);
        btnFilterEvents.setTextColor(defaultTextColor);
    }

    private void loadAnnouncements() {
        announcementRepository.fetchAnnouncements(currentFilterCategory, new AnnouncementRepository.AnnouncementsCallback() {
            @Override
            public void onSuccess(List<Announcement> announcements) {
                if (getContext() == null) return;

                if (announcements.isEmpty()) {
                    Toast.makeText(getContext(), "No announcements found for " + currentFilterCategory + ".", Toast.LENGTH_SHORT).show();
                    // Initialize with an empty list if no results
                    adapter = new AnnouncementAdapter(getContext(), new ArrayList<>(), FacultyAnnouncementFragment.this, announcementRepository);
                } else {
                    // Pass the repository instance for asynchronous poster lookups
                    adapter = new AnnouncementAdapter(getContext(), announcements, FacultyAnnouncementFragment.this, announcementRepository);
                }
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load feed. Check network/rules.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- IMPLEMENTATION OF THE DELETE INTERFACE (For Compliance/Safety) ---
    // Faculty should not be able to delete global announcements.
    @Override
    public void onDeleteClicked(String announcementId, int position) {
        Toast.makeText(getContext(), "Access Denied: Only portal Administrators can delete announcements.", Toast.LENGTH_LONG).show();
        // The database security rule (if isAdmin()) will already prevent this write,
        // but this provides immediate UI feedback.
    }
}