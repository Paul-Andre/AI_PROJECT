package student_player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import bohnenspiel.BohnenspielBoardState;
import bohnenspiel.BohnenspielMove;
import bohnenspiel.BohnenspielPlayer;
import bohnenspiel.BohnenspielMove.MoveType;
import student_player.mytools.MyTools;

/** A Bohnenspiel player submitted by a student. */
public class StudentPlayerMinimaxImprovedTest extends BohnenspielPlayer {

    /** You must modify this constructor to return your student number.
     * This is important, because this is what the code that runs the
     * competition uses to associate you with your agent.
     * The constructor should do nothing else. */
    public StudentPlayerMinimaxImprovedTest() { super("Test"); }
    
    private static final int MAX_SCORE = 10000;
    

    private int evaluationFunction(BohnenspielBoardState boardState) {
    	
    	int winner = boardState.getWinner();
		if (winner == BohnenspielBoardState.NOBODY) {
	    	
	    	int playerScore = boardState.getScore(player_id);
	    	int opponentScore = boardState.getScore(opponent_id);
	    	if (playerScore > 36) {
	    		return MAX_SCORE;
	    	}
	    	if (opponentScore > 36) {
	    		return - MAX_SCORE; 
	    	}

	    	
			int[][] pits = boardState.getPits();

			int sum = 0;
			for (int j = 0; j < 6; j++) {
				sum += pits[player_id][j];
			}
			
	    	
	    	return playerScore-opponentScore + sum;
    	}
    	else if (winner == player_id) {
    		return MAX_SCORE;
    	}
    	else if (winner == opponent_id) {
    		return - MAX_SCORE;
    	}
		
    	return 0;
    }
    
    
    private static class MoveChain {
    	public BohnenspielMove currentMove;
    	public MoveChain nextMoves;
    	
    	public MoveChain(BohnenspielMove m, MoveChain n) {
    		m.getMoveType();
    		this.currentMove = m;
    		this.nextMoves = n;
    	}
    }
    
    private static class ScoreAndMoveChain {
    	int score;
    	MoveChain moveChain;
    	public ScoreAndMoveChain(int s, MoveChain mc) {
    		this.score = s;
    		this.moveChain = mc;
    	}
    	public ScoreAndMoveChain(BohnenspielMove m, ScoreAndMoveChain smc) {
    		this.score = smc.score * -1;
    		this.moveChain = new MoveChain(m, smc.moveChain);
    	}
    }
    
    private static class BoardStateAndMoveAndOrderingScore {
    	BohnenspielBoardState boardState;
    	BohnenspielMove move;
    	int orderingScore;
    	public BoardStateAndMoveAndOrderingScore (BohnenspielBoardState b, BohnenspielMove m, int s) {
    		this.boardState = b;
    		this.move = m;
    		this.orderingScore = s;
    	}
    }
    
    private boolean movesAreEqual(BohnenspielMove a, BohnenspielMove b) {
    	if (a == null || b == null) {
    		return a == b;
    	}
    	BohnenspielMove.MoveType aType = a.getMoveType();
    	BohnenspielMove.MoveType bType = b.getMoveType();
    	
    	if (aType == BohnenspielMove.MoveType.PIT && bType == BohnenspielMove.MoveType.PIT){
    		return a.getPit() == b.getPit();
    	}
    	else {
    		return aType == bType;
    	}
    }
    
    
    
