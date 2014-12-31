/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;


import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Pete
 */
public class ClientPool {

    private Map<Integer, Client> clients = new HashMap<>();
    private int id=0;
    
    public synchronized Client generateClient(Socket s) throws IOException {
        int id =this.id++;
        Client client = new Client(id,s);
        clients.put(client.getId(), client);
        return client;
    }

    public  synchronized Client getClientById(int id){
        return clients.get(id);
    }
    
    public synchronized void remove(Client client) {
        clients.remove(client.getId());
    }
}
