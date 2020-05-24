import java.net.*;
import java.util.HashMap;
import java.io.*;

public class Server {
    public static void main (String args[]) throws IOException, InterruptedException {
        System.out.println("Iniciando Servidor");

        DatagramSocket serverSocket = new DatagramSocket(1234);
        byte[] receiveData;

        HashMap<String,ServerThread> map = new HashMap<String,ServerThread>();

        do {
            receiveData = new byte[128];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            
            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String fromClient = receivePacket.getAddress().getHostAddress() +':'+ receivePacket.getPort();
            System.out.println("Mensagem recebida do cliente " + fromClient + ": " + received);

            if(map.containsKey(fromClient)) {
                ServerThread thread = map.get(fromClient);
                if(thread.isAlive()) thread.receive(received);
            } else {
                ServerThread thread = new ServerThread(fromClient);
                thread.start();
                map.put(fromClient, thread);
                thread.receive(received);
            }

            System.out.println(map.size());
        } while(map.size() < 3);

        serverSocket.close();
    }
}

class ServerThread extends Thread {
    private String fromClient;
    private boolean[] msgs = {false, false, false, false};

    ServerThread(String fromClient) {
        this.fromClient = fromClient;
    }
    
    public void run() {
        try {
            Thread.sleep(10000);
            System.out.println("Consumindo mensagens recebidas do endereço " + fromClient);
            System.out.println("Mensagem 1: " + (msgs[0] ? "OK" : "Não recebida"));
            System.out.println("Mensagem 2: " + (msgs[1] ? "OK" : "Não recebida"));
            System.out.println("Mensagem 3: " + (msgs[2] ? "OK" : "Não recebida"));
            System.out.println("Mensagem 4: " + (msgs[3] ? "OK" : "Não recebida"));
        }
        catch (Exception e) {

        }
    }

    public void receive(String msg) {
        switch(msg) {
            case "1":
                msgs[0] = true;
                break;
            case "2":
                msgs[1] = true;
                break;
            case "3":
                msgs[2] = true;
                break;
            case "4":
                msgs[3] = true;
                break;
            default: break;
        }
    }
}