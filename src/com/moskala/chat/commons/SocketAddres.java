package com.moskala.chat.commons;

import java.net.InetAddress;

public class SocketAddres {
    private final InetAddress inetAddress;
    private final int port;

    public SocketAddres(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getInetAddress() {

        return inetAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocketAddres that = (SocketAddres) o;

        if (port != that.port) return false;
        return inetAddress != null ? inetAddress.equals(that.inetAddress) : that.inetAddress == null;
    }

    @Override
    public int hashCode() {
        int result = inetAddress != null ? inetAddress.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
