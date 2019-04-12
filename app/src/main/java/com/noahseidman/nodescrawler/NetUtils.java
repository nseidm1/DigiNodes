package com.noahseidman.nodescrawler;

import java.net.InetSocketAddress;
import java.net.Socket;

public class NetUtils {

    /**
     * check if a given server is listening on a given port in the limit of timeoutMs
     * @param serverHost server hostname (or ip)
     * @param serverPort server port
     * @param timeoutMs timeout in ms
     */
    public static boolean checkServerListening(String serverHost, int serverPort, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(serverHost, serverPort), timeoutMs);
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}