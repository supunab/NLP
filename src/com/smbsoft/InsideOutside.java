package com.smbsoft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;


public class InsideOutside{

    public static void execute() throws IOException{
        // productions
        HashMap<String, HashMap<String, Float>> unitProductions = new HashMap<>();
        HashMap<String, HashMap<String, HashMap<String, Float>>> otherProductions = new HashMap<>();

        // Obtain the data from the text file
        BufferedReader bf = new BufferedReader(new FileReader("src/ptb.deve_sec22.formatted.txt"));

        String line;

        String[] temp;
        String[] prod;

        Random rand = new Random();

        ArrayList<String[]> sentences = new ArrayList<>();

        ArrayList<String> sentence = new ArrayList<>();

        while ((line = bf.readLine()) != null){
            sentence.clear();

            temp = line.split("<DIV>");

            for(int i = 0; i < temp.length; i ++){
                prod = temp[i].split("->");
                prod[0] = prod[0].trim();
                prod[1] = prod[1].trim();

                if (prod[1].split(" ").length == 1){
                    // unit prod
                    if (!unitProductions.containsKey(prod[0]))
                        unitProductions.put(prod[0], new HashMap<>());

                    if (!unitProductions.get(prod[0]).containsKey(prod[1]))
                        unitProductions.get(prod[0]).put(prod[1], (float) rand.nextInt(1000));

                    sentence.add(prod[1]);

                }else {
                    // other prod
                    if(!otherProductions.containsKey(prod[0]))
                        otherProductions.put(prod[0], new HashMap<>());

                    if(!otherProductions.get(prod[0]).containsKey(prod[1].split(" ")[0]))
                        otherProductions.get(prod[0]).put(prod[1].split(" ")[0], new HashMap<>());

                    if(!otherProductions.get(prod[0]).get(prod[1].split(" ")[0]).containsKey(prod[1].split(" ")[1]))
                        otherProductions.get(prod[0]).get(prod[1].split(" ")[0]).put(prod[1].split(" ")[1], (float) rand.nextInt(1000));

                }
            }

            // add the sentence to sentences
            sentences.add(sentence.toArray(new String[sentence.size()]));
        }

        new InsideOutside(unitProductions, otherProductions, sentences).start(10);


    }

    HashMap<String, HashMap<String, Float>> unitProductions;
    HashMap<String, HashMap<String, HashMap<String, Float>>> otherProductions;

    HashMap<String, HashMap<String, Float>> unitCounts;
    HashMap<String, HashMap<String, HashMap<String, Float>>> otherCounts;

    ArrayList<String[]> sentences;

    HashMap<String , float[][]> inside;
    HashMap<String , float[][]> outside;

    HashMap<String, boolean[][]> insideFound;
    HashMap<String, boolean[][]> outsideFound;

    int n;
    int nt;

    public InsideOutside(HashMap<String, HashMap<String, Float>> unitProductions, HashMap<String, HashMap<String, HashMap<String, Float>>> otherProductions, ArrayList<String[]> sentences) {
        this.unitProductions = unitProductions;
        this.otherProductions = otherProductions;
        this.unitCounts = (HashMap<String, HashMap<String,Float>>) unitProductions.clone();
        this.otherCounts = (HashMap<String, HashMap<String,HashMap<String,Float>>>) otherProductions.clone();
        this.sentences = sentences;
    }

