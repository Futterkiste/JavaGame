import java.io.*;
import java.util.Properties;

/**
 * Created by Tobias on 10.01.2016.
 */
public class SpielerProfil {

    private String userName;
    private int gamesWon;
    private int gamesLost;
    private int highScore;

    public SpielerProfil(String name)
    {
        userName = name;
        gamesLost = 0;
        gamesWon = 0;
        highScore = 0;
    }

    public String toString() {return userName;}
    public void setHighScore(int score) {if(score>highScore) highScore = score;}
    public void wonGame() {++gamesWon;}
    public void lostGame() {++gamesLost;}
    public int getGamesPlayed() { return gamesLost + gamesWon;}
    public int getHighScore() {return highScore;}
    public int getGamesWon() {return gamesWon;}
    public int getGamesLost() {return gamesLost;}

    public void save()
    {
        Properties saveGame = new Properties();
        saveGame.setProperty("Name",userName);
        saveGame.setProperty("GewonneneSpiele",""+gamesWon);
        saveGame.setProperty("VerloreneSpiele",""+gamesLost);
        saveGame.setProperty("Highscore",""+highScore);
        try{
            saveGame.store(new FileOutputStream("profiles/"+userName+".properties"), "Profil von "+userName);
        }
        catch (Exception e)
        {
            System.out.println("Exception occured, Profile is not saved!");
        }
    }

    public void load()
    {
        try {
            Properties props = new Properties();
            FileInputStream in = new FileInputStream("profiles/" + userName + ".properties");
            props.load(in);
            in.close();
            gamesWon = Integer.parseInt(props.getProperty("GewonneneSpiele"));
            gamesLost = Integer.parseInt(props.getProperty("VerloreneSpiele"));
            highScore = Integer.parseInt(props.getProperty("Highscore"));
        }
        catch (Exception e) {
            System.out.println("Profile could not be loaded!");
        }
    }

}
