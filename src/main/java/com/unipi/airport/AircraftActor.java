package com.unipi.airport;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;

import com.unipi.utils.Messages.*;

public class AircraftActor extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final String flightId;
  private long remainingTime = 150;
  private Scheduler scheduler = getContext().getSystem().getScheduler();

  public AircraftActor(String flightId) {
    this.flightId = flightId;
  }

  public static Props props(String flightId) {
    return Props.create(AircraftActor.class, () -> new AircraftActor(flightId));
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
  
  private boolean sufficientFuel(long timeForNextLanding) {
	  if (timeForNextLanding <= this.remainingTime)
		  return true;
	  return false;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
    	/* ========== RICHIESTA DI ATTERRAGGIO ========== */
		.match(
			StartLandingPhase.class,
            r -> {
            	this.remainingTime = r.fuel;
            	getSender()
            		.tell(new LandingRequest(flightId), getSelf());
            	log.info("Landing request by aircraft {} to control tower", this.flightId);
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
	            	scheduler.scheduleOnce(Duration.ofMillis(5000), getSelf(), new InLandingState(r.runway, flightId, getSender()), getContext().getSystem().dispatcher(), null);
            	}
            })
        /* ========== ATTERRAGGIO ========== */
        .match(
        	InLandingState.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new LandingComplete(r.runway, r.flightId), getSelf());
            		/* Viene schedulato un messaggio da ricevere quando l'aereo dovrà ripartire */
            		scheduler.scheduleOnce(Duration.ofMillis(5000), getSelf(), new StartDeparturePhase(flightId, r.controlTower), getContext().getSystem().dispatcher(), null);
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
	            	scheduler.scheduleOnce(Duration.ofMillis(5000), getSelf(), new InTakeOffState(r.runway, flightId, getSender()), getContext().getSystem().dispatcher(), null);
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
