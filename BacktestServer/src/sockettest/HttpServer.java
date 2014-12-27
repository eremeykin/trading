/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sockettest;

import DataLoader.DataLoader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 *
 * @author eremeykin
 */
public class HttpServer {

    public static void main(String[] args) throws Throwable {
        ServerSocket ss = new ServerSocket(8080);
        System.err.println("Waiting for client");
        while (true) {
            Socket s = ss.accept();
            s.setKeepAlive(true);
            System.err.println("Client accepted");
            new Thread(new SocketProcessor(s)).start();
        }
    }

    private static class SocketProcessor implements Runnable {

        private Socket s;
        private InputStream is;
        private OutputStream os;
        private DataLoader dl;
        private static final String CRLF = "\r\n";
        BufferedReader br;

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
            this.dl = new DataLoader();
            br = new BufferedReader(new InputStreamReader(is));
        }

        public void run() {
            try {
                //readInputHeaders();
                writeResponse();
            } catch (Throwable t) {
                /*do nothing*/
            } finally {
                try {
                    s.close();
                } catch (Throwable t) {
                    /*do nothing*/
                }
            }
            System.err.println("Client processing finished");
        }

        private void writeResponse() throws Throwable {
            String response = "HTTP/1.1 200 OK" + CRLF
                    + "Server: TestServer/2014" + CRLF
                    + "Content-Type: application/json" + CRLF
                    + "Transfer-Encoding: chunked" + CRLF
                    + "Connection: close" + CRLF
                    + "Access-Control-Allow-Origin: *\n" + CRLF;
            String result = response;
            os.write(result.getBytes());
            os.flush();

            for (String line : dl) {
                String count = Integer.toHexString(line.length() + 1) + "\r\n\n";
                line += "\r\n";
                os.write((count + line).getBytes());
                os.flush();
                String str = "";
                try{
                do {
                    try {
                        str = br.readLine();
                        str = (str==null) ? "":str;
                        System.err.println(str);
                    } catch (Exception e) {

                    }
                } while (!str.contains("get me next tick"));
                }catch (Throwable e){
                    System.err.println(e);
                }
            }
            os.write("0\r\n\r\n".getBytes());
            os.flush();
        }

        private void readInputHeaders() throws Throwable {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String s = br.readLine();
                System.out.println(s);
                if (s == null || s.trim().length() == 0) {
                    break;
                }
            }
        }
    }
}
