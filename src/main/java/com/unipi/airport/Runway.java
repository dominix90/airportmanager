package com.unipi.airport;

public class Runway {
	
	/* Variabili di stato della pista */
	private String status;
	private Aircraft aircraftInRunway;
	
	public Runway (String status) {
		this.status = status;
		this.aircraftInRunway = null;
	}
	
	public void setAircraftInRunway(Aircraft a) {
		this.aircraftInRunway = a;
	}
	
	public boolean isFree() {
		return status.equals("FREE");
	}
}
