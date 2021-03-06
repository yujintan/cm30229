package behaviours;

import robot.Robot;
import lejos.nxt.LCD;
import lejos.robotics.subsumption.Behavior;
import lejos.util.Delay;

public class FollowWall implements Behavior {
	
	private static final int OUT_OF_RANGE = 255;

    private int[] distances = {0,0,0,0};
    private int distanceIndex = 0;
    
    private int lastAvg = 0;
    private int newAvg = 0;

    private boolean suppressed = false;
    private boolean rotating = false;

    private int turnAngle = 0;
    
    private Robot robot;

    public FollowWall(Robot robot) {
    	this.robot = robot;
    }

	@Override
	public boolean takeControl() {
		// return true if we are close to a wall
		return robot.getUltrasonicSensor().getDistance() < (Robot.CLOSE_DISTANCE * 2);
	}

	@Override
	public void action() {
		LCD.clear();
		LCD.drawString("Following Wall", 0, 1);
		
		suppressed = false;
		
		// check we're not in the process of turning towards or away from a wall
		if(rotating) {
            return;
		}

		int d = robot.getUltrasonicSensor().getDistance();

		if(d == 0 || d >= OUT_OF_RANGE)
			return;
		
		// move forward, before calculating distance again
		robot.getPilot().travel(10, true);
        
        // store distance
        distances[distanceIndex] = d;

		// make sure we have at least 3 readings
		if(distanceIndex < 3) {
			distanceIndex++;
			return;
		}

		if(suppressed)
			return;
		
		// calculate the mean of the previous three distances
		newAvg = (distances[distanceIndex - 1] + distances[distanceIndex - 2] + distances[distanceIndex - 3]) / 3;
		
        distanceIndex++;

		// loop index
		if(distanceIndex >= 4) {
			distanceIndex = 0;
		}

		// check whether this is the first time we've taken an average
		if(lastAvg == 0) {
			lastAvg = newAvg;
			return;
		}

		if(suppressed) {
			return;
		}

		// set the turning angle according to the difference between the last two calculated averages
		// if difference is positive, the robot will turn towards the wall
		// if negative, the robot will turn away from the wall
		if(newAvg - lastAvg >= 10) {
			turnAngle = 20;
		} else if(newAvg - lastAvg >= 5) {
			turnAngle = 15;
		} else if(newAvg - lastAvg >= 3) {
			turnAngle = 15;
		} else if(newAvg - lastAvg == 2) {
			turnAngle = 10;
		} else if(newAvg - lastAvg == 1) {
			turnAngle = 5;
		} else if(newAvg - lastAvg <= -10) {
			turnAngle = -20;
		} else if(newAvg - lastAvg <= -5) {
			turnAngle = -15;
		} else if(newAvg - lastAvg <= -3) {
			turnAngle = -15;
		} else if(newAvg - lastAvg == -2) {
			turnAngle = -10;
		} else if(newAvg - lastAvg == -1) {
			turnAngle = -5;
		} else {
			return;
		}

		rotating = true;

		// turn the robot
		if(turnAngle != 0)
			robot.getPilot().rotate(turnAngle);

		// continue driving forward until our distance from the wall changes
		robot.getPilot().forward();
    
		d = robot.getUltrasonicSensor().getDistance();
		while(Math.abs(newAvg - d) < 3 && d < Robot.CLOSE_DISTANCE * 2 && !suppressed) {
			Thread.yield();
			d = robot.getUltrasonicSensor().getDistance();
		}
    
		resetValues();
		robot.getPilot().stop();
	}

	@Override
	public void suppress() {
		resetValues();
    
		robot.getPilot().stop();
		suppressed = true;
	}
	
	/**
	 * Resets all distances and averages used by this behaviour.
	 */
	private void resetValues() {
		for(int i=0; i<4; i++) {
			distances[i] = 0;
		}
		distanceIndex = 0;
		lastAvg = 0;
		rotating = false;
	}

}
