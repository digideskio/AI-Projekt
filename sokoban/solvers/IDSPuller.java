package sokoban.solvers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import sokoban.Board;
import sokoban.Position;
import sokoban.SearchInfo;
import sokoban.SearchStatus;
import sokoban.Board.Direction;

/**
 * A solver that pulls boxes around (it finds a path from the goal to the
 * start) with iterative deepening and hashing to avoid duplicate states.
 */
public class IDSPuller implements Solver
{
    private final int DEPTH_LIMIT = 1000;
    /**
     * The number of generated nodes
     */
    public static int generatedNodes = 0;
    private static int depth, maxDepth;
    private static Board board;
    
    private static int failedGoalTests;
    private static int numLeafNodes;
    private static int lastLeafCount;
    
    // Extra information for the puller
    private int boxesNotInStart, initialBoxesNotInStart;
    private boolean[][] boxStart;
    private Position playerStart;

    public int getIterationsCount()
    {
        return generatedNodes;
    }

    /**
     * Set of visited boards, including the player position
     */
    private HashSet<Long> visitedBoards;

    /**
     * Boards that just lead to deadlocks or already visited boards. It
     * doesn't make sense to visit these in later iterations.
     */
    private HashSet<Long> failedBoards;

    /**
     * Recursive Depth-First algorithm
     * 
     * @param maxDepth The maximum depth.
     * @return
     */
    private SearchInfo dfs()
    {
        generatedNodes++;

        if (boxesNotInStart == 0) {
            // Found a solution, try to go back to the start
            Position player = board.positions[board.getPlayerRow()][board.getPlayerCol()];
            Deque<Direction> path = board.findPath(playerStart, player);
            if (path != null) {
                SearchInfo result = SearchInfo.emptySolution();
                result.solution.addAll(0, path);
                return result;
            }
        }

        if (depth >= maxDepth) {
            failedGoalTests += boxesNotInStart;
            numLeafNodes++;
            return SearchInfo.Inconclusive;
        }

        // True if at least one successor tree was inconclusive.
        boolean inconclusive = false;

        long hash = board.getZobristKey();
        
        final Position source = board.positions[board.getPlayerRow()][board.getPlayerCol()];
        depth++;

        final byte[][] cells = board.cells;
        for (final Position boxTo : findReachableBoxSquares()) {
            for (final Direction dir : Board.Direction.values()) {
                final Position boxFrom = board.getPosition(boxTo,
                        Board.moves[dir.ordinal()]);
                final Position playerTo = board.getPosition(boxTo, Board.moves[dir
                        .reverse().ordinal()]);
                /*System.out.println(board+"\n\n"+boxesNotInStart+"   "+
                    boxFrom+"("+cells[boxFrom.row][boxFrom.column]+")  "+
                    boxTo+"("+cells[boxTo.row][boxTo.column]+")  "+
                    playerTo+"("+cells[playerTo.row][playerTo.column]+")");*/
                //System.out.println(remainingDepth+": "+board);
                if (Board.is(cells[boxFrom.row][boxFrom.column], Board.BOX)
                        && !Board
                                .is(cells[boxTo.row][boxTo.column], Board.REJECT_BOX)
                        && !Board
                                .is(cells[playerTo.row][playerTo.column], Board.REJECT_PULL)) {
                    // The move is possible
                                        
                    // Move the player and pull the box
                    board.moveBox(boxFrom, boxTo);
                    board.movePlayer(source, playerTo);
                    
                    if (boxStart[boxFrom.row][boxFrom.column]) boxesNotInStart++;
                    if (boxStart[boxTo.row][boxTo.column]) boxesNotInStart--;

                    // Process successor states
                    SearchInfo result = SearchInfo.Failed;
                    if (visitedBoards.add(board.getZobristKey())) {
                        // This state hasn't been visited before
                        result = dfs();
                    }

                    // Restore changes
                    board.moveBox(boxTo, boxFrom);
                    board.movePlayer(playerTo, source);

                    if (boxStart[boxFrom.row][boxFrom.column]) boxesNotInStart--;
                    if (boxStart[boxTo.row][boxTo.column]) boxesNotInStart++;

                    // Evaluate result
                    switch (result.status) {
                        case Solution:
                            // We have found a solution. Find the reverse
                            // path of the move and add it to the solution.
                            result.solution.addLast(dir);
                            if (depth > 1) {
                                result.solution.addAll(board.findPath(boxTo, source));
                            }
                            depth--;
                            return result;
                        case Inconclusive:
                            // Make the parent inconclusive too
                            inconclusive = true;
                            continue;
                        case Failed:
                            // Mark this node as failed
                            continue;
                    }
                }
            }
        }

        depth--;

        if (inconclusive) {
            // Add all successors that failed to the failed set
            return SearchInfo.Inconclusive;
        }
        else {
            // All successors failed, so this node is failed
            failedBoards.add(hash);
            return SearchInfo.Failed;
        }
    }
    
