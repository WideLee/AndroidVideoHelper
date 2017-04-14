package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Main {

    public static final int CODE_BEGIN_RECORD = 1;
    public static final int CODE_END_RECORD = 2;

    public static void main(String[] args) throws IOException, InterruptedException {
        Process config = Runtime.getRuntime().exec("adb forward tcp:9888 tcp:9888");
        Socket socket = new Socket("localhost", 9888);

        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        outputStream.writeInt(CODE_BEGIN_RECORD);
        String location = inputStream.readUTF();

        String[] path = location.split("/");
        String outputFile = path[path.length - 1];

        Thread.sleep(5000);

        outputStream.writeInt(CODE_END_RECORD);
        int ready = inputStream.readInt();
        if (ready == 0) {

            Process retrieve = Runtime.getRuntime().exec(String.format("adb pull %s %s", location, outputFile));
            retrieve.waitFor();

            System.out.println("Save as outputFile");
        } else {
            System.out.println("Error");
        }

        socket.close();
    }
}
