package com.example.vpnwithoutndk;

import org.apache.commons.net.whois.WhoisClient;

import java.io.IOException;
import java.net.SocketException;

public class WhoisTest {

    public static void main(String[] args) {
        WhoisTest obj = new WhoisTest();
        System.out.println(obj.getWhois("facebook.com"));
        System.out.println("Done");

    }

    public String getWhois(String domainName) {

        StringBuilder result = new StringBuilder("");

        WhoisClient whois = new WhoisClient();
        try {

            //default is internic.net
            whois.connect(WhoisClient.DEFAULT_HOST);
            String whoisData1 = whois.query("=" + domainName);
            result.append(whoisData1);
            whois.disconnect();

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();

    }
}