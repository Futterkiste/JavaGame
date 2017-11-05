import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalSize;

import java.io.*;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;

public class Spiel {

    private static String levelToLoad="level\\"+"level_big_testLevel"; //Standardwert für das Level, das zum Spielen geladen werden soll
    private static Koordinate drawnPlayerCoord;
    private static Koordinate drawnCurrent;
    private static int difficulty = 1; //0 = easy, 1 = normal, 2 = hard
    private static TerminalSize terminalSize;
    private static Stammdaten daten;
    private static SpielerProfil aktiverSpieler;

    //Variablen, um die Spielzeit zu messen
    private static long gameStarted = System.currentTimeMillis();
    private static long currentTime = System.currentTimeMillis()-gameStarted;
    private static int timePassed = 0;

    public static void game()
    {
        daten = new Stammdaten();
        daten.load();
        Terminal ausgabeTerminal = TerminalFacade.createSwingTerminal();
        terminalSize = ausgabeTerminal.getTerminalSize();
        Spielfeld feld = new Spielfeld();
        ausgabeTerminal.enterPrivateMode();

        //Begrüßungs-Screen
        welcomeScreen(ausgabeTerminal);

        //Start-Screen
        while(startScreen(ausgabeTerminal,feld))
        {
            ausgabeTerminal.clearScreen();
            ausgabeTerminal.moveCursor(0,0);
            writeStringInTerminal(ausgabeTerminal,"Bitte warten...");
            feld.initializeField(levelToLoad);
            ausgabeTerminal.clearScreen();
            drawTerminalRelative(ausgabeTerminal, feld);
            gameStarted = System.currentTimeMillis();
            ausgabeTerminal.moveCursor(drawnPlayerCoord.x(), drawnPlayerCoord  .y());
            if(!gameControls(ausgabeTerminal, feld))
            {
                daten.save();
                System.exit(0);
                break;
            }
        }
    }

    //Gibt das Spielfeld relativ zur Fenstergröße aus
    public synchronized static void drawTerminalRelative(Terminal input, Spielfeld game)
    {
        input.clearScreen();
        Koordinate spielerCoord = game.getPlayer().getCoordinates();
        try {
            if (spielerCoord.y() - input.getTerminalSize().getRows() / 2 > 0) {
                int i = 0;
                for (int y = spielerCoord.y() - input.getTerminalSize().getRows() / 2;
                     y < spielerCoord.y() + (input.getTerminalSize().getRows() / 2 - 2) && y < game.getHeigth(); y++) {
                    input.moveCursor(0, i);
                    i++;
                    if (spielerCoord.x() - input.getTerminalSize().getColumns() / 2 > 0) {
                        for (int x = spielerCoord.x() - input.getTerminalSize().getColumns() / 2;
                             x < spielerCoord.x() + input.getTerminalSize().getColumns() / 2 && x < game.getWidth(); x++) {
                            setObjectSign(game.getSpielfeld()[x][y], input);
                        }
                        Koordinate dpc = new Koordinate(input.getTerminalSize().getColumns() / 2, input.getTerminalSize().getRows() / 2); //Kurz für DrawnPlayerCoord
                        drawnPlayerCoord = dpc;
                        drawnCurrent = drawnPlayerCoord;
                    } else {
                        for (int x = 0; x < input.getTerminalSize().getColumns() && x < game.getWidth(); x++) {
                            //input.putCharacter(game.getSpielfeld()[x][y].getFieldSign());
                            setObjectSign(game.getSpielfeld()[x][y], input);
                        }
                        Koordinate dpc = new Koordinate(game.getPlayer().getCoordinates().x(), input.getTerminalSize().getRows() / 2); //Kurz für DrawnPlayerCoord
                        drawnPlayerCoord = dpc;
                        drawnCurrent = drawnPlayerCoord;
                    }
                }
            } else {
                for (int y = 0; y < input.getTerminalSize().getRows() - 2 && y < game.getHeigth(); y++) {
                    input.moveCursor(0, y);
                    if (spielerCoord.x() - input.getTerminalSize().getColumns() / 2 > 0) {
                        for (int x = spielerCoord.x() - input.getTerminalSize().getColumns() / 2;
                             x < spielerCoord.x() + input.getTerminalSize().getColumns() / 2 && x < game.getWidth(); x++) {
                            //input.putCharacter(game.getSpielfeld()[x][y].getFieldSign());
                            setObjectSign(game.getSpielfeld()[x][y], input);
                            Koordinate dpc = new Koordinate(input.getTerminalSize().getColumns() / 2, game.getPlayer().getCoordinates().y()); //Kurz für DrawnPlayerCoord
                            drawnPlayerCoord = dpc;
                            drawnCurrent = drawnPlayerCoord;
                        }
                    } else {
                        for (int x = 0; x < input.getTerminalSize().getColumns() && x < game.getWidth(); x++) {
                            //input.putCharacter(game.getSpielfeld()[x][y].getFieldSign());
                            setObjectSign(game.getSpielfeld()[x][y], input);
                            drawnPlayerCoord = game.getPlayer().getCoordinates();
                            drawnCurrent = drawnPlayerCoord;
                        }
                    }
                }
            }
            input.moveCursor(drawnCurrent.x(), drawnCurrent.y());

            //Ein leerer Thread, der nur dafür da ist, writeGameInformation auszuführen (nicht die schönste, aber zweckmäßige Lösung)
            Thread t = new Thread();
            writeGameInformation(input, game, t);
        }

        //Behebt den seltenen Bug, dass sich während dem Zeichnen die Figuren außerhalb des Arrays bewegen und sich damit das Programm aufhängt
        catch (ArrayIndexOutOfBoundsException e)
        {
            drawTerminalRelative(input, game);
        }
    }

    //Misst die Zeit, die bereits während eines Spieles vergangen ist
    public static void updateTimePassed()
    {
        currentTime = System.currentTimeMillis();
        timePassed = (int) ((currentTime - gameStarted)/1000);
    }

    //Standardmethode, um in Menüs auf die Eingabe irgendeiner Taste zu warten
    public static void waitForAnyKey(Terminal terminal)
    {
        boolean input = true;
        while (input)
        {
            try{
                Key key = terminal.readInput();
                if(key.getKind() != null)
                {
                    input = false;
                }
            }
            catch (NullPointerException e){continue;}
        }
    }

