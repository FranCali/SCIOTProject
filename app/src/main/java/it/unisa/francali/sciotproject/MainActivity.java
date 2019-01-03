package it.unisa.francali.sciotproject;


import android.annotation.SuppressLint;
import android.app.Activity;
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

public class MainActivity extends AppCompatActivity {
    final int SEATS_LIMIT = 20;
    final String HOST = "192.168.1.7", PORT_NUMBER = "42651";

    private TextView sentMsgTextView, receivedMsgTextView, currentSeatTextView;
    private Button seatBtn, leaveBtn;
    private RadioGroup roomsRadioGroup;
    private Handler incomingMessageHandler;
    private boolean hasSeat = false;
    private int currentRoom = 0;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLayoutElements();

        seatBtn.setOnClickListener((view) -> takeSeat());
        leaveBtn.setOnClickListener((view) -> leaveSeat());

        incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String receivedMsg = msg.getData().get("msg").toString();
                receivedMsgTextView.setText(receivedMsg);
                updateSeats(receivedMsg);
            }
        };
        new ReceiveMsgTask().execute();
    }

    private void initializeLayoutElements(){
        sentMsgTextView = findViewById(R.id.sentMsgTextView);
        receivedMsgTextView = findViewById(R.id.receivedMsgTextView);
        seatBtn = findViewById(R.id.sit);
        leaveBtn = findViewById(R.id.leave);
        roomsRadioGroup = findViewById(R.id.roomsRadioGroup);
        currentSeatTextView = findViewById(R.id.currentSeatTextView);
    }

    private void updateSeats(String message){
        int checkedRoomNumber = Integer.valueOf(message.split("-")[0]);
        boolean isSitting = Boolean.valueOf(message.split("-")[1]);
        int freeSeats;

        TextView roomTextView = null;

        switch(checkedRoomNumber){
            case 1: roomTextView = findViewById(R.id.freeSeatsRoom1TextView);
                break;
            case 2: roomTextView = findViewById(R.id.freeSeatsRoom2TextView);
                break;
            case 3: roomTextView = findViewById(R.id.freeSeatsRoom3TextView);
                break;
        }

        freeSeats = Integer.valueOf(roomTextView.getText().toString());
        if(freeSeats > 0  && isSitting)
            freeSeats--;
        else if(freeSeats < SEATS_LIMIT  && !isSitting)
            freeSeats++;

        roomTextView.setText(String.valueOf(freeSeats));
    }

    private void takeSeat(){
        int checkedRoom = getCheckedRoom();

        if (hasSeat) {
            Toast t = Toast.makeText(this, "you must first leave your seat! ", Toast.LENGTH_SHORT);
            t.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 100);
            t.show();
        }
        else{
            sendChangedSeatMsg(true, checkedRoom);
            currentSeatTextView.setText(String.valueOf(checkedRoom));
            currentRoom = checkedRoom;
            hasSeat = true;
        }
    }

    private void leaveSeat(){
        if(hasSeat){
            sendChangedSeatMsg(false, currentRoom);
            currentSeatTextView.setText("no seat");
            currentRoom = 0;
            hasSeat = false;
        }
        else{
            Toast.makeText(this, "you are not sit!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendChangedSeatMsg(boolean isSitting, int checkedRoom) {

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

        final String url = protocol + "://" + HOST + ":" + PORT_NUMBER;

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

    private class ReceiveMsgTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            try {

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(HOST);
                factory.setPort(5672);
                factory.setUsername("guest");
                factory.setPassword("guest");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.exchangeDeclare("iot/rooms", "fanout");

                String queueName = sharedPref.getString("queueName", "");

                if (!queueName.equals("")) {
                    queueName = channel.queueDeclare().getQueue();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("queueName", queueName);
                    editor.commit();
                }

                channel.queueBind(queueName, "iot/rooms", "");

                Log.d("debug"," [*] Waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.d("debug"," [x] Received '" + message + "'");
                    Message msg = incomingMessageHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("msg", message);
                    msg.setData(bundle);
                    incomingMessageHandler.sendMessage(msg);
                };

                channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
            }catch (Exception e){
                e.printStackTrace();
            }
            return new Object();
        }
    }
}



