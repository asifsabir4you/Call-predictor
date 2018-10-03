package com.example.rashedalam.callpredictor;

;import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    int dateOfMonth[] = new int[31];
    List<String> list = new ArrayList<>();
    private static final int PERMISSION_READ_STATE = 0;
    // TextView textOut;
    ProgressBar pb1;
    TextView tvCallsInfo, tvDataInfo, tvPhone, tvDataHistory, tvDataPredict, tvCallsPredict;
    FirebaseAuth mAuth;
    Button btnLogOut, btnDownloadInterent, btnPredictIntenet, btnPredictCalls;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //asking for permissions
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CALL_LOG}, PERMISSION_READ_STATE);


//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);


        mAuth = FirebaseAuth.getInstance();

//        ScheduleDaily();

        ScheduleDailyN();

        initArrayWithZero();
        btnPredictIntenet = (Button) findViewById(R.id.btn_predict_internet_usage);
        btnLogOut = (Button) findViewById(R.id.btn_log_out);
        btnDownloadInterent = (Button) findViewById(R.id.btn_download_internet_history);
        btnPredictCalls = (Button) findViewById(R.id.btn_predict_call_usage);
        tvCallsPredict = (TextView) findViewById(R.id.tv_call_predict);
        tvCallsInfo = (TextView) findViewById(R.id.call_info);
        tvDataInfo = (TextView) findViewById(R.id.data_info);
        tvDataHistory = (TextView) findViewById(R.id.data_history);
        tvPhone = (TextView) findViewById(R.id.phone);
        tvDataPredict = (TextView) findViewById(R.id.tv_data_predict);
        tvPhone.setText("Logged as: " + mAuth.getCurrentUser().getPhoneNumber());
        tvDataInfo.setText("Internet usage today: " + String.valueOf(InternetUsage.getReceivedBytes() + InternetUsage.getSentBytes()));

        Log.d("history" + list.size(), toString());
        getDataHistory();


        pb1 = (ProgressBar) findViewById(R.id.pb);

//getting call details and pushing in arrays
        getCallDetails(this);

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                startActivity(new Intent(MainActivity.this, PhoneAuthActivity.class));
                finish();
            }
        });

        btnDownloadInterent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnDownloadInterent.setVisibility(View.GONE);
                String IntenetDisplayString = "Internet History (Bytes): ";
                if (list.size() != 0) {
                    for (int i = 0; i < list.size(); i++) {
                        Log.d("history", list.get(i));
                        IntenetDisplayString = IntenetDisplayString + " | " + list.get(i);
                        Log.d("history", IntenetDisplayString);
                    }
                    tvDataHistory.setText(IntenetDisplayString + " |");
                    btnPredictIntenet.setVisibility(View.VISIBLE);
                    tvDataHistory.setVisibility(View.VISIBLE);
                }

            }
        });
        btnPredictIntenet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //loading internet cluster;
                getInternetCluster();
                tvDataPredict.setVisibility(View.VISIBLE);
            }
        });
        btnPredictCalls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCallsCluster();
                tvCallsPredict.setVisibility(View.VISIBLE);
            }
        });

    }


    //permission results
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_READ_STATE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted!
                    //checks on app running ; do not Toast anything here.
                } else {
                    // permission denied
                    Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();

                }
                return;
            }

        }
    }


    private void getCallDetails(Context context) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //*********no permission situation here// we may Toast...
        }
        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                null, null, null, CallLog.Calls.DATE + " DESC");

        int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = cursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
        while (cursor.moveToNext()) {
            String phNumber = cursor.getString(number);
            String callType = cursor.getString(type);
            String callDate = cursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));

            Calendar c = Calendar.getInstance();
            c.setTime(callDayTime);
            int dayOfWeek = c.get(Calendar.DAY_OF_MONTH);


            String callDuration = cursor.getString(duration);
            int dircode = Integer.parseInt(callType);
            if (dircode == CallLog.Calls.OUTGOING_TYPE) {

//                    stringBuffer.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
//                            + "OUTGOING: " + " \nCall Date:--- " + String.valueOf(dayOfWeek)
//                            + " \nCall duration in sec :--- " + callDuration);
//                    stringBuffer.append("\n----------------------------------");

                pushPhoneLogInArray(dayOfWeek, Integer.parseInt(callDuration));

            }

        }
        cursor.close();
        pb1.setVisibility(View.GONE);
