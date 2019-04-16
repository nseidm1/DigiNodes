package com.noahseidman.nodescrawler.interfaces;

public interface Definition {

    String getCoinName();
    int getMinProtocolVersion();
    int getPort();
    String[] getDnsSeeds();
    boolean getAllowEmptyPeers();
    long getPacketMagic();
}
