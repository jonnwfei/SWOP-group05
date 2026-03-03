import java.util.List;

public class HumanStrategy implements Strategy {
    @Override
    public Bid determineBid() {
        return null;
    }

    @Override
    public Card chooseCardToPlay(List<Card> legalCards) {
        return null;
    }

    @Override
    public Boolean requiresConfirmation() {
        return true;
    }
}
