/**
 * Created by Tobias on 27.12.2015.
 */
public class Koordinate{

    private int x;
    private int y;

    public Koordinate(int X, int Y)
    {
        x = X;
        y = Y;
    }

    public boolean compareTo(Koordinate other)
    {
        if(this.x == other.x() && this.y == other.y())
        {
            return true;
        }
        return false;
    }

    public int x(){return x;}
    public int y(){return y;}

    public void setX(int X){x = X;}
    public void setY(int Y){y = Y;}

    public String toString() {return "X: "+x+", Y: "+y;}
}
