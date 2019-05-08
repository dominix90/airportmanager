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

public class TotalAircraftsNumberActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	/* ========== VARIABILI E STRUTTURE DATI ========== */
	private String airportId;
	private int totalAircraftsNumber;
	private long startingTime;
	private long sampleTime;
	private ArrayList<AbstractMap.SimpleEntry<Long, Integer>> totalAircraftsNumberEvolution;
	
	/* ========== COSTRUTTORE E METODI NATIVI ========== */
	public TotalAircraftsNumberActor(String airportId) {
		this.airportId = airportId;
		totalAircraftsNumber = 0;
		totalAircraftsNumberEvolution = new ArrayList<AbstractMap.SimpleEntry<Long, Integer>>();
		startingTime = 0;
	}
	public static Props props(String airportId) {
	    return Props.create(TotalAircraftsNumberActor.class, () -> new TotalAircraftsNumberActor(airportId));
	}
	
	@Override
	public void preStart() {
		if(Parameters.logAnalysis) log.info("TotalAircraftsNumberActor actor started");
		startingTime = System.currentTimeMillis();
	}
	
	@Override
	public void postStop() throws IOException {
		if(Parameters.logAnalysis) log.info("TotalAircraftsNumberActor actor stopped");
		
		printToCsvFile(totalAircraftsNumberEvolution, "totalAircraftsNumberEvolution");
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
						ChangedTotalAircraftsNumber.class,
			            r -> {
			            	totalAircraftsNumber += r.change;
			            	sampleTime = System.currentTimeMillis() - startingTime;
			            	totalAircraftsNumberEvolution.add( new AbstractMap.SimpleEntry<Long,Integer>(sampleTime, totalAircraftsNumber) );
		            	})
		        .build();
		}

}
