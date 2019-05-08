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

public class ParkingAircraftsNumberActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	/* ========== VARIABILI E STRUTTURE DATI ========== */
	private String airportId;
	private int parkingAircraftsNumber;
	private int emergencyParkingAircraftsNumber;
	private long startingTime;
	private long sampleTime;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> parkingAircraftsNumberEvolution;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> emergencyParkingAircraftsNumberEvolution;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */
	public ParkingAircraftsNumberActor(String airportId) {
		this.airportId = airportId;
		parkingAircraftsNumber = 0;
		emergencyParkingAircraftsNumber = 0;
		parkingAircraftsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		emergencyParkingAircraftsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		startingTime = 0;
		sampleTime = 0;
	}
	public static Props props(String airportId) {
	    return Props.create(ParkingAircraftsNumberActor.class, () -> new ParkingAircraftsNumberActor(airportId));
	}
	
	@Override
	public void preStart() {
		if(Parameters.logAnalysis) log.info("ParkingAircraftsNumberActor actor started");
		startingTime = System.currentTimeMillis();
	}
	
	@Override
	public void postStop() throws IOException {
		if(Parameters.logAnalysis) log.info("ParkingAircraftsNumberActor actor stopped");
		
		printToCsvFile(parkingAircraftsNumberEvolution, "parkingAircraftsNumberEvolution");
		printToCsvFile(emergencyParkingAircraftsNumberEvolution, "emergencyParkingAircraftsNumberEvolution");
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
					ChangedEmergencyParkingAircraftsNumber.class,
		            r -> {
		            	emergencyParkingAircraftsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	emergencyParkingAircraftsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, emergencyParkingAircraftsNumber) );
	            	})
			.match(
					ChangedParkingAircraftsNumber.class,
		            r -> {
		            	parkingAircraftsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	parkingAircraftsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, parkingAircraftsNumber) );
	            	})			
	        .build();
	}
}
