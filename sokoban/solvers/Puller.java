/**
 * 
 */
package sokoban.solvers;

import java.util.ArrayList;
import java.util.LinkedList;

import sokoban.Board;

public class Puller implements Solver
{
    private final Board startBoard;
    private Board board;
    
    private ArrayList<Position> boxes;
    
    public class Position {
        final int x, y;
        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    public class PlayerPosDir extends Position {
        final int bx, by; // relative box position
        public PlayerPosDir(int x, int y, int bx, int by) {
            super(x, y);
            this.bx = bx;
            this.by = by;
        }
    }

    public Puller(Board startBoard)
    {
        this.startBoard = (Board) startBoard.clone();
        int numBoxes = startBoard.getRemainingBoxes();
        startBoard.reverse();
        boxes = new ArrayList<Position>(numBoxes);
    }
    
    /**
     * Resets the board to the starting board.
     */
    private void reset() {
        board = (Board)startBoard.clone();
        
        // Store all boxes
        int b = 0;
        for (int i = 0; i < board.height; ++i) {
            for (int j = 0; j < board.width; ++j) {
                if (board.cells[i][j] == Board.BOX) {
                    boxes.set(b, new Position(i, j));
                    b++;
                }
            }
        }
    }
    
    @Override
    public String solve()
    {
        do {
            reset();
            
            do {
                PlayerPosDir playerPosDir = choosePosition();
                while (moveBox(playerPosDir)) { }
                
            } while (!deadlock());
        } while (!solved());
        
        return null;
    }

    private boolean solved()
    {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean deadlock()
    {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean moveBox(Position box)
    {
        // TODO Auto-generated method stub
        return false;
    }

    private PlayerPosDir choosePosition()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
