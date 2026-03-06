package cli.elements;

public class Response{
    private final Boolean keepRunning;
    private final String content;


    public Response(String s, Boolean b){
        this.keepRunning = b;
        this.content = s;
    }

    public Boolean getContinue(){
        return this.keepRunning;
    }
    public String getContent(){
        return this.content;
    }
}