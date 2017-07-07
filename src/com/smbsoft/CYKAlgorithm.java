package com.smbsoft;

import java.util.ArrayList;

public class CYKAlgorithm {

    public void execute(){
        final int nt = 4;
        final int t = 2;

        // CFG, 0 will be the starting symbol
        ArrayList<int[]> unitProductions = new ArrayList<>();
        ArrayList<int[]> otherProductions = new ArrayList<>();


        // sample CFG
        /*
        S -> AB | BC
        A -> BA | a
        B -> CC | b
        C -> AB | a
        S = 0 , A = 1 , B = 2, C = 3
        a = 0, b = 1
        */

        unitProductions.add(new int[]{1, 0});
        unitProductions.add(new int[]{2, 1});
        unitProductions.add(new int[]{3, 0});

        otherProductions.add(new int[]{0, 1, 2});
        otherProductions.add(new int[]{0, 2, 3});
        otherProductions.add(new int[]{1, 2, 1});
        otherProductions.add(new int[]{2, 3, 3});
        otherProductions.add(new int[]{3, 1, 2});

        // size of the parsing string
        final int n = 5;

        // parsing string baaba = 10010
        int[] string = {1, 0 , 0, 1, 0};

        // Table for CYK
        boolean[][][] table = new boolean[n][n][nt];

        /// init table with false
        for(int i =0 ; i < n; i ++){
            for(int j=0 ; j < n; j++){
                for(int k=0; k < nt; k++){
                    table[i][j][k] = false;
                }
            }
        }

        // fill up the bottom row using unit productions
        for(int i = 0; i < n; i ++){
            for(int[] unit : unitProductions){
                if (string[i] == unit[1])
                    table[0][i][unit[0]] = true;
            }
        }

        // fill up the table
        for(int l = 1; l < n; l++){
            for(int s = 0; s < n - l; s++){
                for(int p = 0; p < l; p ++){
                    for(int[] prod : otherProductions){
                        if (table[p][s][prod[1]] && table[l-p-1][s+1+p][prod[2]])
                            table[l][s][prod[0]] = true;
                    }
                }
            }
        }

        if (table[n-1][0][0])
            System.out.println("String can be generated by the specified Context Free Grammer");
        else
            System.out.println("String cannot be generated by the specified Context Free Grammer");


    }
}
