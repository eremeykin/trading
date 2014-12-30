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
import java.util.*;
import org.apache.log4j.LogManager;

/**
 *
 * @author eremeykin
 */
public class DataLoader implements Iterable<String> {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(DataLoader.class);
    private final static String fileName = "/res/data.txt";
    private final File dataFile;
    BufferedReader reader;

    public DataLoader() throws FileNotFoundException {
        try {
            this.dataFile = new File(getClass().getResource(fileName).getFile());
            reader = new BufferedReader(new FileReader(dataFile));
        } catch (FileNotFoundException | NullPointerException ex) {
            LOG.error("Файл " + fileName + " не найден.\n" + ex);
            throw ex;
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            private String line = "";

            @Override
            public boolean hasNext() {

                return line != null;
            }

            @Override
            public String next() {
                try {
                    line = reader.readLine();
                    return line;
                } catch (IOException ex) {
                    LOG.error("Ошибка вводв-вывода\n" + ex);
                    line = null;
                }
                return line;
            }
        };
    }

}
