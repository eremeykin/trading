/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import DataLoader.DataLoader;
import Server.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.log4j.LogManager;

/**
 *
 * @author Pete
 */
public class ClientProcessor implements Runnable{

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private Socket s;
    private InputStream is;
    private OutputStream os;
    private DataLoader dl;
    private static final String CRLF = "\r\n";

    public ClientProcessor(Socket s) throws Throwable {
        this.s = s;
        this.is = s.getInputStream();
        this.os = s.getOutputStream();
        this.dl = new DataLoader();
        LOG.info("Создан ClientProcessor для сокета "+s);
    }

    public void run() {
        try {
            readInputHeaders();
            writeResponse();
        } catch (InterruptedException | IOException e) {
            System.err.println(e);
        } finally {
            try {
                s.close();
            } catch (Throwable t) {
                System.err.println(t);
            }
        }
        System.err.println("Client processing finished");
    }

    private void writeResponse() throws IOException, InterruptedException {
        String response = "HTTP/1.1 200 OK" + CRLF
                + "Server: TestServer/2014" + CRLF
                + "Content-Type: application/json" + CRLF
                + "Transfer-Encoding: chunked" + CRLF
                + "Connection: close" + CRLF
                + "Access-Control-Allow-Origin: *\n" + CRLF;
        String result = response;
        os.write(result.getBytes());
        os.flush();

        for (String line : dl) {
            String count = Integer.toHexString(line.length() + 1) + "\r\n\n";
            line += "\r\n";
            os.write((count + line).getBytes());
            os.flush();
            System.out.print(">" + line);
            Thread.sleep(2000);
        }
        os.write("0\r\n\r\n".getBytes());
        os.flush();
    }

    private void readInputHeaders() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while (true) {
            try {
                String str = br.readLine();
                System.out.println("<" + str);
                if (str == null || str.trim().length() == 0) {
                    break;
                }
            } catch (Throwable t) {
                System.out.println(t);
            }
        }
    }
}
