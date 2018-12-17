package it.unisa.francali.sciotproject;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendHTTPRequest(1, true);
            }
        });

        // new ConsumerTask().execute();
    }




    private void sendHTTPRequest(int roomNumber, boolean isSit){

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("roomnumber", roomNumber);
            requestBody.put("issit", isSit);
        }catch (JSONException exception){
            Log.e("JsonError", exception.getMessage());
        }

        final String requestBodyString = requestBody.toString();


        RequestQueue queue = Volley.newRequestQueue(this);
        String protocol = "http", host = "172.19.28.197", port = "37827";

        final String url = protocol+ "://" + host + ":" + port;


        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                textView.setText(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VolleyError", error.toString());
            }
        }){
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


            /*@Override
             protected Response<String> parseNetworkResponse(NetworkResponse response) {
                 String responseString = "";
                 if (response != null) {
                     responseString = String.valueOf(response.statusCode);
                 }
                 return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
             }*/
        };
        queue.add(stringRequest);

    }

    public class ConsumerTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                MessageConsumer.consume();
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            textView.setText("finito il task");
            super.onPostExecute(o);
        }
    }
}


