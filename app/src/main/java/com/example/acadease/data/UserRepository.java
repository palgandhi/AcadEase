package com.example.acadease.data;

import com.example.acadease.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final CollectionReference usersRef;

    public UserRepository() {
        // Initialize Firebase SDK instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
    }

    public void loginUser(String email, String password, LoginCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // 1. Authentication Success: Now retrieve the User profile (our schema)
                        fetchUserProfile(firebaseUser.getUid(), callback);
                    } else {
                        callback.onFailure(new Exception("Authentication succeeded, but user object is null."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Authentication failed: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    /**
     * Retrieves the User document from the 'users' collection based on UID.
     */
    public void fetchUserProfile(String uid, LoginCallback callback) {
        usersRef.document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 2. Profile Retrieval Success: Map the data to our User model
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUid(documentSnapshot.getId()); // Attach the Document ID (UID)
                            Log.i(TAG, "Login successful. Role: " + user.getRole());
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(new Exception("User data found but failed to parse profile."));
                        }
                    } else {
                        // 3. Integrity Check Failure: User exists in Auth but not in Firestore
                        Log.w(TAG, "User exists in Auth but not in Firestore: " + uid);
                        callback.onFailure(new Exception("Profile missing. Please contact administration."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve user profile: " + e.getMessage());
                    callback.onFailure(new Exception("Failed to retrieve profile data."));
                });
    }

    /**
     * Interface to pass back results to the Activity/UI layer.
     */
    public interface LoginCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    /**
     * Updates the user's profile image URL in Firestore.
     */
    public void updateProfileImageUrl(String uid, String imageUrl, UpdateCallback callback) {
        usersRef.document(uid)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Profile image URL updated successfully");
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update profile image URL: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}