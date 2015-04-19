/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DataLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.LogManager;
import org.json.JSONObject;

/**
 *
 * @author eremeykin
 */
public class CandlesLoader implements Iterable<String> {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(DataLoader.class);
    private final static String fileName = "/res/Week1_1min.json";
    private final File dataFile;
    FileReader reader;

    public CandlesLoader() {
        File df = null;
        try {
            df = new File(getClass().getResource(fileName).getFile());
        } catch (NullPointerException ex) {
            LOG.error("Файл " + fileName + " не найден.\n" + ex);
            System.exit(-1);
        } finally {
            dataFile = df;
            LOG.info("Файл " + dataFile + " загружен.");
            try {
                reader = new FileReader(dataFile);
            } catch (FileNotFoundException ex) {
                LOG.error("Файл " + fileName + " не найден.\n" + ex);
            }
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            private Scanner scanner = new Scanner(reader);

            @Override
            public boolean hasNext() {
                boolean result = scanner.hasNextLine();
                if (!result) {
                    LOG.error("Нет больше данных");
                }
                return result;
            }

            @Override
            public String next() {
                return scanner.nextLine();
            }
        };
    }

    public String loadCandles(LocalDateTime dateTime, int count) {
        String result = "";
        Queue queue = new Queue(count);
        for (String line : this) {
            JSONObject obj = new JSONObject(line);
            String timeString = obj.get("time").toString();
            LocalDateTime currDateTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME);
            queue.add(line);
            if (currDateTime.compareTo(dateTime) > 0) {
                break;
            }

        }
        result += "{\n"
                + "	\"instrument\" : \"EUR_USD\",\n"
                + "	\"granularity\" : \"S5\",\n"
                + "	\"candles\" : [";
        return result + queue.get() + "]\n" + "}\r\n";
    }

    private class Queue {

        private final String[] values;

        public Queue(int size) {
            this.values = new String[size];
        }

        public void add(String newValue) {
            for (int i = values.length - 1; i > 0; i--) {
                values[i] = values[i-1];
            }
            values[0] = newValue;
        }

        public String get() {
            String res = "";
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    res += values[i] + ",";
                }
            }
            // удаляем последнюю запятую
            return res.substring(0, res.length() - 1);
        }

    }
}
