/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 * @author eremeykin
 */
public class Connection {

    private Socket s;
    private InputStream is;
    private OutputStream os;

    public Connection(Socket s) throws IOException {
        this.s = s;
        this.is = s.getInputStream();
        this.os = s.getOutputStream();
    }

    public InputStream getInputStream() {
        return is;
    }

    public OutputStream getOutputStream() {
        return os;
    }
}
