enum BidType {
    PASS, ASK_PARTNER, SOLO, OPEN_MISERIE, MISERIE
}

/**
 * @author Seppe De Houwer
 * @since 24/2/26
 */
class Bid {
    private Player bidder;
    private BidType type;

    /**
     * @param bidder
     * @param type
     */
    public Bid(Player bidder, BidType type) {
        if (bidder == null) {
            throw IllegalArgumentException();
        }
            this.bidder = bidder;
        if (type == null) {
            throw IllegalArgumentException();
        }
        this.type = type;
    }

    /**
     * @return the bidder
     */
    public Player getBidder() {
        return bidder;
    }

    /**
     * @return the bidtype
     */
    public BidType getType() {
        return type;
    }
}