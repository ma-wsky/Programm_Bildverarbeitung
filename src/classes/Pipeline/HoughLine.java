package classes.Pipeline;

public record HoughLine(int phi, int r, int votes) implements Comparable<HoughLine> {

    @Override
    public int compareTo(HoughLine other) {
        return Integer.compare(other.votes, this.votes);
    }
}
