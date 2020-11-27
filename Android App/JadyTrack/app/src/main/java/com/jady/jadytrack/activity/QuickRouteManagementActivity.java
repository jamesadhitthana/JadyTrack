package com.jady.jadytrack.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jady.jadytrack.OnRecordClick;
import com.jady.jadytrack.R;
import com.jady.jadytrack.entity.QuickRouteHistory;
import com.jady.jadytrack.fragment.QuickRouteHistoryAdapter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class QuickRouteManagementActivity extends AppCompatActivity implements OnRecordClick {

    private RecyclerView recyclerView;
    private QuickRouteHistoryAdapter quickRouteHistoryAdapter;
    private ArrayList<QuickRouteHistory> quickRouteHistoryArrayList;
    private ArrayList<String> historyId;
    private Button editQuickRoute, deleteQuickRoute;
    private int selected_position = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_route_management);

        Intent intentKu = getIntent();

        String userUID = intentKu.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_UID);

        final DatabaseReference historyReference = FirebaseDatabase.getInstance().getReference().child("users/" + userUID + "/trackingHistory");

        //*Back Button//
        final ImageButton buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //END OF: Back Button--//

        quickRouteHistoryArrayList = new ArrayList<>();
        historyId = new ArrayList<>();

        editQuickRoute = (Button) findViewById(R.id.editQuickRoute);
        editQuickRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(selected_position>=0){

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QuickRouteManagementActivity.this);

                    alertDialogBuilder.setTitle(getResources().getString(R.string.alert_title_edit_quick_route));

                    final EditText input = new EditText(QuickRouteManagementActivity.this);
                    input.setText(quickRouteHistoryArrayList.get(selected_position).getNameQuickRoute());
                    input.setSelectAllOnFocus(true);
                    input.setPadding(70, 40, 70, 30);
                    alertDialogBuilder.setView(input);
                    alertDialogBuilder.setPositiveButton(getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            historyReference.child(historyId.get(selected_position)).setValue(input.getText().toString());

                            quickRouteHistoryArrayList.get(selected_position).setNameQuickRoute(input.getText().toString());
                            quickRouteHistoryAdapter.notifyDataSetChanged();
                        }
                    });

                    alertDialogBuilder.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.setCancelable(false);
                    alertDialog.show();

                }

            }
        });
        deleteQuickRoute = (Button) findViewById(R.id.deleteQuickRoute);
        deleteQuickRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(selected_position>=0){

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QuickRouteManagementActivity.this);
                    alertDialogBuilder.setMessage(getResources().getString(R.string.alert_title_delete_quick_route));
                    alertDialogBuilder.setPositiveButton(getResources().getString(R.string.button_yes),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {

                                    historyReference.child(historyId.get(selected_position)).removeValue();

                                    quickRouteHistoryArrayList.remove(selected_position);
                                    quickRouteHistoryAdapter.notifyDataSetChanged();
                                }
                            });

                    alertDialogBuilder.setNegativeButton(getResources().getString(R.string.button_no),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.setCancelable(false);
                    alertDialog.show();

                }

            }
        });

        final boolean[] isHistoryCollected = {false};

        historyReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null && !isHistoryCollected[0]) {

                    HashMap<String, Object> data =
                            (HashMap<String, Object>) dataSnapshot.getValue();

                    for (HashMap.Entry<String, Object> entry : data.entrySet()) {

                        // epoch to date
                        String quickRouteHistoryName = entry.getValue().toString();
                        String formatted = null;

                        if(isValidDate(quickRouteHistoryName)){
                            Date date = new Date(Long.parseLong(quickRouteHistoryName));
                            DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            format.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                            formatted = format.format(date);
                        } else {
                            formatted = quickRouteHistoryName;
                        }

                        quickRouteHistoryArrayList.add(new QuickRouteHistory(formatted));
                        historyId.add(entry.getKey());

                        quickRouteHistoryAdapter.notifyDataSetChanged();
                    }
                    isHistoryCollected[0] = true;

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        recyclerView = findViewById(R.id.recycler_view);
        quickRouteHistoryAdapter = new QuickRouteHistoryAdapter(quickRouteHistoryArrayList, QuickRouteManagementActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(QuickRouteManagementActivity.this));
        recyclerView.setAdapter(quickRouteHistoryAdapter);
    }

    public static boolean isValidDate(String inDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        dateFormat.setLenient(false);
        try {
            Date date = new Date(Long.parseLong(inDate));
            DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            String formatted = format.format(date);
        } catch (Exception pe) {
            return false;
        }
        return true;
    }


    @Override
    public void onClick(int position) {
        selected_position = position;
    }
}
