import com.harvester.entity.HarvesterLogic;

/** No Minecraft runtime or test dependencies required. Throws on regression. */
public final class HarvesterLogicTest {
    private static int assertions;
    private static void check(boolean value) {
        assertions++;
        if (!value) throw new AssertionError("Failed assertion " + assertions);
    }
    private static void near(double actual, double expected) { check(Math.abs(actual - expected) < 1e-9); }
    public static void main(String[] args) {
        check(!HarvesterLogic.isWorking(true, 100, 0));
        check(!HarvesterLogic.isWorking(true, 100, 1e-8));
        check(!HarvesterLogic.isWorking(false, 100, 0.1));
        check(!HarvesterLogic.isWorking(true, 0, 0.1));
        check(!HarvesterLogic.isWorking(true, -1, 0.1));
        check(!HarvesterLogic.isWorking(true, 100, Double.NaN));
        check(!HarvesterLogic.isWorking(true, 100, Double.POSITIVE_INFINITY));
        check(HarvesterLogic.isWorking(true, 100, 0.1));
        near(HarvesterLogic.safeSpeed(Double.NaN), 0.25);
        near(HarvesterLogic.safeSpeed(Double.POSITIVE_INFINITY), 0.25);
        near(HarvesterLogic.safeSpeed(Double.NEGATIVE_INFINITY), 0.25);
        near(HarvesterLogic.safeSpeed(-1), 0.05);
        near(HarvesterLogic.safeSpeed(2), 1);
        near(HarvesterLogic.safeSpeed(0.3), 0.3);
        double[][] expected = {{0,3},{-3,0},{0,-3},{3,0}};
        for (int i=0; i<4; i++) {
            double[] p = HarvesterLogic.headerOffset(i*90,0,3);
            near(p[0],expected[i][0]); near(p[1],expected[i][1]);
        }
        for (int yaw=-360; yaw<=360; yaw+=15) {
            for (int lateral=-4; lateral<=4; lateral++) {
                double[] p=HarvesterLogic.headerOffset(yaw,lateral,3);
                near(p[0]*p[0]+p[1]*p[1], lateral*lateral+9);
                double[] wrapped=HarvesterLogic.headerOffset(yaw+360,lateral,3);
                near(p[0],wrapped[0]); near(p[1],wrapped[1]);
            }
        }
        System.out.println("PASS: " + assertions + " rule/geometry assertions");
    }
}
