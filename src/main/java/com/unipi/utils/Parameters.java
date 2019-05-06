package com.unipi.utils;

public final class Parameters {
	/* ========== PARAMETRI DIMENSIONI AEROPORTO ========== */
	public static final int runwaysNumber = 2;
	public static final int parkingSize = 100;
	
	
	/* ========== PARAMETRI PER LOG CONTROLTOWER E AIRCRAFT ========== */
	public static final boolean logVerbose = true;
	public static final boolean logAirportState = true;
	
	/* ========== PARAMETRI TEMPORALI E PER RNG ========== */
	public static final int seed = 0;
	public static final String timeUnit = "seconds"; //unità di misura per la simulazione, usata per le stampe
	public static final double meanLandingTime = 8*60; //tempo in millisecondi nell'esecuzione, in timeUnit nella simulazione
	public static final double shapeLandingTime = 0.001;
	public static final double meanTakeOffTime = 8*60; //tempo in millisecondi nell'esecuzione, in timeUnit nella simulazione
	public static final double shapeTakeOffTime = 0.001;
	public static final double meanParkingTime = 30*60; //tempo in millisecondi nell'esecuzione, in timeUnit nella simulazione
	public static final double shapeParkingTime = 0.001;
	public static final double meanInterArrivalTime = 8*60; //tempo in millisecondi nell'esecuzione, in timeUnit nella simulazione
	public static final double minFuel = 3*60; //tempo in millisecondi nell'esecuzione, in timeUnit nella simulazione
	public static final double maxFuel = 30*60; //tempo in millisecondi nell'esecuzione, in timeUnit nella simulazione
	public static final long fuelSchedulingTime = 1*60; //in millisecondi nell'esecuzione
	public static final int emergencyPercentage = 0; //percentuale aerei in emergenza	
}
