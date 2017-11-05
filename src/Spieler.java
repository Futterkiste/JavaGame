/**
 * Created by Tobias on 27.12.2015.
 */
public class Spieler extends SpielObject implements Movement{

    private Koordinate k; //Aktuelle Koordinate
    private Koordinate startKoordinate;
    private boolean key;
    private int leben = 3;

    public Spieler (Koordinate c)
    {
        startKoordinate = c;
        k = startKoordinate;
    }

    public int getObjectCode()
    {
        return 6;
    }
    public int getLeben() {return leben;}
    public char getFieldSign()
    {
        return '\u263a';
    }
    public Koordinate getCoordinates() {return k;}
    public Koordinate getStartKoordinate() {return startKoordinate;}

    public void setCoordinates(Koordinate c) {k=c;}
    public void setStartKoordinate(Koordinate c) {startKoordinate = c;}

    public boolean hasKey()
    {
        if(key)
        {
            return true;
        }
        return false;
    }

    /*return 0: Zug gültig
      return 1: Wand im Weg
      return 2: Gegner berührt
      return 3: Versuch, mehr als eine Position zu bewegen, oder eine ArrayOutOfBoundException*/
    public int canMove(int x, int y, Spielfeld feld)
    {
        try {
            if (k.x() + 1 == x || k.x() - 1 == x || k.y() + 1 == y || k.y() - 1 == y) {
                if (feld.getObjectOnCoord(x, y) == 0) {
                    return 1;
                } else if (feld.getObjectOnCoord(x, y) == 3 || feld.getObjectOnCoord(x, y) == 4) {
                    return 2;
                }
                else if (feld.getObjectOnCoord(x, y) == 5)
                {
                    return 0;
                }
                else if (feld.getObjectOnCoord(x, y) == 2)
                {
                    if(key)
                    {
                        return 0;
                    }
                    else
                    {
                        return 1;
                    }
                }
            } else {
                return 3;
            }
            return 0;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return 3;
        }
    }

    public void zieheLebenAb() {--leben;}
    public void gibLeben() {++leben;}
    public void giveKey() {key = true;}
    public void setLeben (int i) {leben = i;}
}
