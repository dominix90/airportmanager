package com.unipi.airport;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;

import org.apache.commons.math3.distribution.*;

import com.unipi.utils.Messages.*;

public class AircraftActor extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final String flightId;
  private final long fuel;
  private Scheduler scheduler = getContext().getSystem().getScheduler();

  public AircraftActor(String flightId, long fuel) {
    this.flightId = flightId;
    this.fuel = fuel;
  }

  public static Props props(String flightId, long fuel) {
    return Props.create(AircraftActor.class, () -> new AircraftActor(flightId, fuel));
  }

  @Override
  public void preStart() {
    log.info("Aircraft actor {} started", flightId);
  }

  @Override
  public void postStop() {
    log.info("Aircraft actor {} stopped", flightId);
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
		
		LogNormalDistribution lnd = new LogNormalDistribution(5.000, 0.500);
		time = lnd.sample();
		
		return time * 1000;
	}
  
  /* Metodo per ottenimento durata parcheggio */
  public double getParkingOccupation() {
		double time = 0.000;
		
		LogNormalDistribution lnd = new LogNormalDistribution(20.000, 2.000);
		time = lnd.sample();
		
		return time * 1000;
	}

  @Override
  public Receive createReceive() {
    return receiveBuilder()
    	/* ========== RICHIESTA DI ATTERRAGGIO ========== */
		.match(
			StartLandingPhase.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
	            	getSender().tell(new LandingRequest(flightId), getSelf());
	            	log.info("Landing request by aircraft {} to control tower", this.flightId);
            	}
            })
    	/* ========== TEMPI DI ATTERRAGGIO ========== */
        .match(
    		RespondLandingTime.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		boolean landingConfirmation = sufficientFuel(r.timeForLanding);
	            	getSender().tell(new LandingConfirmation(landingConfirmation, flightId, 1), getSelf());
	      			log.info("Landing confirmation sent by aircraft {} to control tower", this.flightId);
	      			if (!landingConfirmation)
	      				getContext().stop(getSelf());
            	}
            })
        .match(
    		UpdateLandingTime.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		boolean landingConfirmation = sufficientFuel(r.timeForLanding);
	            	getSender().tell(new LandingConfirmation(landingConfirmation, flightId, 2), getSelf());
	            	log.info("Landing confirmed by aircraft {} after landing queue update", this.flightId);
	            	if (!landingConfirmation)
	      				getContext().stop(getSelf());
            	}
            })
        /* ========== INIZIO FASE DI ATTERRAGGIO ========== */
        .match(
    		StartLanding.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		getSender().tell(new Landing(r.runway, r.flightId), getSelf());
            		/* Viene schedulato un messaggio da ricevere alla fine dell'atterraggio */
            		scheduler.scheduleOnce(Duration.ofMillis(Math.round(getRunwayOccupation())), getSelf(), new InLandingState(r.runway, flightId, getSender()), getContext().getSystem().dispatcher(), null);
            	}
            })
        /* ========== ATTERRAGGIO ========== */
        .match(
        	InLandingState.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new LandingComplete(r.runway, r.flightId), getSelf());
            		/* Viene schedulato un messaggio da ricevere quando l'aereo dovrà ripartire */
            		scheduler.scheduleOnce(Duration.ofMillis(Math.round(getRunwayOccupation())), getSelf(), new StartDeparturePhase(flightId, r.controlTower), getContext().getSystem().dispatcher(), null);
            	}
            })
        /* ========== RICHIESTA DI DECOLLO ========== */
        .match(
            StartDeparturePhase.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new DepartureRequest(r.flightId), getSelf());
	            	log.info("Aircraft {} requested departure", this.flightId);
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
