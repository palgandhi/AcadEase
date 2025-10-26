package com.example.acadease.data;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class StorageRepository {
    private final FirebaseStorage storage;

    public StorageRepository() {
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Uploads an image file to Firebase Storage and returns the public URL.
     * @param fileUri Local Uri of the file to upload.
     * @param userId The UID used to create a unique path (e.g., profiles/UID/profile.jpg).
     * @param callback Interface for success/failure handling.
     */
    public void uploadProfileImage(Uri fileUri, String userId, UploadCallback callback) {
        // Define the storage path: profiles/USER_UID/profile.jpg
        StorageReference ref = storage.getReference()
                .child("profiles")
                .child(userId)
                .child("profile.jpg");

        // Start the upload task
        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL once the upload is complete
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        callback.onSuccess(uri.toString()); // Pass the public URL back
                    }).addOnFailureListener(e -> {
                        callback.onFailure(new Exception("Failed to get download URL: " + e.getMessage()));
                    });
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(new Exception("Image upload failed: " + e.getMessage()));
                });
    }

    public void uploadFile(Uri fileUri, String filePath, UploadCallback callback) {
        // 1. Define the storage path
        StorageReference ref = storage.getReference().child(filePath);

        // 2. Start the upload task
        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL once the upload is complete
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        callback.onSuccess(uri.toString()); // Pass the public URL back
                    }).addOnFailureListener(e -> {
                        callback.onFailure(new Exception("Failed to get download URL: " + e.getMessage()));
                    });
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(new Exception("File upload failed: " + e.getMessage()));
                });
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }
}