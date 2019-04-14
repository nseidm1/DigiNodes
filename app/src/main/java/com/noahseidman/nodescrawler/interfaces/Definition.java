package com.noahseidman.nodescrawler.interfaces;

public interface Definition {

    String getCoinName();
    int getProtocolVersion();
    int getPort();
    String[] getDnsSeeds();
    boolean getAllowEmptyPeers();
    long getPacketMagic();
}
