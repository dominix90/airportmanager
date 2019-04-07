package com.unipi.utils;

import akka.actor.ActorRef;
import com.unipi.airport.*;

public class Messages {
	
	/* SOURCE: AirportSupervisor
	 * DESTINATION: Aircraft
	 * MESSAGE: Puoi richiedere l'atterraggio.
	 */
	public static final class StartLandingPhase {
		public final ActorRef controlTower;
		public final long fuel;

		  public StartLandingPhase(ActorRef controlTower, long fuel) {
			  this.controlTower = controlTower;
			  this.fuel = fuel;
		  }
	  }
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Posso atterrare?
	 */
	public static final class LandingRequest {
		  public final String requestId;
		  public final String flightId;

		  public LandingRequest(String requestId, String flightId) {
			  this.requestId = requestId;
			  this.flightId = flightId;
		  }
	  }
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Posso effettuare un atterraggio d'emergenza?
	 */
	public static final class EmergencyLandingRequest {
		public final String requestId;
		public final String flightId;

		public EmergencyLandingRequest(String requestId, String flightId) {
			  this.requestId = requestId;
			  this.flightId = flightId;
		  }
	  }

	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: Questo è il tempo calcolato per il tuo atterraggio.
	 */  
	public static final class RespondLandingTime {
		  public final String requestId;
		  public final String flightId;
		  public final long timeForLanding;

		  public RespondLandingTime(String requestId, String flightId, long timeForLanding) {
			  this.requestId = requestId;
			  this.flightId = flightId;
			  this.timeForLanding = timeForLanding;
		  }
	  }
	  
	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: E' stata aggiornata la coda d'atterraggio in seguito ad una richiesta d'emergenza.
	 * 			Questo è il nuovo tempo calcolato per il tuo atterraggio.
	 */   
	public static final class UpdateLandingTime {
		  public final String flightId;
		  public final long timeForLanding;

		  public UpdateLandingTime(String flightId, long timeForLanding) {
			  this.flightId = flightId;
			  this.timeForLanding = timeForLanding;
		  }
	  }
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: La mia decisione sull'atterraggio è contenuta nella variabile <value>.
	 */  
	public static final class EmergencyLandingConfirmation {
		  public final boolean value;
		  public final String flightId;

		  public EmergencyLandingConfirmation(boolean value, String flightId) {
			  this.value = value;
			  this.flightId = flightId;
		  }
	  }
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: La mia decisione sull'atterraggio è contenuta nella variabile <value>.
	 */   
	public static final class LandingConfirmation {
		public final boolean value;
		public final String flightId;
		public final int confirmationType; // 1 --> prima richiesta; 2 --> risposta ad aggiornamento code

		public LandingConfirmation(boolean value, String flightId, int confirmationType) {
			this.value = value;
			this.flightId = flightId;
			this.confirmationType = confirmationType;
		}
	}
	
	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: Puoi iniziare l'atterraggio.
	 */
	public static final class StartLanding {
		public final Runway runway;
		public final String flightId;

		public StartLanding(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}
	
	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: Puoi iniziare l'atterraggio.
	 */
	public static final class Landing {
		public final Runway runway;
		public final String flightId;

		public Landing(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}	
	
	/* SOURCE: Aircraft
	 * DESTINATION: Aircraft
	 * MESSAGE: Self message schedulato alla fine della'atterraggio.
	 */
	public static final class InLandingState {
		public final Runway runway;
		public final String flightId;
		public final ActorRef controlTower;

		public InLandingState(Runway runway, String flightId, ActorRef controlTower) {
			this.runway = runway;
			this.flightId = flightId;
			this.controlTower = controlTower;
		}
	}
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Ho terminato l'atterraggio.
	 */   
	public static final class LandingComplete {
		public final Runway runway;
		public final String flightId;
		
		public LandingComplete(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}	
}
