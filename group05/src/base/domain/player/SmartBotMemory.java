package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;

import java.util.ArrayList;
import java.util.List;

public class SmartBotMemory implements GameObserver {

    private Suit currentTrump;
    private final List<Card> unplayedCards;
    private final List<Bid> bidsMemory;
    private final List<PlayTurn> currentTrickPlayTurns;
    private final List<Player> playersAtTable;

    public SmartBotMemory() {
        this.currentTrump = null;
        this.unplayedCards = new Deck().getCards();
        this.bidsMemory = new ArrayList<>();
        this.currentTrickPlayTurns = new ArrayList<>();
        this.playersAtTable = new ArrayList<>();
    }

    // --- Observer Methods (Updating Memory) ---

    @Override
    public void onRoundStarted(List<PlayerId> players) {
        this.currentTrump = null;
        this.bidsMemory.clear();
        this.unplayedCards.clear();
        this.unplayedCards.addAll(new Deck().getCards());
        this.playersAtTable.clear();
        this.playersAtTable.addAll(players);
        this.currentTrickPlayTurns.clear();
    }

    @Override
    public void onTrumpDetermined(Suit trumpSuit) {
        this.currentTrump = trumpSuit;
    }

    @Override
    public void onBidPlaced(PlayerId, BidType) {this.bidsMemory.add(bid);}

    @Override
    public void onTurnPlayed(PlayTurn playTurn) {
        this.unplayedCards.remove(playTurn.playedCard());
        this.currentTrickPlayTurns.add(playTurn);

        if (this.currentTrickPlayTurns.size() == Trick.MAX_TURNS) {
            this.currentTrickPlayTurns.clear();
        }
    }

    // --- Getters ---

    public Suit getCurrentTrump() { return currentTrump; }

    public Bid getHighestBid() {return bidsMemory.stream().max(Bid::compareTo).orElse(null);}

    public Suit getLeadSuit() { return currentTrickPlayTurns.isEmpty() ? null : currentTrickPlayTurns.getFirst().playedCard().suit(); }

    public List<Player> getBidTeam(Player player) {
        Bid bid = bidsMemory.stream().filter(b -> b.getPlayer().equals(player)).findFirst().orElse(null);
        if (bid == null) {return new ArrayList<>();}
        return bid.getTeam(bidsMemory, playersAtTable);
    }

    public PlayTurn getCurrentWinningTurn() {
        Suit leadingSuit = getLeadSuit();
        Card bestCard = null;
        PlayTurn winningPlayTurn = null;

        for (PlayTurn playTurn : currentTrickPlayTurns) {
            Card playedCard = playTurn.playedCard();

            if (bestCard == null) {
                bestCard = playedCard;
                winningPlayTurn = playTurn;
                continue;
            }

            boolean isNewCardTrump = (this.currentTrump != null && playedCard.suit() == this.currentTrump);
            boolean isBestCardTrump = (this.currentTrickPlayTurns != null && bestCard.suit() == this.currentTrump);

            if (isNewCardTrump) {
                // Trump always beats non-trump; highest trump beats lower trump
                if (!isBestCardTrump || playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    bestCard = playedCard;
                    winningPlayTurn = playTurn;
                }
            } else if (!isBestCardTrump) {
                // If no trump is involved, highest rank of the leading suit wins
                if (playedCard.suit() == leadingSuit && playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    bestCard = playedCard;
                    winningPlayTurn = playTurn;
                }
            }
        }
        return winningPlayTurn;
    }

    // --- idk yet ---

    public boolean hasActiveProposal() {
        return this.bidsMemory.stream()
                .anyMatch(bid -> bid.getType() == BidType.PROPOSAL);
    }

    public boolean isLeadPlayer() {
        return this.currentTrickPlayTurns.isEmpty();
    }

    public boolean isTeamWinning(Player askingPlayer, TrickEvaluator rules) {
        PlayTurn winningPlayTurn = getCurrentWinningTurn(rules);

        // If the table is empty, no one is winning!
        if (winningPlayTurn == null) {
            return false;
        }

        List<Player> myTeam = getBidTeam(askingPlayer);
        return myTeam.contains(winningPlayTurn.player());
    }
}