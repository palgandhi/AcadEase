package com.example.acadease;

import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.acadease.data.UserRepository;
import com.example.acadease.model.User;
import com.example.acadease.util.ImageLoader;

public class UserDashboardImageHelper {
    @Nullable private static String cachedUrl;

    public static void ensureProfileIcon(ImageView imageView) {
        UserRepository repo = new UserRepository();
        if (repo.getCurrentFirebaseUser() == null) {
            ImageLoader.load(imageView, null, R.drawable.person);
            return;
        }
        String uid = repo.getCurrentFirebaseUser().getUid();
        if (cachedUrl == null) {
            repo.fetchUserProfile(uid, new UserRepository.LoginCallback() {
                @Override public void onSuccess(User user) {
                    cachedUrl = user.getProfileImageUrl();
                    ImageLoader.load(imageView, cachedUrl, R.drawable.person);
                }
                @Override public void onFailure(Exception e) {
                    ImageLoader.load(imageView, null, R.drawable.person);
                }
            });
        } else {
            ImageLoader.load(imageView, cachedUrl, R.drawable.person);
        }
    }

    public static void invalidate() {
        cachedUrl = null;
    }
}
