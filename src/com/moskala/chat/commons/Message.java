package com.moskala.chat.commons;

import java.io.Serializable;

public class Message implements Serializable {

    public enum MessageType {
        HELLO, ACK, MULTICAST, UDP, NACK, TCP
    }

    private final MessageType messageType;

    private final String userName;

    private final String content;

    public Message(MessageType messageType, String userName, String content) {
        this.messageType = messageType;
        this.userName = userName;
        this.content = content;
    }

    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void display() {
        String channel = messageType.toString();
        System.out.println(String.format("[%s via %s]: %s", userName, channel, content));
    }

    @Override
    public String toString() {
        return messageType.toString() + "." + userName + "." + content;
    }

    public static Message fromString(String s) {
        try {


            String[] var = s.split("\\.");
            String t = var[0];
            String user = var[1];
            if (var.length < 3) {
                return new Message(MessageType.valueOf(t), user, "");
            }
            var = s.split(t + "." + user + ".");

            return new Message(MessageType.valueOf(t), user, var.length == 1 ? "" : var[1]);
        } catch (ArrayIndexOutOfBoundsException e) {

            return new Message(MessageType.HELLO, "server", "hello");
        }


    }
}
