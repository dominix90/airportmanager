package com.unipi.analysis;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;

public class DistributionMain {

	public static void main(String[] args) {
		int mean = 1;
		double stddev = 0.1;
		while (mean < 20) {		
			for (int i = 0; i < 10; i++) {
				System.out.println("Mean: " + mean);
				System.out.println("Value: " + getRunwayOccupation(Math.log(mean), Math.log(mean)*0.001 + 0.001));
			}
			mean++;
			stddev = stddev + 0.1;
		}
		
	}
	
	/* ========== TEMPI DI DECOLLO, ATTERRAGGIO E PARCHEGGIO ========== */
	  /* Metodo per ottenimento durata atterraggio e decollo */
	  public static double getRunwayOccupation(double mean, double stddev) {
			double time = 0.000;
			
			LogNormalDistribution lnd = new LogNormalDistribution(mean, stddev);
			time = lnd.sample();
			
			return time * 1000;
		}
	  
	  /* Generazione del timeForNextArrival */
		public static double getTimeForNextArrival(int rate) {
			double time = 0.000;
			
			ExponentialDistribution exp = new ExponentialDistribution(rate);
			time = exp.sample();
			
			return time;
		}

}
