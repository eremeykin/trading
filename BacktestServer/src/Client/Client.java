/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;
//import org.apache.logging.log4j.Logger;

import java.net.Socket;
import org.apache.log4j.Logger;

/**
 *
 * @author Pete
 */
public class Client {

    private final long id;

    public Client(long id) {
        this.id = id;
    }
    public long getId(){
        return this.id;
    }
    public  String toString(){
        return "Client #"+Long.toString(id);
    }
}
