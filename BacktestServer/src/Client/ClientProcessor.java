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
                        LOG.error("Запрошены данные для несуществующего клиента. Клиент №" + request.parseId() + " " + ex);
                    }
                    break;
                case MAKE_ORDER:
                    try {
                        Client client = HttpServer.getClientPool().getClientById(request.parseId());
                        client.addOrder(request);
                    } catch (NullPointerException ex) {
                        LOG.error("Запрошены данные для несуществующего клиента. Клиент №" + request.parseId() + " " + ex);
                    }
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
        boolean hasBody = result.getType() == Type.WITH_BODY;
        int length = result.parseLength();
        if (hasBody) {
            while (result.bodyLength() < length) {
                char c = scanner.next().charAt(0); //br.read(); //scanner.nextByte();
                body += c;
                result.addBody(body);
                if (c == -1) {
                    break;
                }
            }

        }
        if (result.getType() == Type.NEED_NEXT_TICK) {
            String CRLF = "\r\n";
            String head = "HTTP/1.1 200 OK" + CRLF
                    + "Server: TestServer/2014" + CRLF
                    + "Content-Type: application/json" + CRLF
                    + "Connection: close" + CRLF
                    + "Content-Length: 0" + CRLF
                    + "Client-Identificator: " + this.getId() + CRLF
                    + "Access-Control-Allow-Origin: *\n" + CRLF
                    + CRLF + CRLF;
            OutputStream os = s.getOutputStream();
            os.flush();
            os.write(head.getBytes());
        }

        if (result.getType() == Type.NEED_CANDLES) {
            String CRLF = "\r\n";
            String head = "HTTP/1.1 200 OK" + CRLF
                    + "Server: TestServer/2014" + CRLF
                    + "Content-Type: application/json" + CRLF
                    + "Connection: close" + CRLF
                    + "Content-Length: " + CANDLES.length() + CRLF
                    + "Client-Identificator: " + this.getId() + CRLF
                    + "Access-Control-Allow-Origin: *\n" + CRLF
                    + CRLF;
            OutputStream os = s.getOutputStream();
            os.flush();
            os.write((head + CANDLES).getBytes());
        }
        LOG.debug("Принято сообщение типа " + result.getType().name() + ":\n" + result);
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
                        LOG.info("Отправлен тик для клиента " + client);
                    }
                } while (client.hasMoreData());
                client.sendNext();
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
                try {
                    while (go) {
                        go = is.read() != -1;
                    }
                } catch (IOException ex) {
                    LOG.error("Ошибка при сканировании статуса соединения во внутреннем потоке. "
                            + feeder.client + " потерял связь.");
                }
                feeder.setStop();
            }
        }
    }

    String CANDLES = "{\n"
            + "	\"instrument\" : \"EUR_USD\",\n"
            + "	\"granularity\" : \"S5\",\n"
            + "	\"candles\" : [\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:56:55.000000Z\",\n"
            + "			\"openBid\" : 1.08083,\n"
            + "			\"openAsk\" : 1.08108,\n"
            + "			\"highBid\" : 1.08084,\n"
            + "			\"highAsk\" : 1.08108,\n"
            + "			\"lowBid\" : 1.08083,\n"
            + "			\"lowAsk\" : 1.08106,\n"
            + "			\"closeBid\" : 1.08084,\n"
            + "			\"closeAsk\" : 1.08106,\n"
            + "			\"volume\" : 4,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:57:00.000000Z\",\n"
            + "			\"openBid\" : 1.08084,\n"
            + "			\"openAsk\" : 1.08104,\n"
            + "			\"highBid\" : 1.08084,\n"
            + "			\"highAsk\" : 1.08105,\n"
            + "			\"lowBid\" : 1.08083,\n"
            + "			\"lowAsk\" : 1.08104,\n"
            + "			\"closeBid\" : 1.08083,\n"
            + "			\"closeAsk\" : 1.08104,\n"
            + "			\"volume\" : 3,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:57:10.000000Z\",\n"
            + "			\"openBid\" : 1.08066,\n"
            + "			\"openAsk\" : 1.08113,\n"
            + "			\"highBid\" : 1.08066,\n"
            + "			\"highAsk\" : 1.08126,\n"
            + "			\"lowBid\" : 1.08007,\n"
            + "			\"lowAsk\" : 1.08081,\n"
            + "			\"closeBid\" : 1.08022,\n"
            + "			\"closeAsk\" : 1.08092,\n"
            + "			\"volume\" : 13,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:57:15.000000Z\",\n"
            + "			\"openBid\" : 1.08035,\n"
            + "			\"openAsk\" : 1.08087,\n"
            + "			\"highBid\" : 1.08035,\n"
            + "			\"highAsk\" : 1.08087,\n"
            + "			\"lowBid\" : 1.08035,\n"
            + "			\"lowAsk\" : 1.08087,\n"
            + "			\"closeBid\" : 1.08035,\n"
            + "			\"closeAsk\" : 1.08087,\n"
            + "			\"volume\" : 1,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:57:20.000000Z\",\n"
            + "			\"openBid\" : 1.08044,\n"
            + "			\"openAsk\" : 1.08084,\n"
            + "			\"highBid\" : 1.08044,\n"
            + "			\"highAsk\" : 1.08084,\n"
            + "			\"lowBid\" : 1.08044,\n"
            + "			\"lowAsk\" : 1.08084,\n"
            + "			\"closeBid\" : 1.08044,\n"
            + "			\"closeAsk\" : 1.08084,\n"
            + "			\"volume\" : 1,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:57:30.000000Z\",\n"
            + "			\"openBid\" : 1.08053,\n"
            + "			\"openAsk\" : 1.0808,\n"
            + "			\"highBid\" : 1.08053,\n"
            + "			\"highAsk\" : 1.0808,\n"
            + "			\"lowBid\" : 1.08039,\n"
            + "			\"lowAsk\" : 1.08074,\n"
            + "			\"closeBid\" : 1.08039,\n"
            + "			\"closeAsk\" : 1.08074,\n"
            + "			\"volume\" : 7,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:57:50.000000Z\",\n"
            + "			\"openBid\" : 1.0803,\n"
            + "			\"openAsk\" : 1.08076,\n"
            + "			\"highBid\" : 1.0803,\n"
            + "			\"highAsk\" : 1.08076,\n"
            + "			\"lowBid\" : 1.0803,\n"
            + "			\"lowAsk\" : 1.08076,\n"
            + "			\"closeBid\" : 1.0803,\n"
            + "			\"closeAsk\" : 1.08076,\n"
            + "			\"volume\" : 1,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:00.000000Z\",\n"
            + "			\"openBid\" : 1.08003,\n"
            + "			\"openAsk\" : 1.08078,\n"
            + "			\"highBid\" : 1.08003,\n"
            + "			\"highAsk\" : 1.08078,\n"
            + "			\"lowBid\" : 1.07997,\n"
            + "			\"lowAsk\" : 1.08074,\n"
            + "			\"closeBid\" : 1.07997,\n"
            + "			\"closeAsk\" : 1.08074,\n"
            + "			\"volume\" : 3,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:10.000000Z\",\n"
            + "			\"openBid\" : 1.07983,\n"
            + "			\"openAsk\" : 1.08063,\n"
            + "			\"highBid\" : 1.08016,\n"
            + "			\"highAsk\" : 1.08071,\n"
            + "			\"lowBid\" : 1.07983,\n"
            + "			\"lowAsk\" : 1.08058,\n"
            + "			\"closeBid\" : 1.08016,\n"
            + "			\"closeAsk\" : 1.08058,\n"
            + "			\"volume\" : 5,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:20.000000Z\",\n"
            + "			\"openBid\" : 1.08017,\n"
            + "			\"openAsk\" : 1.08052,\n"
            + "			\"highBid\" : 1.08017,\n"
            + "			\"highAsk\" : 1.08052,\n"
            + "			\"lowBid\" : 1.08017,\n"
            + "			\"lowAsk\" : 1.08052,\n"
            + "			\"closeBid\" : 1.08017,\n"
            + "			\"closeAsk\" : 1.08052,\n"
            + "			\"volume\" : 1,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:30.000000Z\",\n"
            + "			\"openBid\" : 1.08018,\n"
            + "			\"openAsk\" : 1.08068,\n"
            + "			\"highBid\" : 1.0802,\n"
            + "			\"highAsk\" : 1.08071,\n"
            + "			\"lowBid\" : 1.08007,\n"
            + "			\"lowAsk\" : 1.0805,\n"
            + "			\"closeBid\" : 1.08011,\n"
            + "			\"closeAsk\" : 1.0805,\n"
            + "			\"volume\" : 9,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:35.000000Z\",\n"
            + "			\"openBid\" : 1.08019,\n"
            + "			\"openAsk\" : 1.08055,\n"
            + "			\"highBid\" : 1.08034,\n"
            + "			\"highAsk\" : 1.0811,\n"
            + "			\"lowBid\" : 1.07993,\n"
            + "			\"lowAsk\" : 1.08055,\n"
            + "			\"closeBid\" : 1.08011,\n"
            + "			\"closeAsk\" : 1.0809,\n"
            + "			\"volume\" : 27,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:40.000000Z\",\n"
            + "			\"openBid\" : 1.08016,\n"
            + "			\"openAsk\" : 1.08111,\n"
            + "			\"highBid\" : 1.08025,\n"
            + "			\"highAsk\" : 1.08111,\n"
            + "			\"lowBid\" : 1.08016,\n"
            + "			\"lowAsk\" : 1.08106,\n"
            + "			\"closeBid\" : 1.08025,\n"
            + "			\"closeAsk\" : 1.08106,\n"
            + "			\"volume\" : 11,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:45.000000Z\",\n"
            + "			\"openBid\" : 1.08026,\n"
            + "			\"openAsk\" : 1.08106,\n"
            + "			\"highBid\" : 1.08027,\n"
            + "			\"highAsk\" : 1.08121,\n"
            + "			\"lowBid\" : 1.08008,\n"
            + "			\"lowAsk\" : 1.08104,\n"
            + "			\"closeBid\" : 1.08008,\n"
            + "			\"closeAsk\" : 1.08111,\n"
            + "			\"volume\" : 7,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:58:55.000000Z\",\n"
            + "			\"openBid\" : 1.08033,\n"
            + "			\"openAsk\" : 1.08111,\n"
            + "			\"highBid\" : 1.08033,\n"
            + "			\"highAsk\" : 1.08129,\n"
            + "			\"lowBid\" : 1.08021,\n"
            + "			\"lowAsk\" : 1.08111,\n"
            + "			\"closeBid\" : 1.08021,\n"
            + "			\"closeAsk\" : 1.08129,\n"
            + "			\"volume\" : 4,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:59:00.000000Z\",\n"
            + "			\"openBid\" : 1.0802,\n"
            + "			\"openAsk\" : 1.0813,\n"
            + "			\"highBid\" : 1.0802,\n"
            + "			\"highAsk\" : 1.0813,\n"
            + "			\"lowBid\" : 1.08009,\n"
            + "			\"lowAsk\" : 1.08119,\n"
            + "			\"closeBid\" : 1.08009,\n"
            + "			\"closeAsk\" : 1.08119,\n"
            + "			\"volume\" : 2,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:59:05.000000Z\",\n"
            + "			\"openBid\" : 1.08014,\n"
            + "			\"openAsk\" : 1.08123,\n"
            + "			\"highBid\" : 1.08014,\n"
            + "			\"highAsk\" : 1.08123,\n"
            + "			\"lowBid\" : 1.08014,\n"
            + "			\"lowAsk\" : 1.08123,\n"
            + "			\"closeBid\" : 1.08014,\n"
            + "			\"closeAsk\" : 1.08123,\n"
            + "			\"volume\" : 1,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:59:20.000000Z\",\n"
            + "			\"openBid\" : 1.08,\n"
            + "			\"openAsk\" : 1.08106,\n"
            + "			\"highBid\" : 1.08006,\n"
            + "			\"highAsk\" : 1.08106,\n"
            + "			\"lowBid\" : 1.08,\n"
            + "			\"lowAsk\" : 1.08104,\n"
            + "			\"closeBid\" : 1.08006,\n"
            + "			\"closeAsk\" : 1.08104,\n"
            + "			\"volume\" : 7,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:59:25.000000Z\",\n"
            + "			\"openBid\" : 1.08007,\n"
            + "			\"openAsk\" : 1.08103,\n"
            + "			\"highBid\" : 1.08039,\n"
            + "			\"highAsk\" : 1.08131,\n"
            + "			\"lowBid\" : 1.08001,\n"
            + "			\"lowAsk\" : 1.08086,\n"
            + "			\"closeBid\" : 1.0803,\n"
            + "			\"closeAsk\" : 1.0813,\n"
            + "			\"volume\" : 11,\n"
            + "			\"complete\" : true\n"
            + "		},\n"
            + "		{\n"
            + "			\"time\" : \"2015-04-17T20:59:35.000000Z\",\n"
            + "			\"openBid\" : 1.08029,\n"
            + "			\"openAsk\" : 1.0813,\n"
            + "			\"highBid\" : 1.08029,\n"
            + "			\"highAsk\" : 1.08133,\n"
            + "			\"lowBid\" : 1.08014,\n"
            + "			\"lowAsk\" : 1.08122,\n"
            + "			\"closeBid\" : 1.08014,\n"
            + "			\"closeAsk\" : 1.08122,\n"
            + "			\"volume\" : 4,\n"
            + "			\"complete\" : true\n"
            + "		}\n"
            + "	]\n"
            + "}\r\n\r\n";

}
