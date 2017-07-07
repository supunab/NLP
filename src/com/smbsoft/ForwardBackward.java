package com.smbsoft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ForwardBackward{

    // number of states
    private int n;

    // number of words
    private int m;

    private double[][] transProb;
    private double[][] obsProb;

    private double[][] expectedTrans;
    private double[][] expectedObs;

    // Word to index mapper
    private HashMap<String, Integer> wordIndex;


    public void execute() throws IOException{

        // set of all words
        Set<String> words = new HashSet<>();

        BufferedReader bf = new BufferedReader(new FileReader("src/conll2003.eng.testa"));

        String line = "";

        bf.readLine(); bf.readLine();

        while ((line = bf.readLine()) != null){
            if (!line.isEmpty()){
                words.add(line.split(" ")[0]);
            }
        }

        bf.close();

        // number of states
        n = 46;

        // number of possible observations
        m = words.size();

        Iterator<String> iter = words.iterator();

        wordIndex = new HashMap<>();

        int temp = 0;

        while(iter.hasNext()){
            wordIndex.put(iter.next(), temp);
            temp ++;
        }

        // transition probabilities, 0 - start n + 1 - end
        this.transProb = new double[n+2][n+2];

        // observing probabilities
        this.obsProb = new double[n+1][m];

        // init transProb and obsProb with arbitrary values
        initRandomParameters();

        bf = new BufferedReader(new FileReader("src/conll2003.eng.testa"));
        bf.readLine();
        bf.readLine();

        ArrayList<ArrayList<String>> sentenceTokens = new ArrayList<>();
        ArrayList<String> tempSentence = new ArrayList<>();

        while((line = bf.readLine()) != null){
            if (line.isEmpty()){
                if (tempSentence.size() > 1)
                    sentenceTokens.add(tempSentence);
                tempSentence = new ArrayList<>();

            }else{
                tempSentence.add(line.split(" ")[0]);
            }
        }

        bf.close();

        final int iterations = 100;

        //findMostProbableSequence(sentenceTokens.get(3).toArray(new String[sentenceTokens.get(3).size()]));

        for(int i = 0; i < iterations; i ++){
            // calculate the expected counts
            // m + 1 is for total
            this.expectedObs = new double[n+1][m+1];
            // n + 1 is for total
            this.expectedTrans = new double[n+2][n+3];

            for(ArrayList<String> sentence : sentenceTokens)
                expectation(sentence.toArray(new String[sentence.size()]));

            maximization();

            System.out.println("iteration = " + (i + 1));
            //System.out.println("obs " + validateProb(obsProb));
            //System.out.println("trans " + validateProb(transProb));
        }

        // test with the data
        for (int i = 0; i < 10; i ++)
            findMostProbableSequence(sentenceTokens.get(i).toArray(new String[sentenceTokens.get(i).size()]));


        System.out.println(Arrays.deepToString(transProb));
        // do the mapping between tags


    }

    public void initRandomParameters(){
        this.expectedTrans = new double[n+2][n+3];
        this.expectedObs = new double[n+1][m+1];

        Random rand = new Random();

        for(int state = 1; state < n + 1; state ++) {
            expectedTrans[0][state] = rand.nextInt(1000);
            expectedTrans[0][n+2] += expectedTrans[0][state];

            for(int i = 0; i < m ; i ++){
                expectedObs[state][i] = rand.nextInt(1000);
                expectedObs[state][m] += expectedObs[state][i];
            }

        }

        for(int s1 = 1; s1 < n + 1; s1 ++){
            for(int s2 = 1; s2 < n + 2; s2 ++){
                expectedTrans[s1][s2] = rand.nextInt(1000);
                expectedTrans[s1][n+2] += expectedTrans[s1][s2];
            }
        }

        // to generate transProb and obsProb using expected count values
        maximization();

    }


    public void expectation(String[] observation){
        // forward and backward values
        double[][] forwardValues = getForwardValues(observation);
        double[][] backwardValues = getBackwardValues(observation);

        // calculate z
        double z = 0;

        for(int state = 0; state < n + 1; state ++){
            z += forwardValues[observation.length / 2][state] * backwardValues[observation.length / 2][state];
        }

        if (z==0){
            //System.out.println("Z is zero!");
//            System.out.println(observation[observation.length - 1]);
//            System.out.println(Arrays.toString(forwardValues[observation.length / 2]));
//            System.out.println(Arrays.toString(backwardValues[observation.length / 2]));

            return;
        }

        // expected counts from the start
        for(int state = 1 ; state < n + 1; state ++){
            expectedTrans[0][state] += forwardValues[0][state] * backwardValues[0][state];
            expectedTrans[0][n+2] += forwardValues[0][state] * backwardValues[0][state];
        }

        // expected counts end
        for(int state = 1 ; state < n + 1; state ++){
            expectedTrans[state][n+1] += forwardValues[observation.length - 1][state]
                            * backwardValues[observation.length - 1][state];

            expectedTrans[state][n+2] += forwardValues[observation.length - 1][state]
                    * backwardValues[observation.length - 1][state];
        }

        double tempSum = 0;
        // expected trans
        for(int s1 = 1; s1 < n + 1; s1 ++){
            for (int s2 = 1; s2 < n + 1; s2 ++){
                tempSum = 0;
                for(int i = 0; i < observation.length - 1; i ++){
                    tempSum += forwardValues[i][s1] * obsProb[s1][wordIndex.get(observation[i])]
                                            * transProb[s1][s2] * backwardValues[i+1][s2];
                }
                expectedTrans[s1][s2] += tempSum / z;
                // update total transitions
                expectedTrans[s1][n+2] += tempSum / z;
            }
        }

        //expected emits
        for(int i = 0; i < observation.length; i ++){
           for(int state = 1; state < n + 1; state ++){
               expectedObs[state][wordIndex.get(observation[i])] += forwardValues[i][state] * backwardValues[i][state] / z;
               expectedObs[state][m] += forwardValues[i][state] * backwardValues[i][state] / z;
           }
        }

    }

    public void maximization(){
        // update transProb and obsProb based on the expected count values
        this.transProb = new double[n+2][n+2];
        this.obsProb = new double[n+1][m];


        for(int s1 = 0; s1 < n + 1; s1 ++){
            for (int s2 = 1; s2 < n + 2; s2 ++)
                transProb[s1][s2] = expectedTrans[s1][s2] / expectedTrans[s1][n+2];
        }

        for(int state = 1; state < n + 1; state ++)
            for(int w = 0; w < m; w ++)
                obsProb[state][w] = expectedObs[state][w] / expectedObs[state][m];

        //System.out.println("trans val inside : " + validateProb(transProb));

    }

    public void findMostProbableSequence(String[] observation){
        // forward and backward values
        double[][] forwardValues = getForwardValues(observation);
        double[][] backwardValues = getBackwardValues(observation);

        // state probabilities for each observation
        double[][] stateProb = new double[observation.length][n+1];

        int maxiState = 1;
        double maxi = -1;

        for(int obs = 0; obs < observation.length; obs ++){
            maxiState = 1;
            maxi = -1;
            for(int state = 1; state < n + 1; state ++){
                stateProb[obs][state] = forwardValues[obs][state] * backwardValues[obs][state];

                if (stateProb[obs][state] > maxi){
                    maxi = stateProb[obs][state];
                    maxiState = state;
                }
            }

            System.out.println(observation[obs] + " " + maxiState);
        }

        System.out.println("");

    }

    public double[][] getForwardValues(String[] observation){

        double[][] forwardValues = new double[observation.length][n+1];

        // fill forward values
        for(int i = 1; i < n + 1; i ++){
            forwardValues[0][i] = transProb[0][i];
        }

        for(int i = 1; i < observation.length; i++){
            for(int current = 1; current < n+1; current++){
                forwardValues[i][current] = 0;

                // for every state
                for(int prev = 1; prev < n + 1; prev++){
                    forwardValues[i][current] += forwardValues[i-1][prev] * transProb[prev][current] * obsProb[prev][wordIndex.get(observation[i-1])];
                }
            }
        }

        return forwardValues;

    }

    public double[][] getBackwardValues(String[] observation){
        double[][] backwardValues = new double[observation.length][n+1];

        // fill backward values
        for(int i = 1; i < n + 1; i ++){
            backwardValues[observation.length - 1][i] = transProb[i][n+1] * obsProb[i][wordIndex.get(observation[observation.length - 1])]; // n + 1 = END
        }

        for(int i = observation.length - 2; i >= 0; i --){
            for(int current = 1; current < n + 1; current ++){
                backwardValues[i][current] = 0;

                for(int prev = 1; prev < n + 1; prev ++){
                    backwardValues[i][current] += backwardValues[i+1][prev] * transProb[current][prev] * obsProb[current][wordIndex.get(observation[i])];
                }
            }
        }

        return backwardValues;
    }


    // to check the implementation
    public boolean validateProb(double[][] list){

        double temp = 0;
        for(int i = 0 ; i < list.length; i ++){
            temp = 0;
            for (int j =0 ; j < list[0].length; j ++){
                temp += list[i][j];
            }

            System.out.println(temp);
        }

        return true;

    }

    public boolean testSum(double[][] list){
        double temp = 0;
        for(int i =0; i < list.length; i ++){
            temp = 0;
            for(int j =0 ; j < list[0].length - 1; j ++){
                temp += list[i][j];
            }

            System.out.println("Difference : " + (temp - list[i][list[0].length - 1]));
        }
        System.out.println("Done");
        return true;

    }
}