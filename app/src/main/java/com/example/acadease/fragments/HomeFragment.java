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
import android.widget.Toast;

import com.example.acadease.CreateAnnouncementActivity; // Assume this activity is created
import com.example.acadease.R;
import com.example.acadease.adapters.AnnouncementAdapter;
import com.example.acadease.data.AnnouncementRepository;
import com.example.acadease.data.AdminRepository; // Needed for deletion write operation
import com.example.acadease.model.Announcement;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

// CRITICAL: Implement the adapter interface
public class HomeFragment extends Fragment implements AnnouncementAdapter.OnAnnouncementActionListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fabCreateAnnouncement;
    private AnnouncementRepository announcementRepository;
    private AdminRepository adminRepository; // Used for deleting announcement
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

        announcementRepository = new AnnouncementRepository();
        adminRepository = new AdminRepository(); // Initialize AdminRepository for deletes

        recyclerView = view.findViewById(R.id.announcements_recycler_view);
        fabCreateAnnouncement = view.findViewById(R.id.fab_create_announcement);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadAnnouncements();

        fabCreateAnnouncement.setOnClickListener(v -> {
            // Start the activity to create a new announcement
            startActivity(new Intent(getContext(), CreateAnnouncementActivity.class));
        });
    }

    private void loadAnnouncements() {
        announcementRepository.fetchAnnouncements(new AnnouncementRepository.AnnouncementsCallback() {
            @Override
            public void onSuccess(List<Announcement> announcements) {
                if (getContext() == null || announcements.isEmpty()) return;

                // CRITICAL BINDING: Pass 'this' as the listener
                adapter = new AnnouncementAdapter(getContext(), announcements, HomeFragment.this);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load announcements: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- IMPLEMENTATION OF THE DELETE INTERFACE ---

    @Override
    public void onDeleteClicked(String announcementId, int position) {
        // Call AdminRepository to perform the delete operation
        adminRepository.deleteAnnouncement(announcementId, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                // CRITICAL: Update the UI by removing the item from the adapter list
                adapter.removeItem(position);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // NOTE: Lifecycle method to refresh feed when returning to the fragment
    @Override
    public void onResume() {
        super.onResume();
        loadAnnouncements();
    }
}