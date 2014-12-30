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
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.LogManager;

/**
 *
 * @author Pete
 */
public class ClientProcessor extends Thread {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private Socket s;
    private InputStream is;
    private OutputStream os;
    private DataLoader dl;

    public ClientProcessor(Socket s) {

        this.s = s;
        try {
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
            this.dl = new DataLoader();
            LOG.info("Создан ClientProcessor для сокета " + s);
        } catch (IOException ex) {
            LOG.error("Ошибка при создании ClientProcessor");
        }
    }

    @Override
    public void run() {
        try {
            String headers = readInputHeaders();
            processHeaders(headers);
//            writeResponse();
//        } catch (InterruptedException e) {
//            LOG.error("Поток " + Thread.currentThread().getName() + " прерван." + e);
        } catch (IOException e) {
            LOG.error("Ошибка записи при отправке ответа. " + e);
        } finally {
            try {
                s.close();
            } catch (Throwable t) {
                System.err.println(t);
            }
        }
        LOG.info("Поток " + Thread.currentThread().getName() + " завершил обработу клиента");
    }

    private String readInputHeaders() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String inputHeaders = "";
        while (true) {
            try {
                String str = br.readLine();
                inputHeaders += str + "\n";
//                System.out.println("<" + str);
                if (str == null || str.trim().length() == 0) {
                    break;
                }
            } catch (IOException ex) {
                LOG.error("Ошибка при чтении заголовков входящего запроса" + ex);
            }
        }
        return inputHeaders;
    }

    private void processHeaders(String headers) {
        if (headers.contains("need_next_tick")) {
            int id = parseId(headers);
            HttpServer.getClientPool().getClientById(id).setNeedNext();
        } else {
            new TickFeeder().run();
        }
    }

    private int parseId(String headers) {
        StringTokenizer tokenizer = new StringTokenizer(headers, "\r\n");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            System.out.println(token+"!!!");
            if (token.startsWith("Client-Identificator:")) {
                return new Integer(token.substring("Client-Identificator: ".length()));
            }
        }
        throw new Error();
    }

    private class TickFeeder extends Thread {

        private static final String CRLF = "\r\n";
        private final Client client;

        public TickFeeder() {
            this.client = HttpServer.getClientPool().generateClient();
        }

        @Override
        public void run() {
            try {
                String response = "HTTP/1.1 200 OK" + CRLF
                        + "Server: TestServer/2014" + CRLF
                        + "Content-Type: application/json" + CRLF
                        + "Transfer-Encoding: chunked" + CRLF
                        + "Connection: close" + CRLF
                        + "Clien-Identificator: " + client.getId() + CRLF
                        + "Access-Control-Allow-Origin: *\n" + CRLF;
                String result = response;
                os.write(result.getBytes());
                os.flush();
                for (String line : dl) {
                    while (!client.needNext()) {
                    }
                    client.clearNeedNext();
                    String count = Integer.toHexString(line.length() + 1) + "\r\n\n";
                    line += "\r\n";
                    os.write((count + line).getBytes());
                    os.flush();
//            System.out.print(">" + line);
//                    Thread.sleep(2000);
                }
                os.write("0\r\n\r\n".getBytes());
                os.flush();
            } catch (IOException ex) {
                LOG.error("Ошибка при записи запроса. " + ex);
//            } catch (InterruptedException ex) {
//                LOG.error("Поток прерван. " + ex);
            }
        }

    }

}
