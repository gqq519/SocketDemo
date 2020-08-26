package com.gqq.socketdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class SocketServer extends Service {

    private boolean isServerDestroyed = false;
    private String[] messages = new String[]{
            "你好",
            "你的名字",
            "你的年纪",
            "今天天气真好啊",
            "再见"
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // 需要开线程开启服务
        new Thread(new TcpServer()).start();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServerDestroyed = true;
    }

    private class TcpServer implements Runnable {

        @Override
        public void run() {

            ServerSocket serverSocket = null;

            try {
                serverSocket = new ServerSocket(4846);
            } catch (IOException e) {
                System.out.println("tcp server establish failed, part 4846");
                e.printStackTrace();
                return;
            }

            while (!isServerDestroyed) {
                try {
                    final Socket client = serverSocket.accept();

                    System.out.println("accept client");

                    // 消息互动
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                responseMessage(client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void responseMessage(Socket client) throws IOException {
            // 接收客户端消息：从scoket中获取输入流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // 向客户端发送消息：通过socket的输出流，切记要 flush数据
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
            printWriter.println("欢迎");
            while (!isServerDestroyed) {
                String string = bufferedReader.readLine();
                System.out.println("msg from client: " + string);
                if (string == null) {
                    // 客户端断开连接
                    return;
                }
                int i = new Random().nextInt(messages.length);
                String message = messages[i];
                printWriter.println(message);
                System.out.println("send msg: " + message);
            }
            System.out.println("Client quit");
            // 关闭流
            if (printWriter != null) {
                printWriter.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            client.close();
        }
    }
}
