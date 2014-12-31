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

    public ClientProcessor(Socket s) {
        this.setName("ClientProcessor");
        this.s = s;
        LOG.info("Создан ClientProcessor для сокета " + s);
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
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
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
            try {
                HttpServer.getClientPool().getClientById(id).setNeedNext();
            } catch (NullPointerException ex) {
                LOG.error("Попытка запросить данные для несуществующего клиента.");
            }
        } else {
            try {
                new TickFeeder(s).run();
            } catch (IOException ex) {
                LOG.error("Не удалось создать клиента." + ex);
            }
        }
    }

    private int parseId(String headers) {
        StringTokenizer tokenizer = new StringTokenizer(headers, "\r\n");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("Client-Identificator:")) {
                return new Integer(token.substring("Client-Identificator: ".length()));
            }
        }
        throw new Error();
    }

    private class TickFeeder implements Runnable {

        private static final String CRLF = "\r\n";
        private final Client client;
        private final DataLoader dl;
        private boolean stop = false;

        public TickFeeder(Socket s) throws IOException {
            this.client = HttpServer.getClientPool().generateClient(s);
            this.dl = new DataLoader();
        }

        public synchronized void setStop() {
            stop = true;
        }

        @Override
        public void run() {
            LOG.info("Запущен TickFeeder для клиента " + this.client);
            OutputStream os = this.client.conn.getOutputStream();
            InputStream is = this.client.conn.getInputStream();
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
                new ConnectionStatusScanner(is, this).start();
                outer:
                for (String line : dl) {
                    while (!client.needNext()) {
                        if (stop) {
                            LOG.info("TickFeeder для клиента." + client + "должен быть завершен. Соединение потеряно.");
                            break outer;
                        }
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
                LOG.info("Завершен TickFeeder для клиента." + client);
            } catch (IOException ex) {
                LOG.error("Ошибка при записи запроса. " + ex);
                LOG.info("Завершен TickFeeder для клиента " + client);
//            } catch (InterruptedException ex) {
//                LOG.error("Поток прерван. " + ex);
            }

        }

        private class ConnectionStatusScanner extends Thread {

            private final InputStream is;
            private final TickFeeder feeder;

            ConnectionStatusScanner(InputStream is, TickFeeder feeder) {
                this.setName("Status scanner for"+feeder.client);
                this.is = is;
                this.feeder = feeder;
            }

            @Override
            public void run() {
                boolean go = true;
                while (go) {
                    try {
                        go = is.read() != -1;
                    } catch (IOException ex) {
                        LOG.error("Ошибка при сканировании статуса соединения во внутреннем потоке.");
                    }
                }
                feeder.setStop();

            }
        }
    }

}
