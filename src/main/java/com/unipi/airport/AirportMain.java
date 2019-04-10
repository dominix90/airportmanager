package com.unipi.airport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import com.unipi.airport.AirportSupervisor;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;

public class AirportMain {

	private static Properties myProperties;
	
	public static void main(String[] args) throws IOException {
	    ActorSystem system = ActorSystem.create("airport-system");
	    

	    try {
	    	// Create top level supervisor
	    	ActorRef supervisor = system.actorOf(AirportSupervisor.props(), "airport-supervisor");
	    	
	    	System.out.println("Press ENTER to exit the system");
	    	System.out.println(new File("").getAbsolutePath());
	    	System.in.read();
	    	} finally {
	    		system.terminate();
	    }
	}
	
	private static int loadProperties() {
		String configFile = new File("").getAbsolutePath().concat("\\src\\main\\resources\\config.properties");
    	InputStream input = null;
    	myProperties = new Properties();
		
		try {	
			input = AirportMain.class.getResourceAsStream(configFile);
			
    		if (input == null) {
    			System.out.println("Sorry, unable to find " + configFile);
    			return -1;
    		}

    		//load a properties file from class path, inside static method
    		myProperties.load(input);
 
                //get the property value and print it out
                System.out.println(myProperties.getProperty("averageRunwayOccupation"));
    	        System.out.println(myProperties.getProperty("runwaysNumber"));
    	        System.out.println(myProperties.getProperty("parkingSize"));
    	        System.out.println(myProperties.getProperty("emergencyParkingSize"));
    	        
    	        return 0;
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
    		return -1;
        } finally {
        	if(input!=null){
        		try {
        			input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
	}
}
