package com.example.myapplication.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentGalleryBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class GalleryFragment extends Fragment {
    private ArrayList<String> items;
    private ArrayAdapter<String> itemsAdapter;
    private FragmentGalleryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        items = new ArrayList<>();

        CollectionReference tasksRef = db.collection("tasks");

        tasksRef.whereEqualTo("status", "2")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String itemText = document.getString("item");
                            if (itemText != null) {
                                items.add("❌   -   " + itemText); // optionally use status too
                            }
                        }
                        itemsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to load tasks.", Toast.LENGTH_SHORT).show();
                    }
                });

        binding = FragmentGalleryBinding.inflate(inflater, container, false);

        items = new ArrayList<>();
        itemsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, items);
        binding.list.setAdapter(itemsAdapter);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}