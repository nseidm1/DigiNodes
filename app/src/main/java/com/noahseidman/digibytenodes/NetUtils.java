package com.noahseidman.digibytenodes;

import java.net.InetSocketAddress;
import java.net.Socket;

public class NetUtils {

    /**
     * check if a given server is listening on a given port in the limit of timeoutMs
     * @param serverHost server hostname (or ip)
     * @param serverPort server port
     * @param timeoutMs timeout in ms
     * @param exDetails message to provide as exception details when unable to connect
     */
    public static boolean checkServerListening(String serverHost, int serverPort, int timeoutMs, String exDetails) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(serverHost, serverPort), timeoutMs);
            return true;
        } catch (Exception e) {
            String errMsg = String.format("Can't connect to [%s:%d] (timeout was %d ms) - %s, - %s",
                    serverHost, serverPort, timeoutMs, exDetails, e.getMessage());
            return false;
        }
    }

}