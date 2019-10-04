package cpe.iot.tbg.ledbattle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    static final String MESSAGE_PLAY_RESET = "(0)";
    static final String MESSAGE_PLAY_P1 = "(1)";
    static final String MESSAGE_PLAY_P2 = "(2)";

    // Sensors
    SensorManager sm;
    TextView tv_sensor_value;
    TextView tv_sensor_datetime;

    // Network
    private InetAddress address;
    private DatagramSocket UDPSocket;
    EditText et_network_ip;
    EditText et_network_port;
    TextView tv_received;

    // Game
    private int player;
    private boolean playerSet;
    private boolean playing;
    Button btn_play;
    Button btn_player_one;
    Button btn_player_two;
    TextView tv_player_current;
    TextView tv_playing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Sensors
        tv_sensor_value = findViewById(R.id.tv_sensor_value);
        tv_sensor_datetime = findViewById(R.id.tv_sensor_datetime);
        // Network
        et_network_ip = findViewById(R.id.et_ip);
        et_network_port = findViewById(R.id.et_port);
        tv_received = findViewById(R.id.tv_received);
        // Game
        btn_play = findViewById(R.id.btn_play);
        btn_player_one = findViewById(R.id.btn_player_one);
        btn_player_two = findViewById(R.id.btn_player_two);
        tv_player_current = findViewById(R.id.tv_player);
        tv_playing = findViewById(R.id.tv_playing);

        player = 0;
        playerSet = false;
        playing = false;

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(playerSet)
                {
                    if(UDPSocket != null)
                        manageNetwork(false);

                    manageNetwork(true);
                    new ReceiverTask(UDPSocket, tv_received).execute();

                    int port = Integer.parseInt(et_network_port.getText().toString()); // server port
                    new MessageThread(address, port, UDPSocket, MESSAGE_PLAY_RESET).start();

                    playing = true;
                    refreshPlaying();
                }
            }
        });

        btn_player_one.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(playerSet == false)
                {
                    player = 1;
                    playerSet = true;
                    refreshButtons();
                }
            }
        });

        btn_player_two.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(playerSet == false)
                {
                    player = 2;
                    playerSet = true;
                    refreshButtons();
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        manageNetwork(false);
        manageSensors(false);

        playing = false;
        refreshPlaying();
    }

    @Override
    protected void onResume() {
        super.onResume();
        manageSensors(true);
        manageNetwork(false);
    }

    private void refreshButtons()
    {
        tv_player_current.setText(getString(R.string.txt_player_current, player));
        btn_player_one.setClickable(!playerSet);
        btn_player_two.setClickable(!playerSet);
    }

    private void refreshPlaying()
    {
        String playingMessage = getString(R.string.txt_playing_false);

        if(playing)
            playingMessage = getString(R.string.txt_playing_true);


        tv_playing.setText(playingMessage);
    }

    // ========== SENSORS ==========

    private void manageSensors(boolean set)
    {
        if(set)
        {
            try
            {
                sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                sm.registerListener(
                        this,
                        sm.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                        SensorManager.SENSOR_DELAY_UI
                );
            }
            catch(NullPointerException ex) { ex.printStackTrace(); }
        }
        else
        {
            try
            {
                sm.unregisterListener(this);
            }
            catch(Exception ex) { ex.printStackTrace(); }
            finally
            {
                sm = null;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

//        switch(event.sensor.getType())
//        {
//            case Sensor.TYPE_ACCELEROMETER:
//                refreshAccelerometerValues(event.values);
//                break;
//
//            case Sensor.TYPE_PROXIMITY:
//                refreshProximityValues(event.values);
//                break;
//
//            default:
//                break;
//        }

        refreshProximityValues(event.values);
        onAccuracyChanged(event.sensor, event.accuracy);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        Date dt = new Date();
        tv_sensor_datetime.setText(getString(R.string.txt_sensor_datetime, dt));
    }

    private void refreshProximityValues(float values[])
    {
        tv_sensor_value.setText(getString(R.string.txt_sensor_value, values[0]));

        boolean tmp = (values[0] == 0);
        String tmpStr = tmp ? "TRUE" : "FALSE";
        tv_player_current.setText(tmpStr);

        if(playing && values[0] == 0)
        {
            String playmessage = "(_)";

            if(player == 1)
            {
                playmessage = MESSAGE_PLAY_P1;
            }
            else if(player == 2)
            {
                playmessage = MESSAGE_PLAY_P2;
            }

            int port = Integer.parseInt(et_network_port.getText().toString()); // server port
            new MessageThread(address, port, UDPSocket, playmessage).start();

        }
    }

    // ========== NETWORK =========

    private void manageNetwork(boolean set)
    {
        if(set)
        {
            try
            {
//            // Init
//            String ip = "";
//            int port = -1;

            // Assign
            String ip = et_network_ip.getText().toString();
            int port = Integer.parseInt(et_network_port.getText().toString()); // our port

            // Check
            if(ip.isEmpty() || et_network_port.getText().toString().isEmpty())
                return;

                UDPSocket = new DatagramSocket(port);
                address = InetAddress.getByName(ip);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
        else
        {
            try
            {
                UDPSocket.close();
            }
            catch(Exception ex) { ex.printStackTrace(); }
            finally {
                UDPSocket = null;
                address = null;
            }
        }
    }
}

class MessageThread extends Thread {

    InetAddress Address;
    int Port;
    DatagramSocket Socket;
    String Message;

    MessageThread(InetAddress address, int port, DatagramSocket socket, String message) {
        super();
        Address = address;
        Port = port;
        Socket = socket;
        Message = message;
    }

    @Override
    public void run() {
        //super.run();
        try
        {
            byte[] data = Message.getBytes("UTF-8");

            DatagramPacket packet = new DatagramPacket(data, data.length, Address, Port);
            Socket.send(packet);
        }
        catch(Exception ex) // getBytes or send
        {
            ex.printStackTrace();
        }
    }
}

class ReceiverTask extends AsyncTask<Void, byte[], Void> {

    DatagramSocket Socket;
    TextView TextView;

    ReceiverTask(DatagramSocket socket, TextView textView) {
        super();
        Socket = socket;
        TextView = textView;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        while(true){
            byte[] data = new byte [1024]; // Espace de réception des données.
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try
            {
                Socket.receive(packet);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            int size = packet.getLength();
            publishProgress(java.util.Arrays.copyOf(data, size));
        }
    }

    @Override
    protected void onProgressUpdate(byte[]... values) {
        super.onProgressUpdate(values);

        String message = "";
        try
        {
            message = new String(values[0], "UTF-8");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            message = "Erreur à la réception";
        }
        finally
        {
            String displayMessage;

            if(message.equals("(0)"))
            {
                displayMessage = "VOUS AVEZ PERDU.";
            }
            else  if(message.equals("(1)"))
            {
                displayMessage = "VOUS AVEZ GAGNE !";
            }
            else {
                displayMessage = message;
            }

            TextView.setText(displayMessage);
        }
    }
}
