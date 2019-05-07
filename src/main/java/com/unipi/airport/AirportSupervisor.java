package com.unipi.airport;

import akka.actor.*;
import akka.event.*;

import org.apache.commons.math3.distribution.*;

import java.time.Duration;
import java.util.LinkedList;

import com.unipi.utils.Messages.*;
import com.unipi.utils.Parameters;

public class AirportSupervisor extends AbstractActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private Scheduler scheduler = getContext().getSystem().getScheduler();
	//PARAMETRI PER GENERAZIONE flighId
	private int sequenceNumber = 0;
	private static final String[] ALPHA_STRING = {"EW","HV", "FR","AZ","VY"};
	private static final String NUMERIC_STRING = "0123456789";
	//DISTRIBUZIONI
	private int seed;
	private ExponentialDistribution interArrivalTimeDistribution;
	private UniformRealDistribution fuelDistribution;
	private UniformRealDistribution emergencyDistribution;
	private LogNormalDistribution landingTimeDistribution;
	private LogNormalDistribution takeOffTimeDistribution;
	private LogNormalDistribution parkingTimeDistribution;
	//TORRE DI CONTROLLO
	ActorRef controlTower;

	/* ========== COSTRUTTORE E METODI NATIVI ========== */	
	public AirportSupervisor(int seed) {
	    this.seed = seed;
	}
	public static Props props(int seed) {
		return Props.create(AirportSupervisor.class, () -> new AirportSupervisor(seed));
	}

	@Override
	public void preStart() {
		log.info("Airport Application started");
		
		//DISTRIBUZIONI
		interArrivalTimeDistribution = new ExponentialDistribution(Parameters.meanInterArrivalTime);
		interArrivalTimeDistribution.reseedRandomGenerator(seed);
		fuelDistribution = new UniformRealDistribution(Parameters.minFuel,Parameters.maxFuel);
		fuelDistribution.reseedRandomGenerator(seed);
		emergencyDistribution = new UniformRealDistribution(0,100);
		emergencyDistribution.reseedRandomGenerator(seed);
		landingTimeDistribution = new LogNormalDistribution(Math.log(Parameters.meanLandingTime), Parameters.shapeLandingTime);
		landingTimeDistribution.reseedRandomGenerator(seed);
		takeOffTimeDistribution = new LogNormalDistribution(Math.log(Parameters.meanTakeOffTime), Parameters.shapeTakeOffTime);
		takeOffTimeDistribution.reseedRandomGenerator(seed+1000);
		parkingTimeDistribution = new LogNormalDistribution(Math.log(Parameters.meanParkingTime), Parameters.shapeParkingTime);
		parkingTimeDistribution.reseedRandomGenerator(seed+2000);
		
		//INIZIALIZZAZIONE PISTE E AEROPORTO
		Runway[] runways = new Runway[Parameters.runwaysNumber];
		for (int i = 0; i < Parameters.runwaysNumber; i++) {
			runways[i] = new Runway(i+1, "FREE");
		}
	    controlTower = getContext().actorOf(ControlTower.props("CTA-"+seed,runways), "cta-"+seed);
		
		//CREAZIONE AEREO INIZIALE E SCHEDULING SELFMESSAGE PER PROSSIMO AEREO
	    /* Viene generato il primo aereo e schedulato 
	     * il selfmessage per la generazione del traffico di volo */
		String flightId = generateFlightId(6);
    	
    	double nextArrival = interArrivalTimeDistribution.sample();
    	double fuel = fuelDistribution.sample();
    	boolean emergencyState = getEmergencyState(emergencyDistribution.sample());
    	double landingTime = landingTimeDistribution.sample();
    	double takeOffTime = takeOffTimeDistribution.sample();
    	double parkingTime = parkingTimeDistribution.sample();
    	
    	ActorRef aircraft = getContext().actorOf(AircraftActor.props(flightId, Math.round(fuel), emergencyState, landingTime, takeOffTime, parkingTime), flightId.toLowerCase());
    	if(Parameters.logVerbose) log.info("Aircraft {} generated! New aircraft in {} {}", flightId, nextArrival, Parameters.timeUnit);
	    
	    aircraft.tell(new StartLandingRequest(controlTower, flightId), controlTower);
	    scheduler.scheduleOnce(Duration.ofMillis(Math.round(nextArrival)), getSelf(), new AircraftGenerator(), getContext().getSystem().dispatcher(), null);	    
	    
	}

	@Override
	public void postStop() {
		if(Parameters.logVerbose) log.info("Reached {} {}: ending simulation.", Parameters.simDuration, Parameters.timeUnit);
		log.info("Airport Application stopped");
	}

	//metodo per generare flighId
	public String generateFlightId(int count) {
		StringBuilder builder = new StringBuilder();
		builder.append(sequenceNumber);
		builder.append("-");
		
		int initialCount = count;
		int character = -1;
		
		while (count-- != 0) {
			if (count >= initialCount - 1) {
				character = (int)(Math.random()*ALPHA_STRING.length);
				builder.append(ALPHA_STRING[character]);
			}
			else {
				character = (int)(Math.random()*NUMERIC_STRING.length());
				builder.append(NUMERIC_STRING.charAt(character));
			}			
		}
		
		return builder.toString();
	}
	
	
	/* Generazione del boolean inEmergency */
	public boolean getEmergencyState(double emergencyDistSample) {
		if ( emergencyDistSample < Parameters.emergencyPercentage )
			return true;
		return false;
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			/* ========== GENERAZIONE TRAFFICO DI VOLO ========== */
			/* Ogni volta che viene ricevuto questo selfmessage un nuovo aereo viene generato */
			.match(
				AircraftGenerator.class,
	            r -> {
	            	sequenceNumber++;
	            	String flightId = generateFlightId(6);
	            	double nextArrival = interArrivalTimeDistribution.sample();
	            	double fuel = fuelDistribution.sample();
	            	boolean emergencyState = getEmergencyState(emergencyDistribution.sample());
	            	double landingTime = landingTimeDistribution.sample();
	            	double takeOffTime = takeOffTimeDistribution.sample();
	            	double parkingTime = parkingTimeDistribution.sample();
	            	
	            	ActorRef newAircraft = getContext().actorOf(AircraftActor.props(flightId, Math.round(fuel), emergencyState, landingTime, takeOffTime, parkingTime), flightId.toLowerCase());	
	            	newAircraft.tell(new StartLandingRequest(controlTower, flightId), controlTower);
	            	scheduler.scheduleOnce(Duration.ofMillis(Math.round(nextArrival)), getSelf(), new AircraftGenerator(), getContext().getSystem().dispatcher(), null);	            	
	            	if(Parameters.logVerbose) log.info("Aircraft {} generated! New aircraft in {} {}", flightId, nextArrival, Parameters.timeUnit);
            	})
	        .build();
	}
}