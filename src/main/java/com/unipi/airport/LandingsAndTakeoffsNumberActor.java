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

public class LandingsAndTakeoffsNumberActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	/* ========== VARIABILI E STRUTTURE DATI ========== */
	private String airportId;
	private int landingsNumber;
	private int emergencyLandingsNumber;
	private int takeoffsNumber;
	private long startingTime;
	private long sampleTime;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> landingsNumberEvolution;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> emergencyLandingsNumberEvolution;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> takeoffsNumberEvolution;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */
	public LandingsAndTakeoffsNumberActor(String airportId) {
		this.airportId = airportId;
		landingsNumber = 0;
		emergencyLandingsNumber = 0;
		takeoffsNumber = 0;
		landingsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		emergencyLandingsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		takeoffsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		startingTime = 0;
		sampleTime = 0;
	}
	public static Props props(String airportId) {
	    return Props.create(LandingsAndTakeoffsNumberActor.class, () -> new LandingsAndTakeoffsNumberActor(airportId));
	}
	
	@Override
	public void preStart() {
		if(Parameters.logAnalysis) log.info("LandingsAndTakeoffsNumberActor actor started");
		startingTime = System.currentTimeMillis();
	}
	
	@Override
	public void postStop() throws IOException {
		if(Parameters.logAnalysis) log.info("LandingsAndTakeoffsNumberActor actor stopped");
		
		printToCsvFile(landingsNumberEvolution, "landingsNumberEvolution");
		printToCsvFile(emergencyLandingsNumberEvolution, "emergencyLandingsNumberEvolution");
		printToCsvFile(takeoffsNumberEvolution, "takeoffsNumberEvolution");
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
					ChangedEmergencyLandingsNumber.class,
		            r -> {
		            	emergencyLandingsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	emergencyLandingsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, emergencyLandingsNumber) );
	            	})
			.match(
					ChangedLandingsNumber.class,
		            r -> {
		            	landingsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	landingsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, landingsNumber) );
	            	})	
			.match(
					ChangedTakeoffsNumber.class,
		            r -> {
		            	takeoffsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	takeoffsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, takeoffsNumber) );
	            	})	
	        .build();
	}
}
