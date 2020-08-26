package com.gqq.socketdemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int WHAT_MESSAGE_RECEIVED = 1;
    private static final int WHAT_MESSAGE_CONNECTED = 2;

    private TextView tvMessage;
    private Button btnSend;
    private EditText editText;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case WHAT_MESSAGE_RECEIVED:
                    String message = tvMessage.getText().toString();
                    tvMessage.setText(message + (String)msg.obj + "\n");
                    break;
                case WHAT_MESSAGE_CONNECTED:
                    Toast.makeText(MainActivity.this, "连接成功~", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
    private Socket clientSocket;
    private PrintWriter printWriter;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvMessage = findViewById(R.id.tv_message);
        btnSend = findViewById(R.id.btn_send);
        editText = findViewById(R.id.edit_text);
        btnConnect = findViewById(R.id.btn_connect);
        btnSend.setOnClickListener(this);
        btnConnect.setOnClickListener(this);


        // 服务端启动服务
        Intent intent = new Intent(this, SocketServer.class);
        startService(intent);

        // 连接服务器
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectTcp();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clientSocket != null) {
            try {
                clientSocket.shutdownInput();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectTcp() {
        Socket socket = null;
        while (socket == null) {
            try {
                // 连接服务器，通过id地址和端口号
                socket = new Socket("localhost", 4846);
                clientSocket = socket;
                // 拿到输出流
                printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                handler.sendEmptyMessage(WHAT_MESSAGE_CONNECTED);
                System.out.println("client connected success");
            } catch (IOException e) {
                SystemClock.sleep(1000);
                System.out.println("connected failed, retry....");
                e.printStackTrace();
            }
        }

        // 接收服务端的消息
        try {
            // 获取输入流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!MainActivity.this.isFinishing()) {
                String string = bufferedReader.readLine();
                System.out.println("receive:" + string);
                if (string != null) {
                    string = "server: " + string + "\n";
                    handler.obtainMessage(WHAT_MESSAGE_RECEIVED, string).sendToTarget();
                }
            }
            System.out.println("quit...");
            if (printWriter != null) printWriter.close();
            if (bufferedReader != null) bufferedReader.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_send) {
            // 点击发送消息
            final String message = editText.getText().toString();
            if (!TextUtils.isEmpty(message) && printWriter != null) {
                // IO操作不能在主线程
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        printWriter.println(message);
                    }
                }).start();
                editText.setText("");
                tvMessage.setText(tvMessage.getText() + "client: " + message + "\n");
            }
        }
    }
}