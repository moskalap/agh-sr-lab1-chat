package com.moskala.chat.client;

import com.moskala.chat.commons.Message;
import com.moskala.chat.commons.UserExistsException;
import com.moskala.chat.server.Server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class Client {
    private final String hostName;
    private final String userName;
    private final int port;
    private DatagramSocket datagramSocket;
    private SocketChannel socket;
    private MulticastSocket multicastSocket;
    private InetAddress address;
    private InetAddress groupAddres;
    private int groupPort;


    public Client(String userName, String hostName, int port, InetAddress multiCastAddres, int multiPort){
        this.userName = userName;
        this.hostName = hostName;
        this.port = port;
        this.groupAddres = multiCastAddres;
        this.groupPort = multiPort;
    }

    public void init() {
        try {

            socket = SocketChannel.open(new InetSocketAddress(hostName, port));
            if (userNotExistsOnServer()){
                createReceiveThread();
                createSendThread();
                createUDPThreadReceive();
                createMultiCastThread();

            }
            else{
                throw new UserExistsException();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean userNotExistsOnServer() throws IOException, ClassNotFoundException {
        Message msg = new Message(Message.MessageType.HELLO, this.userName, "");
        ByteBuffer b = ByteBuffer.wrap(msg.toString().getBytes());
        socket.write(b);
        b.clear();
        socket.read(b);
        msg = Message.fromString(new String(b.array()).trim()) ;
        b.clear();
        return msg.getMessageType() == Message.MessageType.ACK;
    }

    private void createReceiveThread(){

        new Thread(() -> {

            while (true){
                ByteBuffer b = ByteBuffer.allocate(Server.BUFFER_LENGTH);
                Message msg = null;
                try {

                    socket.read(b);
                    msg = Message.fromString(new String(b.array()).trim()) ;
                    b.clear();
                    msg.display();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }


        }).start();


    }
    private void createUDPThreadReceive(){
        Thread t = new Thread(() -> {
            int portNumber = this.port;

            try {
                datagramSocket = new DatagramSocket();
                address = InetAddress.getByName("localhost");
                byte[] receiveBuffer = new byte[1024];

                while(true) {

                    Arrays.fill(receiveBuffer, (byte)0);
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length, address, portNumber);
                    datagramSocket.receive(receivePacket);
                    Message.fromString(new String(receivePacket.getData()).trim()).display();

                }

            }
            catch(Exception e){
                e.printStackTrace();
            }
            finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }
        });
        t.start();

    }

    private void createMultiCastThread() throws InterruptedException {
        Thread t = new Thread(() -> {

            try {
                multicastSocket = new MulticastSocket(groupPort);
                multicastSocket.joinGroup(groupAddres);

                while (true){
                    byte [] buff = new byte[Server.BUFFER_LENGTH];
                    DatagramPacket recv = new DatagramPacket(buff, buff.length);
                    multicastSocket.receive(recv);
                    Message.fromString(new String(recv.getData()).trim()).display();
                }


            }
            catch(Exception e){
                e.printStackTrace();
            }
            finally {
                if (multicastSocket != null) {
                    multicastSocket.close();
                }
            }
        });
        t.start();
        t.join();

    }
    private void createSendThread() throws IOException, InterruptedException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


        Thread t = new Thread(() -> {

            while (true){
                try {
                    String s = br.readLine();
                    Message.MessageType type = parseType(s);
                    Message msg = null;
                    switch (type){

                        case MULTICAST:
                            msg = new Message(Message.MessageType.MULTICAST, this.userName, s.substring(2, s.length()));
                            multicastSocket.send(new DatagramPacket(msg.toString().getBytes(),
                                    msg.toString().length(), groupAddres, groupPort));

                            break;
                        case UDP:
                            msg = new Message(Message.MessageType.UDP, this.userName, s.substring(2, s.length()));
                            datagramSocket.send(new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), address, port));
                            break;
                        case TCP:
                            msg = new Message(type, this.userName, s);
                            socket.write(ByteBuffer.wrap(msg.toString().getBytes()));
                            break;
                    }



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        });
        t.start();


    }

    private Message.MessageType parseType(String s) {
        if(s.startsWith("-")){
            switch (s.toLowerCase().charAt(1)){
                case 'u':
                    return Message.MessageType.UDP;
                case 'm':
                    return Message.MessageType.MULTICAST;
                default:
                    throw new IllegalStateException("Command not found!");
            }
        }else
            return Message.MessageType.TCP;
    }


    public static void main(String[] args) throws IOException {
        if(args.length < 4) throw new IllegalArgumentException("usage: client hostname port groupadress groupport username");
        else{
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String name = args[4];
            InetAddress g = InetAddress.getByName(args[2]);
            int multiPort = Integer.parseInt(args[3]);
            Client client = new Client(name, host, port, g, multiPort);

            client.init();

        }




    }
}

