/**
 * A class that represents a round played during the game
 * */

/**
 * @author Seppe De Houwer
 * @since 24/02/26
 */
public class Round {
    private List<Player> players;
    private Player currentPlayer;
    private Player dealer;

    private list<Trick> playedTricks = new ArrayList<>();
    private Trick currentTrick;

    private List<Bid> bids;
    private Suit trumpSuit;
    private int multiplier;
    private boolean finished;

    /**
     * @param players
     * @param dealer
     * @param currentplayer
     * @param multiplier
     * @throws IllegalArgumentException if players or dealer is null
     * @throws IllegalArgumentException if there are more than 4 players
     */
    public Round(List<Player> players, Player dealer, Player currentplayer, int multiplier) {
        if (players == null || players.size() != 4) {
            throw new IllegalArgumentException();
        }
        this.players = new ArrayList<>(players);
        if (dealer == null){
            throw new IllegalArgumentException();
        }
        else {
            this.dealer = dealer;
        }

        this.trumpSuit = trumpSuit;
        this.multiplier = multiplier;
        this.bids = new ArrayList<>(null,null,null,null);
    }

    /**
     * @param deck
     * @throws IllegalArgumentException when a deck is not 52 cards
     */
    public void deal(List<Card> deck) {
        if (deck.size() != 52) {
            throw new IllegalArgumentException();
        }
        Iterator<Card> it = deck.iterator();
        int index = 0;
        Card lastDealt = null;
        while (it.hasNext()) {
            Card c = it.next();
            Player p = players.get(index % 4);
            p.addCard(c);
            lastDealt = c;
            index++;
        }
        trumpSuit = lastDealt.getSuit();
    }

    /**
     * this plays a round, unmless there are already 13 rounds played.
     */
    public void playRound() {
        if (finished) {
            return();
        }
        while (tricks.size() < 13) {
            currentTrick = new Trick(Player currentPlayer, Suit trumpsuit);
            currentTrick.playTrick();
            currentPlayer = currentTrick.getWinner();
            tricks.add(currentTrick);
            }
        finished = true;
        }
    }

    /**
     * getters voor info van de class
     * @return values
     */
    public List<Player> getPlayers() {return players; }
    public Player getCurrentPlayer() { return currentPlayer; }
    public List<Bid> getBids() { return bids; }
    public boolean isFinished() { return finished; }
    public Suit getTrumpSuit() { return trumpSuit; }
    public Trick getLastPlayedTrick() {
        if (tricks.isEmpty()) {
            return null;
        }
        return tricks.get(tricks.size() - 1);
    }

}
