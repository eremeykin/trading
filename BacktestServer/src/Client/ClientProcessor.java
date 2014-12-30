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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.LogManager;

/**
 *
 * @author Pete
 */
public class ClientProcessor extends Thread{

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private Socket s;
    private InputStream is;
    private OutputStream os;
    private DataLoader dl;
    private static final String CRLF = "\r\n";
    private final Client client;

    public ClientProcessor(Socket s) {
        this.client = HttpServer.getClientPool().generateClient();
        this.s = s;
        try {
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
            this.dl = new DataLoader();
            LOG.info("Создан ClientProcessor для сокета " + s);
        } catch (IOException ex) {
            LOG.error("Ошибка при создании ClientProcessor для клиента "+client);
        }
    }

    @Override
    public void run() {
        try {
            readInputHeaders();
            writeResponse();
        } catch (InterruptedException e) {
            LOG.error("Поток " + Thread.currentThread() + " прерван." + e);
        } catch (IOException e) {
            LOG.error("Ошибка записи при отпрваке ответа" + e);
        } finally {
            try {
                s.close();
            } catch (Throwable t) {
                System.err.println(t);
            }
        }
        LOG.info("Поток " + Thread.currentThread() + " завершил обработу клиента");
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
            //Thread.sleep(2000);
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
