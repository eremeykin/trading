/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import Order.BusinessRequest;
import Order.Side;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.apache.log4j.LogManager;

/**
 *
 * @author eremeykin
 */
public class HTTPRequest {

    public enum Type {

        NEED_NEXT_TICK,
        NEED_CANDLES,
        WITH_BODY,
        NEW_CLIENT,
        MAKE_ORDER
    }

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private String header;
    private String body = "";

    public HTTPRequest(String header) {
        this.header = header;
//        this.body = body;
    }

    public synchronized void addBody(String body) {
        this.body = body.trim();
    }

    public synchronized int bodyLength() {
        return body.trim().length();
    }

    /**
     * @return the header
     */
    public synchronized String getHeader() {
        return header;
    }

    /**
     * @return the body
     */
    public synchronized String getBody() {
        return body;
    }

    public synchronized int parseLength() {
        Scanner scanner = new Scanner(this.header);
        String line;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (line.startsWith("Content-Length: ")) {
                return Integer.parseInt(line.substring("Content-Length: ".length()));
            }
        }
        LOG.info("Принято сообщение без информации о длине.");
        return 0;
    }

    public synchronized int parseId() {
        StringTokenizer tokenizer = new StringTokenizer(this.header, "\r\n");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("Client-Identificator:")) {
                return new Integer(token.substring("Client-Identificator: ".length()));
            }
        }
        throw new Error("Нет идентификатора клиента");
    }

    @Override
    public synchronized String toString() {
        return "header:\n" + header + "\n" + "body:\n" + body;
    }

    public synchronized Type getType() {
        if (this.header.contains("candles")){
            return Type.NEED_CANDLES;
        }
        if (this.header.contains("need_next_tick")) {
            return Type.NEED_NEXT_TICK;
        }
        if (this.body != null && this.body.contains("instrument")) {
            return Type.MAKE_ORDER;
        }
        if (this.header.contains("Content-Length:") && this.parseLength() != 0) {
            return Type.WITH_BODY;
        }
        return Type.NEW_CLIENT;
    }

    public synchronized Order.BusinessRequest parseToBusinessRequest() {
        Order.BusinessRequest result;
        Scanner scanner = new Scanner(this.body);
        scanner.useDelimiter("&");
        float units = 0;
        float stopLoss = 0;
        float takeProfit = 0;
        Order.Side side = Side.SELL;
        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.startsWith("stopLoss")) {
                stopLoss = Float.parseFloat(token.split("=")[1]);
            }
            if (token.startsWith("takeProfit")) {
                takeProfit = Float.parseFloat(token.split("=")[1]);
            }
            if (token.startsWith("units")) {
                units = Float.parseFloat(token.split("=")[1]);
            }
            if (token.startsWith("side")) {
                side = Order.Side.valueOf(token.split("=")[1].toUpperCase());
            }
        }
        result = new BusinessRequest(units, side, takeProfit, stopLoss);
        return result;
    }

}
