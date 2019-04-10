package com.unipi.airport;

import akka.actor.*;

public class Aircraft {
	
	/* Variabili di classe */
	private String flightId;
	private ActorRef aircraftActor;
	private boolean inEmergency;
	
	public Aircraft (String flightId, ActorRef aircraftActor, boolean inEmergency) {
		this.flightId = flightId;
		this.aircraftActor = aircraftActor;
		this.inEmergency = inEmergency;
	}
	
	public String getFlightId() {
		return this.flightId;
	}
	
	public ActorRef getAircraftActor() {
		return this.aircraftActor;
	}
	
	public boolean getEmergencyState() {
		return inEmergency;
	}
}
