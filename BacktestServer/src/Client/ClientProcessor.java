/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import Server.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
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
            HTTPRequest request = readInput();
            switch (request.getType()) {
                case NEW_CLIENT:
                    new TickFeeder(s).run();
                    break;
                case NEED_NEXT_TICK:
                    HttpServer.getClientPool().getClientById(request.parseId()).setNeedNext();
                    break;
                case MAKE_ORDER:
                    break;
            }
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

    private HTTPRequest readInput() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String header = "";
        String body = "";
        boolean isHeader = true;
        char c = (char) br.read();
        HTTPRequest result = new HTTPRequest("");

        while (true) {
            try {
                System.err.println("\nheader:");
                System.err.println(header.replace("\r", "\\r").replace("\n", "\\n"));
                if (header.contains("\r\n\r")) {
                    isHeader = false;
                    result = new HTTPRequest(header);
                }
                if (isHeader) {
                    header += c;
                } else {
                    if (result.getType() == HTTPRequest.Type.NEW_CLIENT) {
                        break;
                    }
                    if (result.getType() == HTTPRequest.Type.NEED_NEXT_TICK) {
                        break;
                    }
                    if (result.getType() == HTTPRequest.Type.MAKE_ORDER) {
                        body += c;
                    }
                }
                if (c == '\r') {
                    System.err.print("\\r");
                }

                if (c == '\n') {
                    System.err.print("\\n");
                }

                if (c == -1) {
                    break;
                }
                c = (char) br.read();
                //String str = br.readLine();
                //inputHeaders += str + "\n";
//                LOG.debug(inputHeaders);
//                if (str == null || str.trim().length() == 0) {
//                    break;
//                }
            } catch (Throwable ex) {
                LOG.error("Ошибка при чтении заголовков входящего запроса" + ex);
            }
        }
        LOG.info(result + "!!");
        return result;
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
//                    while (!client.needNext()) {
//                        if (stop) {
//                            LOG.info("TickFeeder для клиента. " + client + " должен быть завершен. Соединение потеряно.");
//                            break outer;
//                        }
//                    }
                    if (client.needNext()) {
                        client.sendNext();
                    }
                } while (client.hasMoreData());
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
