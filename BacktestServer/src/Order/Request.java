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
enum Side {

    BUY,
    SELL;

    public Side getOpposite(Side s) {
        return s == BUY ? SELL : BUY;
    }
}

public class Request {

    Instrument instr;
    float units;
    Side side;
    float takeProfit;
    float stopLoss;

    public void parseFromRequestString(String s) {
        
    }

}