    public void start(int iterations) {

        for(int x = 0; x < iterations; x ++){
            calculatePotentials();
            clearCounts();

            for (String[] sentence : sentences) {
                n = sentence.length;

                // init as all zeros
                inside = new HashMap<>();
                outside = new HashMap<>();

                // add all the non terminals to inside, outside values
                Iterator<String> iter1 = unitProductions.keySet().iterator();

                while (iter1.hasNext()) {
                    inside.put(iter1.next(), new float[n][n]);
                    outside.put(iter1.next(), new float[n][n]);
                }

                iter1 = otherProductions.keySet().iterator();

                while (iter1.hasNext()) {
                    inside.put(iter1.next(), new float[n][n]);
                    outside.put(iter1.next(), new float[n][n]);
                }

                nt = inside.keySet().size();

                insideFound = new HashMap<>();
                outsideFound = new HashMap<>();


                iter1 = insideFound.keySet().iterator();

                while (iter1.hasNext()){
                    insideFound.put(iter1.next(), new boolean[n][n]);
                }

                iter1 = outsideFound.keySet().iterator();

                while (iter1.hasNext()){
                    outsideFound.put(iter1.next(), new boolean[n][n]);
                }

                Iterator<String> iter2;
                Iterator<String> iter3;
                String a,b,c;

                // base case - inside terms
                iter1 = unitProductions.keySet().iterator();

                while(iter1.hasNext()){
                    a = iter1.next();

                    iter2 = unitProductions.get(a).keySet().iterator();

                    while (iter2.hasNext()){
                        b = iter2.next();

                        for(int i = 0; i < n; i ++){
                            if (sentence[i].equals(b))
                                inside.get(b)[i][i] = unitProductions.get(a).get(b);
                            insideFound.get(b)[i][i] = true;
                        }
                    }
                }

                //fill up the inside values using recursive function
                // iterate through all the non terminals
                iter1 = inside.keySet().iterator();

                while (iter1.hasNext()){
                    a = iter1.next();

                    for(int i = 0 ; i < n-1; i ++){
                        for(int j = i + 1; j < n; j ++){
                            inside.get(a)[i][j] = getInside(a, i, j);
                        }
                    }
                }


                // base cases - outside
                outside.get("S")[0][sentence.length - 1] = 1; // others are init to zero

                // find outside values
                // iterate through all the non terminals

                iter1 = inside.keySet().iterator();

                while (iter1.hasNext()){
                    a = iter1.next();

                    for(int i = 0 ; i < n-1; i ++){
                        for(int j = i + 1; j < n; j ++){
                            outside.get(a)[i][j] = getOutside(a, i, j);
                        }
                    }
                }

                // calculate the expected counts
                float z = inside.get("S")[0][sentence.length - 1];

                // unit production counts
                iter1 = unitCounts.keySet().iterator();

                while (iter1.hasNext()){
                    a = iter1.next();

                    for(int i = 0; i < sentence.length; i ++){
                        if (unitProductions.get(a).containsKey(sentence[i])){
                            unitCounts.get(a).put(sentence[i],
                                     unitCounts.get(a).get(sentence[i]) + inside.get(a)[i][i] * outside.get(a)[i][i] / z);
                        }
                    }
                }

                // other production counts
                iter1 = otherProductions.keySet().iterator();

                while(iter1.hasNext()){
                    a = iter1.next();

                    iter2 = otherProductions.get(a).keySet().iterator();

                    while (iter2.hasNext()){
                        b = iter2.next();

                        iter3 = otherProductions.get(a).get(b).keySet().iterator();

                        while (iter3.hasNext()){
                            c = iter3.next();

                            for(int i = 0; i < sentence.length - 1; i ++){
                                for(int k = i; k < sentence.length - 1; k ++){
                                    for(int j = k + 1; j < sentence.length; j ++){
                                        otherCounts.get(a).get(b).put(c,
                                                otherCounts.get(a).get(b).get(c) + outside.get(a)[i][j]
                                                        * otherProductions.get(a).get(b).get(c)
                                                        * inside.get(b)[i][k] * inside.get(c)[k+1][j] / z);
                                    }
                                }
                            }
                        }
                    }
                }


            }
        }
    }

