package com.noahseidman.nodescrawler;

import com.noahseidman.coinj.core.PeerAddress;

import java.net.Socket;

public class NetUtils {

    public static boolean checkServerListening(PeerAddress peerAddress, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(peerAddress.toSocketAddress(), timeoutMs);
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}