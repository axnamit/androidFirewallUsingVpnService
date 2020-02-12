package com.example.vpnwithoutndk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class TestAPi {


    public static void main(String[] args) {
        // The URL for which IP address needs to be fetched
        String s = "https://www.egbazar.in/";
        try {
            // Fetch IP address by getByName()
            InetAddress ip = InetAddress.getByName(new URL(s)
                    .getHost());

            // Print the IP address
            System.out.println(ip.getCanonicalHostName()+" Public IP Address of: " + ip.getHostAddress()+" "+ ip.isReachable(13));
        } catch (MalformedURLException e) {
            // It means the URL is invalid
            System.out.println("Invalid URL");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
