package classes.Pipeline;

public class HoughLinePair {

    public final HoughLine line1;
    public final HoughLine line2;

    public final int angleOfIntersection;

    public HoughLinePair(HoughLine line1, HoughLine line2, int angleOfIntersection){
        this.line1 = line1;
        this.line2 = line2;
        this.angleOfIntersection = angleOfIntersection;

    }
}
