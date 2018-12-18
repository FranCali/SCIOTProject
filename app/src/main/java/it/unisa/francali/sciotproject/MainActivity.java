package it.unisa.francali.sciotproject;


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


import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    Button seatBtn, leaveBtn;
    RadioGroup roomsGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        seatBtn = findViewById(R.id.seat);
        leaveBtn = findViewById(R.id.leave);
        roomsGroup = findViewById(R.id.radioRooms);

        seatBtn.setOnClickListener( (view)-> bookSeat(true));
        leaveBtn.setOnClickListener( (view)-> bookSeat(false));

    }

    private void bookSeat(boolean sit){
        int roomNumber = checkRoom();
        callNuclioPublisher(sit, roomNumber);
    }

    private void callNuclioPublisher(boolean sit, int roomNumber){

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("roomnumber", roomNumber);
            requestBody.put("issit", sit);
        }catch (JSONException exception){
            Log.e("JsonError", exception.getMessage());
        }

        final String requestBodyString = requestBody.toString();


        RequestQueue queue = Volley.newRequestQueue(this);
        String protocol = "http", host = "172.19.30.183", port = "37827";

        final String url = protocol+ "://" + host + ":" + port;


        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        (response) -> textView.setText(response),
                        (error) -> Log.e("VolleyError", error.toString()))
        {
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

    private int checkRoom(){
        int roomId = roomsGroup.getCheckedRadioButtonId();
        int roomNumber = 1;


        switch (roomId){
            case R.id.room1: roomNumber=1; break;
            case R.id.room2: roomNumber=2; break;
            case R.id.room3: roomNumber=3; break;
        }

        return roomNumber;
    }
}


