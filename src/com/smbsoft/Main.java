package com.smbsoft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) throws IOException {
//         new ForwardBackward().execute();

//        HashMap<String, HashMap<String, Double>> transProb = new HashMap<>();
//
//        HashMap<String, Double> temp = new HashMap<>();
//        temp.put("SP", 0.335);
//        transProb.put("NN", temp);
//
//        System.out.println(transProb.get("NN").get("SP"));

//        InsideOutside.execute();

        new CRF().execute();
    }
}
