package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.trick.Trick;
import base.domain.trick.Turn;

import java.util.ArrayList;
import java.util.List;

public class SmartBotMemory implements GameObserver {

    private Suit currentTrump;
    private final List<Card> unplayedCards;
    private final List<Bid> bidsMemory;
    private final List<Turn> currentTrickTurns;
    private final List<Player> playersAtTable;

    public SmartBotMemory() {
        this.currentTrump = null;
        this.unplayedCards = new Deck().getCards();
        this.bidsMemory = new ArrayList<>();
        this.currentTrickTurns = new ArrayList<>();
        this.playersAtTable = new ArrayList<>();
    }

    // --- Observer Methods (Updating Memory) ---

    @Override
    public void onRoundStarted(List<Player> players) {
        this.currentTrump = null;
        this.bidsMemory.clear();
        this.unplayedCards.clear();
        this.unplayedCards.addAll(new Deck().getCards());
        this.playersAtTable.clear();
        this.playersAtTable.addAll(players);
        this.currentTrickTurns.clear();
    }

    @Override
    public void onTrumpDetermined(Suit trumpSuit) {
        this.currentTrump = trumpSuit;
    }

    @Override
    public void onBidPlaced(Bid bid) {this.bidsMemory.add(bid);}

    @Override
    public void onTurnPlayed(Turn turn) {
        this.unplayedCards.remove(turn.playedCard());
        this.currentTrickTurns.add(turn);

        if (this.currentTrickTurns.size() == Trick.MAX_TURNS) {
            this.currentTrickTurns.clear();
        }
    }

    // --- Getters ---

    public Suit getCurrentTrump() { return currentTrump; }

    public Bid getHighestBid() {return bidsMemory.stream().max(Bid::compareTo).isPresent() ? bidsMemory.getFirst() : null;}

    public Suit getLeadSuit() { return currentTrickTurns.isEmpty() ? null : currentTrickTurns.getFirst().playedCard().suit(); }

    public List<Player> getBidTeam(Player player) {
        Bid bid = bidsMemory.stream().filter(b -> b.getPlayer().equals(player)).findFirst().orElse(null);
        if (bid == null) {return new ArrayList<>();}
        return bid.getTeam(bidsMemory, playersAtTable);
    }

    public Player getWinningPLayer() {
        Suit leadingSuit = getLeadSuit();
        Player currentWinner = null;
        Card bestCard = null;

        for (Turn turn : currentTrickTurns) {
            Player player = turn.player();
            Card playedCard = turn.playedCard();

            if (bestCard == null) {
                currentWinner = player;
                bestCard = playedCard;
                continue;
            }

            boolean isNewCardTrump = (this.currentTrump != null && playedCard.suit() == this.currentTrump);
            boolean isBestCardTrump = (this.currentTrickTurns != null && bestCard.suit() == this.currentTrump);

            if (isNewCardTrump) {
                // Trump always beats non-trump; highest trump beats lower trump
                if (!isBestCardTrump || playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    currentWinner = player;
                    bestCard = playedCard;
                }
            } else if (!isBestCardTrump) {
                // If no trump is involved, highest rank of the leading suit wins
                if (playedCard.suit() == leadingSuit && playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    currentWinner = player;
                    bestCard = playedCard;
                }
            }
        }
        return  currentWinner;
    }

    // --- idk yet ---

    public boolean hasActiveProposal() {
        return this.bidsMemory.stream()
                .anyMatch(bid -> bid.getType() == BidType.PROPOSAL);
    }

    public boolean isLeadPlayer() {
        return this.currentTrickTurns.isEmpty();
    }
}