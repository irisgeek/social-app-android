package com.rozdoum.socialcomponents.managers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;
import com.rozdoum.socialcomponents.ApplicationHelper;
import com.rozdoum.socialcomponents.enums.ProfileStatus;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectExistListener;
import com.rozdoum.socialcomponents.managers.listeners.OnProfileCreatedListener;
import com.rozdoum.socialcomponents.model.Profile;
import com.rozdoum.socialcomponents.utils.LogUtil;
import com.rozdoum.socialcomponents.utils.PreferencesUtil;

/**
 * Created by Kristina on 10/28/16.
 */

public class ProfileManager {

    private static final String TAG = ProfileManager.class.getSimpleName();
    private static ProfileManager instance;

    private Context context;
    private DatabaseHelper databaseHelper;

    public static ProfileManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileManager(context);
        }

        return instance;
    }

    private ProfileManager(Context context) {
        this.context = context;
        databaseHelper = ApplicationHelper.getDatabaseHelper();
    }

    public Profile buildProfile(FirebaseUser firebaseUser) {
        Profile profile = new Profile(firebaseUser.getUid());
        profile.setEmail(firebaseUser.getEmail());
        profile.setUsername(firebaseUser.getDisplayName());
        profile.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
        return profile;
    }

    public void isProfileExist(String id, final OnObjectExistListener<Profile> onObjectExistListener) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child("profiles").child(id);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                onObjectExistListener.onDataChanged(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void createProfile(Profile profile, OnProfileCreatedListener onProfileCreatedListener) {
        createProfile(profile, null, onProfileCreatedListener);
    }

    public void createProfile(final Profile profile, Uri imageUri, final OnProfileCreatedListener onProfileCreatedListener) {
        if (imageUri == null) {
            databaseHelper.createOrUpdateProfile(profile, onProfileCreatedListener);
            return;
        }

        UploadTask uploadTask = databaseHelper.uploadImage(imageUri);

        if (uploadTask != null) {
            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUrl = task.getResult().getDownloadUrl();
                        LogUtil.logDebug(TAG, "successful upload image, image url: " + String.valueOf(downloadUrl));

                        profile.setPhotoUrl(downloadUrl.toString());
                        databaseHelper.createOrUpdateProfile(profile, onProfileCreatedListener);

                    } else {
                        onProfileCreatedListener.onProfileCreated(false);
                        LogUtil.logDebug(TAG, "fail to upload image");
                    }

                }
            });
        } else {
            onProfileCreatedListener.onProfileCreated(false);
            LogUtil.logDebug(TAG, "fail to upload image");
        }
    }

    public ProfileStatus checkProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return ProfileStatus.NOT_AUTHORIZED;
        } else if (!PreferencesUtil.isProfileCreated(context)){
            return ProfileStatus.NO_PROFILE;
        } else {
            return ProfileStatus.PROFILE_CREATED;
        }
    }
}
