package com.unipi.airport;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.unipi.utils.Messages.*;

public class ControlTower extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	  final String airportId;
	  long timeForNextLanding = 100; //secondi mancanti al prossimo atterraggio

	  public ControlTower(String airportId) {
	    this.airportId = airportId;
	  }

	  public static Props props(String airportId) {
	    return Props.create(ControlTower.class, () -> new ControlTower(airportId));
	  }
	  
	  @Override
	  public void preStart() {
	    log.info("ControlTower actor {} started", airportId);
	  }

	  @Override
	  public void postStop() {
	    log.info("ControlTower actor {} stopped", airportId);
	  }
	  
	  /* Questo metodo è temporaneo, dopo il tempo verrà calcolato e non settato */
	  public void setTimeForNextLanding(long time) {
		  this.timeForNextLanding = time;
	  }

	  @Override
	  public Receive createReceive() {
	    return receiveBuilder()
	        .match(
	        	LandingRequest.class,
	            r -> {
	            	getSender()
	                 	.tell(new RespondTime(r.getRequestId(), r.getFlightId(), timeForNextLanding), getSelf());
	            	log.info("Control tower {} computed {} seconds for aircraft {} landing", this.airportId, timeForNextLanding, r.getFlightId());
	            })
	        .match(
		        EmergencyLandingRequest.class,
	            r -> {
	            	getSender()
                 	.tell(new RespondTime(r.getRequestId(), r.getFlightId(), timeForNextLanding), getSelf());
            	log.info("Control tower {} computed {} seconds for aircraft {} landing", this.airportId, timeForNextLanding, r.getFlightId());
            })
	        .match(
	        		LandingConfirmation.class,
		            r -> {
		            	if(r.getValue())
		            		log.info("AIRCRAFT {} LANDED", r.getFlightId());
		            	else
		            		log.info("AIRCRAFT {} changed course. It is now directed to another airport", r.getFlightId());
		            })
	        .build();
	    
	  }
}
