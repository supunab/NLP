package com.smbsoft;
import java.util.ArrayList;

public class InsideOutside{
    public static void execute(){
        ArrayList<UnitProduction> unitProductions = new ArrayList<>();
        ArrayList<OtherProduction> otherProductions = new ArrayList<>();

        // Add the CFG here

        // number of non terminals
        final int nt = 10;

        // number of terminals
        final int t = 100;

        // observation sequence
        int[] observation = new int[]{0, 1, 2, 2, 1, 4 , 4, 4, 1, 2, 2, 0 , 0, 3};

        new InsideOutside(nt, t)
                .unitProductions(unitProductions)
                .otherProductions(otherProductions)
                .observation(observation)
                .start();

    }

    int[] observation;

    ArrayList<UnitProduction> unitProductions;
    ArrayList<OtherProduction> otherProductions;

    float[][][] inside;
    float[][][] outside;

    boolean[][][] insideFound;
    boolean[][][] outsideFound;

    // Nubmer of non terminals
    final int nt;

    // Number of terminals
    final int t;

    // size of the input sequence
    int n;

    public InsideOutside(int nt, int t){
        this.nt = nt;
        this.t = t;
    }

    public void start(){
        n = observation.length;

        // init as all zeros
        inside = new float[nt][n][n];
        outside = new float[nt][n][n];

        insideFound = new boolean[nt][n][n];
        outsideFound = new boolean[nt][n][n];

        // base case - inside terms
        for(UnitProduction production : unitProductions){
            for(int i = 0; i < n; i ++){
                if (observation[i] == production.production[1])
                    inside[production.production[0]][i][i] = production.potential[i];
                insideFound[production.production[0]][i][i] = true;
            }

        }

        // fill up inside values using recursive function
        for(int nont = 0; nont < nt; nont ++){
            for(int i = 0 ; i < n-1; i ++){
                for(int j = i + 1; j < n; j ++){
                    inside[nont][i][j] = getInside(nont, i, j);
                }
            }
        }

        // base cases - outside
        outside[0][0][observation.length - 1] = 1; // others are init to zero

        // find outside values
        for(int nont = 0; nont < nt; nont ++){
            for(int i = 0 ; i < n-1; i ++){
                for(int j = i + 1; j < n; j ++){
                    outside[nont][i][j] = getInside(nont, i, j);
                }
            }
        }

    }

    public float getInside(int nt, int i, int j){
        if (insideFound[nt][i][j])
            return inside[nt][i][j];

        // if not found, find
        inside[nt][i][j] = 0;
        for(OtherProduction production : otherProductions){
            if (production.production[0] == nt){
                for(int k = i; k < j; k ++){
                    inside[nt][i][j] += production.potential[i][k][j] * getInside(production.production[1], i, k) * getInside(production.production[2], k+1, j);
                }
            }
        }

        insideFound[nt][i][j] = true;
        return inside[nt][i][j];
    }


    public float getOutside(int nt, int i, int j){
        if (outsideFound[nt][i][j])
            return outside[nt][i][j];

        outside[nt][i][j] = 0;

        for(OtherProduction production : otherProductions){

            if (production.production[2] == nt){
                for(int k = 0; k < i; k ++){
                    outside[nt][i][j] += production.potential[k][i-1][j] * getInside(production.production[1], k, i - 1) * getOutside( production.production[0], k, j);
                }

            }

            if(production.production[1] == nt){
                for(int k = j + 1; k < n; k ++){
                    outside[nt][i][j] += production.potential[i][j][k] * getInside(production.production[2], j + 1 , k) * getOutside(production.production[0], i, k);
                }


            }
        }

        outsideFound[nt][i][j] = true;
        return outside[nt][i][j];
    }

    public InsideOutside unitProductions(ArrayList<UnitProduction> unitProductions){
        this.unitProductions = unitProductions;
        return this;
    }

    public InsideOutside otherProductions(ArrayList<OtherProduction> otherProductions){
        this.otherProductions = otherProductions;
        return this;
    }

    public InsideOutside observation(int[] observation){
        this.observation = observation;
        return this;
    }


    private class UnitProduction{
        // should be private
        int[] production;
        float[] potential;

        public UnitProduction(int left, int right, int observations){
            production = new int[2];
            production[0] = left;
            production[1] = right;

            potential = new float[observations];
        }

    }

    private class OtherProduction{
        // should be private
        int[] production;
        float[][][] potential;

        public OtherProduction(int left, int r1, int r2, int observations){
            production = new int[3];
            production[0] = left;
            production[1] = r1;
            production[2] = r2;

            potential = new float[observations][observations][observations];
        }
    }

}