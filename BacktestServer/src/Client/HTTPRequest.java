/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

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
        MAKE_ORDER,
        NEW_CLIENT
    }

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(ClientProcessor.class);
    private String header;
    private String body;

    public HTTPRequest(String header) {
        this.header = header;
//        this.body = body;
    }

    public void addBody(String body) {
        this.body = body;
    }

    /**
     * @return the header
     */
    public String getHeader() {
        return header;
    }

    /**
     * @return the body
     */
    public String getBody() {
        return body;
    }

    public int parseLength() {
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

    public int parseId() {
        StringTokenizer tokenizer = new StringTokenizer(this.header, "\r\n");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("Client-Identificator:")) {
                return new Integer(token.substring("Client-Identificator: ".length()));
            }
        }
        throw new Error();
    }

    @Override
    public String toString() {
        return "header:\n" + header + "\n" + "body:\n" + body;
    }

    public Type getType() {
        if (this.header.contains("need_next_tick")) {
            LOG.debug(Type.NEED_NEXT_TICK.name());
            return Type.NEED_NEXT_TICK;
        }
        if (this.header.contains("Content-Length:")) {
            LOG.debug(Type.MAKE_ORDER.name());
            return Type.MAKE_ORDER;
        }
        LOG.debug(Type.NEW_CLIENT.name());
        return Type.NEW_CLIENT;
    }

}
