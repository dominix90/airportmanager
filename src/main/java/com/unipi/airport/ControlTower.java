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
	private Map<Integer, Aircraft> parking;
	private Map<Integer, Aircraft> emergencyParking;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */	

	public ControlTower(String airportId, Runway[] runways) {
	    this.airportId = airportId;
	    this.runways = runways;
	    this.landingQueue = new LinkedList<Aircraft>();
	    this.departureQueue = new LinkedList<Aircraft>();
	    this.parking = new HashMap<Integer, Aircraft>();
	    this.emergencyParking = new HashMap<Integer, Aircraft>();
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
	
	/* Metodo per la gestione dei casi in cui un aereo decide di abbandonare 
	 * la coda di atterraggio in seguito ad un aggiornamento dei tempi
	 */
	private void deleteAircraft(String aircraftToDelete) {
		Deque<Aircraft> landingQueueTemp = new LinkedList<Aircraft>(); //questa variabile conterrà la coda rigenerata
		Aircraft aircraftTemp; //questa variabile conterrà l'aereo correntemente in esame
		
		for (int i = 0; i < landingQueue.size(); i++) {
			aircraftTemp = landingQueue.removeFirst();
			
			if (!aircraftTemp.getFlightId().equals(aircraftToDelete)) {
				landingQueueTemp.add(aircraftTemp);
			}
		}
		
		/* Dopo aver controllato tutta la coda originale ed aver 
		 * rigenerato una coda parallela senza l'aereo da eliminare 
		 * viene resa ufficiale la nuova coda 
		 */
		landingQueue = landingQueueTemp;
	}
	
	/* Metodo per avviare la fase di atterraggio */
	private void startLanding(ActorRef destination, Runway runway) {
	//private void startLanding(Runway runway) {	
		if (landingQueue.size() > 0) {
			Aircraft landingAircraft = landingQueue.removeFirst();
			log.info("Informing aircraft {} that it can start landing", landingAircraft.getFlightId());
			landingAircraft.getAircraftActor().tell(new StartLanding(runway, landingAircraft.getFlightId()), getSelf());
			// La pista va settata occupata
			runway.setAircraftInRunway(landingAircraft);
			runway.setStatus("OCCUPIED");
		}
	}
	
	/* Metodo per chiudere la fase di atterraggio */
	private void closeLandingPhase(Aircraft landedAircraft, Runway runway) {
		int parkNumber = parking.size() + 1; //il numero del parcheggio del nuovo aereo
		if (parkNumber <= Parameters.parkingSize)
			parking.put(parkNumber, landedAircraft);
		log.info("Aircraft {} is now parked at place n.{}", landedAircraft.getFlightId(), parkNumber);
		// La pista va settata libera
		runway.setAircraftInRunway(null);
		runway.setStatus("FREE");
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
				a.getAircraftActor().tell(new UpdateLandingTime(a.getFlightId(), Parameters.averageRunwayOccupation), getSelf());
			else
				a.getAircraftActor().tell(new UpdateLandingTime(a.getFlightId(), Parameters.averageRunwayOccupation * i / runways.length), getSelf());
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
			log.info("No aircrafts in landing queue!");
		}
		int i = 0;
		String message = "";
		message += "\n********** LANDING QUEUE **********";
		for (Aircraft a : landingQueue) {
			message += "\nPosition" + Integer.toString(i+1) + ": " + a.getFlightId();
			i++;
		}
		message += "\n********** END OF LANDING QUEUE **********";
		log.info(message);
	}
	
	/* Metodo per visualizzare il parcheggio */
	private void printParking() {
		if (parking.size() <= 0) {
			log.info("No aircrafts in parking!");
		}
		int i = 0;
		String message = "";
		message += "\n********** PARKING **********";
		for (Aircraft a : parking.values()) {
			message += "\n----------";
			message += "\n|" + Integer.toString(i+1) + ": " + a.getFlightId();
			message += "\n----------";
			i++;
		}
		message += "\n********** END OF PARKING **********";
		log.info(message);
	}
	
	/* Metodo per visualizzare la coda di partenza */
	private void printDepartureQueue() {
		if (departureQueue.size() <= 0) {
			log.info("No aircrafts in departure queue!");
		}
		int i = 0;
		String message = "";
		message += "\n********** DEPARTURE QUEUE **********";
		for (Aircraft a : departureQueue) {
			message += "\nPosition" + Integer.toString(i+1) + ": " + a.getFlightId();
			i++;
		}
		message += "\n********** END OF DEPARTURE QUEUE **********";
		log.info(message);
	}
	
	/* ========== METODI PER GESTIONE MESSAGGI ========== */	

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			/* ========== RICHIESTE ATTERRAGGIO ========== */
			.match(
	        	LandingRequest.class,
	            r -> {
	            	ActorRef sender = getSender();
	            	long timeForLanding = getTimeForLanding(sender);
            		landingQueue.add(new Aircraft(r.flightId,getSender()));
	            	sender.tell(new RespondLandingTime(r.requestId, r.flightId, timeForLanding), getSelf());
	            	log.info("Control tower {} computed {} seconds for aircraft {} landing", this.airportId, timeForLanding, r.flightId);
	            })
	        .match(
		        EmergencyLandingRequest.class,
	            r -> {
	            	ActorRef sender = getSender();
            		landingQueue.addFirst(new Aircraft(r.flightId,getSender()));
	            	sender.tell(new RespondLandingTime(r.requestId, r.flightId, 0), getSelf());
	            	log.info("Control tower {} computed 0 seconds (EMERGENCY LANDING) for aircraft {} landing", this.airportId, r.flightId);
            })
	        /* ========== CONFERME ATTERRAGGIO ========== */
	        .match(
	        	LandingConfirmation.class,
		            r -> {
		            	if(r.value) {
		            		if (r.confirmationType == 1) {
			            		log.info("Aircraft {} accepted to land", r.flightId);
			            		printLandingQueue(); // metodo di debug
		            		}
		            	}
		            	else {
		            		log.info("Aircraft {} changed course. It is now directed to another airport", r.flightId);
		            		deleteAircraft(r.flightId);
		            		printLandingQueue(); // metodo di debug
		            	}
		            	/* Se almeno una pista è libera si da il via all'atterraggio al primo della coda */
		            	Runway freeRunway = getFreeRunway();
		            	if (freeRunway != null)
		            		startLanding(getSender(),freeRunway);
		            		//startLanding(freeRunway);
		            })
	        .match(
        		EmergencyLandingConfirmation.class,
	            r -> {
	            	if(r.value) {
	            		log.info("EMEREGNCY LANDING for aircraft {} confirmed", r.flightId);
	            		informAircrafts();
	            		printLandingQueue(); // metodo di debug
	            	}
	            	else {
	            		log.info("Aircraft {} changed course. It is now directed to another airport", r.flightId);
            			deleteAircraft(r.flightId);
	            		printLandingQueue(); // metodo di debug
	            	}
	            })
	        /* ========== CHIUSURA FASE ATTERRAGGIO ========== */
	        .match(
        		LandingComplete.class,
	            r -> {
	            	closeLandingPhase(r.runway.getAircraftInRunway(), r.runway);
	            	printParking();
	            })
	        .build();	    
	  }
}
