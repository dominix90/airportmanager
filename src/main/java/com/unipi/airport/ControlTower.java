package com.unipi.airport;

import akka.actor.*;
import akka.event.*;

import java.util.*;

import com.unipi.utils.Messages.*;
import com.unipi.utils.Parameters;

public class ControlTower extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	/* ========== VARIABILI E STRUTTURE DATI ========== */	
	private final String airportId; //id aeroporto
	private Runway[] runways;
	private Deque<Aircraft> landingQueue;
	private Deque<Aircraft> departureQueue;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */	

	public ControlTower(String airportId, Runway[] runways) {
	    this.airportId = airportId;
	    this.runways = runways;
	    this.landingQueue = new LinkedList<Aircraft>();
	    this.departureQueue = new LinkedList<Aircraft>();
	}

	public static Props props(String airportId, Runway[] runways) {
	    return Props.create(ControlTower.class, () -> new ControlTower(airportId, runways));
	}
	  
	@Override
	public void preStart() {
		log.info("ControlTower actor {} started", airportId);
	}

	@Override
	public void postStop() {
		log.info("ControlTower actor {} stopped", airportId);
	}

	/* ========== METODI PER GESTIONE CODE E TEMPI ========== */	
	  
	/* Metodo per il calcolo dei tempi d'atterraggio */
	private long getTimeForLanding(ActorRef aircraft) {
		long time = 0;
		  
		int queueSize = landingQueue.size();
		if (queueSize != 0)
			time = Parameters.averageRunwayOccupation * queueSize / runways.length;
		  
		return time;
	}
	  
	/* Metodo per il calcolo dei tempi di decollo */
	private long getTimeForTakingOff(ActorRef aircraft) {
		long time = 0;
		  
		int departureQueueSize = departureQueue.size();
		int landingQueueSize = landingQueue.size();
		time = Parameters.averageRunwayOccupation * (landingQueueSize + departureQueueSize);
		  
		return time;
	}
	
	/* Aggiornamento dei tempi in caso di atterraggio d'emergenza */
	private void informAircrafts () {
		int i = 0;
		for (Aircraft a : landingQueue) {
			if (i == 0)
				a.getAircraftActor().tell(new UpdateLandingTime(Parameters.averageRunwayOccupation), getSelf());
			else
				a.getAircraftActor().tell(new UpdateLandingTime(Parameters.averageRunwayOccupation * i / runways.length), getSelf());
			i++;
		}
	}
	
	/* ========== METODI GESTIONE PISTE ========== */	
	private Runway getFreeRunway() {
		for (int i = 0; i < runways.length; i++) {
			if (runways[i].isFree())
				return runways[i];
		}
		return null;
	}
	
	/* ========== METODI VISUALIZZAZIONE STATO AEROPORTO ========== */	
	
	/* Metodo per visualizzare la coda di atterraggio */
	private void printLandingQueue() {
		if (landingQueue.size() <= 0) {
			log.info("Nessun aereo si trova in coda di atterraggio");
		}
		int i = 0;
		for (Aircraft a : landingQueue) {
			log.info("Posizione {}: {}", Integer.toString(i+1), a.getFlightId());
			i++;
		}
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(
	        	LandingRequest.class,
	            r -> {
	            	ActorRef sender = getSender();
	            	long timeForLanding = getTimeForLanding(sender);
	            	sender.tell(new RespondLandingTime(r.requestId, r.flightId, timeForLanding), getSelf());
	            	log.info("Control tower {} computed {} seconds for aircraft {} landing", this.airportId, timeForLanding, r.flightId);
	            })
	        .match(
		        EmergencyLandingRequest.class,
	            r -> {
	            	ActorRef sender = getSender();
	            	long timeForLanding = getTimeForLanding(sender);
	            	sender.tell(new RespondLandingTime(r.requestId, r.flightId, timeForLanding), getSelf());
	            	log.info("Control tower {} computed {} seconds for aircraft {} landing", this.airportId, timeForLanding, r.flightId);
            })
	        .match(
        		EmergencyLandingConfirmation.class,
	            r -> {
	            	if(r.value) {
	            		log.info("Aircraft {} accepted to land", r.flightId);
	            		landingQueue.addFirst(new Aircraft(r.flightId,getSender()));
	            		informAircrafts();
	            	}
	            	else
	            		log.info("Aircraft {} changed course. It is now directed to another airport", r.flightId);
	            })
	        .match(
        		LandingConfirmation.class,
	            r -> {
	            	if(r.value) {
	            		log.info("Aircraft {} accepted to land", r.flightId);
	            		landingQueue.addFirst(new Aircraft(r.flightId,getSender()));
	            		printLandingQueue();
	            	}
	            	else
	            		log.info("Aircraft {} changed course. It is now directed to another airport", r.flightId);
	            })
	        .build();	    
	  }
}
