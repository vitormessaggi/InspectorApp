package com.example.inspecta;

import android.app.Activity;
import android.content.Intent;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

final class Perfil {
    static Task<DocumentSnapshot> buscar(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("usuarios").document(uid).get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return task.getResult().exists() ? task : db.collection("users").document(uid).get();
        });
    }

    static void sair(Activity activity) {
        FirebaseAuth.getInstance().signOut();
        activity.startActivity(new Intent(activity, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
