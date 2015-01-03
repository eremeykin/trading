/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;
//import org.apache.logging.log4j.Logger;

import Order.DataItem;
import DataLoader.DataLoader;
import java.io.*;
import java.net.Socket;
import java.util.*;
import Order.*;
import java.sql.SQLException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *
 * @author Pete
 */
public class Client {

    public Connection conn;
    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private static final String CRLF = "\r\n";
    private final String header;
    private final int id;
    private final Iterator<String> iterator;
    private boolean needNext;
    private List<Order> currentOrders = new ArrayList<>();
    private Report<List<Order>> report;
    private DataItem nextDataItem;

    public Client(int id, Socket s) throws IOException, ClassNotFoundException, SQLException {
        this.conn = new Connection(s);
        this.id = id;
        this.report = new Report<>(id);
        this.iterator = new DataLoader().iterator();
        header = "HTTP/1.1 200 OK" + CRLF
                + "Server: TestServer/2014" + CRLF
                + "Content-Type: application/json" + CRLF
                + "Transfer-Encoding: chunked" + CRLF
                + "Connection: close" + CRLF
                + "Clien-Identificator: " + this.getId() + CRLF
                + "Access-Control-Allow-Origin: *\n" + CRLF
                + "2" + CRLF
                + CRLF + CRLF;
        OutputStream os = this.conn.getOutputStream();
        os.write(header.getBytes());
        os.flush();
        nextDataItem = new DataItem(iterator.next());
    }

    public Integer getId() {
        return this.id;
    }

    public String toString() {
        return "Client #" + Long.toString(id);
    }

    public synchronized void setNeedNext() {
//        LOG.info("Установлен needNext для" + this);
        this.needNext = true;
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

    private void scanOrders() {
        for (Order order : currentOrders) {
            if (order.isOpened()) {
                if (order.getBusinessRequest().getSide() == Side.BUY) {
                    if (order.getBusinessRequest().getStopLoss() >= nextDataItem.bid) {
                        order.close(nextDataItem);
                    }
                    if (order.getBusinessRequest().getTakeProfit() <= nextDataItem.bid) {
                        order.close(nextDataItem);
                    }
                }
                if (order.getBusinessRequest().getSide() == Side.SELL) {
                    if (order.getBusinessRequest().getStopLoss() <= nextDataItem.ask) {
                        order.close(nextDataItem);
                    }
                    if (order.getBusinessRequest().getTakeProfit() >= nextDataItem.ask) {
                        order.close(nextDataItem);
                    }
                }

            }
        }
    }

    public synchronized void sendNext() throws IOException {
        this.scanOrders();
        report.add(currentOrders);
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
            line += "\n"+CRLF;
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
        this.currentOrders.add(newOrder);
    }

}
