/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Order;

import Client.Client;
import Client.ClientProcessor;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.LogManager;

/**
 *
 * @author eremeykin
 */
public class Report<T extends List<Order>> {

    public static final org.apache.log4j.Logger LOG = LogManager.getLogger(Report.class);
    private static final File file = new File(Report.class.getResource("/res/report.sqlite").getFile());
    private List<T> content;
    private int clientId;
    private Connection connection;
    private int num = 0;

    public Report(int clientId) throws ClassNotFoundException, SQLException {
        content = new ArrayList<>();
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());

        Statement st = connection.createStatement();
        st.executeUpdate("DROP TABLE IF EXISTS \"main\".\"client" + clientId + "\";");
        st.executeUpdate("CREATE  TABLE \"main\".\"client" + clientId + "\" (\"num\" INTEGER, \"is_open\" BOOL, \"instr\" VARCHAR, \"opening_ask\" FLOAT, \"opening_bid\" FLOAT, \"opening_time\" VARCHAR, \"closing_ask\" FLOAT, \"closing_bid\" FLOAT, \"closing_time\" VARCHAR);");
    }

    public void add(T orders) {
        content.add(orders);
        num++;
        for (Order order : orders) {
            try {
                Statement st = connection.createStatement();

                PreparedStatement ps = connection.prepareStatement("INSERT INTO \"main\".\"client"+clientId+"\" (\"num\",\"is_open\",\"instr\",\"opening_ask\",\"opening_bid\",\"opening_time\",\"closing_ask\",\"closing_bid\",\"closing_time\") VALUES (?,?,?,?,?,?,?,?,?)");
                ps.setString(1, new Integer(num).toString());
                ps.setString(2, new Boolean(order.isOpened()).toString());
                ps.setString(3, order.getOpeningDataItem().instr.name());
                ps.setString(4, new Double(order.getOpeningDataItem().ask).toString());
                ps.setString(5, new Double(order.getOpeningDataItem().bid).toString());
                ps.setString(6, order.getOpeningDataItem().dateTime.toString());
                ps.setString(7, new Double(order.getClosingDataItem().ask).toString());
                ps.setString(8, new Double(order.getClosingDataItem().bid).toString());
                ps.setString(9, order.getClosingDataItem().dateTime.toString());
                ps.executeUpdate();
//
//                st.executeQuery("INSERT INTO \"main\".\"client0\" "
//                        + "(\"num\",\"is_open\",\"instr\",\"opening_ask\",\"opening_bid\",\"opening_time\",\"closing_ask\",\"closing_bid\",\"closing_time\") "
//                        + "VALUES ("
//                        + "\"" + num + ",\"" + order.isOpened() + "," + order.getOpeningDataItem().instr + "," + order.getOpeningDataItem().ask + "," + order.getOpeningDataItem().bid + "," + order.getOpeningDataItem().dateTime + "," + order.getClosingDataItem().ask + "," + order.getClosingDataItem().bid + "," + order.getClosingDataItem().dateTime//"1,2,3,4,5,6,7,8,9"
//                        + ")");
            } catch (SQLException ex) {
                LOG.error("Ошибка при добавлении заявок в отчет (SQL error)" + ex);
            }
        }
    }

}
