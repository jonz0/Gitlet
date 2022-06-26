package byow.lab12;
import org.junit.Test;
import static org.junit.Assert.*;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.*;

/**
 * Draws a world consisting of hexagonal regions.
 */
public class HexWorld {

    private static final int WIDTH = 90;
    private static final int HEIGHT = 52;

    public static void main(String[] args) {
        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        // initialize tiles
        TETile[][] world = new TETile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                world[x][y] = Tileset.NOTHING;
            }
        }

        // Adds hexagon to point in world
//        addHexagon(world, 3, 10, 4);
//        hexCol(3, world, 3, 10, 20);
        tesselate(2, world, 5, 10, 20, true);

        // draws the world to the screen
        ter.renderFrame(world);
    }
    public static void addHexagon(TETile[][] world, int length, int col, int row, boolean random) {
        TETile t = Tileset.WALL;
        if (random) {
            Random r = new Random();
            int rand = r.nextInt(Tileset.tiles.size());
            t = Tileset.tiles.get(rand);
        }
        int height = getHeight(length);
        int width = getWidth(length);

        for (int j = row; j < row + height / 2; j++) {
            int offset = j - row;
            for (int i = col + offset; i < col + width - offset; i++) {
                world[i][j] = t;
            }
        }

        for (int j = row - 1; j >= row - length; j--) {
            int offset = j - row + 1;
            for (int i = col - offset; i < col + width + offset; i++) {
                world[i][j] = t;
            }
        }
    }

    private static int getWidth(int length) {
        return 2 * length + (length - 2);
    }

    private static int getHeight(int length) {
        if (length <= 2) return 4;
        else return 2 * length;
    }

    public static void tesselate(int size, TETile[][] world, int length, int col, int row, boolean random) {
        int hexRow = row;
        int hexCol = col;
        int hexColOffset = length * 2 - 1;
        int sizeOffset = size;
        for (int i = 0; i < size; i++) {
            hexCol(sizeOffset, world, length, hexCol, hexRow, random);
            hexRow -= length;
            hexCol += hexColOffset;
            sizeOffset++;
        }

        sizeOffset -= 2;
        hexRow += length * 2;

        for (int i = 0; i < size - 1; i++) {
            hexCol(sizeOffset, world, length, hexCol, hexRow, random);
            hexCol += hexColOffset;
            hexRow += length;
            sizeOffset--;
        }
    }

    public static void hexCol(int size, TETile[][] world, int length, int col, int row, boolean random) {
        int hexRow = row;
        for (int i = 0; i < size; i++) {
            addHexagon(world, length, col, hexRow, random);
            hexRow += 2 * length;
        }
    }
}
