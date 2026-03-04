import java.util.List;

/**
 * @author Seppe De Houwer, Tommy Wu
 * @since 24/2/26
 */
public interface Bid extends Comparable<Bid> {
    List<Player> getTeam();
    int calculateBasePoints(int tricksWon);
    boolean checkWin(int tricksWon);
    BidRank getRank();

    @Override
    default int compareTo(Bid other) {
        return this.getRank().compareTo(other.getRank());
    }
}