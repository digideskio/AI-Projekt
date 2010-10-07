package sokoban.solvers;

import java.util.LinkedList;
import java.util.Random;

import sokoban.Board;
import sokoban.Board.Direction;

public class IDS implements Solver
{

    private final Board startBoard;
    private final int DEPTH_LIMIT = 1000;
    private Random rand = new Random();

    public IDS(Board startBoard)
    {
        this.startBoard = startBoard;
    }

    /**
     * Recursive Depth-First algorithm
     * 
     * @param maxDepth The maximum depth.
     * @return
     */
    private LinkedList<Board.Direction> dfs(Board board, int maxDepth)
    {
        // System.out.println(board.toString());

        if (board.getRemainingBoxes() == 0) {
            // Found a solution
            return new LinkedList<Board.Direction>();
        }

        if (maxDepth == 0)
            return null;

        for (Board.Direction dir : Board.Direction.values()) {
            // Check that the move is possible
            if (!board.canMove(dir))
                continue;

            // The move is possible

            // Make the move on a copy of the board
            Board successor = (Board) board.clone();
            successor.move(dir);

            // Recurse
            LinkedList<Board.Direction> sol = dfs(successor, maxDepth - 1);
            if (sol != null) {
                sol.addFirst(dir);
                return sol;
            }
        }

        return null;
    }

    /**
     * Returns an array of Direction enums in random order.
     * 
     * @return An array of all Direction enums.
     */
    private Direction[] getDirectionsRandomly()
    {
        Direction[] directions = Board.Direction.values();

        // Shuffle array
        for (int i = 0; i < directions.length; i++) {
            int randomPosition = rand.nextInt(directions.length);
            Direction temp = directions[i];
            directions[i] = directions[randomPosition];
            directions[randomPosition] = temp;
        }
        
        return directions;
    }


    public String solve()
    {
        System.out.println("IDS depth limit (progress): ");
        for (int maxDepth = 1; maxDepth < DEPTH_LIMIT; maxDepth += 3) {
            LinkedList<Board.Direction> solution = dfs(startBoard, maxDepth);
            if (solution != null) {
                System.out.println();
                return Board.solutionToString(solution);
            }
            System.out.print(maxDepth + ".");
        }

        System.out.println("no solution!");
        return null;
    }

}
