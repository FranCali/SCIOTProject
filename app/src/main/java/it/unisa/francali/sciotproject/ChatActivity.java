package it.unisa.francali.sciotproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class ChatActivity extends AppCompatActivity {
    final String NUCLIO_HOST = "192.168.1.7", NUCLIO_PORT_NUMBER = "42651";
    private Button publishBtn;
    private EditText text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        publishBtn = findViewById(R.id.publishBtn);
        text = findViewById(R.id.text);


        publishBtn.setOnClickListener((view)-> {
            String message = text.getText().toString();
            if(text.getText().toString()!=""){
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
                (response) -> {},
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
}
