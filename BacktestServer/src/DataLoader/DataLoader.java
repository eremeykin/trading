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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.LogManager;

/**
 *
 * @author eremeykin
 */
public class DataLoader implements Iterable<String> {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(DataLoader.class);
    private final static String fileName = "/res/smalldata.txt";
    private final File dataFile;
    BufferedReader reader;

    public DataLoader() {
        File df = null;
        try {
            df = new File(getClass().getResource(fileName).getFile());
        } catch (NullPointerException ex) {
            LOG.error("Файл " + fileName + " не найден.\n" + ex);
            System.exit(-1);
        } finally {
            dataFile = df;
            LOG.info("Файл "+dataFile+" загружен.");
            try {
                reader = new BufferedReader(new FileReader(dataFile));
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
                return scanner.hasNextLine();
            }

            @Override
            public String next() {
                return scanner.nextLine();
            }
        };
    }

}
