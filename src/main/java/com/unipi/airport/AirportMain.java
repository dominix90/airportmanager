package com.unipi.airport;

import java.io.IOException;

import com.unipi.airport.AirportSupervisor;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;

public class AirportMain {

	public static void main(String[] args) throws IOException {
	    ActorSystem system = ActorSystem.create("airport-system");

	    try {
	    	// Create top level supervisor
	    	ActorRef supervisor = system.actorOf(AirportSupervisor.props(), "airport-supervisor");

	    	System.out.println("Press ENTER to exit the system");
	    	System.in.read();
	    	} finally {
	    		system.terminate();
	    }
	}
}