    //Methode, die überprüft, ob sich die Größe des Terminal-Fensters geändert hat
    public synchronized static void checkTerminalRescale (Terminal terminal, Spielfeld feld, Thread t)
    {
        //Wenn das aktuelle Terminal in seiner Größe dem gespeicherten Terminal abweicht, wird das Terminal neu gezeichnet
        if(terminalSize.getColumns() != terminal.getTerminalSize().getColumns()
         ||terminalSize.getRows()    != terminal.getTerminalSize().getRows())
        {
            t.suspend();
            drawTerminalRelative(terminal, feld);
            terminalSize = terminal.getTerminalSize();
            t.resume();
        }
    }

    //Schreibt /aktualisiert die Spielinformationen in der letzten Zeile des Spiels
    public synchronized static void writeGameInformation(Terminal terminal, Spielfeld feld, Thread t)
    {
        t.suspend();
        terminal.moveCursor(0,terminal.getTerminalSize().getRows());
        terminal.applyForegroundColor(Terminal.Color.YELLOW);
        writeStringInTerminal(terminal,"                                                                                     ");
        terminal.moveCursor(0,terminal.getTerminalSize().getRows());
        writeStringInTerminal(terminal,"Leben: "+feld.getPlayer().getLeben()+"  Schlüssel: "+feld.getPlayer().hasKey()+"  Zeit: "+currentTime+" Sekunden");
        terminal.applyForegroundColor(Terminal.Color.WHITE);
        terminal.moveCursor(drawnCurrent.x(),drawnCurrent.y());
        t.resume();
    }

    //Berechnet den Score nach dem Spiel
    public static int calculatePoints(Spielfeld feld)
    {
        int Points = 50000;
        currentTime = System.currentTimeMillis();
        long timePassed =(-1*(gameStarted - currentTime)/1000);
        Points = Points - (int) (83 * timePassed);
        if(Points < 0) {Points = 0;}
        int leben = feld.getPlayer().getLeben();
        Points += leben*3500;
        if(leben == 0) {Points -= 20000;}
        Points = (int)(Points * (0.5+difficulty));
        if(Points<0) Points = 0;
        daten.getHighScores().setNewScore(Points);
        return Points;
    }

    //Gibt eine High-Score-Liste aus
    public static void printHighScores(Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Hall of Fame");
        String[] highScores = daten.getHighScores().getOverallHighscores();
        int i;
        for(i = 0; i < highScores.length; i++)
        {
            terminal.moveCursor(0,2+i);
            writeStringInTerminal(terminal,(i+1)+": "+highScores[i]);
        }

        terminal.moveCursor(0,2+i+2);
        writeStringInTerminal(terminal,"> Drücke eine beliebige Taste, um zum Hauptmenü zurückzukehren");
        terminal.moveCursor(0,4+i);

        boolean waitForKey = true;
        while (waitForKey)
        {
            try{
                Key key = terminal.readInput();
                if(key != null)
                {
                    waitForKey = false;
                    break;
                }
            }
            catch (NullPointerException e){continue;}
        }

    }

    //Frägt den Spieler, ob er sein Spiel speichern möchte
    public static void saveCheck(Terminal terminal, Spielfeld feld)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Möchtest du das Spiel vor dem Verlassen speichern?");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"> Ja");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"> Nein");
        terminal.moveCursor(0,2);

