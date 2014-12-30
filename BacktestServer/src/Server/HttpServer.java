/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import DataLoader.DataLoader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *
 * @author eremeykin
 */
public class HttpServer {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(HttpServer.class);

    public static void main(String[] args) throws Throwable {
        ServerSocket ss = new ServerSocket(8080);
        LOG.debug("Debug message");
        LOG.info("Info message");
        LOG.warn("Warn message");
        LOG.error("Error message");
        LOG.fatal("Fatal message");
        while (true) {
            Socket s = ss.accept();
            s.setKeepAlive(true);
            System.err.println("Client accepted");
            //new Thread(new SocketProcessor(s)).start();
        }
    }
}