    private void calculatePotentials(){
        Iterator<String> iter1;
        Iterator<String> iter2;
        Iterator<String> iter3;

        float tempSum = 0;

        iter1 = unitProductions.keySet().iterator();
        String a, b, c;

        while(iter1.hasNext()){
            a = iter1.next();
            tempSum = 0;

            iter2 = unitCounts.get(a).keySet().iterator();

            while(iter2.hasNext()){
                b = iter2.next();
                tempSum += unitCounts.get(a).get(b);
            }

            // update the potentials
            iter2 = unitCounts.get(a).keySet().iterator();

            while (iter2.hasNext()){
                b = iter2.next();
                unitProductions.get(a).put(b , unitCounts.get(a).get(b) / tempSum);
            }
        }

        // for other productions
        iter1 = otherProductions.keySet().iterator();

        while(iter1.hasNext()){
            a = iter1.next();
            tempSum = 0;

            iter2 = otherProductions.get(a).keySet().iterator();

            while(iter2.hasNext()){
                b = iter2.next();

                iter3 = otherProductions.get(a).get(b).keySet().iterator();

                while(iter3.hasNext()){
                    c = iter3.next();

                    tempSum += otherCounts.get(a).get(b).get(c);
                }
            }

            // update the value

            while(iter2.hasNext()){
                b = iter2.next();

                iter3 = otherProductions.get(a).get(b).keySet().iterator();

                while(iter3.hasNext()){
                    c = iter3.next();

                    otherProductions.get(a).get(b).put(c,
                            otherCounts.get(a).get(b).get(c) / tempSum);
                }
            }
        }


    }

    private float getInside(String nt, int i, int j){

        if (insideFound.get(nt)[i][j]){
            return inside.get(nt)[i][j];
        }

        // if not found, find
        inside.get(nt)[i][j] = 0;

        Iterator<String> iter2, iter3;
        String b, c;

        iter2 = otherProductions.get(nt).keySet().iterator();

        while(iter2.hasNext()){
            b = iter2.next();

            iter3 = otherProductions.get(nt).get(b).keySet().iterator();

            while (iter3.hasNext()){
                c = iter3.next();

                for(int k = i; k < j; k ++){
                    inside.get(nt)[i][j] += otherProductions.get(nt).get(b).get(c) * getInside(b, i, k) * getInside(c, k+1, j);
                }

            }
        }

        insideFound.get(nt)[i][j] = true;
        return inside.get(nt)[i][j];
    }


    private float getOutside(String nt, int i, int j){
        if (outsideFound.get(nt)[i][j])
            return outside.get(nt)[i][j];


        outside.get(nt)[i][j] = 0;

        Iterator<String> iter1, iter2, iter3;
        String b, c;

        iter1 = otherProductions.keySet().iterator();

        while(iter1.hasNext()){
            b = iter1.next();

            iter2 = otherProductions.get(b).get(nt).keySet().iterator();

            while (iter2.hasNext()){
                c = iter2.next();

                for(int k = j + 1; k < n; k ++){
                    outside.get(nt)[i][j] += otherProductions.get(b).get(nt).get(c) * getInside(c, j + 1 , k) * getOutside(b, i, k);
                }

            }

            iter2 = otherProductions.get(b).keySet().iterator();

            while (iter2.hasNext()){
                c = iter2.next();

                for(int k = 0; k < i; k ++){
                    outside.get(nt)[i][j] += otherProductions.get(b).get(c).get(nt) * getInside(c, k, i - 1) * getOutside(b, k, j);
                }

            }
        }

        outsideFound.get(nt)[i][j] = true;
        return outside.get(nt)[i][j];
    }

    private void clearCounts(){
        Iterator<String> iter1, iter2, iter3;
        String a, b, c;

        // unitCounts
        iter1 = unitCounts.keySet().iterator();

        while(iter1.hasNext()){
            a = iter1.next();

            iter2 = unitCounts.get(a).keySet().iterator();

            while (iter2.hasNext()){
                b = iter2.next();

                unitCounts.get(a).put(b, 0f);
            }

        }

        // otherCounts
        iter1 = otherCounts.keySet().iterator();

        while(iter1.hasNext()){
            a = iter1.next();

            iter2 = otherCounts.get(a).keySet().iterator();

            while (iter2.hasNext()){
                b = iter2.next();

                iter3 = otherCounts.get(a).get(b).keySet().iterator();

                while (iter3.hasNext()){
                    c = iter3.next();

                    otherCounts.get(a).get(b).put(c, 0f);
                }
            }
        }

    }

    public InsideOutside unitProductions(HashMap unitProductions){
        this.unitProductions = unitProductions;
        return this;
    }

    public InsideOutside otherProductions(HashMap otherProductions){
        this.otherProductions = otherProductions;
        return this;
    }
}