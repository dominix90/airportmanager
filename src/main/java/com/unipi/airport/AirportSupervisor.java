package com.unipi.airport;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.unipi.utils.Messages.*;
import com.unipi.utils.Parameters;

public class AirportSupervisor extends AbstractActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	public static Props props() {
		return Props.create(AirportSupervisor.class);
	}
	
	//AEREI
	ActorRef aircraft1 = getContext().actorOf(AircraftActor.props("FR0001"), "fr0001");
	ActorRef aircraft2 = getContext().actorOf(AircraftActor.props("VY9999"), "vy9999");
	

	@Override
	public void preStart() {
		log.info("Airport Application started");
		//PISTE E AEROPORTO
		Runway[] runways = new Runway[Parameters.runwaysNumber];
		for (int i = 0; i < Parameters.runwaysNumber; i++) {
			runways[i] = new Runway(i+1, "FREE");
		}
	    ActorRef controlTower = getContext().actorOf(ControlTower.props("CTA",runways), "cta");
		aircraft1.tell(new StartLandingPhase(controlTower, 150), controlTower);
		aircraft2.tell(new StartLandingPhase(controlTower, 75), controlTower);
	}

	@Override
	public void postStop() {
		log.info("Airport Application stopped");
	}
	
	// No need to handle any messages
	@Override
	public Receive createReceive() {
		return receiveBuilder()
	        .build();
	}
}