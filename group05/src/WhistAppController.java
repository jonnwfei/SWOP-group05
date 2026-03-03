
/*
  @author Stan Kestens
  @since 25/02/2026
 */
public class WhistAppController {

    private Game game;
    private State state;

    public WhistAppController() {
        this.game = new Game(); // Game just holds players and rounds now
        this.state = new MenuState();
    }

    // You still use the exact pattern from the document!
    private abstract class State {
        abstract void executeState();
        abstract void nextState();
    }

    private class MenuState extends State {
        // TODO: zorgen dat alles gereset wordt
        private String option;  // store user choice

        @Override
        void executeState() {

            Scanner scanner = new Scanner(System.in);
            System.out.println("=== MAIN MENU ===");
            System.out.println("1. Start game");
            System.out.println("2. Start count");
            option = scanner.nextLine();


            System.out.println("=== PLAYER NAMES ===");
            for (int i = 1; i <= 4; i++) {
                System.out.println("Player " + i + " name: ");
                String inputName = scanner.nextLine();
                // Create a new Player object and add it to our list
                players.add(new Player(inputName));
            }

            @Override
            void nextState () {
                if ("1".equals(option)) {
                    changeState(new PlayState());
                } else if ("2".equals(option)) {
                    changeState(new CountState());
                } else {
                    System.out.println("Invalid option");
                    changeState(new MenuState());
                }
            }
        }
    }
    private class BidState extends State {

        private boolean biddingFinished;

        @Override
        void executeState() {
            System.out.println("Bidding...");
            round = new Round(players, dealerPlayer,currentPlayer, 1);
            rounds.add(ronde);
            round.collectBids();
            biddingFinished = true; // placeholder
        }

        @Override
        void nextState() {
            if (biddingFinished) {
                changeState(new PlayState());
            }
        }
    }

    private class PlayState extends State {

        private boolean roundFinished;

        @Override
        void executeState() {
            System.out.println("Playing...");
            round = rounds.get(rounds.size()-1)
            round.playRound();
            Scanner scanner = new Scanner(System.in);
            System.out.println("==Round Done!== ");
            printTussenstand();
            System.out.println("Would you like to: ");
            System.out.println("(1) Play a new round");
            System.out.println("(2) Go to the menu");
            option = scanner.nextLine();
            roundFinished = true; // placeholder
        }

        @Override
        void nextState() {
            if (roundFinished) {
                if ("1".equals(option)) {
                    changeState(new PlayState());
                }
                else if ("2".equals(option)) {
                    changeState(new MenuState());
                }
                else {
                    System.out.println("Invalid option");
                    changeState(new MenuState());
                }
            }
        }
    }
    private class CountState extends State{

    }
    private void printTussenstand(){
        System.out.println("== Score ==")
        for (i = 0; i<=4; i++){
            player = players.get(i);
            score = player.getScore();
            System.out.println("Score player : " + i + ": " + score);
        }
    }



    public void run() {
        while (true) {
            state.executeState();
            state.nextState();
        }
    }
}