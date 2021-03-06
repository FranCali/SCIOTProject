package it.unisa.francali.sciotproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    final String NUCLIO_HOST = "192.168.43.173", NUCLIO_PORT_NUMBER = "41165";
    private Button publishBtn;
    private EditText text;
    private TextView conversationTextView;
    private Handler incomingMessageHandler;
    private SharedPreferences sharedPref;
    private Channel channel;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initializeLayoutElements();
        setupPublishBtn();
        recoverChatHistory();

        incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String receivedMsg = msg.getData().getString("msg");

                Log.d("received message", receivedMsg);
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss", Locale.ITALIAN);
                String chatMessage = ft.format(now) + ' ' + receivedMsg + '\n';
                conversationTextView.append(chatMessage);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        new ReceiveMsgTask().execute();
    }

    @Override
    public void onStop(){
        super.onStop();
        sharedPref = getApplicationContext().getSharedPreferences("state", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("chatHistory", conversationTextView.getText().toString());
        editor.apply();
        closeRabbitMQChannel();
    }

    private void recoverChatHistory(){
        sharedPref = getApplicationContext().getSharedPreferences("state", Context.MODE_PRIVATE);
        String chatHistory = sharedPref.getString("chatHistory", "");
        conversationTextView.setText(chatHistory.trim()+'\n');
    }

    @SuppressLint("StaticFieldLeak")
    void closeRabbitMQChannel(){
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object[] objects) {
                try {
                    channel.abort();
                }catch(IOException e){
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    void initializeLayoutElements(){
        publishBtn = findViewById(R.id.publishBtn);
        text = findViewById(R.id.text);
        conversationTextView = findViewById(R.id.textView);
    }

    void setupPublishBtn(){
        publishBtn.setOnClickListener((view) -> {
            String message = text.getText().toString();

            if (!text.getText().toString().isEmpty()) {
                publishMsg(message);
                text.setText("");
            }
        });
    }

    private void publishMsg(String message) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("msg", message);
        } catch (JSONException exception) {
            Log.e("JsonError", exception.getMessage());
        }

        final String requestBodyString = requestBody.toString();

        RequestQueue queue = Volley.newRequestQueue(this);
        String protocol = "http";

        final String url = protocol + "://" + NUCLIO_HOST + ":" + NUCLIO_PORT_NUMBER;

        Log.d("Request to", url);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                (response) -> Log.d("Sent message",response),
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

    @SuppressLint("StaticFieldLeak")
    private class ReceiveMsgTask extends AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(Object[] params) {
            try {
                sharedPref = getApplicationContext().getSharedPreferences("state", Context.MODE_PRIVATE);
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(NUCLIO_HOST);
                factory.setPort(5672);
                factory.setUsername("guest");
                factory.setPassword("guest");
                Connection connection = factory.newConnection();
                channel = connection.createChannel();
                channel.exchangeDeclare("iot/chat", "fanout");

                String queueName = sharedPref.getString("queueChatName", "");

                if(queueName!=null) {
                    if (queueName.equals("")) {
                        queueName = channel.queueDeclare("", true, false, false, null).getQueue();
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("queueChatName", queueName);
                        editor.apply();
                    }
                }

                channel.queueBind(queueName, "iot/chat", "");

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
