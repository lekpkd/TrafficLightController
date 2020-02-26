package com.fglaos.trafficlightcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import androidx.appcompat.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    private Button btnConnect;
    private TextView tvConsoleOutput, tvConnectionState;
    private RelativeLayout btnPhase1, btnPhase2, btnPhase3, btnPhase4;
    private BluetoothAdapter myBTAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> BTArrayAdapter;
    private ListView listBTAdapterList;
    private int REQUEST_ENABLE_BT = 1200;
    private LinearLayout panDeviceList;
    private BluetoothDevice myBTDevice;
    private BluetoothSocket myBTSocket;
    private OutputStream mBTOut;
    private InputStream mBTIn;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvConsoleOutput = (TextView) findViewById(R.id.tvConsoleOutput);
        tvConnectionState = (TextView) findViewById(R.id.tvConnectionState);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnPhase1 = (RelativeLayout) findViewById(R.id.btnPhase1);
        btnPhase2 = (RelativeLayout) findViewById(R.id.btnPhase2);
        btnPhase3 = (RelativeLayout) findViewById(R.id.btnPhase3);
        btnPhase4 = (RelativeLayout) findViewById(R.id.btnPhase4);
        panDeviceList = (LinearLayout) findViewById(R.id.panDeviceList);
        listBTAdapterList = (ListView) findViewById(R.id.listBTAdapterList);
        imageView = (ImageView) findViewById(R.id.imageView);
        setPhase(R.drawable.traffic_bg_none);
        panDeviceList.setVisibility(View.VISIBLE);

        myBTAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!myBTAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
            consoleAddText("Bluetooth turned on");
        } else {
            consoleAddText("Bluetooth is already on");
        }
        if (myBTAdapter == null) {
            tvConnectionState.setText(getString(R.string.disconnected));
        } else {
            // get paired devices
            pairedDevices = myBTAdapter.getBondedDevices();
            // put it's one to the adapter
            BTArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
            listBTAdapterList.setAdapter(BTArrayAdapter);
            for (BluetoothDevice device : pairedDevices) {
                BTArrayAdapter.add(device.getName() + " (" + device.getAddress() + ")");
            }
            listBTAdapterList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //consoleAddText("Clicked on position: " + position);
                    int i = 0;
                    for (BluetoothDevice device : pairedDevices) {
                        if (position == i) {
                            myBTDevice = device;
                            break;
                        }
                        i++;
                    }
                    consoleAddText("Selected: " + myBTDevice.getName());
                    tvConnectionState.setText("Selected: " + "\n" + myBTDevice.getName());
                    panDeviceList.setVisibility(View.GONE);
                }
            });
        }
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myBTDevice != null)
                    if (btnConnect.getText().toString().toUpperCase().equals(getString(R.string.connect).toUpperCase())) {
                        consoleAddText("Connecting to " + myBTDevice.getName());
                        setPhase(R.drawable.traffic_bg_none);
                        try {
                            UUID SERIAL_UUID = myBTDevice.getUuids()[0].getUuid();
                            myBTSocket = myBTDevice.createInsecureRfcommSocketToServiceRecord(SERIAL_UUID);
                        } catch (IOException e) {
                            consoleAddText(e.getMessage());
                        }

                        try {
                            myBTSocket.connect();
                            consoleAddText("Connected to " + myBTDevice.getName());
                            tvConnectionState.setText(getString(R.string.connected) + "\n" + myBTDevice.getName());
                            btnConnect.setText(getString(R.string.disconnect));


                            mBTOut = myBTSocket.getOutputStream();
                            mBTIn = myBTSocket.getInputStream();

                            mBTOut.write("9".getBytes());
                            setPhase(R.drawable.traffic_bg_0);
                        } catch (IOException e) {
                            try {
                                myBTSocket.close();
                            } catch (IOException ex) {
                                consoleAddText(ex.getMessage());
                            }
                            consoleAddText(e.getMessage());
                        }
                    } else {
                        setPhase(R.drawable.traffic_bg_none);
                        consoleAddText("Disconnecting.. ");
                        try {
                            mBTOut.write("8".getBytes());
                            myBTSocket.close();
                            consoleAddText("Disconnected");
                            btnConnect.setText(getString(R.string.connect));
                        } catch (IOException ex) {
                            consoleAddText(ex.getMessage());
                        }
                    }
            }
        });

        btnPhase1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("1", R.drawable.traffic_bg_1);
            }
        });

        btnPhase2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("2", R.drawable.traffic_bg_2);
            }
        });

        btnPhase3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("3", R.drawable.traffic_bg_3);
            }
        });

        btnPhase4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("4", R.drawable.traffic_bg_4);
            }
        });


    }

    private void sendData(String Data, int DRAWABLE) {
        if (mBTOut != null)
            try {
                mBTOut.write(Data.getBytes());
                setPhase(DRAWABLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (requestCode == REQUEST_ENABLE_BT) {
            if (myBTAdapter.isEnabled()) {
                consoleAddText("Status: Enabled");
            } else {
                consoleAddText("Status: Disabled");
            }
        }
    }

    private void setPhase(int BACKGROUND_DRAWABLE) {
        imageView.setImageDrawable(getResources().getDrawable(BACKGROUND_DRAWABLE));
    }

    private void consoleAddText(String TEXT) {
        tvConsoleOutput.setText(TEXT + "\n" + tvConsoleOutput.getText());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}
