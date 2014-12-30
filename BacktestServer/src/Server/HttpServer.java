/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Client.ClientProcessor;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.log4j.LogManager;

/**
 *
 * @author eremeykin
 */
public class HttpServer {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(HttpServer.class);

    public static void main(String[] args) throws Throwable {
        LOG.info("Сервер запущен.");
        ServerSocket ss = new ServerSocket(8080);
        while (true) {
            Socket s = ss.accept();
            LOG.info("Клиент принят");
            s.setKeepAlive(true);
            new Thread(new ClientProcessor(s)).start();
        }
    }
}
