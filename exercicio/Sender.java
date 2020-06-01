package exercicio;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.io.*;

public class Sender {
    private static final String SENDER_IP = "localhost";
    private static final String RECEIVER_IP = "localhost";
    private static final int SENDER_PORT = 12345;
    private static final int RECEIVER_PORT = 12346;
    private static final int HEADER = 4 * 3; // 4 bytes para cada: numero do pacote, quantidade de pacotes, offset
    private static final int MAX_CONTENT = 48;
    private static final int PACKAGE_SIZE = HEADER + MAX_CONTENT;
    private static final int WINDOW_SIZE = 3;

    Scanner input;
    InetAddress sender_address;
    InetAddress receiver_address;
    boolean sending;
    String message;
    int type;
    ArrayList<Boolean> packages_ackd;

    public static void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println("---------- INICIANDO CLIENTE SENDER ----------");
        System.out.println("Comportamentos de envio:");
        System.out.println("> 1 - envia normalmente,");
        System.out.println("> 2 - envia com perda,");
        System.out.println("> 3 - envia fora de ordem,");
        System.out.println("> 4 - envia com duplicados,");
        System.out.println("> 5 - envia com lentidao,");
        System.out.println("> 0 - encerra a execucao.");
        new Sender();
    }

    Sender() throws UnknownHostException, SocketException {
        input = new Scanner(System.in);
        sender_address = InetAddress.getByName(SENDER_IP);
        receiver_address = InetAddress.getByName(RECEIVER_IP);
        sending = false;

        System.out.println("Digite o comportamento para a proxima mensagem: ");
        type = 1;// input.nextInt();
        if (type == 0)
            return;

        System.out.println("Digite a mensagem na proxima linha: ");
        // message = input.next();
        message = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

        SendThread sendThread = new SendThread();
        sendThread.start();
        AckReceiveThread ackThread = new AckReceiveThread();
        ackThread.start();
    }

    private class SendThread extends Thread {
        private DatagramSocket socketSend;

        SendThread() throws SocketException {
            socketSend = new DatagramSocket();

            System.out.println("Sender iniciado no endereco " + sender_address.getHostAddress() + " na porta "
                    + socketSend.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            try {
                System.out.println("Enviando mensagem: " + message);

                byte[] message_buffer = message.getBytes();
                int num_packages = (message_buffer.length / MAX_CONTENT)
                        + (message_buffer.length % MAX_CONTENT > 0 ? 1 : 0);
                packages_ackd = new ArrayList<Boolean>(num_packages);

                for (int i = 0; i < num_packages; i++) {
                    int content_size = Integer.min(((i + 1) * MAX_CONTENT), message_buffer.length) - i * MAX_CONTENT;
                    int offset = MAX_CONTENT - content_size;

                    ByteBuffer bbuff = ByteBuffer.wrap(new byte[HEADER + content_size]);
                    bbuff.putInt(i).putInt(num_packages).putInt(offset); // monta o header
                    bbuff.put(message_buffer, i * MAX_CONTENT, content_size); // adiciona o conteudo
                    byte[] pack = bbuff.array();

                    DatagramPacket dPacket = new DatagramPacket(pack, pack.length, receiver_address, RECEIVER_PORT);
                    socketSend.send(dPacket);
                    packages_ackd.add(i, false);
                }
            } catch (Exception e) {
                System.err.println("---- ERRO AO ENVIAR PACKAGE -----");
                System.err.println(e);
            }
        }
    }

    private class AckReceiveThread extends Thread {
        private DatagramSocket socketAck;

        AckReceiveThread() throws SocketException {
            socketAck = new DatagramSocket(SENDER_PORT);

            System.out.println("Sender escutando no endereco " + sender_address.getHostAddress() + " na porta "
                    + socketAck.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            try {
                while (true) {
                    byte[] receiveData = new byte[4];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socketAck.receive(receivePacket);

                    byte[] received_bytes = receivePacket.getData();
                    ByteBuffer bbuff = ByteBuffer.wrap(received_bytes);
                    int package_ack = bbuff.getInt();
                    packages_ackd.set(package_ack, true);

                    String fromClient = receiver_address.getHostAddress() + ':' + receivePacket.getPort();

                    System.out.println(
                            "Mensagem de acknowledgement " + (package_ack+1) + " recebida do receiver " + fromClient);
                }
            } catch (Exception e) {
                System.err.println("---- ERRO AO RECEBER ACK -----");
                System.err.println(e);
            } finally {
                socketAck.close();
            }
        }
    }
}