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
import java.io.InputStreamReader;
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
            Client client = null;
            switch (request.getType()) {
                case NEW_CLIENT:
                    client = HttpServer.getClientPool().generateClient(s, request);
                    client.startTickFeeder();
                    break;
                case NEED_NEXT_TICK:
                    try {
                        client = HttpServer.getClientPool().getClientById(request.parseId());
                        client.setNeedNext(s);
                    } catch (NullPointerException ex) {
                        LOG.error("Запрошены данные для несуществующего клиента. Клиент №" + request.parseId() + " " + ex);
                    }
                    break;
                case MAKE_ORDER:
                    try {
                        client = HttpServer.getClientPool().getClientById(request.parseId());
                        client.addOrder(request);
                    } catch (NullPointerException ex) {
                        LOG.error("Запрошены данные для несуществующего клиента. Клиент №" + request.parseId() + " " + ex);
                    }
                    break;
                case NEED_CANDLES:
                    try {
                        client = HttpServer.getClientPool().getClientById(request.parseId());
                        client.sendCandles(s);
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
        LOG.debug("Принято сообщение типа " + result.getType().name() + ":\n" + result);
        return result;
    }
}
