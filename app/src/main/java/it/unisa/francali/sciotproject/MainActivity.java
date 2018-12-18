package it.unisa.francali.sciotproject;


import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
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
    TextView sentMsg, receivedMsg;
    Button seatBtn, leaveBtn;
    RadioGroup roomsGroup;
    Handler incomingMessageHandler;
    final int SEATS_LIMIT = 20;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sentMsg = findViewById(R.id.sentMsg);
        receivedMsg = findViewById(R.id.receivedMsg);
        seatBtn = findViewById(R.id.seat);
        leaveBtn = findViewById(R.id.leave);
        roomsGroup = findViewById(R.id.radioRooms);

        seatBtn.setOnClickListener((view) -> bookSeat(true));
        leaveBtn.setOnClickListener((view) -> bookSeat(false));


        incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                receivedMsg.setText(msg.getData().get("msg").toString());
                changeSeats(msg.getData().get("msg").toString());
            }
        };

        new ConsumeTask().execute();
    }

    private void changeSeats(String message){
        int roomNumber = Integer.valueOf(message.split("-")[0]);
        boolean isSit = Boolean.valueOf(message.split("-")[1]);

        TextView room = findViewById(R.id.seatsRoom1);

        int freeSeats;


        switch(roomNumber){
            case 1: room = findViewById(R.id.seatsRoom1);

                break;
            case 2: room = findViewById(R.id.seatsRoom2);
                break;
            case 3: room = findViewById(R.id.seatsRoom3);
                break;
        }

        freeSeats = Integer.valueOf(room.getText().toString());
        if(freeSeats > 0  && isSit) {
            freeSeats--;
            room.setText(String.valueOf(freeSeats));
        }
        else if(freeSeats < SEATS_LIMIT  && !isSit) {
            freeSeats++;
            room.setText(String.valueOf(freeSeats));
        }
    }

    private void bookSeat(boolean sit) {
        int roomNumber = checkRoom();
        callNuclioPublisher(sit, roomNumber);
    }

    private void callNuclioPublisher(boolean sit, int roomNumber) {

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("roomnumber", roomNumber);
            requestBody.put("issit", sit);
        } catch (JSONException exception) {
            Log.e("JsonError", exception.getMessage());
        }

        final String requestBodyString = requestBody.toString();


        RequestQueue queue = Volley.newRequestQueue(this);
        String protocol = "http", host = "172.19.30.183", port = "37827";

        final String url = protocol + "://" + host + ":" + port;


        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                (response) -> sentMsg.setText(response),
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

    private int checkRoom() {
        int roomId = roomsGroup.getCheckedRadioButtonId();
        int roomNumber = 1;


        switch (roomId) {
            case R.id.room1:
                roomNumber = 1;
                break;
            case R.id.room2:
                roomNumber = 2;
                break;
            case R.id.room3:
                roomNumber = 3;
                break;
        }

        return roomNumber;
    }



    private class ConsumeTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("172.19.30.183");
                factory.setPort(5672);
                factory.setUsername("guest");
                factory.setPassword("guest");
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.exchangeDeclare("iot/rooms", "fanout");
                String queueName = channel.queueDeclare().getQueue();
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
                channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                });
            }catch (Exception e){
                e.printStackTrace();
            }
            return new Object();
        }
    }
}



