package com.example.myapplication.ui.slideshow;

import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentSlideshowBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SlideshowFragment extends Fragment {

    private ArrayList<String> items;
    private ArrayList<String> ids;
    private ListView list;
    private Button button;
    private ArrayAdapter<String> itemsAdapter;
    private FragmentSlideshowBinding binding;
    private int selectedPosition = -1;
    private FirebaseFirestore db;

    Dialog dialog;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        items = new ArrayList<>();
        ids = new ArrayList<>();

        CollectionReference tasksRef = db.collection("tasks");

        tasksRef.whereEqualTo("status", "0")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String itemText = document.getString("item");
                            if (itemText != null) {
                                items.add(itemText); // optionally use status too
                                ids.add(document.getId());
                            }
                        }
                        itemsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to load tasks.", Toast.LENGTH_SHORT).show();
                    }
                });

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        items = new ArrayList<>();
        itemsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, items);
        binding.list.setAdapter(itemsAdapter);

        dialog = new Dialog(requireActivity());
        dialog.setContentView(R.layout.list_on_click_dialog);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dialog_bg));
        dialog.setCancelable(true);

        Button btnDel = dialog.findViewById(R.id.btn_delete);
        Button btnEdt = dialog.findViewById(R.id.btn_edit);
        Button btnDon = dialog.findViewById(R.id.btn_done);

        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedPosition != -1) {
                    updateStatus("2"); // status "1" means "Deleted0"
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Error encountered", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEdt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.editText.setText(items.get(selectedPosition));
                dialog.dismiss();
            }
        });

        btnDon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus("1"); // status "1" means "Completed"
                dialog.dismiss();
            }
        });


        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItem();
            }
        });

        binding.editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    addItem(); // Call your addItem() method
                    return true; // Indicate that the event was handled
                }
                return false;
            }
        });

        binding.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
                dialog.show();
            }
        });

        return binding.getRoot();
    }

    private void remove(int position) {
        Toast.makeText(getContext(), "Item Removed", Toast.LENGTH_LONG).show();
        items.remove(position);
        itemsAdapter.notifyDataSetChanged();
        selectedPosition = -1;
    }

    private void updateStatus(String status) {
        String docId = ids.get(selectedPosition);

        db.collection("tasks")
                .document(docId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    remove(selectedPosition);
                    selectedPosition = -1;
                    Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();
                });
    }

    private void addItem() {
        EditText input = binding.editText;
        String itemText = input.getText().toString().trim();

        if (selectedPosition != -1) {
            String docId = ids.get(selectedPosition);

            db.collection("tasks")
                    .document(docId)
                    .update("item", itemText)
                    .addOnSuccessListener(aVoid -> {
                        items.set(selectedPosition, itemText); // Update UI
                        itemsAdapter.notifyDataSetChanged();
                        binding.editText.setText("");
                        selectedPosition = -1;
                        Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show();
                    });
        } else {
            if (!itemText.isEmpty()) {
                // Create a task object
                Map<String, Object> task = new HashMap<>();
                task.put("item", itemText);
                task.put("status", "0"); // or "pending", etc.
                task.put("lastModified", FieldValue.serverTimestamp());

                // Add to Firestore
                FirebaseFirestore.getInstance()
                        .collection("tasks")
                        .add(task)
                        .addOnSuccessListener(documentReference -> {
                            itemsAdapter.add(itemText); // Add to local list only after success
                            ids.add(documentReference.getId());
                            input.setText("");
                            Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to add task", Toast.LENGTH_SHORT).show();
                        });

            } else {
                Toast.makeText(requireContext(), "Please enter a valid text", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}