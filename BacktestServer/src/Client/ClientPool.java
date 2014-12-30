/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;


import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Pete
 */
public class ClientPool {

    Map<Long, Client> clients = new HashMap<>();

    public Client generateClient() {
        long id = clients.size() + 1;
        Client client = new Client(id);
        clients.put(client.getId(), client);
        return client;
    }

    public void remove(Client client) {
        clients.remove(client.getId());
    }
}
