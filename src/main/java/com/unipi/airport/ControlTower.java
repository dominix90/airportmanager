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
	private int numberOfAircrafts;
	private LinkedList<Aircraft> landingQueue;
	private LinkedList<Aircraft> emergencyLandingQueue;
	private LinkedList<Aircraft> departureQueue;
	private LinkedList<Aircraft> parking;
	private LinkedList<Aircraft> emergencyParking;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */	

	public ControlTower(String airportId, Runway[] runways) {
	    this.airportId = airportId;
	    this.runways = runways;
	    this.numberOfAircrafts = 0;
	    this.landingQueue = new LinkedList<Aircraft>();
	    this.emergencyLandingQueue = new LinkedList<Aircraft>();
	    this.departureQueue = new LinkedList<Aircraft>();
	    this.parking = new LinkedList<Aircraft>();
	    this.emergencyParking = new LinkedList<Aircraft>();
	}

	public static Props props(String airportId, Runway[] runways) {
	    return Props.create(ControlTower.class, () -> new ControlTower(airportId, runways));
	}
	  
	@Override
	public void preStart() {
		if(Parameters.logVerbose) log.info("ControlTower actor {} started", airportId);
	}

	@Override
	public void postStop() {
		if(Parameters.logVerbose) log.info("ControlTower actor {} stopped", airportId);
	}

	/* ========== METODI PER GESTIONE TEMPI NELLE CODE ========== */	
	  
	/* Metodo per il calcolo dei tempi d'atterraggio */
	private long getTimeForLanding(String flightId) {
		long time = 0;
		int RO = 0; //Runway occupancy: se tutte le piste sono occupate questo deve essere 1
		
		if ( getFreeRunway() == null )
			RO = 1;
		
		int flightIndex = -1;		
		for( Aircraft a : landingQueue ) {
			if(a.getFlightId()==flightId) {
				flightIndex = landingQueue.indexOf(a);
				break;
			}
		}
		
		int landingQueueSize = flightIndex + emergencyLandingQueue.size();
		time = Math.round(Parameters.meanLandingTime) * (landingQueueSize+RO) / runways.length;
		  
		return time;
	}
	
	/* Metodo per il calcolo dei tempi d'atterraggio di emergenza */
	private long getTimeForEmergencyLanding(String flightId) {
		long time = 0;
		int RO = 0; //Runway occupancy: se tutte le piste sono occupate questo deve essere 1
		
		if ( getFreeRunway() == null )
			RO = 1;
		
		int flightIndex = -1;		
		for( Aircraft a : emergencyLandingQueue ) {
			if(a.getFlightId()==flightId) {
				flightIndex = emergencyLandingQueue.indexOf(a);
				break;
			}
		}
		
		int queueSize = flightIndex;
		time = Math.round(Parameters.meanLandingTime) * (queueSize+RO) / runways.length;
		  
		return time;
	}
	
	/* Metodo per il calcolo dei tempi di decollo */
	private long getTimeForTakingOff(String flightId) {
		long time = 0;
		int RO = 0; //Runway occupancy: se tutte le piste sono occupate questo deve essere 1
		
		if ( getFreeRunway() == null )
			RO = 1;
		
		int flightIndex = -1;		
		for( Aircraft a : departureQueue ) {
			if(a.getFlightId()==flightId) {
				flightIndex = departureQueue.indexOf(a);
				break;
			}
		}
		
		int landingQueueSize = landingQueue.size() + emergencyLandingQueue.size();
		int departureQueueSize = flightIndex;
		time = ( (Math.round(Parameters.meanTakeOffTime)*departureQueueSize) + (Math.round(Parameters.meanLandingTime)*(landingQueueSize+RO)) ) / runways.length;
		  
		return time;
	}
	
	/* ========== METODI PER RIMOZIONE AEREI DALLE STRUTTURE DATI ========== */	
	
	/* Metodo per la gestione dei casi in cui un aereo decide di abbandonare 
	 * la coda di atterraggio
	 */
	private boolean deleteAircraft(String flightId) {
		/*
		 * for( Aircraft a : emergencyLandingQueue ) { if(a.getFlightId()==flightId) {
		 * emergencyLandingQueue.remove(a); return; } }
		 */
		
		for( Aircraft a : landingQueue ) {
			if(a.getFlightId()==flightId) {
				landingQueue.remove(a);
				return true;
			}
		}
		return false;
	}
	
	/* Metodo per liberare il posto occupato da Aircraft nel parcheggio */
	private void freeParking(String flightId) {
		for( Aircraft a : emergencyParking ) {
			if(a.getFlightId()==flightId) {
				emergencyParking.remove(a);
				return;
			}
		}
		
		for( Aircraft a : parking ) {
			if(a.getFlightId()==flightId) {
				parking.remove(a);
				return;
			}
		}
	}
	
	/* ========== METODI PER GESTIONE ATTERRAGGIO E DECOLLO ========== */	
	
	/* Metodo per avviare la fase di atterraggio */
	private void startLanding(Runway runway) {
		if (emergencyLandingQueue.size() > 0) {
			Aircraft landingAircraft = emergencyLandingQueue.removeFirst();
			if(Parameters.logVerbose) log.info("Informing aircraft {} that it can start landing", landingAircraft.getFlightId());
			landingAircraft.getAircraftActor().tell(new StartLanding(runway, landingAircraft.getFlightId()), getSelf());
			// La pista va settata occupata
			runway.setAircraftInRunway(landingAircraft);
			runway.setStatus("OCCUPIED");
		} else if (landingQueue.size() > 0) {
			Aircraft landingAircraft = landingQueue.removeFirst();
			if(Parameters.logVerbose) log.info("Informing aircraft {} that it can start landing", landingAircraft.getFlightId());
			landingAircraft.getAircraftActor().tell(new StartLanding(runway, landingAircraft.getFlightId()), getSelf());
			// La pista va settata occupata
			runway.setAircraftInRunway(landingAircraft);
			runway.setStatus("OCCUPIED");
		} else {
			/* Se le code di atterraggio sono vuote si controlla quella di partenza */
			startDeparture(runway);
		}
	}
	
	/* Metodo per chiudere la fase di atterraggio */
	private void closeLandingPhase(Aircraft landedAircraft, Runway runway) {
		
		if (landedAircraft.getEmergencyState()) {
			emergencyParking.add(landedAircraft);
			if(Parameters.logVerbose) log.info("Aircraft {} is now parked in the emergencyParking", landedAircraft.getFlightId());	
		} else {
			parking.add(landedAircraft);
			if(Parameters.logVerbose) log.info("Aircraft {} is now parked in the parking", landedAircraft.getFlightId());
		}
		
		// La pista va settata libera	
		runway.setAircraftInRunway(null);
		runway.setStatus("FREE");	
			
	}
	  

	
	/* Metodo per avviare la fase di atterraggio */
	private void startDeparture(Runway runway) {
		if (departureQueue.size() > 0) {
			Aircraft departureAircraft = departureQueue.removeFirst();
			if(Parameters.logVerbose) log.info("Informing aircraft {} that it can start departure", departureAircraft.getFlightId());
			/* Qui va inviato il messaggio per avviare la fase di rullaggio*/
			departureAircraft.getAircraftActor().tell(new StartTakeOff(runway, departureAircraft.getFlightId()), getSelf());
			/* Qui va liberato il parcheggio */
			freeParking(departureAircraft.getFlightId());
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
	
	/* ========== METODI PER INFORMARE AEREI NELLE CODE SUI TEMPI ========== */	
	
	/* Aggiornamento dei tempi di decollo */
	private void informDepartureAircrafts () {
		if(Parameters.logVerbose) log.info("Informing departure aircrafts about updated times");
		for (Aircraft a : departureQueue) {
			a.getAircraftActor().tell(new UpdateDepartureTime(a.getFlightId(), getTimeForTakingOff(a.getFlightId())), getSelf());
		}
		if(Parameters.logVerbose) log.info("Departure aircrafts informed about updated times");
	}
	
	/* Aggiornamento dei tempi di atterraggio */
	private void informAllAircrafts () {
		if(Parameters.logVerbose) log.info("Informing aircrafts in emergency about updated times");
		for (Aircraft a : emergencyLandingQueue) {
			a.getAircraftActor().tell(new UpdateLandingTime(a.getFlightId(), getTimeForEmergencyLanding(a.getFlightId())), getSelf());
		}
		if(Parameters.logVerbose) log.info("Aircrafts in emergency informed about updated times");
		informLandingDepartureAircrafts();
	}
	
	/* Aggiornamento dei tempi di atterraggio di emergenza */
	private void informLandingDepartureAircrafts () {
		if(Parameters.logVerbose) log.info("Informing landing aircrafts about updated times");
		for (Aircraft a : landingQueue) {
			a.getAircraftActor().tell(new UpdateLandingTime(a.getFlightId(), getTimeForLanding(a.getFlightId())), getSelf());
		}
		if(Parameters.logVerbose) log.info("Landing aircrafts informed about updated times");
		informDepartureAircrafts();
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
		String message;
		message = "";
		message += "\n********** LANDING QUEUE **********";
		if (landingQueue.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : landingQueue) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId();
				i++;
			}
		}
		message += "\n********** END OF LANDING QUEUE **********";
		log.info(message);
	}
	
	/* Metodo per visualizzare la coda di atterraggio di emergenza */
	private void printEmergencyLandingQueue() {
		String message;
		message = "";
		message += "\n********** EMERGENCY LANDING QUEUE **********";
		if (emergencyLandingQueue.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : emergencyLandingQueue) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId();
				i++;
			}
		}
		message += "\n********** END OF EMERGENCY LANDING QUEUE **********";
		log.info(message);
	}
	
	/* Metodo per visualizzare il parcheggio */
	private void printParking() {
		String message;
		message = "";
		message += "\n********** PARKING **********";
		if (parking.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : parking) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId();
				i++;
			}
		}
		message += "\n********** END OF PARKING **********";
		log.info(message);
	}
	
	/* Metodo per visualizzare la coda di partenza */
	private void printDepartureQueue() {
		String message;
		message = "";
		message += "\n********** DEPARTURE QUEUE **********";
		if (departureQueue.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : departureQueue) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId();
				i++;
			}
		}
		message += "\n********** END OF DEPARTURE QUEUE **********";
		log.info(message);
	}
	
	/* Metodo per visualizzare lo stato delle piste */
	private void printRunways() {
		String message = "";
		message += "\n********** RUNWAYS STATE **********";
		
		for (int i = 0; i < runways.length; i++) {
			message += "\nRunway " + Integer.toString(i+1) + ": " + runways[i].getStatus();
			if (!runways[i].isFree())
				message+= " by " + runways[i].getAircraftInRunway().getFlightId();
		}
		
		message += "\n********** END OF RUNWAYS STATE **********";
		log.info(message);
	}
	
	private void printAirportState() {
		String message;
		message = "";
		
		message += "\n********** NUMBER OF AIRCRAFTS **********";
		message += "\n" + numberOfAircrafts;
		
		message += "\n********** EMERGENCY LANDING QUEUE **********";
		if (emergencyLandingQueue.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : emergencyLandingQueue) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId() + " : " + getTimeForEmergencyLanding(a.getFlightId()) + " " + Parameters.timeUnit;
				i++;
			}
		}
		//message += "\n********** END OF EMERGENCY LANDING QUEUE **********";
		
		message += "\n********** LANDING QUEUE **********";
		if (landingQueue.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : landingQueue) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId() + " : " + getTimeForLanding(a.getFlightId()) + " " + Parameters.timeUnit;
				i++;
			}
		}
		//message += "\n********** END OF LANDING QUEUE **********";
		
		message += "\n********** RUNWAYS STATE **********";
		for (int i = 0; i < runways.length; i++) {
			message += "\nRunway " + Integer.toString(i+1) + ": " + runways[i].getStatus();
			if (!runways[i].isFree())
				message+= " by " + runways[i].getAircraftInRunway().getFlightId();
		}
		//message += "\n********** END OF RUNWAYS STATE **********";
		
		message += "\n********** EMERGENCY PARKING **********";
		if (emergencyParking.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : emergencyParking) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId();
				i++;
			}
		}
		//message += "\n********** END OF EMERGENCY PARKING **********";
		
		message += "\n********** PARKING **********";
		if (parking.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : parking) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId();
				i++;
			}
		}
		//message += "\n********** END OF PARKING **********";
		
		message += "\n********** DEPARTURE QUEUE **********";
		if (departureQueue.size() <= 0) {
			message+="\nEmpty";
		}
		else {
			int i = 0;
			for (Aircraft a : departureQueue) {
				message += "\nPosition" + Integer.toString(i + 1) + ": " + a.getFlightId() + " : " + getTimeForTakingOff(a.getFlightId()) + " " + Parameters.timeUnit;
				i++;
			}
		}
		//message += "\n********** END OF DEPARTURE QUEUE **********";
		
		message += "\n********************************************";
		
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
	            	if(Parameters.logVerbose) log.info("Landing request received from aircraft {}", r.flightId);
	            	if ( numberOfAircrafts < Parameters.parkingSize ) {
	            		ActorRef sender = getSender();
		            	landingQueue.add(new Aircraft(r.flightId,getSender(),false));
	            		++numberOfAircrafts; //incremento numero di aerei nell'aeroporto
		            	long timeForLanding = getTimeForLanding(r.flightId);
		            	sender.tell(new RespondLandingTime(r.flightId, timeForLanding), getSelf());
		            	if(Parameters.logVerbose) log.info("Control tower {} computed {} {} for aircraft {} landing", this.airportId, timeForLanding, Parameters.timeUnit, r.flightId);
		            	/* Se almeno una pista è libera si da il via all'atterraggio al primo della coda */
		            	//printRunways();
		            	//Runway freeRunway = getFreeRunway();
		            	//if (freeRunway != null)
		            	//	startLanding(freeRunway);
		            	//if(Parameters.logAirportState) printAirportState();
	            	} else {
	            		if(Parameters.logVerbose) log.info("The airport is full! Landing denial for aircraft {}", r.flightId);
		            	getSender().tell(new LandingDenial(r.flightId), getSelf());
	            	}	       
	            })
	        .match(
		        EmergencyLandingRequest.class,
	            r -> {
	            	if(Parameters.logVerbose) log.info("Emergency landing request received from aircraft {}", r.flightId);
	            	ActorRef sender = getSender();
	            	emergencyLandingQueue.add(new Aircraft(r.flightId,getSender(),true));
            		++numberOfAircrafts; //incremento numero di aerei nell'aeroporto
	            	long timeForEmergencyLanding = getTimeForEmergencyLanding(r.flightId);
	            	sender.tell(new RespondLandingTime(r.flightId, timeForEmergencyLanding), getSelf());
	            	if(Parameters.logVerbose) log.info("Control tower {} computed {} {} (EMERGENCY LANDING) for aircraft {} landing", this.airportId, timeForEmergencyLanding, Parameters.timeUnit, r.flightId);
	            	/* Se almeno una pista � libera si da il via all'atterraggio al primo della coda */
	            	//printRunways();
	            	//Runway freeRunway = getFreeRunway();
	            	//if (freeRunway != null)
	            	//	startLanding(freeRunway);
	            	//if(Parameters.logAirportState) printAirportState();
            })
	        /* ========== CONFERME ATTERRAGGIO ========== */
	        .match(
	        	LandingConfirmation.class,
		            r -> {
		            	if(r.value) {
		            		if(Parameters.logVerbose) log.info("Aircraft {} accepted to land", r.flightId);
			            	informDepartureAircrafts();
			            	if(Parameters.logAirportState) printAirportState();
			            	//printLandingQueue(); // metodo di debug
		            	}
		            	else {
		            		if(Parameters.logVerbose) log.info("Aircraft {} changed course. It is now directed to another airport", r.flightId);
		            		deleteAircraft(r.flightId);
		            		--numberOfAircrafts;
		            		//if(Parameters.logAirportState) printAirportState();
		            		//printLandingQueue(); // metodo di debug
		            	}
		            	
		            	/* Se almeno una pista � libera si da il via all'atterraggio al primo della coda */
		            	//printRunways();
		            	Runway freeRunway = getFreeRunway();
		            	if (freeRunway != null) {
		            		startLanding(freeRunway);
		            		if(Parameters.logAirportState) printAirportState();
		            	}
		            })	        
	        .match(
        		EmergencyLandingConfirmation.class,
	            r -> {
	            	if(r.value) {
	            		if(Parameters.logVerbose) log.info("EMERGENCY LANDING for aircraft {} confirmed", r.flightId);
	            		informLandingDepartureAircrafts();
	            		if(Parameters.logAirportState) printAirportState();
	            		//printEmergencyLandingQueue(); // metodo di debug
	            	}
	            	/* Se almeno una pista � libera si da il via all'atterraggio al primo della coda */
	            	//printRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null) {
	            		startLanding(freeRunway);
	            		if(Parameters.logAirportState) printAirportState();
	            	}
	            })
	        /* ========== AEREO DIVENTATO IN EMERGENZA ========== */
	        .match(
        		NowInEmergency.class,
	            r -> {
	            	if(Parameters.logVerbose) log.info("Aircraft {} is now in emergency state", r.flightId);
	            	ActorRef sender = getSender();
	            	//rimuovo l'aereo dalla coda di atterraggio (con emergency state false) e lo aggiungo alla coda di emergenza (con emergency state true)
	            	if( deleteAircraft(r.flightId) ) {
	            		//se l'aereo è nella coda di atterraggio lo sposto in quella di emergenza
		            	emergencyLandingQueue.add(new Aircraft(r.flightId,getSender(),true));
		            	long timeForEmergencyLanding = getTimeForEmergencyLanding(r.flightId);	            	
		            	sender.tell(new RespondLandingTime(r.flightId, timeForEmergencyLanding), getSelf());
		            	if(Parameters.logVerbose) log.info("Control tower {} computed {} {} (EMERGENCY LANDING) for aircraft {} landing", this.airportId, timeForEmergencyLanding, Parameters.timeUnit, r.flightId);
		            	
		            	if(Parameters.logAirportState) printAirportState();
		            	
		            	/* Se almeno una pista � libera si da il via all'atterraggio al primo della coda */
		            	//printRunways();
		            	//Runway freeRunway = getFreeRunway();
		            	//if (freeRunway != null) {
		            	//	startLanding(freeRunway);
		            	//	if(Parameters.logAirportState) printAirportState();
		            	//}
	            	}
	            	else {
	            		//altrimenti significa che l'aereo sta atterrando, quindi lo informo che non è in emergenza
	            		if(Parameters.logVerbose) log.info("Aircraft {} is already landing and it is not in emergency", r.flightId);
	            		sender.tell(new YouAreNotInEmergency(r.flightId), getSelf());
	            	}
	            	
	            })
	        /* ========== ATTERRAGGIO ========== */
	        .match(
        		Landing.class,
	            r -> {
	            	if(Parameters.logVerbose) log.info("Aircraft {} is now landing in runway {}", r.flightId, r.runway.getRunwayNumber());
	            })
	        /* ========== CHIUSURA FASE ATTERRAGGIO ========== */
	        .match(
        		LandingComplete.class,
	            r -> {
	            	if(Parameters.logVerbose) log.info("Aircraft {}: LANDED", r.flightId);
	            	closeLandingPhase(r.runway.getAircraftInRunway(), r.runway);
	            	//printParking();
	            	if(Parameters.logAirportState) printAirportState();
	            	/* Aggiorno tutti gli aerei in coda sugli orari di atterraggio */
	            	informAllAircrafts();
	            	/* Se almeno una pista � libera si da il via all'atterraggio al primo della coda */
	            	//printRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null) {
	            		startLanding(freeRunway);
	            		if(Parameters.logAirportState) printAirportState();
	            	}
	            })
	        /* ========== RICHIESTE DECOLLO ========== */
			.match(
				DepartureRequest.class,
	            r -> {
	            	if(Parameters.logVerbose) log.info("Departure request received from aircraft {}", r.flightId);
	            	ActorRef sender = getSender();
	            	departureQueue.add(new Aircraft(r.flightId,getSender(),r.inEmergency));
	            	long timeForDeparture = getTimeForTakingOff(r.flightId);
	            	sender.tell(new RespondDepartureTime(r.flightId, timeForDeparture), getSelf());
	            	if(Parameters.logVerbose) log.info("Control tower {} computed {} {} for aircraft {} departure", this.airportId, timeForDeparture, Parameters.timeUnit, r.flightId);
	            	//printDepartureQueue();
	            	if(Parameters.logAirportState) printAirportState();
	            	/* Se almeno una pista � libera si da il via all'atterraggio al primo della coda */
	            	//printRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null) {
	            		startLanding(freeRunway);
	            		if(Parameters.logAirportState) printAirportState();
	            	}
	            })
	        /* ========== DECOLLO ========== */
	        .match(
        		TakingOff.class,
	            r -> {
	            	if(Parameters.logVerbose) log.info("Aircraft {} is now taking off from runway {}", r.flightId, r.runway.getRunwayNumber());
	            })
	        /* ========== CHIUSURA FASE DECOLLO ========== */
	        .match(
        		TakeOffComplete.class,
	            r -> {
	            	if(Parameters.logVerbose) log.info("Aircraft {}: DEPARTED", r.flightId);
	            	closeDeparturePhase(r.runway);
	            	--numberOfAircrafts;
	            	//printParking();
	            	if(Parameters.logAirportState) printAirportState();
	            	/* Aggiorno tutti gli aerei in coda sugli orari di atterraggio */
	            	informAllAircrafts();
	            	/* Se almeno una pista � libera si da il via all'atterraggio al primo della coda */
	            	//printRunways();
	            	Runway freeRunway = getFreeRunway();
	            	if (freeRunway != null) {
	            		startLanding(freeRunway);
	            		if(Parameters.logAirportState) printAirportState();
	            	}
	            })
	        .build();	    
	  }
}
