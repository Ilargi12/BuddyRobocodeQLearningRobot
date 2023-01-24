package BuddyRobocode;
import robocode.*;
import java.awt.Color;

import static BuddyRobocode.MathTransformations.*;

/**
 * BuddyRobot - a robot by BPlewnia
 */
public class BuddyRobot extends AdvancedRobot
{
	//the reward policy should be killed > bullet hit > hit robot > hit wall > bullet miss > got hit by bullet
	private double currentReward = 0.0;
	private final double goodEventReward = 5.0;
	private final double badEventReward = -2.0;
	private final double roundWonReward = 10;
	private final double roundLostReward = -10;

	private int wallWasHit = 0;
	private int wasHitByBullet = 0;

	private final double learningRate = 0.8;
	private final double discountFactor = 0.95;
	private double epsilon = 0.5;

	private EnemyRobot enemyRobot = new EnemyRobot();
	private QTable qTable = new QTable();
	private int numberOfWonRounds = 0;
	private int currentState;
	private int currentAction;
	private int every100Rounds = 0;
	private int roundCount = 1;
	private int totalNumRounds = 0;

	/**
	 * run: BuddyRobot's default behavior
	 */
	public void run() {
		setColors(Color.black,Color.yellow,Color.blue); // body,gun,radar
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		State.initialEnergy = this.getEnergy();

		while(true) {
			currentState = getRobotState();
			currentAction = getCurrentAction(currentState);
			pickMovement();
			updateKnowledge();
			this.currentReward = 0.0;
			rotateAndFireGun();
		}
	}

	private void rotateAndFireGun() {
		double firepower = Screen.width / enemyRobot.distance;
		firepower = firepower > 3 ? 3 : firepower;
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		rotateGunAngle(firepower);
		setFire(firepower);
		execute();
	}

	private void rotateGunAngle(double firepower) {
		long bulletArrivalTime, bulletTransmissionTime;

		double distance = euclideanDistance(getX(), getY(), enemyRobot.x, enemyRobot.y);
		double bulletVelocity = 20 - (firepower * 3);
		bulletTransmissionTime = (int) Math.round((distance / bulletVelocity));
		bulletArrivalTime = bulletTransmissionTime + getTime() - 9;

		double[] coordinate = calculateEnemyRobotPositionForBullet(bulletArrivalTime, enemyRobot);
		double gunOffset = getGunHeadingRadians() - (Math.PI / 2 - Math.atan2(coordinate[1] - getY(), coordinate[0] - getX()));
		setTurnGunLeftRadians(normalize(gunOffset));
	}

	private void updateKnowledge() {
		double qValue = this.qTable.get(currentState, currentAction);
		qValue += this.learningRate * (this.currentReward + this.discountFactor * this.qTable.getMaxQValue(currentState) - qValue);
		this.qTable.set(currentState, currentAction, qValue);
	}

	private void pickMovement() {
		// reset flags
		this.wasHitByBullet = 0;
		this.wallWasHit = 0;

		switch (currentAction) {
			case Action.moveForward -> setAhead(Action.moveDistance);
			case Action.moveBack -> setBack(Action.moveDistance);
			case Action.moveForwardRight -> {
				setAhead(Action.moveDistance);
				setTurnRight(45.0);
			}
			case Action.moveForwardLeft -> {
				setAhead(Action.moveDistance);
				setTurnLeft(45.0);
			}
			case Action.moveBackRight -> {
				setBack(Action.moveDistance);
				setTurnRight(45.0);
			}
			case Action.moveBackLeft -> {
				setBack(Action.moveDistance);
				setTurnLeft(45.0);
			}
		}
		execute();
	}

	private int getCurrentAction(int currentState) {
		if (Math.random() > epsilon)
			return this.qTable.getOptimalAction(currentState);
		return (int) (Math.random() * Action.actionsCount);
	}

	private int getRobotState() {
		int curDistance = State.calcDistanceState(enemyRobot.distance);
		int enemyBearing = State.getEnemyBearing(enemyRobot.bearing);
		int curEnergy = State.calcEnergyState(getEnergy());
		int heading = State.getDirection(getHeading());
		int currentState = State.getState(curDistance, enemyBearing, heading, curEnergy, wallWasHit, wasHitByBullet);
		return currentState;
	}


	// EVENTS
	/**
	 * onScannedRobot: What to do when you see another robot
	 * Behaviour: Update the enemy robot fields.
	 */
	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		double absoluteBearing = (getHeading() + e.getBearing()) % (360) * Math.PI/180;
		enemyRobot.bearing = e.getBearingRadians();
		enemyRobot.heading = e.getHeadingRadians();
		enemyRobot.velocity = e.getVelocity();
		enemyRobot.distance = e.getDistance();
		enemyRobot.energy = e.getEnergy();
		enemyRobot.x = getX() + Math.sin(absoluteBearing) * e.getDistance();
		enemyRobot.y = getY() + Math.cos(absoluteBearing) * e.getDistance();
		enemyRobot.time = getTime();
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 * Behaviour: Get penalty.
	 * 			  Get closer to the enemy robot because it just fired a bullet.
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		wasHitByBullet = 1;
		currentReward -= e.getBullet().getPower();

		double angle = Math.PI/2 - Math.atan2(enemyRobot.y - getY(), enemyRobot.x -getX());
		setTurnRightRadians(normalize(angle - getHeadingRadians())+2);
		setAhead(100);
		execute();
	}

	/**
	 * onHitRobot: What to do when you hit a robot
	 * Behaviour: Get penalty.
	 */
	@Override
	public void onHitRobot(HitRobotEvent e) {
		currentReward += badEventReward;
	}

	/**
	 * onHitWall: What to do when you hit a wall
	 * Behaviour: Receive penalty.
	 * 			  If the distance to EnemyRobot is more than 200 -> Get closer.
	 */
	@Override
	public void onHitWall(HitWallEvent event) {
		wallWasHit = 1;
		currentReward += badEventReward;
		if(euclideanDistance(getX(), getY(),enemyRobot.x, enemyRobot.y)>200){
			double bearingToEnemy= getBearingToEnemyRobot(getX(), getY(), getHeadingRadians(), enemyRobot);
			setTurnRightRadians(bearingToEnemy);
			setAhead(200);
			execute();
		}
	}

	/**
	 * onBulletHit: What to do when your bullet hits an enemy robot
	 * Behaviour: Receive reward.
	 */
	@Override
	public void onBulletHit(BulletHitEvent event) {
		currentReward += goodEventReward;
	}

	/**
	 * onBulletMissed: What to do when your bullet misses an enemy robot
	 * Behaviour: Receive penalty.
	 */
	@Override
	public void onBulletMissed(BulletMissedEvent event) {
		currentReward += badEventReward;
	}

	/**
	 * onWin: What to do when you win a round
	 * Behaviour: Receive reward and update knowledge.
	 */
	@Override
	public void onWin(WinEvent event) {
		currentReward += roundWonReward;
		numberOfWonRounds++;
		this.updateKnowledge();
	}

	/**
	 * onDeath: What to do when you die in a round
	 * Behaviour: Receive penalty.
	 */
	@Override
	public void onDeath(DeathEvent event) {
		currentReward += roundLostReward;
		updateKnowledge();
	}

}
