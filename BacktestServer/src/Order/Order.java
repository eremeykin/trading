/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Order;

/**
 *
 * @author eremeykin
 */
public class Order {

    private boolean isOpened;
    private Request request;
    private final DataItem openingItem;
    private DataItem closingItem;

    public Order(DataItem openingItem) {
        isOpened = true;
        this.openingItem = openingItem;
    }

    public void close(DataItem closingItem) {
        this.isOpened = false;
        this.closingItem = closingItem;
    }
}
