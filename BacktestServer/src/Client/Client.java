/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;
//import org.apache.logging.log4j.Logger;

import DataLoader.DataLoader;
import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *
 * @author Pete
 */
public class Client {

    private static final String CRLF = "\r\n";
    private final String header;

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private final int id;
    private boolean needNext;
    public Connection conn;
    private final Iterator<String> iterator;
    private DataItem dataItem;

    public Client(int id, Socket s) throws IOException {
        this.conn = new Connection(s);
        this.id = id;
        this.iterator = new DataLoader().iterator();
        header = "HTTP/1.1 200 OK" + CRLF
                + "Server: TestServer/2014" + CRLF
                + "Content-Type: application/json" + CRLF
                + "Transfer-Encoding: chunked" + CRLF
                + "Connection: close" + CRLF
                + "Clien-Identificator: " + this.getId() + CRLF
                + "Access-Control-Allow-Origin: *\n" + CRLF
                + "2" + CRLF
                + CRLF + CRLF;
        OutputStream os = this.conn.getOutputStream();
        os.write(header.getBytes());
        os.flush();
    }

    public Integer getId() {
        return this.id;
    }

    public String toString() {
        return "Client #" + Long.toString(id);
    }

    public synchronized void setNeedNext() {
        LOG.info("Установлен needNext для" + this);
        this.needNext = true;
    }

    public synchronized void clearNeedNext() {
        LOG.info("Снят needNext для " + this);
        this.needNext = false;
    }

    public synchronized boolean needNext() {
        return this.needNext;
    }

    public synchronized boolean sendNext() throws IOException {
        OutputStream os = this.conn.getOutputStream();
        this.clearNeedNext();
        if (iterator.hasNext()) {
            String line = iterator.next();
            String count = Integer.toHexString(line.length() + 2) + "\r\n";
            line += CRLF+"\n";
            os.write((count + line).getBytes());
            os.flush();
            //LOG.info(line);
            dataItem = new DataItem(line);
            LOG.info(dataItem);
            return true;
        } else {
            os.write("0\r\n\r\n".getBytes());
            os.flush();
            return false;
        }
    }

}
