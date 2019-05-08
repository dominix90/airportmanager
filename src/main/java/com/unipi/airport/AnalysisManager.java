package com.unipi.airport;

import akka.actor.*;
import akka.actor.AbstractActor.Receive;
import akka.event.*;

import java.time.Duration;
import java.util.*;

import com.unipi.utils.Messages.*;
import com.unipi.utils.Parameters;

public class AnalysisManager extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	/* ========== VARIABILI E STRUTTURE DATI ========== */
	private String airportId;
	private ActorRef totalAircraftsNumberActor;
	private ActorRef queueAircraftsNumberActor;
	private ActorRef parkingAircraftsNumberActor;
	private ActorRef landingsAndTakeoffsNumberActor;
	private ActorRef hijackedAircaftsNumberActor;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */
	public AnalysisManager(String airportId) {
		this.airportId = airportId;
	}
	public static Props props(String airportId) {
	    return Props.create(AnalysisManager.class, () -> new AnalysisManager(airportId));
	}
	
	@Override
	public void preStart() {
		if(Parameters.logAnalysis) log.info("AnalysisManager actor started");
		
		//INIZIALIZZAZIONE ATTORI FIGLI
		totalAircraftsNumberActor = getContext().actorOf(TotalAircraftsNumberActor.props(airportId), "total-aircrafts-number-actor");
		queueAircraftsNumberActor = getContext().actorOf(QueueAircraftsNumberActor.props(airportId), "queue-aircrafts-number-actor");
		parkingAircraftsNumberActor = getContext().actorOf(ParkingAircraftsNumberActor.props(airportId), "parking-aircrafts-number-actor");
		landingsAndTakeoffsNumberActor = getContext().actorOf(LandingsAndTakeoffsNumberActor.props(airportId), "landings-and-takeoffs-number-actor");
		hijackedAircaftsNumberActor = getContext().actorOf(HijackedAircraftsNumberActor.props(airportId), "hijackes-aircrafts-number-actor");
	}
	
	@Override
	public void postStop() {
		if(Parameters.logAnalysis) log.info("AnalysisManager actor stopped");
	}
	
	//GESTIONE MESSAGGI RICEVUTI
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(
					NewAircraftInLandingQueue.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: NewAircraftInLandingQueue");
		            	totalAircraftsNumberActor.tell(new ChangedTotalAircraftsNumber(1), getSelf());
		            	queueAircraftsNumberActor.tell(new ChangedLandingQueueAircraftsNumber(1), getSelf());
	            	})
			.match(
					NewAircraftInEmergencyQueue.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: NewAircraftInEmergencyQueue");
		            	totalAircraftsNumberActor.tell(new ChangedTotalAircraftsNumber(1), getSelf());
		            	queueAircraftsNumberActor.tell(new ChangedEmergencyQueueAircraftsNumber(1), getSelf());
	            	})
			.match(
					AircraftRejectedLanding.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftRejectedLanding");
		            	totalAircraftsNumberActor.tell(new ChangedTotalAircraftsNumber(-1), getSelf());
		            	queueAircraftsNumberActor.tell(new ChangedLandingQueueAircraftsNumber(-1), getSelf());
		            	hijackedAircaftsNumberActor.tell(new ChangedHijackedAircraftsBecauseOfFuelNumber(1), getSelf());
	            	})
			.match(
					AircraftTookOff.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftTookOff");
		            	totalAircraftsNumberActor.tell(new ChangedTotalAircraftsNumber(-1), getSelf());
	            	})
			.match(
					AircraftEmergencyLanding.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftEmergencyLanding");
		            	queueAircraftsNumberActor.tell(new ChangedEmergencyQueueAircraftsNumber(-1), getSelf());
		            	landingsAndTakeoffsNumberActor.tell(new ChangedEmergencyLandingsNumber(1), getSelf());
	            	})
			.match(
					AircraftLanding.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftLanding");
		            	queueAircraftsNumberActor.tell(new ChangedLandingQueueAircraftsNumber(-1), getSelf());
		            	landingsAndTakeoffsNumberActor.tell(new ChangedLandingsNumber(1), getSelf());
	            	})
			.match(
					AircraftTakingOff.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftTakingOff");
		            	queueAircraftsNumberActor.tell(new ChangedDepartureQueueAircraftsNumber(-1), getSelf());
		            	landingsAndTakeoffsNumberActor.tell(new ChangedTakeoffsNumber(1), getSelf());
	            	})
			.match(
					AircraftNowInEmergency.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftNowInEmergency");
		            	queueAircraftsNumberActor.tell(new ChangedLandingQueueAircraftsNumber(-1), getSelf());
		            	queueAircraftsNumberActor.tell(new ChangedEmergencyQueueAircraftsNumber(1), getSelf());
	            	})
			.match(
					AircraftRequestedDeparture.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftRequestedDeparture");
		            	queueAircraftsNumberActor.tell(new ChangedDepartureQueueAircraftsNumber(1), getSelf());
	            	})
			.match(
					AircraftInEmergencyLeftParking.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftInEmergencyLeftParking");
		            	parkingAircraftsNumberActor.tell(new ChangedEmergencyParkingAircraftsNumber(-1), getSelf());
	            	})
			.match(
					AircraftLeftParking.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftLeftParking");
		            	parkingAircraftsNumberActor.tell(new ChangedParkingAircraftsNumber(-1), getSelf());
	            	})
			.match(
					AircraftInEmergencyParked.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftInEmergencyParked");
		            	parkingAircraftsNumberActor.tell(new ChangedEmergencyParkingAircraftsNumber(1), getSelf());
	            	})
			.match(
					AircraftParked.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: AircraftParked");
		            	parkingAircraftsNumberActor.tell(new ChangedParkingAircraftsNumber(1), getSelf());
	            	})
			.match(
					HijackedAircraftBecauseOfAirportFull.class,
		            r -> {
		            	if(Parameters.logAnalysis) log.info("Message received: HijackedAircraftBecauseOfAirportFull");		            	
		            	hijackedAircaftsNumberActor.tell(new ChangedHijackedAircraftsBecauseOfAirportFull(1), getSelf());
	            	})
	        .build();
	}
}
