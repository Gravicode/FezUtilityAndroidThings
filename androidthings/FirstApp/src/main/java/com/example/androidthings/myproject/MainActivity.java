/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.myproject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;
//fezhat board
import com.google.android.things.contrib.driver.fezhat.FezHat;
import com.google.android.things.contrib.driver.fezhat.Akselerasi;
import com.google.android.things.contrib.driver.fezhat.Color;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
//azure lib
import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubClientProtocol;
import com.microsoft.azure.iothub.IotHubEventCallback;
import com.microsoft.azure.iothub.IotHubMessageResult;
import com.microsoft.azure.iothub.IotHubStatusCode;
import com.microsoft.azure.iothub.Message;
import com.microsoft.azure.iothub.MessageCallback;

import com.google.gson.Gson;

import java.text.DecimalFormat;
/**
 * Skeleton of the main Android Things activity. Implement your device's logic
 * in this class.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 *
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    String connString = "HostName=FreeDeviceHub.azure-devices.net;DeviceId=AndroidThingsDevice;SharedAccessKey=U5mH0B3POECuZlTCmMA9xEIql1+S+ASQBWOPG1d3iBY=";

    private static final int INTERVAL_BETWEEN_BLINKS_MS = 2000;

    private DeviceClient client;
    private Handler mHandler = new Handler();

    private FezHat hat;
    private boolean next;
    private int i;

    protected TextView TxtStatus;
    protected TextView TxtTemp;
    protected TextView TxtLight;
    protected Button BtnReceive;
    protected TextView TxtAccel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        try {
            PeripheralManagerService service = new PeripheralManagerService();
            Log.d(TAG, "Available GPIO: " + service.getGpioList());
            Log.d(TAG, "onCreate");
            try {
                Setup();
            } catch (URISyntaxException ex) {

            }
            mHandler.post(mBlinkRunnable);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    protected void Setup() throws URISyntaxException, IOException {
        TxtAccel = (TextView) findViewById(R.id.txtAccel);
        TxtLight = (TextView) findViewById(R.id.txtLight);
        TxtTemp = (TextView) findViewById(R.id.txtTemp);
        TxtStatus = (TextView) findViewById(R.id.txtStatus);
        BtnReceive = (Button) findViewById(R.id.btnReceive);
        BtnReceive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    ReceiveData();
                } catch (IOException io) {

                } catch (URISyntaxException es) {

                }
            }
        });
        this.hat = FezHat.Create();
        this.hat.S1.SetLimits(500, 2400, 0, 180);
        this.hat.S2.SetLimits(500, 2400, 0, 180);
        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        client = new DeviceClient(connString, protocol);

        try {
            client.open();
        } catch (IOException e1) {
            System.out.println("Exception while opening IoTHub connection: " + e1.toString());
        } catch (Exception e2) {
            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
        }
    }

    public void ReceiveData() throws URISyntaxException, IOException {


        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        DeviceClient client = new DeviceClient(connString, protocol);

        if (protocol == IotHubClientProtocol.MQTT) {
            MessageCallbackMqtt callback = new MessageCallbackMqtt();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        } else {
            MessageCallback callback = new MessageCallback();
            Counter counter = new Counter(0);
            client.setMessageCallback(callback, counter);
        }

        try {
            client.open();
        } catch (IOException e1) {
            System.out.println("Exception while opening IoTHub connection: " + e1.toString());
        } catch (Exception e2) {
            System.out.println("Exception while opening IoTHub connection: " + e2.toString());
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.close();
    }

    // Our MQTT doesn't support abandon/reject, so we will only display the messaged received
    // from IoTHub and return COMPLETE
    protected static class MessageCallbackMqtt implements com.microsoft.azure.iothub.MessageCallback {
        public IotHubMessageResult execute(Message msg, Object context) {
            Counter counter = (Counter) context;
            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            counter.increment();

            return IotHubMessageResult.COMPLETE;
        }
    }

    protected static class MessageCallback implements com.microsoft.azure.iothub.MessageCallback {
        public IotHubMessageResult execute(Message msg, Object context) {
            Counter counter = (Counter) context;
            System.out.println(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            int switchVal = counter.get() % 3;
            IotHubMessageResult res;
            switch (switchVal) {
                case 0:
                    res = IotHubMessageResult.COMPLETE;
                    break;
                case 1:
                    res = IotHubMessageResult.ABANDON;
                    break;
                case 2:
                    res = IotHubMessageResult.REJECT;
                    break;
                default:
                    // should never happen.
                    throw new IllegalStateException("Invalid message result specified.");
            }

            System.out.println("Responding to message " + counter.toString() + " with " + res.name());

            counter.increment();

            return res;
        }
    }

    public void SendMessage(SensorData data) throws URISyntaxException, IOException {
        Gson gson = new Gson();
        String msgStr = gson.toJson(data);
        try {
            Message msg = new Message(msgStr);
            msg.setProperty("messageCount", Integer.toString(i));
            System.out.println(msgStr);
            EventCallback eventCallback = new EventCallback();
            client.sendEventAsync(msg, eventCallback, i);
        } catch (Exception e) {
        }

    }

    protected static class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object context) {
            Integer i = (Integer) context;
            System.out.println("IoT Hub responded to message " + i.toString()
                    + " with status " + status.name());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mHandler.removeCallbacks(mBlinkRunnable);
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected Runnable mBlinkRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                mHandler.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);
                DecimalFormat formatter = new DecimalFormat("######.000");
                SensorData data = new SensorData();
                Akselerasi accel = hat.GetAcceleration();
                data.Light = hat.GetLightLevel();
                data.Temp = hat.GetTemperature();
                data.Accelleration = "X:" + String.valueOf(accel.X) + " Y: " + String.valueOf(accel.Y) + " Z:" + String.valueOf(accel.Z);
                String lightStr = String.valueOf(data.Light);
                String Temp = formatter.format(data.Temp);
                String AccelStr = data.Accelleration;
                String Btn18Str = String.valueOf(hat.IsDIO18Pressed());
                String Btn22Str = String.valueOf(hat.IsDIO22Pressed());
                String AnalogStr = String.valueOf(hat.ReadAnalog(FezHat.AnalogPin.Ain1));
                TxtLight.setText("Light: " + lightStr);
                TxtAccel.setText("Acceleration: " + AccelStr);
                TxtTemp.setText("Temp: " + Temp + " C");
                Log.e(TAG, "Light:" + lightStr);
                Log.e(TAG, "Temp:" + Temp);
                Log.e(TAG, "Acceleration:" + AccelStr);
                Log.e(TAG, "Counter:" + i);
                Log.e(TAG, "Next:" + next);

                if ((i++ % 5) == 0) {
                    String LedsTextBox = String.valueOf(next);

                    hat.setDIO24On(next);
                    hat.D2.setColor(next ? Color.Green() : Color.Black());
                    hat.D3.setColor(next ? Color.Green() : Color.Black());

                    hat.WriteDigital(FezHat.DigitalPin.DIO16, next);
                    hat.WriteDigital(FezHat.DigitalPin.DIO26, next);

                    hat.SetPwmDutyCycle(FezHat.PwmPin.Pwm5, next ? 1.0 : 0.0);
                    hat.SetPwmDutyCycle(FezHat.PwmPin.Pwm6, next ? 1.0 : 0.0);
                    hat.SetPwmDutyCycle(FezHat.PwmPin.Pwm7, next ? 1.0 : 0.0);
                    hat.SetPwmDutyCycle(FezHat.PwmPin.Pwm11, next ? 1.0 : 0.0);
                    hat.SetPwmDutyCycle(FezHat.PwmPin.Pwm12, next ? 1.0 : 0.0);

                    next = !next;
                }

                if (hat.IsDIO18Pressed()) {
                    hat.S1.setPosition(hat.S1.getPosition() + 5.0);
                    hat.S2.setPosition(hat.S2.getPosition() + 5.0);

                    if (hat.S1.getPosition() >= 180.0) {
                        hat.S1.setPosition(0.0);
                        hat.S2.setPosition(0.0);
                    }
                }

                if (hat.IsDIO22Pressed()) {
                    if (hat.MotorA.getSpeed() == 0.0) {
                        hat.MotorA.setSpeed(0.7);
                        hat.MotorB.setSpeed(-0.7);
                    }
                } else {
                    if (hat.MotorA.getSpeed() != 0.0) {
                        hat.MotorA.setSpeed(0.0);
                        hat.MotorB.setSpeed(0.0);
                    }
                }
                Log.d(TAG, "jalan lagi...");
                //sending data to azure

                try {
                    SendMessage(data);
                    Log.d(TAG, "data has been pushed to azure...");
                    String timeStamp = new SimpleDateFormat("dd MM yy HH:mm:ss").format(Calendar.getInstance().getTime());
                    TxtStatus.setText("Data has been pushed to azure at " + timeStamp);
                } catch (URISyntaxException ex) {

                } finally {

                }
            } catch (IOException e) {
                Log.e(TAG, "Error on Jalan", e);

            }
        }
    };

    protected static class Counter {
        protected int num;

        public Counter(int num) {
            this.num = num;
        }

        public int get() {
            return this.num;
        }

        public void increment() {
            this.num++;
        }

        @Override
        public String toString() {
            return Integer.toString(this.num);
        }
    }
}
