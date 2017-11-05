import java.io.File;
import java.util.LinkedList;

/**
 * Created by Tobias on 10.01.2016.
 */
public class Stammdaten {

    public Stammdaten() {
        highScores = new HighScoreTable();
        profile = new LinkedList<>();
    }

    private HighScoreTable highScores;
    private LinkedList<SpielerProfil> profile;

    public HighScoreTable getHighScores() {return highScores;}
    public void setNewPlayer(SpielerProfil p) {profile.addLast(p);}

    public void save()
    {
        highScores.save();
        for(int i = 0; i<profile.size();i++)
        {
            profile.get(i).save();
        }
    }

    public void load()
    {
        File players = new File("profiles");
        String[] profiles = players.list();
        for(int i = 0; i < profiles.length; i++)
        {
            String[] splittedSave = profiles[i].split(".properties",2);
            String spielerName = splittedSave[0];
            SpielerProfil p = new SpielerProfil(spielerName);
            setNewPlayer(p);
            p.load();
        }
        highScores.load();
    }

    public String[] getPlayers()
    {
        String[] playerNames = new String[profile.size()];
        for(int i = 0; i < profile.size(); i++)
        {
            playerNames[i] = profile.get(i).toString();
        }
        return playerNames;
    }

    public SpielerProfil getSpecificPlayer(String name)
    {
        for(int i = 0; i < profile.size(); i++)
        {
            if(profile.get(i).toString().equals(name))
            {
                return profile.get(i);
            }
        }
        return null;
    }
}
