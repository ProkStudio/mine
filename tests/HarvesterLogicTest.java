import com.harvester.entity.HarvesterLogic;

/** Dependency-free regression checks. Run with JDK 21 or newer. */
public final class HarvesterLogicTest {
    private static int assertions;
    private static void check(boolean value) {
        assertions++;
        if (!value) throw new AssertionError("Failed assertion " + assertions);
    }
    private static void near(double actual, double expected) {
        check(Math.abs(actual - expected) < 1e-8);
    }
    public static void main(String[] args) {
        check(!HarvesterLogic.isWorking(true, 100, 0));
        check(!HarvesterLogic.isWorking(true, 100, 1e-8));
        check(!HarvesterLogic.isWorking(false, 100, .1));
        check(!HarvesterLogic.isWorking(true, 0, .1));
        check(!HarvesterLogic.isWorking(true, -1, .1));
        check(!HarvesterLogic.isWorking(true, 100, Double.NaN));
        check(!HarvesterLogic.isWorking(true, 100, Double.POSITIVE_INFINITY));
        check(HarvesterLogic.isWorking(true, 100, .1));
        near(HarvesterLogic.safeSpeed(Double.NaN), .15);
        near(HarvesterLogic.safeSpeed(Double.POSITIVE_INFINITY), .15);
        near(HarvesterLogic.safeSpeed(-1), .05);
        near(HarvesterLogic.safeSpeed(2), .6);
        near(HarvesterLogic.safeSpeed(.3), .3);
        near(HarvesterLogic.steer(40, 0, 3), 40);
        near(HarvesterLogic.steer(40, 1, 3), 43);
        near(HarvesterLogic.steer(40, -1, 3), 37);
        near(HarvesterLogic.steer(179, 1, 3), -178);
        near(HarvesterLogic.steer(0, 99, 3), 3);
        check(HarvesterLogic.fuelAfter(100, 1, 2, false, false) == 100);
        check(HarvesterLogic.fuelAfter(100, 1, 2, true, false) == 99);
        check(HarvesterLogic.fuelAfter(100, 1, 2, true, true) == 97);
        check(HarvesterLogic.fuelAfter(1, 1, 2, true, true) == 0);
        check(HarvesterLogic.fuelAfter(100, -1, -2, true, true) == 100);
        check(HarvesterLogic.fuelAfter(100, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true) == 0);
        check(HarvesterLogic.withinLimit(11, 12));
        check(!HarvesterLogic.withinLimit(12, 12));
        check(!HarvesterLogic.withinLimit(0, 0));
        check(!HarvesterLogic.withinLimit(-1, 12));
        check(HarvesterLogic.diggable(3, 3, false, false, false));
        check(!HarvesterLogic.diggable(-1, 3, false, false, false));
        check(!HarvesterLogic.diggable(4, 3, false, false, false));
        check(!HarvesterLogic.diggable(1, 3, true, false, false));
        check(!HarvesterLogic.diggable(1, 3, false, true, false));
        check(!HarvesterLogic.diggable(1, 3, false, false, true));
        check(!HarvesterLogic.diggable(Double.NaN, 3, false, false, false));
        double[][] expected = {{0,3},{-3,0},{0,-3},{3,0}};
        for (int i=0; i<4; i++) {
            double[] p=HarvesterLogic.headerOffset(i*90,0,3);
            near(p[0],expected[i][0]); near(p[1],expected[i][1]);
        }
        for (int yaw=-360; yaw<=360; yaw+=15) for (int lateral=-4; lateral<=4; lateral++) {
            double[] p=HarvesterLogic.headerOffset(yaw,lateral,3);
            near(p[0]*p[0]+p[1]*p[1],lateral*lateral+9);
            double[] wrapped=HarvesterLogic.headerOffset(yaw+360,lateral,3);
            near(p[0],wrapped[0]); near(p[1],wrapped[1]);
        }
        System.out.println("PASS: " + assertions + " transport rule assertions");
    }
}
