package com.thecoldheartofspace.karl.bluetoothbutton;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    boolean isDiscoverable = false;
    boolean bluetoothAvailable = false;
    UUID uuid = new UUID(0xBEADEDFACADE1122L, 0x3344556677889900L); // KDM use this on the other side too
    String DEVICE_NAME = "KarlIsAwesome"; // KDM use this on the other side too
    BluetoothAdapter bluetooth = null;
    BluetoothSocket socket = null;

    String status = "Not connected";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupButtons();
    }

    void setupButtons()
    {
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if(bluetooth != null)
        {
            if (bluetooth.isEnabled())
            {
                status = bluetooth.getName() + " : " + bluetooth.getAddress();
                bluetoothAvailable = true;
            }
            else
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);

                Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth disabled. Enable bluetooth.", Toast.LENGTH_LONG);
                toast.show();
            }
        }
        else
        {
            Toast toast = Toast.makeText(getApplicationContext(), "No bluetooth adapter available.", Toast.LENGTH_LONG);
            toast.show();
        }

        Button discoverableButton = (Button) findViewById(R.id.makeDiscoverableButton);
        discoverableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if(!isDiscoverable)
                {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
                    startActivity(discoverableIntent);
                    Snackbar.make(view, "Attempting to become discoverable", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
        discoverableButton.setEnabled(bluetoothAvailable);

        Button panicButton = (Button) findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket != null && socket.isConnected()) {
                    try {
                        byte[] b = new byte[4];
                        b[0] = 1;
                        socket.getOutputStream().write(b, 0, 1);
                        Snackbar.make(view, "Sending a panic message.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } catch (IOException e) {
                        Snackbar.make(view, "Failed to send via bluetooth.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                }
                else
                {
                    Snackbar.make(view, "Not connected. Cannot send panic message.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
        panicButton.setEnabled(bluetoothAvailable);

        BluetoothDevice commDevice = null;
        Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
        StringBuilder sb = new StringBuilder(1000);
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                sb.append(device.getName() + " : " + device.getAddress() + "\n");
                if(device.getName().equals(DEVICE_NAME))
                {
                    commDevice=device;
                }
            }

        TextView tv  = (TextView) findViewById(R.id.textView);
        tv.setText(sb.toString());
        }
        View parentLayout = findViewById(R.id.content_main);

        if(bluetoothAvailable && commDevice != null)
        {
            try
            {
                socket = commDevice.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
            }
            catch (IOException e)
            {
                Snackbar.make(parentLayout, "Could not create a bluetooth socket", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if(socket != null)
        {
            try
            {
                socket.close();
            }
            catch (Exception ex)
            {
                // nothing!
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == 1  && resultCode  == RESULT_OK)
            {  //String requiredValue = data.getStringExtra("Key");
                setupButtons();
            }
            if(requestCode == 30)
            {
                // View parentLayout = findViewById(R.id.content_main);
                if(resultCode == RESULT_CANCELED)
                {
                    isDiscoverable = false;
                    // Snackbar.make(parentLayout, "Not discoverable", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
                else
                {
                    isDiscoverable = true;
                    // Snackbar.make(parentLayout, "Discoverable", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
