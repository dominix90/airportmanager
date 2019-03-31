package com.unipi.airport;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.unipi.utils.Messages.*;

public class Aircraft extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final String flightId;
  private long remainingTime = 150;

  public Aircraft(String flightId) {
    this.flightId = flightId;
  }

  public static Props props(String flightId) {
    return Props.create(Aircraft.class, () -> new Aircraft(flightId));
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
            	this.remainingTime = r.getFuel();
            	getSender()
            		.tell(new LandingRequest("00000001", flightId), getSelf());
            	log.info("Landing request by aircraft {} to control tower", this.flightId);
            })
        .match(
        	RespondTime.class,
            r -> {
            	getSender()
            		.tell(new LandingConfirmation(r.getRequestId(), sufficientFuel(r.getTimeForLanding()), flightId), getSelf());
      			log.info("Landing confirmation sent by aircraft {} to control tower", this.flightId);
            })
        .build();
  }
}