    private ScoreAndMoveChain negamax
    (final BohnenspielBoardState boardState, int alpha, int beta, int color, int remainingDepth, MoveChain previousOptimalMoves)
    {
    	
    	ArrayList<BohnenspielMove> moves = boardState.getLegalMoves();
    	
    	if (moves.size() == 0) {
    		return new ScoreAndMoveChain(evaluationFunction(boardState),null);
    	}
    	

    	Collections.shuffle(moves);
    	
    	
    	if (remainingDepth == 1) {
    		// If remainingDepth is 1, it is assumed that we are at one level deeper than last time, so we don't need to look at previousOptimal
    		
    		BohnenspielMove bestMove = null;
    		int bestScore = - MAX_SCORE;
    		for (BohnenspielMove move: moves) {
    			BohnenspielBoardState clonedBoard = (BohnenspielBoardState) boardState.clone();
    			clonedBoard.move(move);
    			int score = color * evaluationFunction(clonedBoard);
    			
    			if (score > bestScore) {
    				bestMove = move;
    				bestScore = score;
    			}
    			if (score == MAX_SCORE) {
    				break;
    			}
    		}
			return new ScoreAndMoveChain(bestScore, new MoveChain(bestMove, null)); 
    	}
    	


    	ScoreAndMoveChain bestScoreAndMoveChain = null;

    	if (previousOptimalMoves != null) {
			BohnenspielBoardState clonedBoard = (BohnenspielBoardState) boardState.clone();
			clonedBoard.move(previousOptimalMoves.currentMove);
			bestScoreAndMoveChain = new ScoreAndMoveChain(previousOptimalMoves.currentMove, negamax(clonedBoard, -beta, -alpha, -1 * color, remainingDepth - 1, previousOptimalMoves.nextMoves));
			if (bestScoreAndMoveChain.score == MAX_SCORE) {
				return bestScoreAndMoveChain;
			}
			if (bestScoreAndMoveChain.score > alpha) {
				alpha = bestScoreAndMoveChain.score;
			}
    	}
    	
    	
    	
    	int movesArraySize = moves.size();
    	if (previousOptimalMoves != null) {
    		movesArraySize -= 1;
    	}
    	
    	BoardStateAndMoveAndOrderingScore[] nextStates = new BoardStateAndMoveAndOrderingScore[movesArraySize]; 

    	int j = 0;
    	for(BohnenspielMove move : moves) {
    		if (previousOptimalMoves== null || !movesAreEqual(move, previousOptimalMoves.currentMove)) {
    			BohnenspielBoardState clonedBoard = (BohnenspielBoardState) boardState.clone();
    			clonedBoard.move(move);
    			int score = color * evaluationFunction(clonedBoard);
    			if (score == MAX_SCORE) {
    				return new ScoreAndMoveChain(MAX_SCORE, new MoveChain(move, null));
    			}
    			nextStates[j] = new BoardStateAndMoveAndOrderingScore(clonedBoard, move, score);
    			j++;
    		}
    	}
    	
    	
    	Arrays.sort(nextStates, new Comparator<BoardStateAndMoveAndOrderingScore>() {
    		public int compare(BoardStateAndMoveAndOrderingScore a, BoardStateAndMoveAndOrderingScore b) {
    			return b.orderingScore - a.orderingScore;
    		}
    	});
    	
    	for (BoardStateAndMoveAndOrderingScore childState : nextStates) {
    		
    		ScoreAndMoveChain negaMaxResult = negamax(childState.boardState, -beta, -alpha, -1 * color, remainingDepth - 1, null);
            ScoreAndMoveChain obtainedScoreAndMoves = new ScoreAndMoveChain(childState.move, negaMaxResult);
            if (obtainedScoreAndMoves.score == MAX_SCORE) {
				return obtainedScoreAndMoves;
			}
            if (bestScoreAndMoveChain == null || obtainedScoreAndMoves.score > bestScoreAndMoveChain.score) {
            	bestScoreAndMoveChain = obtainedScoreAndMoves;
            }
            if (obtainedScoreAndMoves.score > alpha) {
				alpha = obtainedScoreAndMoves.score;
			}
            if (alpha >= beta) {
            	break;
            }
            
    	}
    	
    	if (bestScoreAndMoveChain == null) {
    		System.out.println("IS A NULL AT END and there were "+moves.size()+" possible moves and previousOpt is null: "+(previousOptimalMoves==null));
    		
    	}
    	return bestScoreAndMoveChain;
    	
    }
    
    
    public BohnenspielMove chooseMove(final BohnenspielBoardState boardState)
    {
    	long startTime = System.nanoTime();
    	
    	//System.out.println("Turn " + boardState.getTurnNumber());
    	if (boardState.getTurnNumber() == 0) {
    		return new BohnenspielMove(2);
    	}
    	
    	final long timeout = (boardState.getTurnNumber() == 0)? 29500 : 690; 
    	//final long timeout = (boardState.getTurnNumber() == 0)? 29000 : 600; // Play with Youri;
    	
        int[][] pits = boardState.getPits();
        
        int sum = 0;
        for (int i=0; i<2; i++) {
        	for (int j=0; j<6; j++) {
        		sum += pits[i][j];
        	}
        }
        
        MoveChain previousBestMoves = null;
        
        
        
        int baseDepth = (boardState.getTurnNumber() == 0)? 16 : 13;
        for (int depth=baseDepth; depth<250; depth++) {
        
        	
	        //http://stackoverflow.com/questions/1164301/how-do-i-call-some-blocking-method-with-a-timeout-in-java
        	
	        ExecutorService executor = Executors.newSingleThreadExecutor();
	        
	        final int final_depth = depth;
	        final MoveChain finalPreviousBestMoves = previousBestMoves; 
	        Callable<ScoreAndMoveChain> task = new Callable<ScoreAndMoveChain>() {
	           public ScoreAndMoveChain call() {
	
	        	   int alpha = -MAX_SCORE;
	        	   int beta = MAX_SCORE;
	        	   int color = 1;
	        	   
	        	   ScoreAndMoveChain result = negamax(boardState, alpha, beta, color, final_depth, finalPreviousBestMoves);
					
	        	   return result;
	           }
	        };
	        
	        Future<ScoreAndMoveChain> future = executor.submit(task);
	        try {
	        	ScoreAndMoveChain newScoreAndMoves = future.get(timeout*1000000l - (System.nanoTime() - startTime), TimeUnit.NANOSECONDS);
	        	if (newScoreAndMoves.score == MAX_SCORE) {
	        		System.out.println("Winning after searching "+depth+" levels! At turn "+ boardState.getTurnNumber());
	        		return newScoreAndMoves.moveChain.currentMove;
	        	}
        		if (newScoreAndMoves.score == -MAX_SCORE) {
	        		System.out.println("Giving up after searching "+depth+" levels. :(");
	        		return newScoreAndMoves.moveChain.currentMove;
        		}
	        	previousBestMoves = newScoreAndMoves.moveChain;
	        	System.out.println(" "+depth+ " " +(System.nanoTime() - startTime)/1000000);
	           
	        } catch (TimeoutException ex) {
	        	
	        	System.out.println("Test looked "+(depth-1)+" moves ahead with "+sum+" beans on the board. At turn "+ boardState.getTurnNumber());
	        	return previousBestMoves.currentMove;
	        } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
	           future.cancel(true); // may or may not desire this
	        }
        }


    	System.out.println("Different eval looked "+250+" moves ahead with "+sum+" beans on the board.");
        
        
        return previousBestMoves.currentMove;
    }
}