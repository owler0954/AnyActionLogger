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
            out = new DataOutputStream(socket.getOutputStream());
        } catch (UnknownHostException u) {
            System.out.println(u);
            return;
        } catch (IOException i) {
            System.out.println(i);
            return;
        }// string to read message from input
// keep reading until "Over" is input
        try {
            if(out==null){return;}
            out.writeUTF(sendedPackage.type);
            out.writeUTF(sendedPackage.reason);
            out.writeUTF(sendedPackage.name);
            out.writeUTF(sendedPackage.server);
            out.writeUTF(sendedPackage.author);
            out.writeUTF(sendedPackage.banTime+"");
            out.writeUTF(sendedPackage.data.toString());
        } catch (IOException i) {
            System.out.println(i);
            return;
        }
// close the connection
        try {
            out.close();
            socket.close();
        } catch (IOException i) {
            System.out.println(i);
            return;
        }
        System.out.println("Closed");
        return;
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
