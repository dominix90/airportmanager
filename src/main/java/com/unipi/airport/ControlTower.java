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
	private Deque<ActorRef> landingQueue;
	private Deque<ActorRef> departureQueue;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */	

	public ControlTower(String airportId) {
	    this.airportId = airportId;
	    this.landingQueue = new LinkedList<ActorRef>();
	    this.departureQueue = new LinkedList<ActorRef>();
	}

	public static Props props(String airportId) {
	    return Props.create(ControlTower.class, () -> new ControlTower(airportId));
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
			time = Parameters.averageRunwayOccupation * queueSize / Parameters.runwaysNumber;
		  
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
		for (ActorRef a : landingQueue) {
			if (i == 0)
				a.tell(new UpdateLandingTime(Parameters.averageRunwayOccupation), getSelf());
			else
				a.tell(new UpdateLandingTime(Parameters.averageRunwayOccupation * i / Parameters.runwaysNumber), getSelf());
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
	            		landingQueue.addFirst(getSender());
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
	            		landingQueue.add(getSender());
	            	}
	            	else
	            		log.info("Aircraft {} changed course. It is now directed to another airport", r.flightId);
	            })
	        .build();
	    
	  }
}
