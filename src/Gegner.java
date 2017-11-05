/**
 * Created by Tobias on 27.12.2015.
 */
public class Gegner extends SpielObject implements Movement{

    private boolean canMove;
    private Koordinate k;

    public Gegner(boolean move, Koordinate c)
    {
        k = c;
        canMove = move;
    }

    public int getObjectCode()
    {
        if(canMove)
        {
            return 4;
        }
        return 3;
    }
    public Koordinate getK() {return k;}
    public void setK(Koordinate c) { k = c;}
    public char getFieldSign()
    {
        if(canMove)
        {
            return '\u0264';
        }
        return '\u02a2';
    }

    /* return 0, wenn der Zug legitim ist
       return 1, wenn der Spieler damit getroffen wird
       return 2, wenn der Zug ungültig ist*/
    public int canMove(int x, int y, Spielfeld feld)
    {
        try
        {
            if (k.x() + 1 == x || k.x() - 1 == x || k.y() + 1 == y || k.y() - 1 == y)
            {
                if (feld.getObjectOnCoord(x, y) == 99) {
                    return 0;
                }
                else if(feld.getObjectOnCoord(x, y) == 6)
                {
                    return 1;
                }
                return 2;
            }
            return 2;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return 2;
        }
    }
}
