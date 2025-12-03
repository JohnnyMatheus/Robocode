<h1 align="center">Inteligência Artificial - Robocode</h1>

## <p align="center">👨🏽‍🎓Nome completo: Johnny Matheus Nogueira de Medeiro, Nathaniel Nicolas Rissi Soares, Nelson Ramos Rodrigues Junior</p>

## <p align="center">🏫Turma: Ciências da Computação UNOESC - São Miguel do Oeste</p>

<hr />

<p align="justify">O Robocode é uma plataforma/ambiente de programação criada para ensinar lógica de programação, inteligência artificial e Java através de batalhas entre robôs virtuais.</p>

<p>Objetivo: Criar um robô com lógica de:</p>

- Movimento inteligente
- Mira precisa
- Tiro eficiente
- Estratégia defensiva

<h1 align="center"> Anatomia do robô</h1>

<div align="center">
<img src="https://github.com/JohnnyMatheus/Robocode/blob/main/imagens/anatomia%20do%20robo.jpg">

</div>

- Corpo – Suporta a arma com o radar na parte superior. O corpo é usado para mover o robô para frente e para trás, bem como para virar à esquerda ou à direita.
- Canhão – Montado no corpo e usado para disparar balas de energia. O canhão pode girar para a esquerda ou para a direita. Possui um radar na parte superior.
- Radar – Montado na arma, é usado para detectar outros robôs quando em movimento. O radar pode girar para a esquerda ou para a direita. Ele gera onScannedRobot()alertas quando detecta robôs.

<h1 align="center"> Estratégia do Robô CoderTankRobot </h1>

<p align="justify">O CoderTankRobot foi projetado para ser um robô competitivo, combinando um radar extremamente eficiente, mira preditiva e movimentos evasivos inteligentes. Ele utiliza recursos avançados do Robocode através da classe AdvancedRobot, permitindo ações simultâneas (andar, mirar e escanear ao mesmo tempo).</p>

<div align="center">

| Sistema                           | Função                                |
| --------------------------------- | ------------------------------------- |
| **Radar Ultra Lock**              | Mantém alvo travado 100% do tempo     |
| **Desvio por Energy Drop**        | Detecta tiros inimigos e desvia       |
| **Mira preditiva linear**         | Calcula onde o inimigo estará         |
| **Movimento lateral inteligente** | Dificulta ser atingido                |
| **Anti-Wall**                     | Evita bordas da arena                 |
| **Tiros inteligentes**            | Potência otimizada conforme distância |
| **Reação a impacto**              | Muda padrão quando acertado           |

</div>

<div align="center">
    <img src= "https://github.com/JohnnyMatheus/Robocode/blob/main/video/Video%20Robocode.gif"/>
</div>

# Código

```
package CoderTank;

import robocode.*;
import robocode.util.Utils;
import java.awt.*;
import java.awt.geom.Point2D;

/**
 * CoderTankRobot - versão TURBO com Radar Ultra Lock
 * Autores: Johnny Matheus Nogueira de Medeiro, Nathaniel Nicolas Rissi Soares, Nelson Ramos Rodrigues Junior.
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
```


## 🧠 Desenvolvedores

| [<img src="https://avatars.githubusercontent.com/u/128015032?v=4" width=115><br>👑Game Master👑<br><sub>🐦‍🔥Johnny Matheus Nogueira de Medeiro🐦‍🔥</sub>](https://github.com/JohnnyMatheus) | [<img src="https://avatars.githubusercontent.com/u/166051346?v=4" width=115><br><sub>Nelson Ramos Rodrigues Junior</sub>](#) | [<img src="https://avatars.githubusercontent.com/u/165223471?v=4" width=115><br><sub>Nathaniel Nicolas Rissi Soares</sub>](#) |
| :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------------: |

## 🔷 Professor

| [<img src="https://avatars.githubusercontent.com/u/7074409?v=4" width=115><br><sub>Vinicius Almeida dos Santos</sub>](https://github.com/ViniciusAS) |
| :--------------------------------------------------------------------------------------------------------------------------------------------------: |
