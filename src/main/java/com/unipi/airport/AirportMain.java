package com.unipi.airport;

import java.io.IOException;
import com.unipi.airport.AirportSupervisor;
import com.unipi.utils.Parameters;
import akka.actor.ActorSystem;
import akka.actor.ActorRef;

public class AirportMain {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		ActorSystem[] systems = new ActorSystem[Parameters.seed.length];
		
	    for ( int i=0; i<Parameters.seed.length; ++i ) {
			systems[i] = ActorSystem.create("airport-system-" + Parameters.seed[i]);
			ActorRef supervisor = systems[i].actorOf(AirportSupervisor.props(Parameters.seed[i]), "airport-supervisor-" + Parameters.seed[i]);
			Thread.sleep(Parameters.simDuration);
			systems[i].terminate();
		}

	}
}
