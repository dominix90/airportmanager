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
		.match(
			StartLandingPhase.class,
            r -> {
            	this.remainingTime = r.fuel;
            	getSender()
            		.tell(new LandingRequest(flightId), getSelf());
            	log.info("Landing request by aircraft {} to control tower", this.flightId);
            })
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
        .match(
    		StartLanding.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		getSender().tell(new Landing(r.runway, r.flightId), getSelf());
	            	log.info("Aircraft {} started landing phase in runway {}", this.flightId, r.runway.getRunwayNumber());
	            	scheduler.scheduleOnce(Duration.ofMillis(5000), getSelf(), new InLandingState(r.runway, flightId, getSender()), getContext().getSystem().dispatcher(), null);
            	}
            })
        .match(
        	InLandingState.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new LandingComplete(r.runway, r.flightId), getSelf());
	            	log.info("Aircraft {} completed landing phase in runway {}", this.flightId, r.runway.getRunwayNumber());
	            	scheduler.scheduleOnce(Duration.ofMillis(5000), getSelf(), new StartDeparture(flightId, r.controlTower), getContext().getSystem().dispatcher(), null);
            	}
            })
        .match(
            StartDeparture.class,
            r -> {
            	if (this.flightId.equals(r.flightId)) {
            		r.controlTower.tell(new DepartureRequest(r.flightId), getSelf());
	            	log.info("Aircraft {} requested departure", this.flightId);
            	}
            })
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
        .build();
  }
}