//showing in textview
        showCallData();
    }

    public void pushPhoneLogInArray(int dateDay, int usage) {

        switch (dateDay) {
            case 1:
                dateOfMonth[0] += usage;
                break;
            case 2:
                dateOfMonth[1] += usage;
                break;
            case 3:
                dateOfMonth[2] += usage;
                break;
            case 4:
                dateOfMonth[3] += usage;
                break;
            case 5:
                dateOfMonth[4] += usage;
                break;
            case 6:
                dateOfMonth[5] += usage;
                break;
            case 7:
                dateOfMonth[6] += usage;
                break;
            case 8:
                dateOfMonth[7] += usage;
                break;
            case 9:
                dateOfMonth[8] += usage;
                break;
            case 10:
                dateOfMonth[9] += usage;
                break;
            case 11:
                dateOfMonth[10] += usage;
                break;
            case 12:
                dateOfMonth[11] += usage;
                break;
            case 13:
                dateOfMonth[12] += usage;
                break;
            case 14:
                dateOfMonth[13] += usage;
                break;
            case 15:
                dateOfMonth[14] += usage;
                break;
            case 16:
                dateOfMonth[15] += usage;
                break;
            case 17:
                dateOfMonth[16] += usage;
                break;
            case 18:
                dateOfMonth[17] += usage;
                break;
            case 19:
                dateOfMonth[18] += usage;
                break;
            case 20:
                dateOfMonth[19] += usage;
                break;
            case 21:
                dateOfMonth[20] += usage;
                break;
            case 22:
                dateOfMonth[21] += usage;
                break;
            case 23:
                dateOfMonth[22] += usage;
                break;
            case 24:
                dateOfMonth[23] += usage;
                break;
            case 25:
                dateOfMonth[24] += usage;
                break;
            case 26:
                dateOfMonth[25] += usage;
                break;
            case 27:
                dateOfMonth[26] += usage;
                break;
            case 28:
                dateOfMonth[27] += usage;
                break;
            case 29:
                dateOfMonth[28] += usage;
                break;
            case 30:
                dateOfMonth[29] += usage;
                break;
            case 31:
                dateOfMonth[30] += usage;
                break;
            default:
                //do nothing...
        }

    }

    public void initArrayWithZero() {
        for (int i = 0; i < 31; i++) {
            dateOfMonth[i] = 0;
        }
    }

    public void showCallData() {
        String callsInfoString = "";
        for (int i = 0; i < 31; i++) {
            callsInfoString = callsInfoString + "day: " + (i + 1) + " duration: " + dateOfMonth[i] + "\n";
        }
        tvCallsInfo.setText(callsInfoString);

        //now upload outgoing to server
        uploadOutgoingAcrossUser(dateOfMonth);
    }


