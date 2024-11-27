import javax.sound.midi.*;
public class MusicTest1 {
    public void play() {
        try {
            Sequencer sequencer = MidiSystem.getSequencer();
            System.out.println("Successfully got sequencer");
        }
        catch (MidiUnavailableException e) {
            System.out.println("Bummer");
        }
    }

    public static void main(String[] args) {
        System.out.println(tryCatchExperiment());
    }

    public static String tryCatchExperiment() {
        try {
            System.out.println("Try block has started");
            throw new RuntimeException("Oops!");
//            return "try-success";
        }
        catch(Exception e) {
            System.out.println("Catch block arriveth");
            throw e;
//            return "caught";
            
        }
        finally {
            System.out.println("And now we are here");
        }
    }

}
