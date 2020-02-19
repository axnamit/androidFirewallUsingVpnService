package com.example.vpnwithoutndk;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class TestAPi {


    private static long count;

    public static void main(String[] args) {
        // The URL for which IP address needs to be fetched
        String s = "https://web.whatsapp.com/";/*143.204.254.3 *//*13.35.131.142*/
        try {
            // Fetch IP address by getByName()
            InetAddress ip = InetAddress.getByName(new URL(s)
                    .getHost());

            // Print the IP address
            System.out.println(ip.getCanonicalHostName() + " Public IP Address of: " + ip.getHostAddress() + " " + ip);

           /* InetAddress addr = InetAddress.getByName(ip.getHostAddress());
            String host = addr.getHostName();
            System.out.println("hostname" + host);

            String html = "https://amazon.in/";
            Document doc = Jsoup.connect(html).get();
            String title = doc.title();
            Elements bodies = doc.select("body");
            ArrayList<Element> elements = new ArrayList<>();

            for (Element e : bodies) {
                elements.add(e);
            }

            Element[] elementArr = elements.toArray(new Element[]{});
            String[] prices = new String[elementArr.length];
            for (int i = 0; i < elementArr.length; i++) {
                prices[i] = elements.get(i).text();
            }

            for (String sa:prices) {
                count++;
                System.out.println(
                       count+ "\n"+" prices.length+" +"ntext =="+sa
                );
            }

           *//* for (Element str:elementArr) {
                prices[i] = elements.get(i).text();
                str.getAllElements();

            }*//*
           *//* for (Element body : bodies) {
                System.out.println(body.getAllElements());
                String[] arr= (String[]) body.getAllElements().toArray();

            }*/
        } catch (MalformedURLException e) {
            // It means the URL is invalid
            System.out.println("Invalid URL");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