        boolean choose = true;
        boolean save = true;
        while(choose)
        {
            try{
                Key key = terminal.readInput();
                switch (key.getKind())
                {
                    case ArrowUp: terminal.moveCursor(0,2);
                                  save = true;
                                  break;
                    case ArrowDown: terminal.moveCursor(0,3);
                                    save = false;
                                    break;
                    case Enter: if(save) {saveGame(terminal, feld);}
                                choose = false;
                                break;
                }
            }
            catch (NullPointerException e){}
        }
    }

    //Ordnet den Spielobjekten automatisch Farben zu und setzt deren Zeichen
    public static void setObjectSign(SpielObject object, Terminal terminal)
    {
        Terminal.Color colour;
        switch (object.getFieldSign())
        {
            case 'ʢ': colour = Terminal.Color.RED;
                      break;
            case 'ɤ': colour = Terminal.Color.RED;
                      break;
            case '☺': colour = Terminal.Color.YELLOW;
                       break;
            case 'ŧ': colour = Terminal.Color.CYAN;
                      break;
            default: colour = Terminal.Color.WHITE;
                     break;
        }
        terminal.applyForegroundColor(colour);
        terminal.putCharacter(object.getFieldSign());
        terminal.applyForegroundColor(Terminal.Color.WHITE);
    }

    //Steuerung während dem Spiel mitsamt Kollisionsregeln und Gegnerbewegung
    public static boolean gameControls(Terminal terminal, Spielfeld feld)
    {
        boolean spielZuende = false;
        boolean spiel = true;

        //Extra-Threads, mit dem sich die gegnerischen Figuren bewegen. Je nach Schwierigkeitsgrad wird ein Thread gestartet
        Thread easy = new Thread()
        {
            public void run(){
                while (!isInterrupted())
                {
                    if(enemyMovement(terminal,feld)==true){drawTerminalRelative(terminal, feld);}
                    try{
                        sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        interrupt();
                    }
                }
            }
        };

        Thread normal = new Thread()
        {
            public void run(){
                while (!isInterrupted())
                {
                    if(enemyMovement(terminal,feld)==true){drawTerminalRelative(terminal, feld);}
                    try{
                        sleep(500);
                    }
                    catch (InterruptedException e)
                    {
                        interrupt();
                    }
                }
            }
        };

        Thread hard = new Thread()
        {
            public void run(){
                while (!isInterrupted())
                {
                    if(enemyMovement(terminal,feld)==true){drawTerminalRelative(terminal, feld);}
                    try{
                        sleep(250);
                    }
                    catch (InterruptedException e)
                    {
                        interrupt();
                    }
                }
            }
        };

        Thread thread;

        switch (difficulty)
        {
            case 0: thread = easy;
                    break;
            case 1: thread = normal;
                    break;
            case 2: thread = hard;
                    break;
            default: thread = normal;
                    break;
        }

        //Thread, der die verstrichene Zeit im Bildschirm einblendet / aktualisiert
        Thread writeTime = new Thread()
        {
            public void run()
            {
                while(!isInterrupted())
                {
                    try{
                        currentTime = (int)((System.currentTimeMillis()-gameStarted)/1000);
                        writeGameInformation(terminal,feld,thread);
                        sleep(1000);}
                    catch(InterruptedException e){}
                }
            }
        };

        writeGameInformation(terminal,feld,thread);
        writeTime.start();
        thread.start();

        while (spiel)
        {
            checkTerminalRescale(terminal,feld,thread);
            try
            {
                Key key = terminal.readInput();
                Koordinate current = feld.getPlayer().getCoordinates();
                Koordinate next = moveTo("",feld,false);
                Koordinate drawnNext = moveTo("",feld,false);

                //Definition der Handlungen bei verschiedenem Tasteninput (Navigation per Pfeiltasten)
                switch (key.getKind())
                {
                    case ArrowDown: next = moveTo("DOWN",feld,false);
                                    drawnNext = moveTo("DOWN",feld,true);
                                    break;
                    case ArrowUp:   next = moveTo("UP",feld,false);
                                    drawnNext = moveTo("UP",feld,true);
                                    break;
                    case ArrowLeft: next = moveTo("LEFT",feld,false);
                                    drawnNext = moveTo("LEFT",feld,true);
                                    break;
                    case ArrowRight:next = moveTo("RIGHT",feld,false);
                                    drawnNext = moveTo("RIGHT",feld,true);
                                    break;
                    case Escape:    thread.suspend();
                                    writeTime.suspend();
                                    updateTimePassed();
                                    boolean gameContinue = pauseMenu(terminal,feld);
                                    if(!gameContinue)
                                    {spiel = false;
                                        terminal.exitPrivateMode();
                                        System.exit(0);
                                        return false;}
                                    terminal.moveCursor(drawnCurrent.x(),drawnCurrent.y());
                                    writeTime.resume();
                                    thread.resume();
                                    break;
                    default:        next = moveTo("",feld,false);
                                    drawnNext = moveTo("",feld,true);
                }

                //Navigation per WASD und Feld erneut Laden per 'p'
                if(key.getKind() == Key.Kind.NormalKey)
                {
                    switch (key.getCharacter())
                    {
                        case 'w': next = moveTo("UP",feld,false);
                                  drawnNext = moveTo("UP",feld,true);
                                  break;
                        case 'W': next = moveTo("UP",feld,false);
                                  drawnNext = moveTo("UP",feld,true);
                                  break;
                        case 'a': next = moveTo("LEFT",feld,false);
                                  drawnNext = moveTo("LEFT",feld,true);
                                  break;
                        case 'A': next = moveTo("LEFT",feld,false);
                                  drawnNext = moveTo("LEFT",feld,true);
                                  break;
                        case 's': next = moveTo("DOWN",feld,false);
                                  drawnNext = moveTo("DOWN",feld,true);
                                  break;
                        case 'S': next = moveTo("DOWN",feld,false);
                                  drawnNext = moveTo("DOWN",feld,true);
                                  break;
                        case 'd': next = moveTo("RIGHT",feld,false);
                                  drawnNext = moveTo("RIGHT",feld,true);
                                  break;
                        case 'D': next = moveTo("RIGHT",feld,false);
                                  drawnNext = moveTo("RIGHT",feld,true);
                                  break;
                        case 'p': drawTerminalRelative(terminal, feld);
                                  break;
                        case 'P': drawTerminalRelative(terminal, feld);
                                  break;
                    }
                }

                //Behandlung der möglichen Szenarien bei Bewegung (Kein Hindernis, Gegner, Ausgang)
                    //Kein Hindernis im Weg
                    if (feld.getPlayer().canMove(next.x(), next.y(), feld) == 0)
                    {
                        //Falls der Ausgang (mit Schlüssel) erreicht wird, ist das Spiel beendet
                        if(feld.getObjectOnCoord(next.x(),next.y()) == 2)
                        {
                            spiel = false;
                            feld.getDynamicEnemies().clear();
                            thread.stop();
                            writeTime.stop();
                            endScreen(terminal,feld,true);
                        }
                        else
                        {
                            //Jeder weitere Schlüssel, der nach dem ersten aufgenommen wird, gibt ein Extraleben
                            if(feld.getObjectOnCoord(next.x(),next.y()) == 5 && feld.getPlayer().hasKey())
                            {
                                feld.getPlayer().gibLeben();
                            }
                            if(feld.getObjectOnCoord(next.x(),next.y()) == 5)
                            {
                                feld.getPlayer().giveKey();
                            }
                            terminal.moveCursor(drawnCurrent.x(), drawnCurrent.y());
                            terminal.putCharacter(' ');
                            terminal.moveCursor(drawnNext.x(), drawnNext.y());
                            setObjectSign(feld.getPlayer(),terminal);
                            drawnCurrent = drawnNext;
                            feld.movePlayerPosition(current, next);
                            writeGameInformation(terminal,feld,thread);
                            if (feld.getPlayer().getLeben() <= 0)
                            {
                                spiel = false;
                                feld.getDynamicEnemies().clear();
                                thread.stop();
                                writeTime.stop();
                                endScreen(terminal, feld, false);
                                return false;
                            }
                            if(drawnCurrent.x()< 1 ||drawnCurrent.y()< 1
                             ||drawnCurrent.x()>terminal.getTerminalSize().getColumns()-2
                             ||drawnCurrent.y()>terminal.getTerminalSize().getRows()-5)
                            {
                                drawTerminalRelative(terminal, feld);
                            }
                            terminal.moveCursor(drawnCurrent.x(), drawnCurrent.y());
                        }
                    }
                    else if(feld.getPlayer().canMove(next.x(), next.y(), feld) == 2)
                    {
                        feld.getPlayer().zieheLebenAb();
                        next = feld.getPlayer().getStartKoordinate();
                        terminal.moveCursor(drawnCurrent.x(), drawnCurrent.y());
                        terminal.putCharacter(' ');
                        drawnPlayerCoord = feld.getPlayer().getStartKoordinate();
                        drawnCurrent = drawnPlayerCoord;
                        setObjectSign(feld.getPlayer(),terminal);
                        feld.movePlayerPosition(current,next);
                        if(feld.getPlayer().getLeben() <= 0)
                        {
                            spiel = false;
                            feld.getDynamicEnemies().clear();
                            thread.stop();
                            writeTime.stop();
                            endScreen(terminal,feld,false);
                            return false;
                        }
                        drawTerminalRelative(terminal, feld);
                        writeGameInformation(terminal,feld,thread);
                        terminal.moveCursor(drawnCurrent.x(), drawnCurrent.y());
                    }

            }
            catch (NullPointerException e)
            {
                continue;
            }
        }
        thread.stop();
        writeTime.stop();
        return true;
    }

    //Hilfsmethode, um Codeduplikate zum Bewegen zu vermeiden, gibt die angepeilte Koordinate an
    public static Koordinate moveTo(String direction, Spielfeld feld, boolean drawn)
    {
        Koordinate newCoord;
        Koordinate aktCoord;
        if(drawn) {aktCoord = drawnCurrent;}
        else {aktCoord = feld.getPlayer().getCoordinates();}


        switch (direction)
        {
            case "DOWN": newCoord = new Koordinate(aktCoord.x(),aktCoord.y()+1);
                         break;
            case "UP":   newCoord = new Koordinate(aktCoord.x(),aktCoord.y()-1);
                         break;
            case "LEFT": newCoord = new Koordinate(aktCoord.x()-1,aktCoord.y());
                         break;
            case "RIGHT":newCoord = new Koordinate(aktCoord.x()+1,aktCoord.y());
                         break;
            default:     newCoord = new Koordinate(aktCoord.x(),aktCoord.y());
                         break;
        }
        return newCoord;
    }

    //Bewegt die dynamischen Gegner bei jedem Spielerzug in eine zufällige Richtung
    //Return true: Der Spieler hat keine Leben mehr
    public synchronized static boolean enemyMovement (Terminal terminal, Spielfeld feld)
    {
        LinkedList<Gegner> gegnerListe = feld.getDynamicEnemies();

        //Für alle Gegner im Spiel wird die Bewegung neu "ausgewürfelt"
        for(int i = 0; i < feld.getDynamicEnemies().size(); i++)
        {
            Koordinate current = gegnerListe.get(i).getK();

            //Berechnung anhand des "Meta-Spielfelds", wo der aktuelle Gegner gezeichnet im Feld liegt
            Koordinate currentEnemyFieldPosition = new Koordinate(drawnCurrent.x()-(feld.getPlayer().getCoordinates().x()-current.x()),
                                                                  drawnCurrent.y()-(feld.getPlayer().getCoordinates().y()-current.y()));
            Koordinate nextEnemyFieldPosition;

            /*Bedingung, das nur Gegner, die im Feld liegen, neu gesetzt werden (also sich bewegen)
            if(currentEnemyFieldPosition.x() >= 0 && currentEnemyFieldPosition.x()< terminal.getTerminalSize().getColumns() &&
               currentEnemyFieldPosition.y() >= 0 && currentEnemyFieldPosition.y()< terminal.getTerminalSize().getRows()-3)
            {*/
                //Bedingung, was passiert, falls der Gegner am Rand der Karte ist
                if(currentEnemyFieldPosition.y() == terminal.getTerminalSize().getRows()-3
                 || currentEnemyFieldPosition.x() == terminal.getTerminalSize().getColumns()
                 ||currentEnemyFieldPosition.x() < 0 || currentEnemyFieldPosition.y() < 0)
                {
                    terminal.moveCursor(currentEnemyFieldPosition.x(),currentEnemyFieldPosition.y());
                    terminal.putCharacter(' ');
                    continue;
                }
                Gegner currentEnemy = gegnerListe.get(i);
                Koordinate next;
                boolean canSet = false;

                /*Anhand einer Zufallszahl wird eine Koordinate bestimmt.
                  Dann wird überprüft, ob sich der Gegner an diese Position bewegen kann.
                  Falls sich der Gegner nicht bewegen kann, startet die Schleife neu.
                  Zuletzt wird die Bewegung ausgeführt (und ggf. dem Spieler ein Leben abgezogen).*/
                while (!canSet)
                {
                    int direction = (int)((Math.random()) * 4 + 1);
                    switch (direction)
                    {
                        case 1: next = new Koordinate(current.x(), current.y()-1);
                            nextEnemyFieldPosition = new Koordinate(currentEnemyFieldPosition.x(), currentEnemyFieldPosition.y()-1);
                            break;
                        case 2: next = new Koordinate(current.x(), current.y()+1);
                            nextEnemyFieldPosition = new Koordinate(currentEnemyFieldPosition.x(), currentEnemyFieldPosition.y()+1);
                            break;
                        case 3: next = new Koordinate(current.x()+1, current.y());
                            nextEnemyFieldPosition = new Koordinate(currentEnemyFieldPosition.x()+1, currentEnemyFieldPosition.y());
                            break;
                        case 4: next = new Koordinate(current.x()-1, current.y());
                            nextEnemyFieldPosition = new Koordinate(currentEnemyFieldPosition.x()-1, currentEnemyFieldPosition.y());
                            break;
                        default:next = new Koordinate(current.x(), current.y());
                            nextEnemyFieldPosition = new Koordinate(currentEnemyFieldPosition.x(), currentEnemyFieldPosition.y());
                            break;
                    }
                    int canMove = gegnerListe.get(i).canMove(next.x(),next.y(),feld);
                    if(canMove == 0 || canMove == 1)
                    {
                        if(canMove == 1)
                        {
                            feld.getPlayer().zieheLebenAb();
                            drawnPlayerCoord = feld.getPlayer().getStartKoordinate();
                            System.out.println(feld.getPlayer().getLeben());
                            if (feld.getPlayer().getLeben() <= 0)
                            {
                                return true;
                            }
                            Koordinate playerNext = feld.getPlayer().getStartKoordinate();
                            Koordinate playerCurrent = feld.getPlayer().getCoordinates();


                            terminal.moveCursor(drawnCurrent.x(),drawnCurrent.y());
                            terminal.putCharacter(' ');
                            terminal.moveCursor(drawnPlayerCoord.x(),drawnPlayerCoord.y());
                            drawnCurrent = drawnPlayerCoord;
                            setObjectSign(feld.getPlayer(), terminal);
                            feld.getPlayer().setCoordinates(playerNext);
                            feld.movePlayerPosition(playerCurrent,playerNext);
                        }

                        else if(currentEnemyFieldPosition.x() >= 0 && currentEnemyFieldPosition.x()< terminal.getTerminalSize().getColumns() &&
                                currentEnemyFieldPosition.y() >= 0 && currentEnemyFieldPosition.y()< terminal.getTerminalSize().getRows()-3)
                        {  terminal.moveCursor(currentEnemyFieldPosition.x(), currentEnemyFieldPosition.y());
                            terminal.putCharacter(' ');
                            terminal.moveCursor(nextEnemyFieldPosition.x(), nextEnemyFieldPosition.y());
                            setObjectSign(currentEnemy, terminal);
                        }

                        currentEnemy.setK(next);
                        feld.moveEnemyPosition(current,next,currentEnemy);
                        System.out.println("Gegner "+i+" Koordinaten: "+currentEnemy.getK().toString());
                        canSet = true;
                    }

                }
            }

        //}
        terminal.moveCursor(drawnCurrent.x(),drawnCurrent.y());
        return false;
    }

    //Hilfsmethode, um unkompliziert ganze Strings im Terminal auszugeben
    public static void writeStringInTerminal (Terminal terminal, String string)
    {
        int line = 0;
        for(int i = 0; i < string.length(); i++)
        {
            if(string.charAt(i) == '\n') terminal.moveCursor(0,++line);
            terminal.putCharacter(string.charAt(i));
        }
    }

    //Erklärung der Spielmission
    public static void welcomeScreen (Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Willkommen, Spieler!");
        terminal.moveCursor(0,1);
        writeStringInTerminal(terminal,"Deine Mission ist es, einen Schlüssel zu ergattern und das Labyrinth zu verlassen.");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"Lass dich nicht von den Gegnern erwischen!");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"Viel Glück!");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"Mit den Pfeiltasten kannst du im Menü navigieren,");
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,"im Spiel steuerst du deine Figur entweder mit den Pfeiltasten oder mit WASD.");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,"Für weitere Informationen zum Spiel sieh einfach im Spielhandbuch nach!");
        terminal.moveCursor(0,7);
        writeStringInTerminal(terminal,"Wähle nun dein Profil:");
        terminal.moveCursor(0,9);
        String[] players = daten.getPlayers();
        int i;
        for(i = 0; i < players.length; i++)
        {
            terminal.moveCursor(0,9+i);
            writeStringInTerminal(terminal,"> "+players[i]);
        }

        terminal.moveCursor(0,9+i+1);
        writeStringInTerminal(terminal,"> Neues Profil anlegen");
        terminal.moveCursor(0,9);
        int cursorPosition = 0;

        boolean input = false;
        while (!input)
        {
            try
            {
                Key key = terminal.readInput();
                switch (key.getKind())
                {
                    case ArrowDown:
                        if(cursorPosition == i-1)
                        {
                        terminal.moveCursor(0,9+i+1);
                        cursorPosition = i+1;
                        break;
                        }
                        if(cursorPosition < i+1)
                        {
                        ++cursorPosition;
                        terminal.moveCursor(0, 9 + cursorPosition);
                        break;
                        }
                        else break;

                    case ArrowUp:
                        if(cursorPosition == i+1)
                        {
                            terminal.moveCursor(0,9+i-1);
                            cursorPosition = i-1;
                            break;
                        }
                        if(cursorPosition > 0)
                        {
                            --cursorPosition;
                            terminal.moveCursor(0, 9 + cursorPosition);
                            break;
                        }
                        else break;

                    case Enter:
                        if(cursorPosition == i+1)
                        {
                            addNewPlayer(terminal);
                            input = true;
                            break;
                        }
                        aktiverSpieler = daten.getSpecificPlayer(players[cursorPosition]);
                        input = true;
                        break;
                }
            }
            catch (NullPointerException e)
            {
                continue;
            }
        }

    }

    //Screen nach Beendigung eines Spieles
    public static void endScreen (Terminal terminal, Spielfeld feld, boolean gewonnen)
    {
        terminal.clearScreen();
        currentTime = System.currentTimeMillis();
        terminal.moveCursor(0,0);
        if(gewonnen)
        {
            writeStringInTerminal(terminal,"Herzlichen Glückwunsch, du hast gewonnen!");
            aktiverSpieler.wonGame();
        }
        else
        {
            writeStringInTerminal(terminal,"Du hast verloren!");
            aktiverSpieler.lostGame();
        }
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"Du hast "+(-1*(gameStarted-currentTime)/1000)+" Sekunden gebraucht.");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"Du hast insgesamt "+calculatePoints(feld)+" Punkte erreicht.");
        aktiverSpieler.setHighScore(calculatePoints(feld));
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,"Dein aktueller HighScore liegt bei "+aktiverSpieler.getHighScore()+" Punkten.");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,"> Jetzt die Bestenliste ansehen");
        terminal.moveCursor(0,8);
        writeStringInTerminal(terminal,"> In das Hauptmenü zurückkehren");
        boolean endscreen = true;
        boolean seeHighscores = true;
        terminal.moveCursor(0,6);
        while (endscreen)
        {
            try {
                Key key = terminal.readInput();
                switch (key.getKind())
                {
                    case ArrowDown: terminal.moveCursor(0,8);
                         seeHighscores = false;
                         break;
                    case ArrowUp: terminal.moveCursor(0,6);
                         seeHighscores = true;
                         break;
                    case Enter: endscreen = false;
                                if(seeHighscores) printHighScores(terminal);
                                break;
                }
            }
            catch (NullPointerException e)
            {
                continue;
            }
        }

    }

    //Menü zu Beginn des Spiels mit Levelauswahl und Ladefunktion
    public static boolean startScreen(Terminal terminal,Spielfeld feld)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Lanterna-Game: Willkommen "+aktiverSpieler.toString());
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"> Starte ein neues Spiel");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"> Lade einen Spielstand");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"> Wähle ein Level");
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,"> Schwierigkeit");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,"> Handbuch");
        terminal.moveCursor(0,7);
        writeStringInTerminal(terminal,"> Dein Profil");
        terminal.moveCursor(0,8);
        writeStringInTerminal(terminal,"> HighScores");
        terminal.moveCursor(0,9);
        writeStringInTerminal(terminal,"> Schließe das Spiel");


        terminal.moveCursor(0,2);

        //Warten auf Tasteneingabe, falls das Spiel beendet werden soll, wird das Spiel abgebrochen
        int cursorPosition = 2;
        boolean pause = true;
        while (pause)
        {
            try {
                Key key = terminal.readInput();
                switch (key.getKind())
                {
                    case ArrowDown: if(cursorPosition<9){terminal.moveCursor(0,++cursorPosition);}
                        break;
                    case ArrowUp:   if(cursorPosition>2){terminal.moveCursor(0,--cursorPosition);}
                        break;
                    case Enter:     switch (cursorPosition)
                    {
                        case 2: pause = false;
                                break;
                        case 3: loadGame(terminal);
                                pause = false;
                                startScreen(terminal,feld);
                                break;
                        case 4: loadScreen(terminal);
                                pause = false;
                                startScreen(terminal,feld);
                                break;
                        case 5: difficultyMenu(terminal);
                                pause = false;
                                startScreen(terminal,feld);
                                break;
                        case 6: manual(terminal);
                                pause = false;
                                startScreen(terminal,feld);
                                break;
                        case 7: profileInfos(terminal);
                                pause = false;
                                startScreen(terminal,feld);
                                break;
                        case 8: printHighScores(terminal);
                                pause = false;
                                startScreen(terminal,feld);
                                break;
                        case 9: pause = false;
                                terminal.exitPrivateMode();
                                daten.save();
                                System.exit(0);
                                return false;
                    }
                    default:        break;
                }
            }
            catch (NullPointerException e)
            {
                continue;
            }
        }
        return true;
    }

    //Hier kann ein neues Spielerprofil erzeugt werden
    public static void addNewPlayer(Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Gib deinen Profilnamen ein: ");
        String playerName = scannerInTerminal(terminal);
        //Aus dem Namen wird ein Profil generiert, dass nun in den Stammdaten hinterlegt wird
        SpielerProfil p = new SpielerProfil(playerName);
        daten.setNewPlayer(p);
        aktiverSpieler = p;
    }

    //Mit esc aufgerufenes Pause-Menü
    public static boolean pauseMenu(Terminal terminal, Spielfeld feld)
    {
        terminal.clearScreen();

        //Aufbau des Pause-Bildschirms
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Pause");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"> Weiterspielen");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"> Speichern");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"> Laden");
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,"> Handbuch");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,"> Verlasse das Spiel");

        terminal.moveCursor(0,2);

        //Warten auf Tasteneingabe, falls das Spiel beendet werden soll, wird "false" zurückgegeben
        int cursorPosition = 2;
        boolean pause = true;
        while (pause)
        {
            try {
                Key key = terminal.readInput();
                switch (key.getKind())
                {
                    case ArrowDown: if(cursorPosition<6){terminal.moveCursor(0,++cursorPosition);}
                        break;
                    case ArrowUp:   if(cursorPosition>2){terminal.moveCursor(0,--cursorPosition);}
                        break;
                    case Enter:     switch (cursorPosition)
                    {
                        case 2: terminal.clearScreen();
                                pause = false;
                                drawTerminalRelative(terminal,feld);
                                break;
                        case 3: pause = false;
                                saveGame(terminal,feld);
                                pauseMenu(terminal,feld);
                                break;
                        case 4: pause = false;
                                loadGame(terminal);
                                pauseMenu(terminal,feld);
                                break;
                        case 5: pause = false;
                                manual(terminal);
                                pauseMenu(terminal,feld);
                                break;
                        case 6: saveCheck(terminal, feld);
                                pause = false;
                                terminal.exitPrivateMode();
                                return false;
                    }
                    default:        break;
                }
            }
            catch (NullPointerException e)
            {
                continue;
            }
        }
        return true;
    }

    //Menü zum Wählen der Schwierigkeit, verändert die globale Variable "difficulty"
    public static void difficultyMenu (Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Wähle deine Schwierigkeit");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"> Einfach");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"> Standard");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"> Schwer");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,"> Zurück zum Menü");

        terminal.moveCursor(0,2);
        int cursorPosition = 2;
        boolean pickDiff = true;

        while(pickDiff) {
            try {
                Key key = terminal.readInput();
                switch (key.getKind()) {
                    case ArrowDown: if(cursorPosition==4){terminal.moveCursor(0,6);cursorPosition = 6;break;}
                                    if(cursorPosition<6){terminal.moveCursor(0,++cursorPosition);}
                                    break;
                    case ArrowUp:   if(cursorPosition==6) {terminal.moveCursor(0,4);cursorPosition = 4; break;}
                                    if(cursorPosition>2){terminal.moveCursor(0,--cursorPosition);}
                                    break;
                    case Enter:     switch (cursorPosition)
                                    {
                                        case 2: difficulty = 0;
                                                pickDiff = false;
                                                break;
                                        case 3: difficulty = 1;
                                                pickDiff = false;
                                                break;
                                        case 4: difficulty = 2;
                                                pickDiff = false;
                                                break;
                                        case 6: pickDiff = false;
                                                break;
                                    }
                }
            }
            catch (NullPointerException e)
            {continue;}
        }
    }

    //Gibt die Infos aus, die über das jeweilige Profil gespeichert sind
    public static void profileInfos (Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Profil von "+aktiverSpieler.toString());
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"Gespielte Spiele: "+aktiverSpieler.getGamesPlayed());
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"Gewonnene Spiele: "+aktiverSpieler.getGamesWon());
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"Verlorene Spiele: "+aktiverSpieler.getGamesLost());
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,"Persönlicher HighScore: "+aktiverSpieler.getHighScore());
        terminal.moveCursor(0,7);
        writeStringInTerminal(terminal,"> Zurück");
        terminal.moveCursor(0,7);
        waitForAnyKey(terminal);
    }

    //Übersicht über die verschiedenen Rubriken des Handbuchs => Legende und "Hilfe"-Menü
    public static void manual(Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Spielhandbuch");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"> Beschreibung der Spielfiguren");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"> Sonderregeln & Punkteberechnung");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"> Tipps & Tricks");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,"> Zurück zum Menü");

        terminal.moveCursor(0,2);
        int cursorPosition = 2;
        boolean manual = true;

        while(manual) {
            try {
                Key key = terminal.readInput();
                switch (key.getKind()) {
                    case ArrowDown: if(cursorPosition==4){terminal.moveCursor(0,6);cursorPosition = 6;break;}
                        if(cursorPosition<6){terminal.moveCursor(0,++cursorPosition);}
                        break;
                    case ArrowUp:   if(cursorPosition==6) {terminal.moveCursor(0,4);cursorPosition = 4; break;}
                        if(cursorPosition>2){terminal.moveCursor(0,--cursorPosition);}
                        break;
                    case Enter:     switch (cursorPosition)
                    {
                        case 2: legend(terminal);
                                manual = false;
                                manual(terminal);
                                break;
                        case 3: scoreExplanation(terminal);
                                manual = false;
                                manual(terminal);
                                break;
                        case 4: tipps(terminal);
                                manual = false;
                                manual(terminal);
                                break;
                        case 6: manual = false;
                                break;
                    }
                }
            }
            catch (NullPointerException e)
            {continue;}
        }
    }

    //Spiellegende
    public static void legend(Terminal terminal)
    {
        Gegner statisch = new Gegner(false,new Koordinate(0,0));
        Gegner dynamisch = new Gegner(true, new Koordinate(0,0));
        Spieler player = new Spieler(new Koordinate(0,0));
        schluessel key = new schluessel();
        Wand wand = new Wand();
        Ausgang exit = new Ausgang();

        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Spiel-Legende");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,player.getFieldSign()+": Das bist du. Du musst irgendwie aus dem Labyrinth entkommen.");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,wand.getFieldSign()+": Dies ist eine Wand. Sie hindert dich am weiterkommen.");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,statisch.getFieldSign()+": Dies ist ein statischer Gegner. Auch wenn er sich nicht bewegt, solltest du ihn lieber nicht berühren!");
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,dynamisch.getFieldSign()+": Dies ist ein dynamischer Gegner. Pass auf, man weiß nie, wohin er als nächstes gehen wird!");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,key.getFieldSign()+": Dies ist ein Schlüssel. Sammle einen Schlüssel ein, um das Labyrinth verlassen zu können.");
        terminal.moveCursor(0,7);
        writeStringInTerminal(terminal,exit.getFieldSign()+": Hier ist der Ausgang. Sobald du einen Schlüssel besitzt, solltest du dich schleunigst zu diesem aufmachen.");
        terminal.moveCursor(0,9);
        writeStringInTerminal(terminal,"> Zurück");
        terminal.moveCursor(0,9);

        waitForAnyKey(terminal);
    }

    //Tipps zum Spiel
    public static void tipps(Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Tipps & Tricks");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"1) Es lohnt sich, mehr als nur einen Schlüssel einzusammeln.");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"   Jeder zusätzliche Schlüssel gibt dir ein Extra-Leben. ");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"2) Vorsicht! Wenn Gegner einen Ausgang erreichen, blockieren sie diesen!");
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,"3) Mit der Taste 'P' kannst du das Spielfeld wieder auf deinen Charakter zentrieren.");
        terminal.moveCursor(0,7);
        writeStringInTerminal(terminal,"> Zurück");
        terminal.moveCursor(0,7);

        waitForAnyKey(terminal);
    }

    //Erklärung, wie sich der Score errechnet
    public static void scoreExplanation(Terminal terminal)
    {
        terminal.clearScreen();
        terminal.moveCursor(0,0);
        writeStringInTerminal(terminal,"Wie bekommst du möglichst viele Punkte?");
        terminal.moveCursor(0,2);
        writeStringInTerminal(terminal,"Zu Beginn eines Spiels hat jeder Spieler 50.000 Punkte.");
        terminal.moveCursor(0,3);
        writeStringInTerminal(terminal,"Im Laufe des Spiels wirken verschiedene Faktoren auf diesen Punktestand ein:");
        terminal.moveCursor(0,4);
        writeStringInTerminal(terminal,"Schwierigkeit: Desto schwerer dein Spiel ist, desto mehr Punkte bekommst du auch.");
        terminal.moveCursor(0,5);
        writeStringInTerminal(terminal,"Leben: Jedes Leben gibt dir 3500 Extrapunkte.");
        terminal.moveCursor(0,6);
        writeStringInTerminal(terminal,"Spiel verloren: Wenn du keine Leben mehr hast, kostet dich das 20.000 Punkte.");
        terminal.moveCursor(0,7);
        writeStringInTerminal(terminal,"Verstrichene Zeit: Pro Sekunde werden dir Punkte abgezogen.");
        terminal.moveCursor(0,9);
        writeStringInTerminal(terminal,"> Zurück");
        terminal.moveCursor(0,9);

        waitForAnyKey(terminal);

    }

    //Bildschirm bzw Skript zum Laden von Levels
    public static void loadScreen(Terminal terminal)
    {
        terminal.clearScreen();
        Hashtable levels = new Hashtable();
        int itemsInList= 0;

        terminal.moveCursor(0, 0);
        writeStringInTerminal(terminal, "Bitte wähen Sie ein Level aus:");

        //Liest das Verzeichnis "level" aus und speichert deren Dateinamen in savegames[]
        File f = new File("level");
        String[] savegames = f.list();
        //Speichert den Dateinamen ohne ".properties" in splittedSave
        for(int i = 0; i < savegames.length; i++)
        {
            String[] splittedSave = savegames[i].split(".properties",2);
            levels.put( i+2,splittedSave[0]);
            terminal.moveCursor(0,2+i);
            writeStringInTerminal(terminal,"> "+splittedSave[0]);
            itemsInList++;
        }

        terminal.moveCursor(0,itemsInList+3);
        writeStringInTerminal(terminal,"> Zurück");

        terminal.moveCursor(0,2);

        int cursorPosition = 2;
        boolean onLoadScreen = true;

        while (onLoadScreen) {
            try {
                Key key = terminal.readInput();
                switch (key.getKind()) {
                    case ArrowDown:
                        if (cursorPosition == itemsInList + 1) {
                            cursorPosition += 2;
                            terminal.moveCursor(0, cursorPosition);
                        }
                        if (cursorPosition < itemsInList + 3) {
                            terminal.moveCursor(0, ++cursorPosition);
                        }
                        break;
                    case ArrowUp:
                        if (cursorPosition == itemsInList + 3) {
                            cursorPosition--;
                            terminal.moveCursor(0, cursorPosition);
                        }
                        if (cursorPosition > 2) {
                            terminal.moveCursor(0, --cursorPosition);
                        }
                        break;
                    case Enter:
                        if (cursorPosition == itemsInList +3)
                        {
                            onLoadScreen = false;
                            terminal.clearScreen();
                            break;
                        }
                        //Setzt die globale Variable "levelToLoad" auf das gewählte Level
                        levelToLoad = "level\\"+levels.get(cursorPosition).toString();
                        terminal.clearScreen();
                        onLoadScreen = false;
                }
            } catch (NullPointerException e) {
                continue;
            }
        }

    }

    //Methode, mit der direkt im Terminal ein vom Programm verwertbarer String eingegeben werden darf
    public static String scannerInTerminal (Terminal terminal)
    {
        String dateiname = "";
        boolean writesName = true;

        //Lässt den Spieler einen Namen für seine Datei eingeben, inklusive funktionierender Backspace-Taste
        while (writesName) {
            try {
                Key key = terminal.readInput();
                switch (key.getKind()) {
                    case NormalKey:
                        terminal.putCharacter(key.getCharacter());
                        dateiname += key.getCharacter();
                        break;
                    case Enter:
                        terminal.moveCursor(0, 2);
                        writeStringInTerminal(terminal, "Die Datei " + dateiname + " wird nun gespeichert...");
                        writesName = false;
                        break;
                    case Backspace:
                        if (dateiname.length() > 0) {
                            //+40, da "Geben Sie Ihrem Spielstand einen Namen: " 40 Zeichen hat
                            terminal.moveCursor(40 + dateiname.length() - 1, 0);
                            terminal.putCharacter(' ');
                            terminal.moveCursor(40 + dateiname.length() - 1, 0);
                            dateiname = dateiname.substring(0, dateiname.length() - 1);
                        }
                }
            } catch (NullPointerException e) {
                continue;
            }
        }
        return dateiname;
    }

    //Methode zum Speichern des Spiels
    public static void saveGame(Terminal terminal, Spielfeld feld)
    {
        terminal.clearScreen();
        writeStringInTerminal(terminal,"Geben Sie Ihrem Spielstand einen Namen: ");
        String dateiname = scannerInTerminal(terminal);

        //Erstellt eine neue Properties-Datei, in der die Angaben nach vorgegebenen Muster gespeichert werden (Der Player wird als Eingang gespeichert)
        Properties savegame = new Properties();
        for(int y = 0; y<feld.getHeigth(); y++)
            for (int x = 0; x < feld.getWidth(); x++)
            {
                if(feld.getSpielfeld()[x][y].getObjectCode() < 7)
                {
                    if(feld.getSpielfeld()[x][y].getObjectCode() == 6)
                    {
                        savegame.setProperty(x+","+y,"1");
                    }
                    else
                    savegame.setProperty(x+","+y,""+feld.getSpielfeld()[x][y].getObjectCode());
                }
            }

        //Setzen der einzelnen Eigenschaften
        savegame.setProperty("Height",""+feld.getHeigth());
        savegame.setProperty("Width",""+feld.getWidth());
        savegame.setProperty("Time",""+timePassed);
        savegame.setProperty("Leben",""+feld.getPlayer().getLeben());
        savegame.setProperty("PositionDrawnX",""+drawnPlayerCoord.x());
        savegame.setProperty("PositonDrawnY", ""+drawnPlayerCoord.y());
        savegame.setProperty("startPositionX",""+feld.getPlayer().getStartKoordinate().x());
        savegame.setProperty("startPositionY",""+feld.getPlayer().getStartKoordinate().y());

        if(feld.getPlayer().hasKey())
        {
            savegame.setProperty("Key","true");
        }
        try {
            savegame.store(new FileOutputStream("saves/"+dateiname+".properties"), "Gespeicherter Spielstand");
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found!");
        }
        catch (IOException e)
        {
            System.out.println("IOException!");
        }
        terminal.clearScreen();
    }

    //Methode zum Laden eines Spielstandes
    public static void loadGame(Terminal input)
    {
        //Tabelle, in der die String-Fragmente der gespeicherten Spielstände enthalten sind
        Hashtable saves = new Hashtable();
        String fileToLoad = "";
        int itemsInList= 0;
        boolean noLevelSelected = false;

        input.clearScreen();
        input.moveCursor(0,0);
        //Liest automatisch alle vorhandenen Spielstände aus
        writeStringInTerminal(input,"Bitte wähle den Spielstand aus, den du laden möchtest:");

        //Liest das Verzeichnis "saves" ein
        File f = new File("saves");

        //Speichert die Dokumentenbezeichnungen aller Dokumente in "saves"
        String[] savegames = f.list();

        //Löscht den ".properties" Anhang eines jeden Speicherstandes aus dem String und speichert diese gekürzte Version in splittedSave
        for(int i = 0; i < savegames.length; i++)
        {
            String[] splittedSave = savegames[i].split(".properties",2);
            saves.put( i+2,splittedSave[0]);
            input.moveCursor(0,2+i);
            writeStringInTerminal(input,"> "+splittedSave[0]);
            itemsInList++;
        }

        input.moveCursor(0,itemsInList+3);
        writeStringInTerminal(input,"> Back");

        //Standardablauf zum einlesen von Tasten inklusive eine leere Zeile überspringen
        input.moveCursor(0,2);
        int cursorPosition = 2;
        boolean pickSaveGame = true;
        while (pickSaveGame)
        {
            try {
                Key key = input.readInput();
                switch (key.getKind()) {
                    case ArrowDown:
                        if(cursorPosition == itemsInList+1){cursorPosition+=2;input.moveCursor(0,cursorPosition);}
                        if (cursorPosition < itemsInList+3) {
                            input.moveCursor(0, ++cursorPosition);
                        }
                        break;
                    case ArrowUp:
                        if(cursorPosition == itemsInList+3){cursorPosition--;input.moveCursor(0,cursorPosition);}
                        if (cursorPosition > 2) {
                            input.moveCursor(0, --cursorPosition);
                        }
                        break;
                    case Enter:
                        //Falls der Spieler Back wählt, wird kein Spielstand geladen
                        if(cursorPosition==itemsInList+3)
                                    {
                                    pickSaveGame=false;
                                    input.clearScreen();
                                    noLevelSelected=true;
                                    break;
                                    }
                                //Der aktuell gewählte Spielstand wird im String fileToLoad gespeichert und die Schleife beendet
                                fileToLoad = saves.get(cursorPosition).toString();
                                input.clearScreen();
                                pickSaveGame = false;
                }
            }
            catch (NullPointerException e)
            {
                continue;
            }
        }
        //Falls ein Level gewählt wurde, wird ein neues Spielfeld generiert und aus dem gewählten Level generiert
        if(!noLevelSelected) {
            Spielfeld feld = new Spielfeld();
            feld.initializeField("saves/" + fileToLoad);

            //Einige Werte, die direkt das "Spiel"-Objekt betreffen, werden händisch in dieser Methode gesetzt
            try {
                Properties props = new Properties();
                FileInputStream in = new FileInputStream("saves/" + fileToLoad + ".properties");
                props.load(in);
                in.close();
                String startX = props.getProperty("startPositionX");
                String startY = props.getProperty("startPositionY");
                String time = props.getProperty("Time");
                int x = Integer.parseInt(startX);
                int y = Integer.parseInt(startY);
                timePassed = Integer.parseInt(time);
                gameStarted = gameStarted-timePassed*1000+2000;

                Koordinate start = new Koordinate(x,y);
                feld.getPlayer().setStartKoordinate(start);
            }
            catch (Exception e){
                System.out.println("Es wurde keine Startposition festgelegt!");
            }

            //Das Feld wird im Terminal gezeichnet und das Spiel gestartet
            drawTerminalRelative(input, feld);
            Koordinate playerPosition = feld.getPlayer().getCoordinates();
            input.moveCursor(playerPosition.x(), playerPosition.y());
            gameControls(input, feld);
        }

    }

    public static void main(String[] args) {
        game();
    }
}
