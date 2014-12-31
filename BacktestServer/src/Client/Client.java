/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;
//import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *
 * @author Pete
 */
public class Client {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private final int id;
    private boolean needNext;
    public Connection conn;

    public Client(int id, Socket s) throws IOException {
        this.conn = new Connection(s);
        this.id = id;
    }

    public Integer getId() {
        return this.id;
    }

    public String toString() {
        return "Client #" + Long.toString(id);
    }

    public synchronized void setNeedNext() {
        this.needNext = true;
    }

    public synchronized void clearNeedNext() {
        this.needNext = false;
    }

    public synchronized boolean needNext() {
        return this.needNext;
    }
}
