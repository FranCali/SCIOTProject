package it.unisa.francali.sciotproject;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    final int SEATS_LIMIT = 20;
    final String NUCLIO_HOST = "192.168.43.173", NUCLIO_PORT_NUMBER = "42651";

    private TextView sentMsgTextView, receivedMsgTextView, currentSeatTextView, freeSeatsRoom1TextView, freeSeatsRoom2TextView, freeSeatsRoom3TextView;
    private Button seatBtn, leaveBtn, chatBtn;
    private RadioGroup roomsRadioGroup;
    private Handler incomingMessageHandler, incomingBackupMessageHandler;
    private boolean hasSeat = false;
    private int currentRoomNumber = 0;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLayoutElements();
        checkIfFirstLaunch();
        recoverState();

        seatBtn.setOnClickListener((view) -> takeSeat());
        leaveBtn.setOnClickListener((view) -> leaveSeat());
        chatBtn.setOnClickListener((view) -> {
            Intent intentToChat = new Intent(getBaseContext(), ChatActivity.class);
            startActivity(intentToChat);
        });


        incomingBackupMessageHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                String receivedMsg = msg.getData().getString("msg");
                new rePublishBackupMsgTask().execute(receivedMsg);
                if(receivedMsg!=null)
                    initializeSeats(receivedMsg);//Initialize seats at first app launch after installation
            }
        };

        incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String receivedMsg = msg.getData().getString("msg");
                receivedMsgTextView.setText(receivedMsg);
                if(receivedMsg!=null)
                    updateSeats(receivedMsg);
            }
        };
        new ReceiveMsgTask().execute();
    }

    private void initializeSeats(String message){
        String[] msgArray = message.split("-");
        freeSeatsRoom1TextView = findViewById(R.id.freeSeatsRoom1TextView);
        freeSeatsRoom2TextView = findViewById(R.id.freeSeatsRoom2TextView);
        freeSeatsRoom3TextView = findViewById(R.id.freeSeatsRoom3TextView);

        freeSeatsRoom1TextView.setText(msgArray[0]);
        freeSeatsRoom2TextView.setText(msgArray[1]);
        freeSeatsRoom3TextView.setText(msgArray[2]);
    }

    private void checkIfFirstLaunch(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("state", Context.MODE_PRIVATE);
        boolean firstLaunch = sharedPref.getBoolean("firstLaunch", true);
        if(firstLaunch) {
            new getCurrentRoomsStatusTask().execute();
            sharedPref.edit().putBoolean("firstLaunch", false).commit();
        }
    }

    private class getCurrentRoomsStatusTask extends AsyncTask<Object, Void, String>{

        @Override
        protected String doInBackground(Object... objects) {
            try {

                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(NUCLIO_HOST);
                factory.setPort(5672);
                factory.setUsername("guest");
                factory.setPassword("guest");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.queueDeclare("seatsBackup", false, false, false, null);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.d("debug", " [x] Received '" + message + "'");
                    Message msg = incomingBackupMessageHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("msg", message);
                    msg.setData(bundle);
                    incomingBackupMessageHandler.sendMessage(msg);

                    try {
                        channel.close();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                    connection.close();

                };
                channel.basicConsume("seatsBackup", true, deliverCallback, consumerTag -> { });

            }catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }


    private class rePublishBackupMsgTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... receivedMsg) {
            try {

                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(NUCLIO_HOST);
                factory.setPort(5672);
                factory.setUsername("guest");
                factory.setPassword("guest");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.queueDeclare("seatsBackup", false, false, false, null);
                channel.basicPublish("", "seatsBackup", null, receivedMsg[0].getBytes());

            }catch (Exception e){
                e.printStackTrace();
            }
            return "";
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("state", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("currentRoom", ((TextView) findViewById(R.id.currentSeatTextView)).getText().toString());
        editor.putString("seatsRoom1", ((TextView) findViewById(R.id.freeSeatsRoom1TextView)).getText().toString());
        editor.putString("seatsRoom2", ((TextView) findViewById(R.id.freeSeatsRoom2TextView)).getText().toString());
        editor.putString("seatsRoom3", ((TextView) findViewById(R.id.freeSeatsRoom3TextView)).getText().toString());
        editor.apply();
    }

    private void initializeLayoutElements() {
        sentMsgTextView = findViewById(R.id.sentMsgTextView);
        receivedMsgTextView = findViewById(R.id.receivedMsgTextView);
        seatBtn = findViewById(R.id.sit);
        leaveBtn = findViewById(R.id.leave);
        chatBtn = findViewById(R.id.chatBtn);
        roomsRadioGroup = findViewById(R.id.roomsRadioGroup);
        currentSeatTextView = findViewById(R.id.currentSeatTextView);
        freeSeatsRoom1TextView = findViewById(R.id.freeSeatsRoom1TextView);
        freeSeatsRoom2TextView = findViewById(R.id.freeSeatsRoom2TextView);
        freeSeatsRoom3TextView = findViewById(R.id.freeSeatsRoom3TextView);
    }

    private void recoverState() {

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("state", Context.MODE_PRIVATE);

        if (sharedPref != null) {
            switch (sharedPref.getString("currentRoom", "No seat")) {
                case "No seat":
                    currentRoomNumber = 0;
                    break;
                case "1":
                    currentRoomNumber = 1;
                    hasSeat = true;
                    break;
                case "2":
                    currentRoomNumber = 2;
                    hasSeat = true;
                    break;
                case "3":
                    currentRoomNumber = 3;
                    hasSeat = true;
                    break;
            }
        }
        currentSeatTextView.setText(sharedPref.getString("currentRoom", "No seat"));
        freeSeatsRoom1TextView.setText(sharedPref.getString("seatsRoom1", "20"));
        freeSeatsRoom2TextView.setText(sharedPref.getString("seatsRoom2", "20"));
        freeSeatsRoom3TextView.setText(sharedPref.getString("seatsRoom3", "20"));
    }

    private void updateSeats(String message) {
        int checkedRoomNumber = Integer.valueOf(message.split("-")[0]);
        boolean isSitting = Boolean.valueOf(message.split("-")[1]);
        int freeSeats;

        TextView roomTextView = null;

        switch (checkedRoomNumber) {
            case 1:
                roomTextView = findViewById(R.id.freeSeatsRoom1TextView);
                break;
            case 2:
                roomTextView = findViewById(R.id.freeSeatsRoom2TextView);
                break;
            case 3:
                roomTextView = findViewById(R.id.freeSeatsRoom3TextView);
                break;
        }

        if (roomTextView != null) {
            freeSeats = Integer.valueOf(roomTextView.getText().toString());
            if (freeSeats > 0 && isSitting)
                freeSeats--;
            else if (freeSeats < SEATS_LIMIT && !isSitting)
                freeSeats++;

            roomTextView.setText(String.valueOf(freeSeats));
        }
    }

    private void takeSeat() {
        int checkedRoom = getCheckedRoom();

        if (hasSeat) {
            Toast t = Toast.makeText(this, "you must first leave your seat! ", Toast.LENGTH_SHORT);
            t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
            t.show();
        } else {
            sendChangedSeatMsg(true, checkedRoom);
            currentSeatTextView.setText(String.valueOf(checkedRoom));
            currentRoomNumber = checkedRoom;
            hasSeat = true;
        }
    }

    @SuppressLint("SetTextI18n")
    private void leaveSeat() {
        if (hasSeat) {
            sendChangedSeatMsg(false, currentRoomNumber);
            currentSeatTextView.setText("no seat");
            currentRoomNumber = 0;
            hasSeat = false;
        } else {
            Toast.makeText(this, "you are not sit!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendChangedSeatMsg(boolean isSitting, int checkedRoom) { //Calling Nuclio function for publishing message

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("roomNumber", checkedRoom);
            requestBody.put("isSitting", isSitting);
        } catch (JSONException exception) {
            Log.e("JsonError", exception.getMessage());
        }

        final String requestBodyString = requestBody.toString();

        RequestQueue queue = Volley.newRequestQueue(this);
        String protocol = "http";

        final String url = protocol + "://" + NUCLIO_HOST + ":" + NUCLIO_PORT_NUMBER;

        Log.d("Request to", url);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                (response) -> sentMsgTextView.setText(response),
                (error) -> Log.e("VolleyError", error.toString())) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                byte[] body = new byte[0];
                try {
                    body = requestBodyString.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e("Exception", "Unable to gets bytes from JSON", e.fillInStackTrace());
                }
                return body;
            }
        };
        queue.add(stringRequest);
    }

    private int getCheckedRoom() {
        int roomId = roomsRadioGroup.getCheckedRadioButtonId();
        int checkedRoom = -1;

        switch (roomId) {
            case R.id.room1RadioBtn:
                checkedRoom = 1;
                break;
            case R.id.room2RadioBtn:
                checkedRoom = 2;
                break;
            case R.id.room3RadioBtn:
                checkedRoom = 3;
                break;
        }

        return checkedRoom;
    }

    @SuppressLint("StaticFieldLeak")
    private class ReceiveMsgTask extends AsyncTask<Object, Void, String> {

        @Override
        protected String doInBackground(Object[] objects) {
            try {
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(NUCLIO_HOST);
                factory.setPort(5672);
                factory.setUsername("guest");
                factory.setPassword("guest");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.exchangeDeclare("iot/rooms", "topic");

                String queueName = sharedPref.getString("queueName", "");

                if (queueName != null && queueName.equals("")) {
                    queueName = channel.queueDeclare(queueName, true, false, false, null).getQueue();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("queueName", queueName);
                    editor.apply();
                }

                channel.queueBind(queueName, "iot/rooms", "rooms");

                Log.d("debug", " [*] Waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.d("debug", " [x] Received '" + message + "'");
                    Message msg = incomingMessageHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("msg", message);
                    msg.setData(bundle);
                    incomingMessageHandler.sendMessage(msg);
                };

                channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }
}