//    public void ScheduleDaily() {
//
//        Calendar cur_cal = new GregorianCalendar();
//        cur_cal.setTimeInMillis(System.currentTimeMillis());//set the current time and date for this calendar
//
//        Calendar cal = new GregorianCalendar();
//        cal.add(Calendar.DAY_OF_YEAR, cur_cal.get(Calendar.DAY_OF_YEAR));
//        cal.set(Calendar.HOUR_OF_DAY, 10);
//        cal.set(Calendar.MINUTE, 43);
//        cal.set(Calendar.SECOND, cur_cal.get(Calendar.SECOND));
//        cal.set(Calendar.MILLISECOND, cur_cal.get(Calendar.MILLISECOND));
//        cal.set(Calendar.DATE, cur_cal.get(Calendar.DATE));
//        cal.set(Calendar.MONTH, cur_cal.get(Calendar.MONTH));
//        Intent intent = new Intent(MainActivity.this, BroadCastClass.class);
//        PendingIntent pintent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
//        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pintent);  //24*60*60*1000
//    }

    public void ScheduleDailyN() {
        Intent intent = new Intent(this, BroadCastClass.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(), 234324243, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                , 60 * 60 * 1000, pendingIntent);

        Toast.makeText(this, "Server data update scheduled. 1hr interval!", Toast.LENGTH_LONG).show();
    }


    public void uploadOutgoingAcrossUser(int usageArray[]) {

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
//enabling offline capabalities,

        DatabaseReference myRef = database.getReference("User").child(mAuth.getCurrentUser().getPhoneNumber()).child("outgoing");

        for (int i = 0; i < usageArray.length; i++) {
            myRef.child(String.valueOf(i)).setValue(String.valueOf(usageArray[i]));
        }
//        Toast.makeText(this, "Outgoing Synced to server!!", Toast.LENGTH_SHORT).show();

    }

    public void getDataHistory() {
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference InternetDataRef = mDatabase.getReference("User")
                .child(mAuth.getCurrentUser().getPhoneNumber())
                .child("data");
//        final String[] InternetString = {""};
//        InternetData.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                Toast.makeText(MainActivity.this,dataSnapshot.getValue(String.class)
//                        , Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//            }
//        });
//        return InternetString[0];

        InternetDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    final String Internet = snapshot.child(dataSnapshot.getKey()).getValue(String.class);
                    Log.d("history 2", Internet);
                    list.add(Internet);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Log.d("history 2", "returning************");

    }


    public void getInternetCluster() {
        float m1, m2, q = 0.0f, w = 0.0f;
        int k_means;
        int z = 0, i = 0;
        int z1 = 0;
        float avg1, avg2;
//        System.out.println("Enter no of elements in cluster");
        k_means = list.size();
        int a[] = new int[k_means];
        int c1[] = new int[k_means];
        int c2[] = new int[k_means];
        System.out.println("Enter elements in cluster");
        for (i = 0; i < k_means; i++) {
            a[i] = Integer.parseInt(list.get(i));
        }
//        System.out.println("Enter value of m1 and m2");
        m1 = Integer.parseInt(list.get(0));
        m2 = Integer.parseInt(String.valueOf(list.size() - 1));
        Operations op = new Operations();
        while (q != m2 && w != m2) {
            for (i = 0; i < k_means; i++) {
                if (Math.abs(a[i] - m1) < Math.abs(a[i] - m2)) {
                    c1[z] = a[i];
                    z++;
                } else {
                    c2[z1] = a[i];
                    z1++;
                }
            }
            z = 0;
            z1 = 0;
//            System.out.print("Cluster 1\t");
//            op.display(c1,k_means);
//            System.out.print("Cluster 2\t");
//            op.display(c2,k_means);
            q = m1;
            w = m2;
            m1 = op.average(c1, k_means);
//            System.out.print("average of cluster1 "+m1);
//            System.out.println();
            m2 = op.average(c2, k_means);
//            System.out.print("average of cluster2 "+m2);
//            System.out.println();
            tvDataPredict.setText("Min Usage: " + Math.round(m1) + " Bytes" +
                    "\n" + "Max Usage: " + Math.round(m2) + " Bytes");
        }
    }


    public void getCallsCluster() {
        float m1, m2, q = 0.0f, w = 0.0f;
        int k_means;
        int z = 0, i = 0;
        int z1 = 0;
        float avg1, avg2;
//        System.out.println("Enter no of elements in cluster");
        k_means = dateOfMonth.length;
        int a[] = new int[k_means];
        int c1[] = new int[31];
        int c2[] = new int[31];
//        System.out.println("Enter elements in cluster");
        for (i = 0; i < k_means; i++) {
            a[i] = Integer.parseInt(String.valueOf(dateOfMonth[i]));
        }
//        System.out.println("Enter value of m1 and m2");
        m1 = Integer.parseInt(String.valueOf(dateOfMonth[0]));
        m2 = Integer.parseInt(String.valueOf(dateOfMonth[dateOfMonth.length - 1]));
        Operations op = new Operations();
        while (q != m2 && w != m2) {
            for (i = 0; i < k_means; i++) {
                if (Math.abs(a[i] - m1) < Math.abs(a[i] - m2)) {
                    c1[z] = a[i];
                    z++;
                } else {
                    c2[z1] = a[i];
                    z1++;
                }
            }
            z = 0;
            z1 = 0;
//            System.out.print("Cluster 1\t");
//            op.display(c1,k_means);
//            System.out.print("Cluster 2\t");
//            op.display(c2,k_means);
            q = m1;
            w = m2;
            m1 = op.average(c1, k_means);
//            System.out.print("average of cluster1 "+m1);
//            System.out.println();
            m2 = op.average(c2, k_means);
//            System.out.print("average of cluster2 "+m2);
//            System.out.println();
            tvCallsPredict.setText("Min : " + Math.round(m1 / 60) + " Min" +
                    "\n" + "Max : " + Math.round(m2 / 60) + " Min");
        }
    }


}
