package com.unipi.airport;

import akka.actor.*;
import akka.event.*;

import org.apache.commons.math3.distribution.*;

import java.time.Duration;

import com.unipi.utils.Messages.*;
import com.unipi.utils.Parameters;

public class AirportSupervisor extends AbstractActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private Scheduler scheduler = getContext().getSystem().getScheduler();

	public static Props props() {
		return Props.create(AirportSupervisor.class);
	}
	
	//TORRE DI CONTROLLO
	ActorRef controlTower;

	@Override
	public void preStart() {
		log.info("Airport Application started");
		
		//AEREO INIZIALE
		String flightId = generateFlightId(6);
    	ActorRef aircraft = getContext().actorOf(AircraftActor.props(flightId, Math.round(getFuelValue())), flightId.toLowerCase());
    	double nextArrival = getTimeForNextArrival();
    	scheduler.scheduleOnce(Duration.ofMillis(Math.round(nextArrival)), getSelf(), new AircraftGenerator(), getContext().getSystem().dispatcher(), null);
    	log.info("Aircraft {} generated! New aircraft in {} milliseconds", flightId, nextArrival);
    	
    	/* DEBUG */
		/*
		 * for (int i = 0; i < 10; i++) { nextArrival = getTimeForNextArrival();
		 * log.info("Aircraft {} generated! New aircraft in {} milliseconds", flightId,
		 * nextArrival); } getContext().stop(getSelf());
		 */
    	/*END DEBUG */
		
		//PISTE E AEROPORTO
		Runway[] runways = new Runway[Parameters.runwaysNumber];
		for (int i = 0; i < Parameters.runwaysNumber; i++) {
			runways[i] = new Runway(i+1, "FREE");
		}
	    controlTower = getContext().actorOf(ControlTower.props("CTA",runways), "cta");
	    
	    /* Viene generato il primo aereo e schedulato 
	     * il selfmessage per la generazione del traffico di volo */
	    aircraft.tell(new StartLandingPhase(controlTower, flightId), controlTower);
	}

	@Override
	public void postStop() {
		log.info("Airport Application stopped");
	}
	
	/* Generazione del flightId */
	private static final String ALPHA_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String NUMERIC_STRING = "0123456789";

	public static String generateFlightId(int count) {
		StringBuilder builder = new StringBuilder();
		
		int initialCount = count;
		int character = -1;
		
		while (count-- != 0) {
			if (count >= initialCount - 2) {
				character = (int)(Math.random()*ALPHA_STRING.length());
				builder.append(ALPHA_STRING.charAt(character));
			}
			else {
				character = (int)(Math.random()*NUMERIC_STRING.length());
				builder.append(NUMERIC_STRING.charAt(character));
			}			
		}
		
		return builder.toString();
	}
	
	/* Generazione del timeForNextArrival */
	public double getTimeForNextArrival() {
		double time = 0.000;
		
		ExponentialDistribution exp = new ExponentialDistribution(6.000);
		time = exp.sample();
		
		return time * 1000;
	}
	
	/* Generazione del valore carburante */
	public double getFuelValue() {
		double fuel = 0.000;
		
		UniformRealDistribution ud = new UniformRealDistribution(0,10);
		fuel = ud.sample();
		
		return fuel * 1000;
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			/* ========== GENERAZIONE TRAFFICO DI VOLO ========== */
			/* Ogni volta che viene ricevuto questo selfmessage un nuovo aereo viene generato */
			.match(
				AircraftGenerator.class,
	            r -> {
	            	String flightId = generateFlightId(6);
	            	double nextArrival = getTimeForNextArrival();
	            	ActorRef newAircraft = getContext().actorOf(AircraftActor.props(flightId, Math.round(getFuelValue())), flightId.toLowerCase());	
	            	newAircraft.tell(new StartLandingPhase(controlTower, flightId), controlTower);
	            	scheduler.scheduleOnce(Duration.ofMillis(Math.round(nextArrival)), getSelf(), new AircraftGenerator(), getContext().getSystem().dispatcher(), null);	            	
	            	log.info("Aircraft {} generated! New aircraft in {} milliseconds", flightId, nextArrival);
	            })
	        .build();
	}
}