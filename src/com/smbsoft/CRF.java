package com.smbsoft;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CRF {

    private int stateCount;
    private int obsCount;

    private HashMap<String, Integer> wordIndex;
    private HashMap<String, Integer> stateIndex;


    public void execute() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("src/conll2003.eng.testa"));
        ArrayList<String[]> sentences = new ArrayList<>();
        ArrayList<String> sentence = new ArrayList<>();

        wordIndex = new HashMap<>();
        stateIndex = new HashMap<>();

        br.readLine(); br.readLine();

        String line;
        obsCount = 0;
        stateCount = 1; // starting state is 0

        String word, tag;

        while ( (line = br.readLine()) != null){
            if (line.isEmpty()){
                sentences.add(sentence.toArray(new String[sentence.size()]));
                sentence.clear();
            }else {
                word = line.split(" ")[0];
                tag = line.split(" ")[1];

                if (!wordIndex.containsKey(word))
                    wordIndex.put(word, obsCount++);

                if (!stateIndex.containsKey(tag))
                    stateIndex.put(tag, stateCount++);

                sentence.add(word);
                sentence.add(tag);
            }
        }

        br.close();

        System.out.println("stateCount = " + stateCount);
        System.out.println("obsCount = " + obsCount);

        float[] w = new float[stateCount * stateCount + stateCount * obsCount];

        Random rand = new Random();

        for(int i = 0; i < w.length; i ++){
            w[i] = rand.nextFloat();
        }

        final float trainingRate = 0.5f;
        final int iterations = 100;
        final float lambda = 0.0001f;

        for(int i = 0; i < iterations; i ++){
            // get forward values
            // get backward values

            float[] gradient = new float[stateCount * stateCount + stateCount * obsCount];

            for(String[] sent : sentences){
                float[][] forwardValues = new float[sent.length/2][stateCount];
                float[][] backwardValues = new float[sent.length/2][stateCount];

                // base case - forward and backward values
                for(int state = 1; state < stateCount; state ++){
                    forwardValues[0][state] = (float) Math.exp(getDotProduct(sent, 0, state, 0, w));
                    backwardValues[sent.length/2 - 1][state] = 1;
                }

                // forward values
                for(int obs = 1; obs < sent.length/2 ; obs ++)
                    for(int state = 1; state < stateCount; state ++)
                        for(int prev = 1; prev < stateCount; prev ++)
                            forwardValues[obs][state] += forwardValues[obs - 1][prev]
                                    + (float) Math.exp(getDotProduct(sent, prev, state, obs, w));

                // backward values
                for(int obs = sent.length/2 - 2; obs >= 0 ; obs --)
                    for(int state = 1; state < stateCount; state ++)
                        for(int prev = 1; prev < stateCount; prev ++)
                            backwardValues[obs][state] += backwardValues[obs + 1][prev]
                                    + (float) Math.exp(getDotProduct(sent, prev, state, obs, w));


                // getting the gradient
                // first part
                for(int k = 0; k < gradient.length; k ++){

                    gradient[k] += getKFeature(sent, 0 , stateIndex.get(sent[1]), 0, w, k);

                    for(int obs = 1; obs < sent.length / 2; obs ++){
                        gradient[k] += getKFeature(sent, stateIndex.get(sent[2 * obs - 1]), stateIndex.get(sent[2 * obs + 1]), obs, w, k);
                    }
                }

                // second part
                for(int k = 0; k < gradient.length; k ++){
                    // start state
                    for(int state = 1; state < stateCount; state ++){
                        gradient[k] -= getKFeature(sent, 0, state, 0, w, k)
                                + getDotProduct(sent, 0, state, 0, w)
                                + backwardValues[0][state];
                    }

                    for(int obs = 1; obs < sent.length / 2; obs ++){
                        for(int s1 = 1; s1 < stateCount; s1 ++){
                            for(int s2 = 1; s2 < stateCount; s2 ++){
                                gradient[k] -= getKFeature(sent, s1, s2, obs, w, k)
                                        + forwardValues[obs-1][s1] + getDotProduct(sent, s1, s2, obs, w)
                                        + backwardValues[obs][s2];
                            }
                        }
                    }
                }

                System.out.println("Sentence done!");

            }


            // - lambda * wk
            for(int k = 0; k < gradient.length; k ++){
                w[k] += trainingRate * (gradient[k] - lambda * w[k]);
            }

            System.out.println("iteration = " + (i+1));
        }


    }


    private float getDotProduct(String[] sentence, int prevState, int currentState, int i, float[] w){
        // I'll model using the same parameters used in HMMs, transition and emission
        // first state * state number of elements will be taken for trans. Next obs * state elements is for emission

        // only the relevant elements will be 0, others will be 1

        // then return the sum of the relevant indexes of w

        float sum = 0;

        sum += w[prevState * stateCount + currentState];

        sum += w[stateCount * stateCount + currentState * obsCount + wordIndex.get(sentence[i * 2])]; // i * 2 because sentence format is with word, tag , word, tag

        return sum;
    }

    private float getKFeature(String[] sentence, int prevState, int currentState, int i, float[] w, int k){
        // returns the k th element of the feature vector, 0 indexed

        if (k==(prevState * stateCount + currentState) ||
                k == (stateCount * stateCount + currentState * obsCount + wordIndex.get(sentence[i * 2]))){
            return 1.0f;
        }
        return 0f;

    }


    // preprocessing data
    // generating feature functions --> feature vector
    // forward backward algorithm
}
