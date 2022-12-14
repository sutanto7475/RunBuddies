package com.example.runbuddies;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.w3c.dom.Document;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * The purpose of this class is to hold ALL the code to communicate with Firebase.  This class
 * will connect with Firebase auth and Firebase firestore.  Each class that needs to verify
 * authentication OR access data from the database will reference a variable of this class and
 * call a method of this class to handle the task.  Essentially this class is like a "gopher" that
 * will go and do whatever the other classes want or need it to do.  This allows us to keep all
 * our other classes clean of the firebase code and also avoid having to update firebase code
 * in many places.  This is MUCH more efficient and less error prone.
 */
public class FirebaseHelper {
    public static Object FirestoreCallback;
    public final String TAG = "Denna";
    private static String uid = null;      // var will be updated for currently signed in user
    private FirebaseAuth mAuth;
    public static FirebaseFirestore db;
    private ArrayList<Run> myRuns;
    private ArrayList<Profile> myProfile;
    public ArrayList<Profile> matches;
    private Profile currProfile;

    public String myLevel;
    public String myState;
    public String myCity;
    public String myBio;
    public String myName;
    public String myEmail;



    public FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        myRuns = new ArrayList<>();
        myProfile = new ArrayList<>();
        matches = new ArrayList<>();
        currProfile = new Profile();
    }


    public FirebaseAuth getMAuth() {
        return mAuth;
    }

    public void logOutUser() {
        mAuth.signOut();
        uid = null;
    }

    public void attachReadDataToUser() {
        // This is necessary to avoid the issues we ran into with data displaying before it
        // returned from the asynch method calls
        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getUid();
            readProfileData(new FirestoreCallback() {
                @Override
                public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> myProfile) {
                    Log.d(TAG, "Inside attachReadDataToUser, readProfileData " + myProfile.toString());
                    readRunData(new FirestoreCallback() {
                        @Override
                        public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> ProfileList) {
                            Log.d(TAG, "Inside attachReadDataToUser, readRunData onCallback " + myRuns.toString());
                            readProfileMatches(new FirestoreCallback() {
                                @Override
                                public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> ProfileList) {
                                    Log.d(TAG, "Inside attachReadDataToUser, readProfileMatches onCallback " + matches.toString());
                                }
                            });
                        }
                    });
                }
            });

        } else {
            Log.d(TAG, "No one logged in");
        }

    }


    public void addUserToFirestore(String name, String newUID) {
        // Create a new user with their name
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        // Add a new document with a docID = to the authenticated user's UID
        db.collection("users").document(newUID)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, name + "'s user account added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding user account", e);
                    }
                });

    }

    public void addRunData(Run r) {
        // add Run r to the database
        // this method is overloaded and incorporates the interface to handle the asynch calls
        addRunData(r, new FirestoreCallback() {
            @Override
            public void onCallback(ArrayList<Run> myR, ArrayList<Profile> myProfile) {
                Log.i(TAG, "Inside addData, onCallback :" + myRuns.toString());
            }
        });

    }

    private void addRunData(Run r, FirestoreCallback firestoreCallback) {
        db.collection("users").document(uid).collection("myRunList")
                .add(r)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // This will set the docID key for the Run that was just added.
                        db.collection("users").document(uid).collection("myRunList").
                                document(documentReference.getId()).update("docID", documentReference.getId());
                        Log.i(TAG, "just added " + r.getName());
                        readRunData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error adding document", e);
                    }
                });
    }

    public void addProfile(Profile p) {
        // add Profile p to the database
        // this method is overloaded and incorporates the interface to handle the asynch calls
        addProfile(p, new FirestoreCallback() {
            @Override
            public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> myProfile) {
                Log.i(TAG, "Inside addData, onCallback :" + myProfile.toString());
            }
        });
    }

    private void addProfile(Profile p, FirestoreCallback firestoreCallback) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", p.getName());
        user.put("level", p.getLevel());
        user.put("state", p.getState());
        user.put("city", p.getCity());
        user.put("bio", p.getBio());
        user.put("docID", mAuth.getUid());
        user.put("email", p.getEmail());
        // Add a new document with a docID = to the authenticated user's UID
        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, p.getName() + "'s user profile added");
                        attachReadDataToUser();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding profile", e);
                    }
                });
    }


    public ArrayList<Run> getWishListItemsRuns() {
        return myRuns;
    }

    public ArrayList<Profile> getWishListItemProfile() {

        return myProfile;
    }


    public void updateUId(String uid) {
        FirebaseHelper.uid = uid;
    }


    /* https://www.youtube.com/watch?v=0ofkvm97i0s
   This video is good!!!   Basically he talks about what it means for tasks to be asynchronous
   and how you can create an interface and then using that interface pass an object of the interface
   type from a callback method and access it after the callback method.  It also allows you to delay
   certain things from occurring until after the onSuccess is finished.
    */

    public void readRunData(FirestoreCallback firestoreCallback) {
        myRuns.clear();        // empties the AL so that it can get a fresh copy of data
        db.collection("users").document(uid).collection("myRunList")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot doc : task.getResult()) {
                                Run run = doc.toObject(Run.class);
                                myRuns.add(run);
                                Log.i(TAG, "Success adding data: " + myRuns);
                            }
                            Log.i(TAG, "Success reading data: " + myRuns.toString());
                            firestoreCallback.onCallback(myRuns, myProfile);
                        } else {
                            Log.d(TAG, "Error getting documents: " + task.getException());
                        }
                    }
                });

    }

    public void readProfileData(FirestoreCallback firestoreCallback) {
        myProfile.clear();        // empties the AL so that it can get a fresh copy of data
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        currProfile = documentSnapshot.toObject(Profile.class);
                        Log.d(TAG, "Current profile is " + currProfile.getName());
                        firestoreCallback.onCallback(myRuns, myProfile);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "failure in readProfileData " + e.getMessage());
                    }
                });
    }

    public ArrayList<Profile> getAllProfiles(FirestoreCallback firestoreCallback) {
        ArrayList<Profile> allProfiles = new ArrayList<Profile>();
        db.collection("users").document(uid).collection("myProfile")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot doc : task.getResult()) {
                                Profile profile = doc.toObject(Profile.class);
                                allProfiles.add(profile);
                            }

                            Log.i(TAG, "Success reading data: " + allProfiles.toString());
                            firestoreCallback.onCallback(myRuns, myProfile);
                        } else {
                            Log.d(TAG, "Error getting documents: " + task.getException());
                        }
                    }
                });
        return allProfiles;
    }

    //https://stackoverflow.com/questions/48499310/how-to-return-a-documentsnapshot-as-a-result-of-a-method/48500679#48500679
    public interface FirestoreCallback {
        void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> myProfile);
    }


    public void editData(Run r) {
        // edit Run r to the database
        // this method is overloaded and incorporates the interface to handle the asynch calls
        editData(r, new FirestoreCallback() {
            @Override
            public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> myProfile) {
                Log.i(TAG, "Inside editData, onCallback " + myRuns.toString());
            }
        });
    }

    private void editData(Run r, FirestoreCallback firestoreCallback) {
        String docId = r.getDocID();
        db.collection("users").document(uid).collection("myRunList")
                .document(docId)
                .set(r)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Success updating document");
                        readRunData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error updating document", e);
                    }
                });
    }

    public void editProfile(Profile p) {
        // edit Profile p to the database
        // this method is overloaded and incorporates the interface to handle the asynch calls
        editProfile(p, new FirestoreCallback() {

            @Override
            public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> myProfileList) {
                Log.i(TAG, "Inside editData, onCallback " + myProfile.toString());
            }
        });
    }

    public Profile getProfile(){
        return currProfile;
    }
    public ArrayList<Profile> getMatches() {
        return matches;
    }

    public void readProfileMatches(FirestoreCallback firestoreCallback){
        matches.clear();
        CollectionReference usersRef = db.collection("users");
            usersRef.get().

        addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete (@NonNull Task < QuerySnapshot > task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Profile tempP = document.toObject(Profile.class);
                        // made a helper method based on your criteria for a match
                        if (tempP.matches(currProfile)) {
                            matches.add(tempP);
                            Log.d(TAG, "added a match " + tempP.getName());
                        }
                    }

                    firestoreCallback.onCallback(myRuns, myProfile);
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });

    }

    private void editProfile(Profile p, FirestoreCallback firestoreCallback) {
        String docId = p.getDocID();
        db.collection("users").document(uid).collection("myProfileList")
                .document(docId)
                .set(p)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Success updating document");
                        readProfileData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error updating document", e);
                    }
                });
    }

    public void deleteData(Run r) {
        // delete item w from database
        // this method is overloaded and incorporates the interface to handle the asynch calls
        deleteData(r, new FirestoreCallback() {
            @Override
            public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> myProfile) {
                Log.i(TAG, "Inside deleteData, onCallBack" + myRuns.toString());
            }


        });

    }

    private void deleteData(Run r, FirestoreCallback firestoreCallback) {
        // delete item w from database
        String docId = r.getDocID();
        db.collection("users").document(uid).collection("myRunList")
                .document(docId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, r.getName() + " successfully deleted");
                        readRunData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error deleting document", e);
                    }
                });
    }

    public void deleteProfile(Profile p) {
        // delete item w from database
        // this method is overloaded and incorporates the interface to handle the asynch calls
        deleteProfile(p, new FirestoreCallback(){
            @Override
            public void onCallback(ArrayList<Run> myRuns, ArrayList<Profile> myProfile) {
                Log.i(TAG, "Inside deleteData, onCallBack" + myProfile.toString());
            }


        });

    }

    private void deleteProfile(Profile p, FirestoreCallback firestoreCallback) {
        // delete item w from database
        String docId = p.getDocID();
        db.collection("users").document(uid).collection("myProfileList")
                .document(docId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, p.getName() + " successfully deleted");
                        readProfileData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error deleting document", e);
                    }
                });
    }

}


