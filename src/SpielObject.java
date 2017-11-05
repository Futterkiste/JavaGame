/**
 * Created by Tobias on 27.12.2015.
 */
public abstract class SpielObject {

    public boolean isCollision(Koordinate k,Koordinate other)
    {
        if(other.x() == k.x() && other.y() == k.y())
        {
            return true;
        }
        return false;
    }

    public abstract int getObjectCode();

    public abstract char getFieldSign();
}
