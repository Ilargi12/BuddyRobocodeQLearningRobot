package BuddyRobocode;

public class MathTransformations {
    public static double normalize(double angle) {
        if (angle > Math.PI) angle -= 2*Math.PI;
        if (angle < -Math.PI) angle += 2*Math.PI;
        return angle;
    }

    public static double euclideanDistance(double x1, double y1, double x2, double y2) {
        double xDiff = x2 - x1, yDiff = y2 - y1;
        return Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
    }

    public static double[] calculateEnemyRobotPositionForBullet(long bulletArrivalTime, EnemyRobot enemyRobot) {
        double diff = bulletArrivalTime - enemyRobot.time;
        double coordinate[] = new double[2];
        coordinate[0] = enemyRobot.x + diff * enemyRobot.velocity * Math.sin(enemyRobot.heading);
        coordinate[1] = enemyRobot.y + diff * enemyRobot.velocity * Math.cos(enemyRobot.heading);
        return coordinate;
    }

    public static double getBearingToEnemyRobot(double x, double y, double myHeading, EnemyRobot enemyRobot){
        double angle = Math.PI/2 - Math.atan2(enemyRobot.y - y, enemyRobot.x-x);
        return  normalize(angle - myHeading);
    }
}
