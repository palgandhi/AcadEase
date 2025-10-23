package com.example.acadease.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Needed for the filter buttons
import android.widget.TextView;
import android.widget.Toast;

import com.example.acadease.CreateAnnouncementActivity;
import com.example.acadease.R;
import com.example.acadease.adapters.AnnouncementAdapter;
import com.example.acadease.data.AnnouncementRepository;
import com.example.acadease.data.AdminRepository;
import com.example.acadease.model.Announcement;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment implements AnnouncementAdapter.OnAnnouncementActionListener {

    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton fabCreateAnnouncement;
    private TextView greetingTextView;

    // UI for Filters
    private Button btnFilterAcademic, btnFilterSports, btnFilterAll, btnFilterEvents;
    private String currentFilterCategory = "All"; // Default filter state

    // Repositories
    private AnnouncementRepository announcementRepository;
    private AdminRepository adminRepository;

    private AnnouncementAdapter adapter;

    public HomeFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        fabCreateAnnouncement = view.findViewById(R.id.fab_create_announcement);
        greetingTextView = view.findViewById(R.id.greeting_text);

        // 2. Map Filter Buttons
        btnFilterAcademic = view.findViewById(R.id.btn_filter_academic);
        btnFilterSports = view.findViewById(R.id.btn_filter_sports);
        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterEvents = view.findViewById(R.id.btn_filter_events);

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

        // Initial Load
        loadAnnouncements();

        // Admin functionality: Create New Announcement
        fabCreateAnnouncement.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), CreateAnnouncementActivity.class));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAnnouncements(); // Refresh feed when returning
    }

    private void setupGreeting() {
        String userName = "Pal";
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
     * Updates the current filter state and reloads the announcements.
     */

    // Inside HomeFragment.java

    private void resetFilterButtonStyles() {
        // 1. Retrieve the colors safely
        int defaultBgColor = requireContext().getColor(R.color.white); // Get the integer color for white/light background
        int defaultTextColor = requireContext().getColor(R.color.text_dark); // Get the integer color for dark text

        // 2. Create a ColorStateList from the single color integer (CRITICAL FIX)
        android.content.res.ColorStateList defaultTintList = android.content.res.ColorStateList.valueOf(defaultBgColor);

        // 3. Apply default (unselected) style to all buttons

        // Background Tint
        btnFilterAcademic.setBackgroundTintList(defaultTintList);
        btnFilterSports.setBackgroundTintList(defaultTintList);
        btnFilterAll.setBackgroundTintList(defaultTintList);
        btnFilterEvents.setBackgroundTintList(defaultTintList);

        // Text Color
        btnFilterAcademic.setTextColor(defaultTextColor);
        btnFilterSports.setTextColor(defaultTextColor);
        btnFilterAll.setTextColor(defaultTextColor);
        btnFilterEvents.setTextColor(defaultTextColor);
    }

    /**
     * Updates the current filter state and reloads the announcements.
     */
    private void applyFilter(String category) {
        currentFilterCategory = category;

        // Reset all button styles first
        resetFilterButtonStyles();

        // Find the primary (dark grey) color from the theme
        int primaryDarkGrey = requireContext().getColor(R.color.primary_dark_grey);
        int whiteTextColor = requireContext().getColor(R.color.text_light);

        // Find the button that was clicked
        Button clickedButton = null;
        if ("Academic".equals(category)) clickedButton = btnFilterAcademic;
        else if ("Sports".equals(category)) clickedButton = btnFilterSports;
        else if ("All".equals(category)) clickedButton = btnFilterAll;
        else if ("Events".equals(category)) clickedButton = btnFilterEvents;

        // Apply selected style
        if (clickedButton != null) {
            // Apply the primary dark grey color tint
            clickedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryDarkGrey));
            // Change text to light color
            clickedButton.setTextColor(whiteTextColor);
        }

        loadAnnouncements();
    }

    /**
     * Loads announcements based on the currentFilterCategory.
     */
    private void loadAnnouncements() {
        // CRITICAL: The repository fetch method needs the filter category.
        announcementRepository.fetchAnnouncements(currentFilterCategory, new AnnouncementRepository.AnnouncementsCallback() {
            @Override
            public void onSuccess(List<Announcement> announcements) {
                if (getContext() == null) return;

                if (announcements.isEmpty()) {
                    Toast.makeText(getContext(), "No announcements found for " + currentFilterCategory + ".", Toast.LENGTH_SHORT).show();
                }

                adapter = new AnnouncementAdapter(getContext(), announcements, HomeFragment.this, announcementRepository);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load feed. Check network/rules.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- IMPLEMENTATION OF THE DELETE INTERFACE (unchanged) ---

    @Override
    public void onDeleteClicked(String announcementId, int position) {
        // ... deletion logic remains the same ...
        adminRepository.deleteAnnouncement(announcementId, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                if (adapter != null) {
                    adapter.removeItem(position);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}