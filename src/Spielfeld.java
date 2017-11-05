import javax.sound.sampled.Line;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

public class Spielfeld {

    private static int width;
    private static int heigth;

    private static SpielObject [][] spielfeld;
    private static Spieler spielFigur;
    private static LinkedList<Gegner> dynamischeGegner = new LinkedList<>();

    public static void initializeField(String dataName) //nur z.B. level_small nötig
    {
        try {
            Properties props = new Properties();
            FileInputStream in = new FileInputStream(dataName+".properties");
            props.load(in);
            in.close();

            String h = props.getProperty("Height");
            heigth = Integer.parseInt(h);
            String w = props.getProperty("Width");
            width = Integer.parseInt(w);
            spielfeld = new SpielObject[width][heigth];
            for(int y = 0; y < heigth; y++)
            {
                for (int x = 0; x < width; x++)
                {
                    String coord = props.getProperty(x+","+y,"99");
                    int objectCode = Integer.parseInt(coord);
                    switch (objectCode)
                    {
                        case 0: spielfeld[x][y] = new Wand();
                                break;
                        case 1: Koordinate k = new Koordinate(x,y);
                                spielFigur = new Spieler(k);
                                spielfeld[x][y] = spielFigur;
                                break;
                        case 2: spielfeld[x][y] = new Ausgang();
                                break;
                        case 3: Koordinate c = new Koordinate(x,y);
                                spielfeld[x][y] = new Gegner(false,c);
                                break;
                        case 4: Koordinate z = new Koordinate(x,y);
                                Gegner gegner = new Gegner(true,z);
                                dynamischeGegner.addLast(gegner);
                                spielfeld[x][y] = gegner;
                                break;
                        case 5: spielfeld[x][y] = new schluessel();
                                break;
                        default: spielfeld[x][y] = new Leer();
                                break;
                    }
                }
            }
            try {
                String key = props.getProperty("Key");
                if (key.compareTo("true") == 0) {
                    spielFigur.giveKey();
                }
                String leben = props.getProperty("Leben");
                spielFigur.setLeben(Integer.parseInt(leben));
            }
            catch (NullPointerException e){
            }
        }
        catch (IOException e)
        {
            System.out.println("File not found!");
        }
    }

    public static int getObjectOnCoord(int X, int Y) //Gibt das Objekt der Koordinate als Code aus
    {
        return spielfeld[X][Y].getObjectCode();
    }

    public static void printField()
    {
        for(int i = 0; i < width; i++)
        {
            for(int j = 0; j < heigth; j++)
            {
                System.out.print(spielfeld[j][i].getFieldSign()+"\t");
            }
            System.out.print("\n");
        }
    }

    public static int getWidth() {return width;}
    public static int getHeigth(){return heigth;}
    public static SpielObject[][] getSpielfeld() {return spielfeld;}
    public static Spieler getPlayer() {return spielFigur;}
    public static LinkedList<Gegner> getDynamicEnemies() {return dynamischeGegner;}

    //Methode, um im "Metadaten"-Spielfeld Spielerbewegungen zu verfolgen
    public static void movePlayerPosition(Koordinate current, Koordinate next)
    {
        spielfeld[current.x()][current.y()] = new Leer();
        spielFigur.setCoordinates(next);
        spielfeld[next.x()][next.y()] = spielFigur;
    }
    public static void moveEnemyPosition (Koordinate current, Koordinate next, Gegner enemy)
    {
        spielfeld[current.x()][current.y()] = new Leer();
        spielfeld[next.x()][next.y()] = enemy;
    }
}
