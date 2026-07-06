package classes.Pipeline;

public class HoughLine implements Comparable<HoughLine>{

    public final int phi;
    public final int r;
    public final int votes;

    public HoughLine(int phi, int r, int votes){
        this.phi = phi;
        this.r = r;
        this.votes = votes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HoughLine houghLine = (HoughLine) o;
        return phi == houghLine.phi && r == houghLine.r;
    }

    @Override
    public int compareTo(HoughLine other) {
        return Integer.compare(other.votes, this.votes);
    }
}
