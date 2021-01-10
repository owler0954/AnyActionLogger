package anyActionLogger;

import com.google.gson.Gson;

import java.util.Date;
import java.net.*;
import java.io.*;

public class SocketConnector {
    // initialize socket and input output streams
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;

    // constructor to put ip address and port
    public SocketConnector(SendedPackage sendedPackage) {
        try {
            socket = new Socket("127.0.0.1", 5000);
            System.out.println("Connected");
            input = new DataInputStream(System.in);
            out = new DataOutputStream(socket.getOutputStream());
        } catch (UnknownHostException u) {
            System.out.println(u);
        } catch (IOException i) {
            System.out.println(i);
        }// string to read message from input
// keep reading until "Over" is input
        try {
            Gson g = new Gson();
            out.writeUTF(g.toJson(sendedPackage));
        } catch (IOException i) {
            System.out.println(i);
        }
// close the connection
        try {
            input.close();
            out.close();
            socket.close();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static class SendedPackage {
        String type;
        String reason;
        String name;
        String server;
        String author;
        int banTime;
        Date data;

        public SendedPackage(String type, String reason, String name, String server, String author, int banTime, Date data) {
            this.type = type;
            this.reason = reason;
            this.name = name;
            this.server = server;
            this.author = author;
            this.banTime = banTime;
            this.data = data;
        }
    }
}
