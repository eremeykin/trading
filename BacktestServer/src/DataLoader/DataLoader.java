/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DataLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eremeykin
 */
public class DataLoader implements Iterable<String> {

    File data = new File("/home/eremeykin/PythonCode/data.txt");
    BufferedReader reader;

    public DataLoader() throws Throwable {
        reader = new BufferedReader(new FileReader(data));
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            private boolean isEmpty = false;

            @Override
            public boolean hasNext() {
                return !isEmpty;
            }

            @Override
            public String next() {
                try {
                    String line = reader.readLine();
                    isEmpty = line == null;
                    if (isEmpty)
                        throw new NoSuchElementException("End of file");
                    return line;
                } catch (IOException ex) {
                    return null;
                }
            }
        };
    }

}
