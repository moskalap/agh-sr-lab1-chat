package com.moskala.chat.server;

import com.moskala.chat.commons.Message;
import com.moskala.chat.commons.SocketAddres;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static final int BUFFER_LENGTH = 1024;
    private final int PORT;
    private final ServerSocketChannel tcpSocket;
    private final Map<String, SocketChannel> clientsOutputStreams = new ConcurrentHashMap<String, SocketChannel>();
    private final Selector selector;
    private final Set<SocketAddres> adresses = new HashSet<SocketAddres>();

    public Server(int port) throws IOException {

        this.PORT = port;
        this.selector = Selector.open();
        tcpSocket = ServerSocketChannel.open();
        tcpSocket.socket().bind(new InetSocketAddress(port));
        tcpSocket.configureBlocking(false);
        tcpSocket.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void runUDP() {
        new Thread(() -> {
            DatagramSocket socket = null;
            int portNumber = this.PORT;

            try {
                socket = new DatagramSocket(portNumber);
                byte[] receiveBuffer = new byte[BUFFER_LENGTH];

                while (true) {

                    Arrays.fill(receiveBuffer, (byte) 0);
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    SocketAddres sourceAdress = new SocketAddres(receivePacket.getAddress(), receivePacket.getPort());
                    adresses.add(sourceAdress);
                    Message msg = Message.fromString(new String(receivePacket.getData()).trim());
                    msg.display();
                    broadcastUDP(msg, socket, sourceAdress);

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }).start();


    }

    private void broadcastUDP(Message msg, DatagramSocket socket, SocketAddres sourceAddres) throws IOException {
        adresses
                .parallelStream()
                .filter(socketAddres -> !socketAddres.equals(sourceAddres))
                .forEach(socketAddres -> {
                    try {
                        socket.send(new DatagramPacket(msg.toString().getBytes(), msg.toString().length(),
                                socketAddres.getInetAddress(), socketAddres.getPort()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void runTCPServer() throws InterruptedException, IOException {
        Iterator<SelectionKey> iterator;
        SelectionKey selectionKey;
        while (this.tcpSocket.isOpen()) {
            selector.select();
            iterator = this.selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                selectionKey = iterator.next();
                iterator.remove();

                if (selectionKey.isAcceptable()) {
                    accept(selectionKey);
                }
                if (selectionKey.isReadable()) {
                    read(selectionKey);
                }
            }

        }

    }

    private void read(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Message m = receive(socketChannel);
        m.display();
        process(m, socketChannel, selectionKey);
    }

    private void process(Message msg, SocketChannel socketChannel, SelectionKey key) throws IOException {
        switch (msg.getMessageType()) {
            case HELLO:
                System.out.println("Hello " + msg.getUserName());
                if (clientsOutputStreams.containsKey(msg.getUserName())) {
                    write(new Message(
                            Message.MessageType.NACK,
                            "server",
                            "user taken"), socketChannel);
                } else {
                    clientsOutputStreams.put(msg.getUserName(), socketChannel);
                    write(new Message(
                            Message.MessageType.ACK,
                            "server",
                            "ok"), socketChannel);
                    broadcastTCP(new Message(
                            Message.MessageType.ACK,
                            "server",
                            String.format("%s joined to chat", msg.getUserName())).toString(), key);

                }
                break;
            case TCP:
                broadcastTCP(msg.toString(), key);
                break;


        }

    }

    private void write(Message message, SocketChannel socketChannel) throws IOException {
        socketChannel.write(ByteBuffer.wrap(message.toString().getBytes()));
    }

    private Message receive(SocketChannel socketChannel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1024);

        StringBuilder sb = new StringBuilder();

        while ((socketChannel.read(buf)) > 0) {
            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            sb.append(new String(bytes));
            buf.clear();
        }

        return Message.fromString(sb.toString());

    }

    private void accept(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
        String address = (new StringBuilder(socketChannel.socket().getInetAddress().toString()))
                .append(":")
                .append(socketChannel.socket().getPort())
                .toString();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ, address);

    }


    private void broadcastTCP(String msg, SelectionKey excludeKey) throws IOException {
        ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && !key.equals(excludeKey) && key.channel() instanceof SocketChannel) {
                SocketChannel sch = (SocketChannel) key.channel();
                sch.write(msgBuf);
                msgBuf.rewind();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 1) throw new IllegalArgumentException("Usage: server port_no");
        else {
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);
            server.runUDP();
            server.runTCPServer();

        }


    }
}
