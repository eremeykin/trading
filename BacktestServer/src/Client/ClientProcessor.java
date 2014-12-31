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

        private final Client client;
        private boolean stop = false;

        public TickFeeder(Socket s) throws IOException {
            this.client = HttpServer.getClientPool().generateClient(s);
        }

        public synchronized void setStop() {
            stop = true;
        }

        @Override
        public void run() {
            LOG.info("Запущен TickFeeder для клиента " + this.client);
            try {
                new ConnectionStatusScanner(client.conn.getInputStream(), this).start();
                outer:
                do {
                    while (!client.needNext()) {
                        if (stop) {
                            LOG.info("TickFeeder для клиента. " + client + "должен быть завершен. Соединение потеряно.");
                            break outer;
                        }
                    }
                } while (client.sendNext());
                LOG.info("Завершен TickFeeder для клиента. " + client);
            } catch (IOException ex) {
                LOG.error("Ошибка при записи запроса. " + ex);
                LOG.info("Завершен TickFeeder для клиента. " + client);
            }

        }

        private class ConnectionStatusScanner extends Thread {

            private final InputStream is;
            private final TickFeeder feeder;

            ConnectionStatusScanner(InputStream is, TickFeeder feeder) {
                this.setName("Status scanner for" + feeder.client);
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
