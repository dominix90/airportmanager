package com.unipi.airport;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.unipi.utils.Messages.*;

public class AircraftActor extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final String flightId;
  private long remainingTime = 150;

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
	        StartLanding.class,
            r -> {
            	this.remainingTime = r.fuel;
            	getSender()
            		.tell(new LandingRequest("00000001", flightId), getSelf());
            	log.info("Landing request by aircraft {} to control tower", this.flightId);
            })
        .match(
    		RespondLandingTime.class,
            r -> {
            	getSender()
            		.tell(new LandingConfirmation(sufficientFuel(r.timeForLanding), flightId), getSelf());
      			log.info("Landing confirmation sent by aircraft {} to control tower", this.flightId);
            })
        .match(
    		UpdateLandingTime.class,
            r -> {
            	getSender()
            		.tell(new LandingConfirmation(sufficientFuel(r.timeForLanding), flightId), getSelf());
      			log.info("Landing confirmation sent by aircraft {} to control tower", this.flightId);
            })
        .build();
  }
}
