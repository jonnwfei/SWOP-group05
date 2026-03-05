package base.domain.round;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

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

    public void askBids() {
        playerbids = new ArrayList<>();
        for (Player p : players) {
            playerbids.add(p.askBid());
        }
        this.bids = playerbids
        return;
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
            currentPlayer = currentTrick.getWinningPlayer();
            tricks.add(currentTrick);
            }
        finished = true;
        }
    }

    public HashMap<Player, Integer> calculateScores() {
        HashMap<Player, Integer> result = new HashMap<>();
        for (Player p : players) {
            result.put(p, 0);
        }
        if (!finished) {
            return result;
        }
        for (Bid bid : bids) {
            if (bid.getType != PROPOSAL && bid.ACCEPTANCE
            int points = bid.calculatePoints(bid, getTricks());
            Player bidder = bid.getBidder();

            result.put(bidder, result.get(bidder) + points);

            for (Player p : players) {
                if (!p.equals(bidder)) {
                    result.put(p, result.get(p) - (points / 3));
                }
            }
        }
        return result;
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
    public setTrumpSuit(Suit trump) {trumpSuit = trump; }
    public List<Trick> getTricks() { return playedTricks; }
    public Trick getLastPlayedTrick() {
        if (tricks.isEmpty()) {
            return null;
        }
        return tricks.get(tricks.size() - 1);
    }

}
