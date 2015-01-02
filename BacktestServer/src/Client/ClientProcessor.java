/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import Client.HTTPRequest.Type;
import Server.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
                    try {
                        HttpServer.getClientPool().getClientById(request.parseId()).setNeedNext();
                    } catch (NullPointerException ex) {
                        LOG.error("Запрошены данные для несуществующего клиента" + ex);
                    }
                    break;
                case MAKE_ORDER:
                    Client client = HttpServer.getClientPool().getClientById(request.parseId());
                    client.addOrder(request);
                    break;
            }
        } catch (IOException e) {
            LOG.error("Ошибка записи при отправке ответа. " + e);
        } catch (ClassNotFoundException ex) {
            LOG.fatal("Не найден класс для соединения с БД." + ex);
            System.exit(-1);
        } catch (SQLException ex) {
            LOG.fatal("Ошибка при выполнении SLQ запроса." + ex);
            System.exit(-1);
        } finally {
            try {
                s.close();
            } catch (Throwable t) {
                System.err.println(t);
            }
        }
        LOG.info("Поток " + Thread.currentThread().getName() + " завершил обработу клиента");
    }

    private synchronized HTTPRequest readInput() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String header = "";
        boolean isHeader = true;
        Scanner scanner = new Scanner(br);
        scanner.useDelimiter("\r\n\r\n");
        HTTPRequest result = new HTTPRequest(scanner.next());
//        LOG.debug(result);
        scanner.useDelimiter("");
        String body = "";
        boolean hasBody = result.getType() == Type.MAKE_ORDER;
        int length = result.parseLength();
        if (hasBody) {
            //String body = "";
            while (result.bodyLength() < length) {
                char c = scanner.next().charAt(0); //br.read(); //scanner.nextByte();
                body += c;
                result.addBody(body);
                if (c == -1) {
                    break;
                }
            }
            String CRLF = "\r\n";
            String head = "HTTP/1.1 200 OK" + CRLF
                    + "Server: TestServer/2014" + CRLF
                    + "Content-Type: application/json" + CRLF
                    + "Connection: close" + CRLF
                    + "Clien-Identificator: " + this.getId() + CRLF
                    + "Access-Control-Allow-Origin: *\n" + CRLF
                    + CRLF + CRLF;
            OutputStream os = s.getOutputStream();
            os.write(head.getBytes());
            os.flush();
        }
        LOG.debug(result);
        return result;
    }

    private class TickFeeder implements Runnable {

        private final Client client;
        private boolean stop = false;

        public TickFeeder(Socket s) throws IOException, SQLException, ClassNotFoundException {
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
