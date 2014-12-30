/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Client.ClientPool;
import Client.ClientProcessor;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.LogManager;

/**
 *
 * @author eremeykin
 */
public class HttpServer {

    private static final org.apache.log4j.Logger LOG = LogManager.getLogger(HttpServer.class);
    private static final ClientPool cPool = new ClientPool();

    public static ClientPool getClientPool(){
        return HttpServer.cPool;
    }
    
    public static void main(String[] args) {
        try {
            LOG.info("Сервер запущен.");
            ServerSocket ss = new ServerSocket(8080);
            while (true) {
                Socket s = ss.accept();
                LOG.info("Клиент принят");
                s.setKeepAlive(true);
                new ClientProcessor(s).start();
            }
        } catch (IOException ex) {
            LOG.fatal("Ошибка при создании ServerSocket" + ex);
        }
    }
}
