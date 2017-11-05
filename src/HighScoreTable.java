import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Created by Tobias on 10.01.2016.
 */
public class HighScoreTable {

    private LinkedList<Integer> leaderScores = new LinkedList<>();

    public void setNewScore(int score)
    {
        leaderScores.addLast(score);
    }

    public void sortList()
    {
        int temp;
        for(int i = 1; i < leaderScores.size(); i++)
        {
            temp = leaderScores.get(i);
            int j = i;
            while (j > 0 && leaderScores.get(j-1) < temp)
            {
                leaderScores.set(j,leaderScores.get(j-1));
                j--;
            }
            leaderScores.set(j,temp);
        }
    }

    public String[] getOverallHighscores()
    {
        sortList();
        String[] ausgabe = new String[leaderScores.size()];
        for (int i = 0; i < leaderScores.size(); i++)
        {
            ausgabe[i] = ""+leaderScores.get(i);
        }
        return ausgabe;
    }

    public void save()
    {
        Properties saveGame = new Properties();
        saveGame.setProperty("Length",""+leaderScores.size());
        for(int i = 0; i < leaderScores.size(); i++)
        {
            saveGame.setProperty(""+i,""+leaderScores.get(i));
        }

        try{
            saveGame.store(new FileOutputStream("parameters/highScores.properties"),"");
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
            FileInputStream in = new FileInputStream("parameters/highScores.properties");
            props.load(in);
            in.close();
            int propsLength = Integer.parseInt(props.getProperty("Length"));
            for(int i = 0; i < propsLength; i++)
            {
                setNewScore(Integer.parseInt(props.getProperty(""+i)));
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("Highscores-File could not be loaded!");
        }
        catch (IOException e) {
            System.out.println("Highscores Error occured");
        }
    }
}
