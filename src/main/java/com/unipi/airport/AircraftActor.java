package com.unipi.airport;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;

import org.apache.commons.math3.distribution.*;

import com.unipi.utils.Parameters;
import com.unipi.utils.Messages.*;

public class AircraftActor extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final String flightId;
  private long fuel;
  private boolean inEmergency;
  private Scheduler scheduler = getContext().getSystem().getScheduler();
  private Cancellable fuelScheduling;
  private long previousFuelCheckTime;
  private ActorRef controlTower;

  public AircraftActor(String flightId, long fuel, boolean inEmergency) {
    this.flightId = flightId;
    this.fuel = fuel;
    this.inEmergency = inEmergency;
    this.controlTower = null;
  }

  public static Props props(String flightId, long fuel, boolean inEmergency) {
    return Props.create(AircraftActor.class, () -> new AircraftActor(flightId, fuel, inEmergency));
  }

  @Override
  public void preStart() {
	  if(Parameters.logVerbose) log.info("Aircraft actor {} started", flightId);
	  /* Viene schedulato un messaggio da ricevere per il consumo di carburante se l'aereo non è in emergenza */
	  if(!inEmergency) {
	  	fuelScheduling = scheduler.scheduleOnce(Duration.ofMillis(1000), getSelf(), new FuelReserve(flightId), getContext().getSystem().dispatcher(), null);
	  	previousFuelCheckTime = System.currentTimeMillis();
	  }
  }

  @Override
  public void postStop() {
	  if(Parameters.logVerbose) log.info("Aircraft actor {} stopped", flightId);
  }
  
  public String getFlightId() {
	  return this.flightId;
  }
  
  /* Metodo per il controllo del carburante */
  private boolean sufficientFuel(long timeForNextLanding) {
	  if (timeForNextLanding <= fuel)
		  return true;
	  return false;
  }
  
  /* ========== TEMPI DI DECOLLO, ATTERRAGGIO E PARCHEGGIO ========== */
  /* Metodo per ottenimento durata atterraggio e decollo */
  public double getRunwayOccupation() {
		double time = 0.000;
		
		LogNormalDistribution lnd = new LogNormalDistribution(2.000, 0.200);
		time = lnd.sample();
		
		return time * 1000;
  }
  
  /* Metodo per ottenimento durata parcheggio */
  public double getParkingOccupation() {
		double time = 0.000;
		
		LogNormalDistribution lnd = new LogNormalDistribution(3.000, 0.300);
		time = lnd.sample();
		
		return time * 1000;
	}

  @Override
  public Receive createReceive() {
    return receiveBuilder()
    		/* ========== CARBURANTE ========== */
		.match(
			FuelReserve.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		long actualFuelCheckTime = System.currentTimeMillis();
            		long consumedFuel = actualFuelCheckTime - previousFuelCheckTime;
            		previousFuelCheckTime = actualFuelCheckTime;
            		fuel = fuel - consumedFuel;
            		if (fuel <= 0) {
            			/* Se il carburante � non positivo allora l'aereo usa il carburante d'emergenza 
            			 * e diventa in stato di emergenza.
            			 * Il timer viene annullato.
            			 */
            			inEmergency = true;
            			fuelScheduling.cancel();
	            		if (controlTower != null)
	            			controlTower.tell(new NowInEmergency(flightId), getSelf());
            		} else {
            			/* Viene schedulato un messaggio da ricevere per il consumo di carburante */
            			fuelScheduling = scheduler.scheduleOnce(Duration.ofMillis(1000), getSelf(), new FuelReserve(flightId), getContext().getSystem().dispatcher(), null);
            		}
            	}
            })	
    	/* ========== RICHIESTA DI ATTERRAGGIO ========== */
		.match(
			StartLandingRequest.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		if (inEmergency)
            			getSender().tell(new EmergencyLandingRequest(flightId), getSelf());
            		else
            			getSender().tell(new LandingRequest(flightId), getSelf());
            		if(Parameters.logVerbose) log.info("Landing request by aircraft {} to control tower", this.flightId);  
	            	controlTower = r.controlTower;
            	}
            })
		.match(
			LandingDenial.class,
	            r -> {
	            	if (this.flightId.equals(r.flightId)) {
		            	getContext().stop(getSelf());
	            	}
	            })
    	/* ========== TEMPI DI ATTERRAGGIO ========== */
        .match(
    		RespondLandingTime.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		if (inEmergency) {
            			getSender().tell(new EmergencyLandingConfirmation(true, flightId), getSelf());
            			if(Parameters.logVerbose) log.info("Landing confirmation sent by aircraft {} to control tower", this.flightId);
            		}
            		else {
            			boolean landingConfirmation = sufficientFuel(r.timeForLanding);
            			getSender().tell(new LandingConfirmation(landingConfirmation, flightId), getSelf());
            			if(Parameters.logVerbose) log.info("Landing confirmation sent by aircraft {} to control tower", this.flightId);
		      			if (!landingConfirmation)
		      				getContext().stop(getSelf());
            		}
            	}
            })
        .match(
    		UpdateLandingTime.class, //da modificare, deve essere sincrono
            r -> {
            	if (this.flightId.equals(r.flightId)) {            		
            		//if (inEmergency) {
            		//	getSender().tell(new EmergencyLandingConfirmation(true, flightId, 2), getSelf());
            		//	log.info("Landing confirmed by aircraft {} after landing queue update", this.flightId);
            		//}
            		//else {
            		//	boolean landingConfirmation = sufficientFuel(r.timeForLanding);
		            //	if (!landingConfirmation) {
		            //		getSender().tell(new LandingConfirmation(landingConfirmation, flightId, 2), getSelf());	            	
		            //		log.info("Landing canceled by aircraft {} after landing queue update", this.flightId);
		      		//		getContext().stop(getSelf());
		            //	}
            		//}
            	}
            })
        /* ========== INIZIO ATTERRAGGIO ========== */
        .match(
    		StartLanding.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		/* Viene annullato il timer di consumo carburante se ancora attivo */
            		if (!inEmergency)	
            			fuelScheduling.cancel();
            		getSender().tell(new Landing(r.runway, r.flightId), getSelf());
            		/* Viene schedulato un messaggio da ricevere alla fine dell'atterraggio */
            		long runwayOccupation = Math.round(getRunwayOccupation());
            		scheduler.scheduleOnce(Duration.ofMillis(runwayOccupation), getSelf(), new InLandingState(r.runway, flightId, getSender()), getContext().getSystem().dispatcher(), null);
            	}
            })
        /* ========== ATTERRAGGIO ========== */
        .match(
        	InLandingState.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new LandingComplete(r.runway, r.flightId), getSelf());
            		/* Viene schedulato un messaggio da ricevere quando l'aereo dovr� ripartire */
            		scheduler.scheduleOnce(Duration.ofMillis(Math.round(getParkingOccupation())), getSelf(), new StartDeparturePhase(flightId, r.controlTower), getContext().getSystem().dispatcher(), null);
            	}
            })
        /* ========== RICHIESTA DI DECOLLO ========== */
        .match(
            StartDeparturePhase.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new DepartureRequest(r.flightId,inEmergency), getSelf());
            		if(Parameters.logVerbose) log.info("Aircraft {} requested departure", this.flightId);
            	}
            })
        /* ========== TEMPI DI DECOLLO ========== */
        .match(
    		RespondDepartureTime.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		
            	}
            })
        .match(
    		UpdateDepartureTime.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		
            	}
            })
        /* ========== INIZIO FASE DI DECOLLO ========== */
        .match(
        	StartTakeOff.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		getSender().tell(new TakingOff(r.runway, r.flightId), getSelf());
            		/* Viene schedulato un messaggio da ricevere alla fine del decollo */
	            	scheduler.scheduleOnce(Duration.ofMillis(Math.round(getRunwayOccupation())), getSelf(), new InTakeOffState(r.runway, flightId, getSender()), getContext().getSystem().dispatcher(), null);
            	}
            })
        /* ========== DECOLLO ========== */
        .match(
        	InTakeOffState.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new TakeOffComplete(r.runway, r.flightId), getSelf());
            		/* L'aereo abbandona il sistema */
            		getContext().stop(getSelf());
            	}
            })
        .build();
  }
}
