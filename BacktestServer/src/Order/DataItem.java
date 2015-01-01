/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Order;

import org.json.*;
import java.time.*;
import java.time.format.*;

/**
 *
 * @author eremeykin
 */
enum Instrument {

    EUR_USD
}

public class DataItem {

    private final String prototype;
    public final Instrument instr = Instrument.EUR_USD;
    public double ask;
    public double bid;
    public LocalDateTime dateTime;

    public DataItem(String jsonString) {
        prototype = jsonString;
        JSONObject obj = new JSONObject(jsonString);
        ask = (double) obj.getJSONObject("tick").get("ask");
        bid = (double) obj.getJSONObject("tick").get("bid");
        String timeString = obj.getJSONObject("tick").getString("time");
        timeString = timeString.substring(0, timeString.length() - 1);
        dateTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME);
    }

    @Override
    public String toString() {
        return "instrument:" + instr.name() + "\n"
                + "time:" + dateTime + "\n"
                + "ask:" + ask + "\n"
                + "bid:" + bid + "\n";
    }

    public String getPrototype() {
        return prototype;
    }

}
