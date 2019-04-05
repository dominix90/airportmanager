package com.unipi.airport;

import akka.actor.*;

public class Aircraft {
	
	/* Variabili di classe */
	private String flightId;
	private ActorRef aircraftActor;
	
	public Aircraft (String flightId, ActorRef aircraftActor) {
		this.flightId = flightId;
		this.aircraftActor = aircraftActor;
	}
	
	public String getFlightId() {
		return this.flightId;
	}
	
	public ActorRef getAircraftActor() {
		return this.aircraftActor;
	}
}
