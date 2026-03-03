//@author: Tommy Wu
//@since: 24/02/2026
import java.util.List;

public interface Strategy {
    Bid determineBid();
    Card chooseCardToPlay(List<Card> legalCards);
    Boolean requiresConfirmation();
}
