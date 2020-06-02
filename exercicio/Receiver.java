package exercicio;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.io.*;

public class Receiver {
    private static final String SENDER_IP = "localhost";
    private static final String RECEIVER_IP = "localhost";
    private static final int SENDER_PORT = 12345;
    private static final int RECEIVER_PORT = 12346;
    private static final int HEADER = 4 * 3; // 4 bytes para cada: numero do pacote, quantidade de pacotes, offset
    private static final int MAX_CONTENT = 10;
    private static final int PACKAGE_SIZE = HEADER + MAX_CONTENT;

    InetAddress sender_address;
    InetAddress receiver_address;
    boolean receiving;
    StringBuilder message;
    int last_received;
    boolean[] received_packages;
    DatagramSocket socketAck;

    public static void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println("---------- INICIANDO SERVIDOR RECEIVER ----------");
        new Receiver();
    }

    public Receiver() throws UnknownHostException, SocketException {
        receiver_address = InetAddress.getByName(RECEIVER_IP);
        receiving = false;
        socketAck = new DatagramSocket();

        ReceiveThread receiveThread = new ReceiveThread();
        receiveThread.start();
    }

    private class ReceiveThread extends Thread {
        private DatagramSocket socketReceive;

        ReceiveThread() throws SocketException {
            socketReceive = new DatagramSocket(RECEIVER_PORT);

            System.out.println("Receiver escutando no endereco " + receiver_address.getHostAddress() + " na porta "
                    + RECEIVER_PORT);
        }

        @Override
        public void run() {
            super.run();

            try {
                while (true) {
                    byte[] receiveData = new byte[PACKAGE_SIZE];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socketReceive.receive(receivePacket);

                    byte[] received_bytes = receivePacket.getData();
                    ByteBuffer bbuff = ByteBuffer.wrap(received_bytes);
                    int package_number = bbuff.getInt();
                    int num_packages = bbuff.getInt();
                    int offset = bbuff.getInt();
                    String received_content = new String(received_bytes, HEADER,
                            received_bytes.length - HEADER - offset);

                    sender_address = receivePacket.getAddress();
                    String fromClient = sender_address.getHostAddress() + ':' + receivePacket.getPort();

                    if (!receiving) {
                        receiving = true;
                        received_packages = new boolean[num_packages];
                        last_received = -1;
                        message = new StringBuilder();
                    }

                    boolean is_repeated = false;
                    if (package_number == last_received + 1) {
                        received_packages[package_number] = true;
                        last_received++;
                        sendAck(package_number);
                        message.append(received_content);
                    } else {
                        if (received_packages[package_number]) {
                            is_repeated = true;
                        }
                        sendAck(last_received);
                    }

                    System.out.println("Mensagem " + (is_repeated ? "repetida " : "") + (package_number + 1) + " de "
                            + num_packages + " recebida do sender " + fromClient + ", " + received_content.length()
                            + " bytes: " + received_content);

                    if(received_packages[received_packages.length-1] == true) {
                        receiving = false;
                        System.out.println("MENSAGEM COMPLETA COM " + message.length() + " bytes: " + message.toString());
                    }
                }
            } catch (Exception e) {
                System.err.println("---- ERRO AO RECEBER PACKAGE -----");
                System.err.println(e);
            } finally {
                socketReceive.close();
                socketAck.close();
            }
        }
    }

    private void sendAck(int package_number) throws IOException {
        try {
            ByteBuffer bbuff = ByteBuffer.wrap(new byte[4]);
            bbuff.putInt(package_number);
            DatagramPacket dPacket = new DatagramPacket(bbuff.array(), 4, sender_address, SENDER_PORT);
            socketAck.send(dPacket);
        } catch (Exception e) {
            System.err.println("---- ERRO AO ENVIAR ACK -----");
            System.err.println(e);
        }
    }
}