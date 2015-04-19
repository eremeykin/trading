/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;
//import org.apache.logging.log4j.Logger;

import static Client.ClientProcessor.LOG;
import DataLoader.CandlesLoader;
import Order.DataItem;
import DataLoader.DataLoader;
import java.io.*;
import java.net.Socket;
import java.util.*;
import Order.*;
import Server.HttpServer;
import java.sql.SQLException;
import org.apache.log4j.LogManager;

/**
 *
 * @author Pete
 */
public final class Client {

    public Connection conn;
    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private static final String CRLF = "\r\n";
    private final int id;
    private final Iterator<String> iterator;
    private boolean needNext;
    private List<Order> currentOrders = new ArrayList<>();
    private Report<List<Order>> report;
    private DataItem nextDataItem;
    private final Socket socket;
    private final HTTPRequest request;
    private final TickFeeder tickFeeder;

    public Client(int id, Socket s, HTTPRequest request) throws IOException, ClassNotFoundException, SQLException {
        socket = s;
        this.conn = new Connection(s);
        this.id = id;
        this.report = new Report<>(id);
        this.iterator = new DataLoader().iterator();
        this.request = request;
        this.tickFeeder = new TickFeeder(s);
        nextDataItem = new DataItem(iterator.next());
    }

    public Integer getId() {
        return this.id;
    }

    public String toString() {
        return "Client #" + Long.toString(id);
    }

    public synchronized void setNeedNext(Socket s) throws IOException {
//        LOG.info("Установлен needNext для" + this);
        this.needNext = true;
        String CRLF = "\r\n";
        // Ответ на need_next запрос а не для самого потока тиков.
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

    public synchronized void clearNeedNext() {
//        LOG.info("Снят needNext для " + this);
        this.needNext = false;
    }

    public synchronized boolean needNext() {
        return this.needNext;
    }

    public synchronized boolean hasMoreData() {
        return iterator.hasNext();
    }

    private synchronized void scanOrders() {
        //for (Order order : currentOrders) {
        Iterator<Order> it = currentOrders.iterator();
        while (it.hasNext()) {
            Order order = it.next();
            if (order.isOpened()) {
                if (order.getBusinessRequest().getSide() == Side.BUY) {
                    if (order.getBusinessRequest().getStopLoss() >= nextDataItem.bid) {
                        order.close(nextDataItem);
                        report.add(order);
                        it.remove();
                    }
                    if (order.getBusinessRequest().getTakeProfit() <= nextDataItem.bid) {
                        order.close(nextDataItem);
                        report.add(order);
                        it.remove();
                    }
                }
                if (order.getBusinessRequest().getSide() == Side.SELL) {
                    if (order.getBusinessRequest().getStopLoss() <= nextDataItem.ask) {
                        order.close(nextDataItem);
                        report.add(order);
                        it.remove();
                    }
                    if (order.getBusinessRequest().getTakeProfit() >= nextDataItem.ask) {
                        order.close(nextDataItem);
                        report.add(order);
                        it.remove();
                    }
                }

            }
        }
    }

    public synchronized void sendNext() throws IOException {
        this.scanOrders();
        if (!this.needNext) {
            LOG.fatal("Вызван метод sendNext() для клиента, которой не готов принять данные.");
            throw new Error("Вызван метод sendNext() для клиента, которой не готов принять данные.");
        }
        OutputStream os = this.conn.getOutputStream();
        this.clearNeedNext();
        if (hasMoreData()) {
            String line = nextDataItem.getPrototype();
            nextDataItem = new DataItem(iterator.next());
            String count = Integer.toHexString(line.length() + 1) + "\r\n";
            line += "\n" + CRLF;
            os.write((count + line).getBytes());
            os.flush();
            //LOG.info(nextDataItem);
        } else {
            os.write("0\r\n\r\n".getBytes());
            os.flush();
        }
    }

    public synchronized void addOrder(HTTPRequest orderRequest) {
        Order newOrder = new Order(nextDataItem, orderRequest.parseToBusinessRequest());
        report.add(newOrder);
        this.currentOrders.add(newOrder);
    }

    void startTickFeeder() {
        if (!tickFeeder.isAlive()) {
            tickFeeder.start();
        } else {
            LOG.error("Попытка запустить TickFeeder второй раз.");
        }
    }

    void sendCandles(Socket s) throws IOException {
//        String candles = "";
        String candles = new CandlesLoader().loadCandles(nextDataItem.getDateTime(),25);
        String head = "HTTP/1.1 200 OK" + CRLF
                + "Server: TestServer/2014" + CRLF
                + "Content-Type: application/json" + CRLF
                + "Connection: close" + CRLF
                + "Content-Length: " + candles.length() + CRLF
                + "Client-Identificator: " + this.getId() + CRLF
                + "Access-Control-Allow-Origin: *\n" + CRLF
                + CRLF;
        // Это переделать. Влияет на производительность 
        OutputStream os = s.getOutputStream();
        os.write((head + candles).getBytes());
        os.flush();
        LOG.error("OK");
    }

    private class TickFeeder extends Thread {

        private boolean stop = false;

        public TickFeeder(Socket s) throws IOException, SQLException, ClassNotFoundException {
        }

        public synchronized void setStop() {
            stop = true;
        }

        @Override
        public void run() {
            try {
                LOG.info("Запущен TickFeeder для клиента " + Client.this);
                String header = "HTTP/1.1 200 OK" + CRLF
                        + "Server: TestServer/2014" + CRLF
                        + "Content-Type: application/json" + CRLF
                        + "Transfer-Encoding: chunked" + CRLF
                        + "Connection: close" + CRLF
                        + "Client-Identificator: " + Client.this.getId() + CRLF
                        + "Access-Control-Allow-Origin: *\n" + CRLF
                        + "2" + CRLF
                        + CRLF + CRLF;
                OutputStream os = Client.this.conn.getOutputStream();
                os.write(header.getBytes());
                os.flush();
            } catch (IOException ex) {
                LOG.error(ex);
                LOG.error("Ошибка при отправке заголвка для клиента " + Client.this);
            }
            try {
                new ConnectionStatusScanner(conn.getInputStream(), this).start();
                outer:
                do {
                    if (Client.this.needNext()) {
                        Client.this.sendNext();
                        LOG.info("Отправлен тик для клиента " + Client.this);
                    }
                } while (Client.this.hasMoreData());
                //Client.this.sendNext();
                LOG.info("Завершен TickFeeder для клиента. " + Client.this);
            } catch (IOException ex) {
                LOG.error("Ошибка при записи запроса. " + ex);
                LOG.info("Завершен TickFeeder для клиента. " + Client.this);
            }
        }

        private class ConnectionStatusScanner extends Thread {

            private final InputStream is;
            private final TickFeeder feeder;

            ConnectionStatusScanner(InputStream is, TickFeeder feeder) {
                this.setName("Status scanner for" + Client.this);
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
                            + Client.this + " потерял связь.");
                }
                feeder.setStop();
            }
        }
    }

}
