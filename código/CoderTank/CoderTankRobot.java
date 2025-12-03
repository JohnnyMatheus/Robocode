package CoderTank;

import robocode.*;
import robocode.util.Utils;
import java.awt.*;
import java.awt.geom.Point2D;

/**
 * CoderTankRobot - versão TURBO com Radar Ultra Lock
 * Autores: Johnny Matheus N., Nathaniel Rissi, Nelson R. Rodrigues Jr.
 */
public class CoderTankRobot extends AdvancedRobot {

    private int moveDirection = 1;
    private double lastEnemyEnergy = 100;
    private long lastScanTime = 0;

    @Override
    public void run() {

        setBodyColor(Color.BLACK);
        setGunColor(Color.RED);
        setRadarColor(Color.YELLOW);
        setBulletColor(Color.ORANGE);

        // RADAR INDEPENDENTE = MAIS FORTE
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        // Radar começa varrendo infinito
        setTurnRadarRight(Double.POSITIVE_INFINITY);

        while (true) {

            // Se o radar ficou muito tempo sem ver ninguém → PANICO MODE
            if (getTime() - lastScanTime > 10) {
                setTurnRadarRight(Double.POSITIVE_INFINITY);
            }

            // Movimento leve contínuo
            setAhead(30 * moveDirection);
            setTurnRight(5);

            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        lastScanTime = getTime();

        double enemyBearing = e.getBearing();
        double enemyDistance = e.getDistance();
        double absBearing = getHeading() + enemyBearing;
        double enemyVelocity = e.getVelocity();
        double enemyHeading = e.getHeading();

        // ---------- RADAR ULTRA LOCK ----------
        double radarTurn = Utils.normalRelativeAngleDegrees(absBearing - getRadarHeading());
        double extraLock = 2.2;  // OVERSHOOT AGRESSIVO
        setTurnRadarRight(radarTurn * extraLock);

        // ---------- DETECÇÃO DE TIRO DO INIMIGO ----------
        double drop = lastEnemyEnergy - e.getEnergy();
        if (drop > 0 && drop <= 3) {
            moveDirection *= -1;
            setAhead(150 * moveDirection);
        }
        lastEnemyEnergy = e.getEnergy();

        // ---------- MIRA PREDITIVA SIMPLES (MUITO EFICIENTE) ----------
        double bulletPower = chooseBulletPower(e);
        double bulletSpeed = 20 - 3 * bulletPower;
        double time = enemyDistance / bulletSpeed;

        double predictedX = getX() + enemyDistance * Math.sin(Math.toRadians(absBearing))
                + enemyVelocity * Math.sin(Math.toRadians(enemyHeading)) * time;

        double predictedY = getY() + enemyDistance * Math.cos(Math.toRadians(absBearing))
                + enemyVelocity * Math.cos(Math.toRadians(enemyHeading)) * time;

        Point2D.Double p = project(predictedX, predictedY);

        double aimAngle = Math.toDegrees(Math.atan2(p.x - getX(), p.y - getY()));
        double gunTurn = Utils.normalRelativeAngleDegrees(aimAngle - getGunHeading());
        setTurnGunRight(gunTurn);

        if (Math.abs(getGunTurnRemaining()) < 12)
            fire(bulletPower);

        // ---------- MOVIMENTO EVASIVO ----------
        double angle = Utils.normalRelativeAngleDegrees(enemyBearing + 90 - (20 * moveDirection));
        setTurnRight(angle);

        if (enemyDistance < 120)
            setAhead(-80 * moveDirection);
        else
            setAhead(100 * moveDirection);

        avoidWalls();
        execute();
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        moveDirection *= -1;
        setAhead(160 * moveDirection);
        setTurnRight(30);
        execute();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        double turnGunAmt = Utils.normalRelativeAngleDegrees(
                e.getBearing() + getHeading() - getGunHeading()
        );

        setTurnGunRight(turnGunAmt);
        fire(3);

        moveDirection *= -1;
        setAhead(100 * moveDirection);
        execute();
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        moveDirection *= -1;
        setBack(120);
        setTurnRight(90);
        execute();
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        // se nosso alvo morrer → radar libera e varre tudo
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    // ----- HELPERS ----------------------------------------------------------------------

    private double chooseBulletPower(ScannedRobotEvent e) {
        double d = e.getDistance();
        double myE = getEnergy();

        if (d < 120 && myE > 25) return 3;
        if (d < 350) return 2;
        return 1;
    }

    private Point2D.Double project(double x, double y) {
        double w = getBattleFieldWidth();
        double h = getBattleFieldHeight();
        x = Math.max(20, Math.min(w - 20, x));
        y = Math.max(20, Math.min(h - 20, y));
        return new Point2D.Double(x, y);
    }

    private void avoidWalls() {
        double m = 70;
        if (getX() < m || getY() < m ||
                getX() > getBattleFieldWidth() - m ||
                getY() > getBattleFieldHeight() - m) {

            moveDirection *= -1;
            setBack(150);
            setTurnRight(80);
        }
    }
}
