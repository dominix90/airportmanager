package com.unipi.airport;

public class Runway {
	
	/* Variabili di stato della pista */
	private int runwayNumber;
	private String status;
	private Aircraft aircraftInRunway;
	
	public Runway (int runwayNumber, String status) {
		this.setRunwayNumber(runwayNumber);
		this.status = status;
		this.aircraftInRunway = null;
	}
	
	/* GETTER */
	public Aircraft getAircraftInRunway() {
		return aircraftInRunway;
	}
	
	public String getStatus() {
		return status;
	}

	public int getRunwayNumber() {
		return runwayNumber;
	}
	
	/* SETTER */
	public void setAircraftInRunway(Aircraft a) {
		this.aircraftInRunway = a;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}

	public void setRunwayNumber(int runwayNumber) {
		this.runwayNumber = runwayNumber;
	}
	
	/* Metodo per controllare se la pista è libera */
	public boolean isFree() {
		return status.equals("FREE");
	}
}
