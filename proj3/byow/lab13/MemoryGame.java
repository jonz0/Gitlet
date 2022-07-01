package byow.lab13;

import byow.Core.RandomUtils;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;
import java.util.Scanner;

public class MemoryGame {
    /** The width of the window of this game. */
    private int width;
    /** The height of the window of this game. */
    private int height;
    /** The current round the user is on. */
    private int round;
    /** The Random object used to randomly generate Strings. */
    private Random rand;
    /** Whether or not the game is over. */
    private boolean gameOver;
    /** Whether or not it is the player's turn. Used in the last section of the
     * spec, 'Helpful UI'. */
    private boolean playerTurn;
    /** The characters we generate random Strings from. */
    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    /** Encouraging phrases. Used in the last section of the spec, 'Helpful UI'. */
    private static final String[] ENCOURAGEMENT = {"You can do this!", "I believe in you!",
                                                   "You got this!", "You're a star!", "Go Trojans!",
                                                   "Too easy for you!", "Wow, so impressive!"};

    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Enter seed");

//        String seedStr = scan.nextLine();
//        long seed = Long.parseLong(seedStr);
        long seed = 5;

        MemoryGame game = new MemoryGame(40, 40, seed);
        game.startGame();
    }

    public MemoryGame(int width, int height, long seed) {
        /* Sets up StdDraw so that it has a width by height grid of 16 by 16 squares as its canvas
         * Also sets up the scale so the top left is (0,0) and the bottom right is (width, height)
         */
        this.width = width;
        this.height = height;
        StdDraw.setCanvasSize(this.width * 16, this.height * 16);
        Font font = new Font("Monaco", Font.BOLD, 30);
        StdDraw.setFont(font);
        StdDraw.setXscale(0, this.width);
        StdDraw.setYscale(0, this.height);
        StdDraw.clear(Color.BLACK);
        StdDraw.enableDoubleBuffering();

        //TODO: Initialize random number generator
        rand = new Random(seed);

    }

    public String generateRandomString(int n) {
        //TODO: Generate random string of letters of length n

        StringBuilder s = new StringBuilder();

        while (n > 0) {
            int randChar = rand.nextInt(26);
            s.append(CHARACTERS[randChar]);
            n--;
        }
        return s.toString();
    }

    public void drawFrame(String s) {
        //TODO: Take the string and display it in the center of the screen
        //TODO: If game is not over, display relevant game information at the top of the screen

        StdDraw.clear();
        Font font = new Font("SansSerif", Font.BOLD, 30);
        StdDraw.setFont(font);
        StdDraw.setPenColor(StdDraw.BLACK);

        StdDraw.text(width / 2.0, height / 2.0, s);
        StdDraw.show();
    }

    public void flashSequence(String letters) {
        //TODO: Display each character in letters, making sure to blank the screen between letters
        playerTurn = false;
        for (char c : letters.toCharArray()) {
            String s = Character.toString(c);
            drawFrame(s);
            StdDraw.pause(500);
            drawFrame("");
            StdDraw.pause(500);
        }
    }

    public String solicitNCharsInput(int n) {
        //TODO: Read n letters of player input
        String ret = "";
        int count = 0;
        drawFrame("");
        while(true){
            if(StdDraw.hasNextKeyTyped()) {
                ret += Character.toString(StdDraw.nextKeyTyped());
                drawFrame(ret);
                count++;
            }
            if(count == n){
                break;
            }
        }

        return ret;
    }

    public void startGame() {
        //TODO: Set any relevant variables before the game starts
        gameOver = false;
        drawFrame("Let's begin!");
        StdDraw.pause(1000);
        drawFrame("");
        StdDraw.pause(500);
        round = 1;
        //TODO: Establish Engine loop
        while (!gameOver) {
            String target = generateRandomString(round);
            flashSequence(target);
            playerTurn = true;
            String temp = solicitNCharsInput(round);
            StdDraw.pause(1275);
            if (temp.equals(target)) {
                playerTurn = false;
                drawFrame(ENCOURAGEMENT[rand.nextInt(ENCOURAGEMENT.length)]);
                StdDraw.pause(500);
                round++;
            } else {
                gameOver = true;
                drawFrame("GAME OVER");
            }
        }
    }
}