    private Collection<Position> findReachableBoxSquares() {
        if (depth == 1) {
            Collection<Position> boxes = new HashSet<Position>(board.boxCount);
            for (int row = 1; row < board.height-1; row++) {
                for (int col = 1; col < board.width-1; col++) {
                    if (!Board.is(board.cells[row][col], Board.BOX)) {
                        continue;
                    }
                    
                    for (Direction dir : Direction.values()) {
                        int spaceRow = row+Board.moves[dir.ordinal()][0];
                        int spaceCol = col+Board.moves[dir.ordinal()][1];
                        if (spaceRow > 0 && spaceRow < board.height-1
                            && spaceCol > 0 && spaceCol < board.width-1) {
                            boxes.add(board.positions[spaceRow][spaceCol]);
                        }
                    }
                }
            }
            return boxes;
        } else {
            return board.findReachableBoxSquares();
        }
    }

    public String solve(final Board startBoard)
    {
        failedBoards = new HashSet<Long>();
        final int lowerBound = lowerBound(startBoard);
        System.out.println("lowerBound(): " + lowerBound);
        System.out.println("IDS depth limit (progress): ");
        
        reverseBoard(startBoard);
        
        int step = 3;
        lastLeafCount = -1;
        for (maxDepth = lowerBound; maxDepth < DEPTH_LIMIT; maxDepth += step) {
            System.out.print(maxDepth + ".");

            visitedBoards = new HashSet<Long>(failedBoards);
            depth = 0;
            board = (Board) startBoard.clone();
            boxesNotInStart = initialBoxesNotInStart;
            visitedBoards.add(board.getZobristKey());
            failedGoalTests = 0;

            final SearchInfo result = dfs();
            if (result.solution != null) {
                System.out.println();
                return Board.solutionToString(result.solution);
            }
            else if (result.status == SearchStatus.Failed) {
                System.out.println("no solution!");
                return null;
            }
            
            // If we have many boxes in the goals we can take a larger step
            int nonGoalPerNode = failedGoalTests / numLeafNodes;
            int goalStep = lowerBound / (board.boxCount - nonGoalPerNode + 1);
            
            // If we have pruned so many nodes we have less leaf nodes this
            // time we take a larger step
            int depthChangeStep = 10 * (numLeafNodes / lastLeafCount);
            
            step = Math.max(3, Math.max(goalStep, depthChangeStep));
            lastLeafCount = numLeafNodes;
        }

        System.out.println("maximum depth reached!");
        return null;
    }
    
    private void reverseBoard(final Board board) {
        // Store starting positions
        playerStart = board.positions[board.getPlayerRow()][board.getPlayerCol()];
        boxStart = new boolean[board.height][board.width];
        initialBoxesNotInStart = board.boxCount;
        for (int row = 0; row < board.height; row++) {
            for (int column = 0; column < board.width; column++) {
                if (Board.is(board.cells[row][column], Board.BOX)) {
                    if (Board.is(board.cells[row][column], Board.GOAL)) {
                        initialBoxesNotInStart--;
                    }
                    board.cells[row][column] &= ~Board.BOX;
                    boxStart[row][column] = true;
                }
            }
        }
        
        // Put the boxes in the goals
        for (int row = 0; row < board.height; row++) {
            for (int column = 0; column < board.width; column++) {
                if (Board.is(board.cells[row][column], Board.GOAL)) {
                    board.cells[row][column] |= Board.BOX;
                }
            }
        }
        
        board.forceReachabilityUpdate();
    }

    private static int lowerBound(final Board board)
    {
        final ArrayList<Position> boxes = new ArrayList<Position>();
        final Queue<Position> goals = new LinkedList<Position>();
        for (int row = 0; row < board.height; row++) {
            for (int col = 0; col < board.width; col++) {
                if (Board.is(board.cells[row][col], Board.BOX)) {
                    boxes.add(board.positions[row][col]);
                }
                if (Board.is(board.cells[row][col], Board.GOAL)) {
                    goals.add(board.positions[row][col]);
                }
            }
        }
        int result = 0;
        while (!goals.isEmpty()) {
            final Position goal = goals.poll();
            Position minBox = null;
            int min = Integer.MAX_VALUE;
            for (final Position box : boxes) {
                final int tmp = distance(goal, box);
                if (tmp < min) {
                    min = tmp;
                    minBox = box;
                }
            }

            boxes.remove(minBox);
            result += min;
        }
        return result;
    }

    /**
     * Approximate the distance between two positions
     * 
     * The distance will be the absolute minimum and are guaranteed to be equal
     * to or greater then the real distance.
     * 
     * TODO It might be smarter to implement a search that takes the actual
     * board into account.
     * 
     * @param a One of the positions
     * @param b The other position
     * @return The approximate distance between the two.
     */
    private static int distance(final Position a, final Position b)
    {
        return Math.abs(a.column - b.column) + Math.abs(a.row - b.row);
    }
}
