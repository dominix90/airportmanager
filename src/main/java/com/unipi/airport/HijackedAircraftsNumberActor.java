package com.unipi.airport;

import akka.actor.*;
import akka.actor.AbstractActor.Receive;
import akka.event.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

import com.unipi.utils.Messages.*;
import com.unipi.utils.Parameters;

public class HijackedAircraftsNumberActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	/* ========== VARIABILI E STRUTTURE DATI ========== */
	private String airportId;
	private int hijackedAircraftsBecauseOfAirportFullNumber;
	private int hijackedAircraftsBecauseOfFuelNumber;
	private long startingTime;
	private long sampleTime;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> hijackedAircraftsBecauseOfAirportFullNumberEvolution;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> hijackedAircraftsBecauseOfFuelNumberEvolution;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */
	public HijackedAircraftsNumberActor(String airportId) {
		this.airportId = airportId;
		hijackedAircraftsBecauseOfAirportFullNumber = 0;
		hijackedAircraftsBecauseOfFuelNumber = 0;
		hijackedAircraftsBecauseOfAirportFullNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		hijackedAircraftsBecauseOfFuelNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		startingTime = 0;
		sampleTime = 0;
	}
	public static Props props(String airportId) {
	    return Props.create(HijackedAircraftsNumberActor.class, () -> new HijackedAircraftsNumberActor(airportId));
	}
	
	@Override
	public void preStart() {
		if(Parameters.logAnalysis) log.info("HijackedAircaftsNumberActor actor started");
		startingTime = System.currentTimeMillis();
	}
	
	@Override
	public void postStop() throws IOException {
		if(Parameters.logAnalysis) log.info("HijackedAircaftsNumberActor actor stopped");
		
		printToCsvFile(hijackedAircraftsBecauseOfAirportFullNumberEvolution, "hijackedAircraftsBecauseOfAirportFullNumberEvolution");
		printToCsvFile(hijackedAircraftsBecauseOfFuelNumberEvolution, "hijackedAircraftsBecauseOfFuelNumberEvolution");
	}
	
	//METODO PER CREARE FILE CSV
	private void printToCsvFile(ArrayList<AbstractMap.SimpleEntry<Long, Integer>> totalAircraftsNumberEvolution, String collectionName) throws IOException
	{
		String fileContent = "";
		BufferedWriter writer = new BufferedWriter(new FileWriter(Parameters.analysisDataFolderPath + collectionName+airportId+".txt"));
		
		for( AbstractMap.SimpleEntry<Long, Integer> entry : totalAircraftsNumberEvolution ) {
			fileContent = Long.toString(entry.getKey()) + ',' + Integer.toString(entry.getValue()) + '\n';
			writer.write(fileContent);
		}
	    
	    writer.close();
	}
	
	//GESTIONE MESSAGGI RICEVUTI
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(
					ChangedHijackedAircraftsBecauseOfAirportFull.class,
		            r -> {
		            	hijackedAircraftsBecauseOfAirportFullNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	hijackedAircraftsBecauseOfAirportFullNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, hijackedAircraftsBecauseOfAirportFullNumber) );
	            	})
			.match(
					ChangedHijackedAircraftsBecauseOfFuelNumber.class,
		            r -> {
		            	hijackedAircraftsBecauseOfFuelNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	hijackedAircraftsBecauseOfFuelNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, hijackedAircraftsBecauseOfFuelNumber) );
	            	})
	        .build();
	}
}
