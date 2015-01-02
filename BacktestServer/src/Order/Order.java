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
    private BusinessRequest request;
    private final DataItem openingItem;
    private DataItem closingItem = null;

    public Order(DataItem openingItem, BusinessRequest request) {
        this.isOpened = true;
        this.openingItem = openingItem;
        this.request = request;
    }

    public void close(DataItem closingItem) {
        this.isOpened = false;
        this.closingItem = closingItem;
    }

    
    public boolean isOpened() {
        return this.isOpened;
    }
    
    public DataItem getOpeningDataItem(){
        return openingItem;
    }
     public BusinessRequest getBusinessRequest(){
         return  request;
     }
}
