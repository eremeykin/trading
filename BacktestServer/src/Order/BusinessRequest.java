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


public class BusinessRequest {

    Instrument instr;
    private float units;
    private Side side;
    private float takeProfit;
    private float stopLoss;

    public BusinessRequest(float units, Side side, float takeProfit, float stopLoss) {
        this.units = units;
        this.side = side;
        this.takeProfit = takeProfit;
        this.stopLoss = stopLoss;
    }

    /**
     * @return the units
     */
    public float getUnits() {
        return units;
    }

    /**
     * @return the side
     */
    public Side getSide() {
        return side;
    }

    /**
     * @return the takeProfit
     */
    public float getTakeProfit() {
        return takeProfit;
    }

    /**
     * @return the stopLoss
     */
    public float getStopLoss() {
        return stopLoss;
    }

}
