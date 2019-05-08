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

public class QueueAircraftsNumberActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	/* ========== VARIABILI E STRUTTURE DATI ========== */
	private String airportId;
	private int landingQueueAircraftsNumber;
	private int emergencyQueueAircraftsNumber;
	private int departureQueueAircraftsNumber;
	private long startingTime;
	private long sampleTime;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> landingQueueAircraftsNumberEvolution;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> emergencyQueueAircraftsNumberEvolution;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> departureQueueAircraftsNumberEvolution;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */
	public QueueAircraftsNumberActor(String airportId) {
		this.airportId = airportId;
		landingQueueAircraftsNumber = 0;
		emergencyQueueAircraftsNumber = 0;
		departureQueueAircraftsNumber = 0;
		landingQueueAircraftsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		emergencyQueueAircraftsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		departureQueueAircraftsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		startingTime = 0;
		sampleTime = 0;
	}
	public static Props props(String airportId) {
	    return Props.create(QueueAircraftsNumberActor.class, () -> new QueueAircraftsNumberActor(airportId));
	}
	
	@Override
	public void preStart() {
		if(Parameters.logAnalysis) log.info("QueueAircraftsNumberActor actor started");
		startingTime = System.currentTimeMillis();
	}
	
	@Override
	public void postStop() throws IOException {
		if(Parameters.logAnalysis) log.info("QueueAircraftsNumberActor actor stopped");
		
		printToCsvFile(landingQueueAircraftsNumberEvolution, "landingQueueAircraftsNumberEvolution");
		printToCsvFile(emergencyQueueAircraftsNumberEvolution, "emergencyQueueAircraftsNumberEvolution");
		printToCsvFile(departureQueueAircraftsNumberEvolution, "departureQueueAircraftsNumberEvolution");
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
					ChangedEmergencyQueueAircraftsNumber.class,
		            r -> {
		            	emergencyQueueAircraftsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	emergencyQueueAircraftsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, emergencyQueueAircraftsNumber) );
	            	})
			.match(
					ChangedLandingQueueAircraftsNumber.class,
		            r -> {
		            	landingQueueAircraftsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	landingQueueAircraftsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, landingQueueAircraftsNumber) );
	            	})
			.match(
					ChangedDepartureQueueAircraftsNumber.class,
		            r -> {
		            	departureQueueAircraftsNumber += r.change;
		            	sampleTime = System.currentTimeMillis() - startingTime;
		            	departureQueueAircraftsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, departureQueueAircraftsNumber) );
	            	})
	        .build();
	}

}
