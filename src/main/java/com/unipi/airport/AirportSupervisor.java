package com.unipi.airport;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.unipi.utils.Messages.*;

public class AirportSupervisor extends AbstractActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	public static Props props() {
		return Props.create(AirportSupervisor.class);
	}
	
	ActorRef aircraft1 = getContext().actorOf(Aircraft.props("FR0001"));
	ActorRef aircraft2 = getContext().actorOf(Aircraft.props("VY9999"));
    ActorRef controlTower = getContext().actorOf(ControlTower.props("CTA"));

	@Override
	public void preStart() {
		log.info("Airport Application started");
		aircraft1.tell(new StartLanding(controlTower, 150), controlTower);
		aircraft2.tell(new StartLanding(controlTower, 75), controlTower);
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