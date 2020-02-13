package com.example.vpnwithoutndk;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class TestAPi {


    public static void main(String[] args) {
        // The URL for which IP address needs to be fetched
        String s = "https://www.amazon.com/";/*143.204.254.3 *//*13.35.131.142*/
        try {
            // Fetch IP address by getByName()
            InetAddress ip = InetAddress.getByName(new URL(s)
                    .getHost());

            // Print the IP address
            System.out.println(ip.getCanonicalHostName()+" Public IP Address of: " + ip.getHostAddress()+" "+ ip);

            InetAddress addr = InetAddress.getByName("13.35.131.142");
            String host = addr.getHostName();
            System.out.println("hostname"+host);
        } catch (MalformedURLException e) {
            // It means the URL is invalid
            System.out.println("Invalid URL");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
