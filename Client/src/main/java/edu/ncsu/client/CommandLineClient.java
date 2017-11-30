package edu.ncsu.client;

import edu.ncsu.chord.ChordID;
import edu.ncsu.store.StoreClientAPI;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class CommandLineClient {

    private final transient static Logger logger = Logger.getLogger(CommandLineClient.class);
    /* All keys-values used for get & put will bef taken & verified from below file */
    private static String ipListPath = "/tmp/ip";
    ArrayList<ChordID<InetAddress>> ipList;
    StoreClientAPI handle;

    public CommandLineClient() {
        ipList = new ArrayList<>();
        try {
            FileReader fr = new FileReader(ipListPath);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                ipList.add(new ChordID<>(InetAddress.getByName(line)));
            }
            Collections.sort(ipList);
            logger.debug("Active nodes: " + ipList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Set the very first entry in this list as bootstrap IP
        ClientConfig.bootStrapIP = ipList.get(0).getKey().toString().substring(1);
        System.out.println("Connecting to " + ClientConfig.bootStrapIP);
        handle = ClientRMIUtils.getRemoteClient();
        System.out.println("Connected..");
    }

    private static String help() {
        return "Following queries are supported\n" +
                "1. GET <key>: Returns the latest value assigned to this <key>\n" +
                "2. PUT <key> <value>: Puts the given key value into chronstore\n" +
                "3. GETR <key> <fromtimestamp> <totimestamp>: Returns all values assigned to " +
                "the <key> between given timestamps (timestamps should be epoch time values\n" +
                "4. DIFF <key> <fromtimestamp> <totimestamp>: Returns the consecutive differences" +
                " between all values assigned to <key> in given timestamp range.";
    }

    private static void prompt() {
        System.out.print("> ");
    }

    private void doGet(String query) {
        String tokens[] = query.split(" ");
        System.out.println("Getting value for " + tokens[1]);
        try {
            Object o = handle.get(tokens[1]);
            if (o == null)
                System.out.println("No such key found!");
            else
                System.out.println(o.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doPut(String query) {
        String tokens[] = query.split(" ");
        System.out.println("Putting (" + tokens[1] + ", " + tokens[2] + ")");
        try {
            handle.put(tokens[1], tokens[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doGetr(String query) {
        query = query.replace("%NOW%", Long.toString(System.currentTimeMillis()));
        String tokens[] = query.split(" ");
        System.out.println("Getting value for " + tokens[1] + " between " + tokens[2] + "-" + tokens[3]);
        try {
            List<Object> l = handle.get(tokens[1], Long.parseLong(tokens[2]), Long.parseLong(tokens[3]));
            System.out.print("[");
            for (Object o : l) {
                if (o == null)
                    System.out.print("No such key, ");
                else
                    System.out.print(o.toString() + ", ");
            }
            System.out.println("]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDiff(String query) {

    }

    private void doDelete(String query) {
        String tokens[] = query.split(" ");
        System.out.println("Deleting " + tokens[1]);
        try {
            handle.delete(tokens[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void process(String query) {
        if (query.startsWith("GETR"))
            doGetr(query);
        else if (query.startsWith("GET"))
            doGet(query);
        else if (query.startsWith("PUT"))
            doPut(query);
        else if (query.startsWith("DELETE"))
            doDelete(query);
        else if (query.startsWith("DIFF"))
            doDiff(query);
        else
            System.out.println("[Error] Not a valid query");

    }

    public static void main(String args[]) {
        Scanner sc= new Scanner(System.in);
        CommandLineClient cli = new CommandLineClient();
        help();
        while (true) {
            prompt();
            String query = sc.nextLine();
            cli.process(query);
        }
    }
}
