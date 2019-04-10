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
	private Aircraft[] parking;
	private Aircraft[] emergencyParking;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */	

	public ControlTower(String airportId, Runway[] runways) {
	    this.airportId = airportId;
	    this.runways = runways;
	    this.landingQueue = new LinkedList<Aircraft>();
	    this.departureQueue = new LinkedList<Aircraft>();
	    this.parking = new Aircraft[Parameters.parkingSize];
	    this.emergencyParking = new Aircraft[Parameters.emergencyParkingSize];
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
		Deque<Aircraft> landingQueueTemp = new LinkedList<Aircraft>(); //questa variabile conterr√† la coda rigenerata
		Aircraft aircraftTemp; //questa variabile conterr√† l'aereo correntemente in esame
		
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
		if (landingQueue.size() > 0) {
			Aircraft landingAircraft = landingQueue.removeFirst();
			log.info("Informing aircraft {} that it can start landing", landingAircraft.getFlightId());
			landingAircraft.getAircraftActor().tell(new StartLanding(runway, landingAircraft.getFlightId()), getSelf());
			// La pista va settata occupata
			runway.setAircraftInRunway(landingAircraft);
			runway.setStatus("OCCUPIED");
		} else {
			/* Se la coda di atterraggio Ë vuota si controlla quella di partenza */
			startDeparture(runway);
		}
	}
	
	/* Metodo per chiudere la fase di atterraggio */
	private void closeLandingPhase(Aircraft landedAircraft, Runway runway) {
		int parkNumber = -1; //il numero del parcheggio del nuovo aereo
		
		if (landedAircraft.getEmergencyState()) {
			for (int i = 0; i < emergencyParking.length; i++) {
				if (emergencyParking[i] == null) {
					parkNumber = i;
					break;
				}
			}
			
			if (parkNumber < 0) {
				log.info("No free places in parking!");
				return;
			} else {
				emergencyParking[parkNumber] = landedAircraft;
				log.info("Aircraft {} is now parked at place n.{} of the emergencyParking", landedAircraft.getFlightId(), parkNumber + 1);
				// La pista va settata libera	
				runway.setAircraftInRunway(null);
				runway.setStatus("FREE");			
			}	
		} else {
			for (int i = 0; i < parking.length; i++) {
				if (parking[i] == null) {
					parkNumber = i;
					break;
				}
			}
			
			if (parkNumber < 0) {
				log.info("No free places in parking!");
				return;
			} else {
				parking[parkNumber] = landedAircraft;
				log.info("Aircraft {} is now parked at place n.{}", landedAircraft.getFlightId(), parkNumber + 1);
				// La pista va settata libera	
				runway.setAircraftInRunway(null);
				runway.setStatus("FREE");			
			}	
		}
			
	}
	  
	/* Metodo per il calcolo dei tempi di decollo */
	private long getTimeForTakingOff(ActorRef aircraft) {
		long time = 0;
		  
		int departureQueueSize = departureQueue.size();
		int landingQueueSize = landingQueue.size();
		time = Parameters.averageRunwayOccupation * (landingQueueSize + departureQueueSize) / runways.length;
		  
		return time;
	}
	
	/* Metodo per avviare la fase di atterraggio */
	private void startDeparture(Runway runway) {
		if (departureQueue.size() > 0) {
			Aircraft departureAircraft = departureQueue.removeFirst();
			log.info("Informing aircraft {} that it can start departure", departureAircraft.getFlightId());
			/* Qui va inviato il messaggio per avviare la fase di rullaggio*/
			departureAircraft.getAircraftActor().tell(new StartTakeOff(runway, departureAircraft.getFlightId()), getSelf());
			/* Qui va liberato il parcheggio */
			freeParking(departureAircraft);
			// La pista va settata occupata
			runway.setAircraftInRunway(departureAircraft);
			runway.setStatus("OCCUPIED");
		}
	}
	
	/* Metodo per chiudere la fase di atterraggio */
	private void closeDeparturePhase(Runway runway) {
		
		// La pista va settata libera
		runway.setAircraftInRunway(null);
		runway.setStatus("FREE");
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
		/* QUESTO VA MODIFICATO */
		if (isParkingEmpty()) {
			log.info("No aircrafts in parking!");
			return;
		} else {
			String message = "";
			message += "\n********** PARKING **********";
			for (int i = 0; i < parking.length; i++) {
				message += "\n----------";
				if (parking[i] == null)
					message += "\n|" + Integer.toString(i+1) + ": EMPTY";
				else
					message += "\n|" + Integer.toString(i+1) + ": AIRCRAFT " + parking[i].getFlightId();
				message += "\n----------";
			}
			message += "\n********** END OF PARKING **********";
			log.info(message);
		}
	}
	
	/* Metodo per controllare se ci sono posti liberi nel parcheggio */
	private boolean isThereFreeParking() {
		for (int i = 0; i < parking.length; i++) {
			if (parking[i] == null)
				return true;
		}
		return false;
	}
	
	/* Metodo per controllare se il parcheggio Ë vuoto */
	private boolean isParkingEmpty() {
		for (int i = 0; i < parking.length; i++) {
			if (parking[i] != null)
				return false;
		}
		return true;
	}
	
	/* Metodo per liberare il posto occupato da Aircraft nel parcheggio */
	private void freeParking(Aircraft a) {
		if (a.getEmergencyState()) {
			for (int i = 0; i < emergencyParking.length; i++) {
				if (emergencyParking[i] == null)
					continue;
				if (emergencyParking[i].getFlightId().equals(a.getFlightId())) {
					emergencyParking[i] = null;
					return;
				}
			}
		} else {
			for (int i = 0; i < parking.length; i++) {
				if (parking[i] == null)
					continue;
				if (parking[i].getFlightId().equals(a.getFlightId())) {
					parking[i] = null;
					return;
				}
			}
		}
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
	
	/* Metodo per visualizzare lo stato delle piste */
	private void checkRunways() {
		String message = "";
		message += "\n********** RUNWAYS STATE **********";
		
		for (int i = 0; i < runways.length; i++) {
			message += "\nRunway " + Integer.toString(i+1) + ": " + runways[i].getStatus();
		}
		
		message += "\n********** END OF RUNWAYS STATE **********";
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
	            	if (isThereFreeParking()) {
	            		ActorRef sender = getSender();
		            	long timeForLanding = getTimeForLanding(sender);
	            		landingQueue.add(new Aircraft(r.flightId,getSender(),false));
		            	sender.tell(new RespondLandingTime(r.flightId, timeForLanding), getSelf());
		            	log.info("Control tower {} computed {} seconds for aircraft {} landing", this.airportId, timeForLanding, r.flightId);
		            	/* Se almeno una pista Ë libera si da il via all'atterraggio al primo della coda */
		            	checkRunways();
		            	Runway freeRunway = getFreeRunway();
		            	if (freeRunway != null)
		            		startLanding(getSender(),freeRunway);
	            	} else {
	            		log.info("Parking is full! Landing denial for aircraft {}", r.flightId);
		            	getSender().tell(new LandingDenial(r.flightId), getSelf());
	            	}	            	
	            })
	        .match(
		        EmergencyLandingRequest.class,
	            r -> {
	            	ActorRef sender = getSender();
            		landingQueue.addFirst(new Aircraft(r.flightId,getSender(),true));
	            	sender.tell(new RespondLandingTime(r.flightId, 0), getSelf());
	            	log.info("Control tower {} computed 0 seconds (EMERGENCY LANDING) for aircraft {} landing", this.airportId, r.flightId);
	            	/* Se almeno una pista Ë libera si da il via all'atterraggio al primo della coda */
	            	checkRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null)
	            		startLanding(getSender(),freeRunway);
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
		            	/* Se almeno una pista Ë libera si da il via all'atterraggio al primo della coda */
		            	checkRunways();
		            	Runway freeRunway = getFreeRunway();
		            	if (freeRunway != null)
		            		startLanding(getSender(),freeRunway);
		            })	        
	        .match(
        		EmergencyLandingConfirmation.class,
	            r -> {
	            	if(r.value) {
	            		if (r.confirmationType == 1) {
		            		log.info("EMERGENCY LANDING for aircraft {} confirmed", r.flightId);
		            		informAircrafts();
		            		printLandingQueue(); // metodo di debug
	            		}
	            	}
	            	else {
	            		log.info("Aircraft {} changed course. It is now directed to another airport", r.flightId);
            			deleteAircraft(r.flightId);
	            		printLandingQueue(); // metodo di debug
	            	}
	            	/* Se almeno una pista Ë libera si da il via all'atterraggio al primo della coda */
	            	checkRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null)
	            		startLanding(getSender(),freeRunway);
	            })
	        /* ========== ATTERRAGGIO ========== */
	        .match(
        		Landing.class,
	            r -> {
	            	log.info("Aircraft {} is now landing in runway {}", r.flightId, r.runway.getRunwayNumber());
	            })
	        /* ========== CHIUSURA FASE ATTERRAGGIO ========== */
	        .match(
        		LandingComplete.class,
	            r -> {
	            	log.info("Aircraft {}: LANDED", r.flightId);
	            	closeLandingPhase(r.runway.getAircraftInRunway(), r.runway);
	            	printParking();
	            	/* Aggiorno tutti gli aerei in coda sugli orari di atterraggio */
	            	informAircrafts();
	            	/* Se almeno una pista Ë libera si da il via all'atterraggio al primo della coda */
	            	checkRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null)
	            		startLanding(getSender(),freeRunway);
	            })
	        /* ========== RICHIESTE DECOLLO ========== */
			.match(
				DepartureRequest.class,
	            r -> {
	            	ActorRef sender = getSender();
	            	long timeForDeparture = getTimeForTakingOff(sender);
            		departureQueue.add(new Aircraft(r.flightId,getSender(),r.inEmergency));
	            	sender.tell(new RespondDepartureTime(r.flightId, timeForDeparture), getSelf());
	            	log.info("Control tower {} computed {} seconds for aircraft {} departure", this.airportId, timeForDeparture, r.flightId);
	            	printDepartureQueue();
	            	/* Se almeno una pista Ë libera si da il via all'atterraggio al primo della coda */
	            	checkRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null)
	            		startLanding(getSender(),freeRunway);
	            })
	        /* ========== DECOLLO ========== */
	        .match(
        		TakingOff.class,
	            r -> {
	            	log.info("Aircraft {} is now taking off from runway {}", r.flightId, r.runway.getRunwayNumber());
	            })
	        /* ========== CHIUSURA FASE DECOLLO ========== */
	        .match(
        		TakeOffComplete.class,
	            r -> {
	            	log.info("Aircraft {}: DEPARTED", r.flightId);
	            	closeDeparturePhase(r.runway);
	            	printParking();
	            	/* Aggiorno tutti gli aerei in coda sugli orari di atterraggio */
	            	informAircrafts();
	            	/* Se almeno una pista Ë libera si da il via all'atterraggio al primo della coda */
	            	checkRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null)
	            		startLanding(getSender(),freeRunway);
	            })
	        .build();	    
	  }
}
