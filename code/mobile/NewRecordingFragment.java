package ch.epfl.esl.sportstracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import static ch.epfl.esl.sportstracker.MyProfileFragment.USER_ID;

public class NewRecordingFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    private View fragmentView;

    public static final String RECORDIND_ID = "recID";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceAddress;
    private OnFragmentInteractionListener mListener;
    private SPORT chosenActivity = SPORT.RUNNING;
    private String recordingKeySaved;
    private Switch switchHRbelt;
    private SwitchBeltOnCheckedChangeListener switchBeltOnCheckedChangeListener;
    private static final int BLE_CONNECTION = 1;

    public NewRecordingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_new_recording, container, false);

        ImageButton buttonRunning = fragmentView.findViewById(R.id.runningButton);
        buttonRunning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActivityNameAndImage(SPORT.RUNNING);
            }
        });

        ImageButton buttonCycling = fragmentView.findViewById(R.id.cyclingButton);
        buttonCycling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActivityNameAndImage(SPORT.CYCLING);
            }
        });

        ImageButton buttonSkiing = fragmentView.findViewById(R.id.skiingButton);
        buttonSkiing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActivityNameAndImage(SPORT.SKIING);
            }
        });

        ImageButton buttonClimbing = fragmentView.findViewById(R.id.climbingButton);
        buttonClimbing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActivityNameAndImage(SPORT.CLIMBING);
            }
        });

        // BLE connection
        switchBeltOnCheckedChangeListener = new SwitchBeltOnCheckedChangeListener();
        switchHRbelt = fragmentView.findViewById(R.id.switchBelt);
        FloatingActionButton newRecording = fragmentView.findViewById(R.id.saveNewRecButton);
        newRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = getActivity().getIntent();
                final String userID = intent.getExtras().getString(USER_ID);

                final FirebaseDatabase database = FirebaseDatabase.getInstance();
                final DatabaseReference profileGetRef = database.getReference("profiles");
                final DatabaseReference recordingRef = profileGetRef.child(userID).child("recordings").push();

                final Switch switchWatch = fragmentView.findViewById(R.id.switchWatch);
                final Switch switchBelt = fragmentView.findViewById(R.id.switchBelt);

                recordingRef.runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                        mutableData.child("datetime").setValue(System.currentTimeMillis());
                        mutableData.child("exercise_type").setValue(chosenActivity);
                        mutableData.child("switch_watch").setValue(switchWatch.isChecked());
                        mutableData.child("switch_hr_belt").setValue(switchBelt.isChecked());

                        recordingKeySaved = recordingRef.getKey();

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                        Toast.makeText(getContext(), R.string.save_recording_success, Toast.LENGTH_SHORT).show();

                        // Start watch activity to get HR data if the switch
                        // is on
                        if (switchWatch.isChecked()) {
                            startRecordingOnWear();
                        }

                        Intent intentStartLive = new Intent(getActivity(), ExerciseLiveActivity.class);
                        intentStartLive.putExtra(USER_ID, userID);
                        intentStartLive.putExtra(RECORDIND_ID, recordingKeySaved);
                        intentStartLive.putExtra(EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
                        startActivity(intentStartLive);
                    }
                });

                // Save the recording timestamp locally and update the alarm
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putLong(PrefsActivity.LAST_ACTIVITY_TIMESTAMP, System.currentTimeMillis()).apply();
                AlarmUpdater.updateNotificationSystem(getContext());
            }
        });


        return fragmentView;
    }

    private class SwitchBeltOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked) {
                Intent intent = new Intent(getActivity(), DeviceScanActivity.class);
                startActivityForResult(intent, BLE_CONNECTION);
            } else {
                compoundButton.setText(R.string.hr_belt);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode== BLE_CONNECTION && resultCode== Activity.RESULT_OK){
            mDeviceAddress=data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement " + "" + "" + "" + "OnFragmentInteractionListener");
        }
    }

    private void startRecordingOnWear() {
        Log.d(TAG, "Entered smart watch hr reading");
        Intent intentStartRec = new Intent(getActivity(), WearService.class);
        intentStartRec.setAction(WearService.ACTION_SEND.STARTACTIVITY.name());
        intentStartRec.putExtra(WearService.ACTIVITY_TO_START, BuildConfig.W_recordingactivity);
        getActivity().startService(intentStartRec);
    }

    private void setActivityNameAndImage(SPORT activity) {
        chosenActivity = activity;
        Drawable image = getResources().getDrawable(R.drawable.ic_logo);

        ImageView activityImage = fragmentView.findViewById(R.id.imageActivity);
        TextView activityName = fragmentView.findViewById(R.id.nameActivity);

        String name = "";
        switch (activity) {
            case RUNNING:
                image = getResources().getDrawable(R.mipmap.running);
                name = getString(R.string.running);
                break;
            case CYCLING:
                image = getResources().getDrawable(R.mipmap.cycling);
                name = getString(R.string.cycling);
                break;
            case SKIING:
                image = getResources().getDrawable(R.mipmap.skiing);
                name = getString(R.string.skiing);
                break;
            case CLIMBING:
                image = getResources().getDrawable(R.mipmap.climbing);
                name = getString(R.string.climbing);
                break;
        }

        activityName.setText(name);
        activityImage.setImageDrawable(image);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        switchHRbelt.setOnCheckedChangeListener(switchBeltOnCheckedChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        switchHRbelt.setOnCheckedChangeListener(null);
    }

    enum SPORT {RUNNING, CYCLING, SKIING, CLIMBING}

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating
     * .html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
