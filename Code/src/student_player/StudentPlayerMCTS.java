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
public class StudentPlayerMCTS extends BohnenspielPlayer {

    /** You must modify this constructor to return your student number.
     * This is important, because this is what the code that runs the
     * competition uses to associate you with your agent.
     * The constructor should do nothing else. */
    public StudentPlayerMCTS() { super("~~~~MCTS~~~~"); }
    
    private int evaluationFunction(BohnenspielBoardState board_state) {
    	return board_state.getScore(player_id) - board_state.getScore(opponent_id);
    }
    
    private int evaluateWinnerWhenThereAreNoMovesPossibleYetForSomeReasonTheGameIsNotFinished ( BohnenspielBoardState board ) {
    	int sum = 0;
    	int[][] pits = board.getPits();
    	for (int i=0; i<6; i++) {
    		sum += pits[0][i];
    		sum -= pits[1][i];
    	}
    	int p0Score = board.getScore(0) - board.getScore(1) + sum;
    	if (p0Score == 0) {
    		return BohnenspielBoardState.DRAW;
    	}
    	else if (p0Score > 0 ) {
    		return 0;
    	}
    	else {
    		return 1;
    	}
    }
    
    
    /** returns who won after random descent **/
    private int randomDescent(BohnenspielBoardState a) {
    	Random rand = new Random();
    	while (true) {
	    	int winner = a.getWinner();
	    	if (winner != BohnenspielBoardState.NOBODY) {
	    		return winner;
	    	}
	    	else {
	        	ArrayList<BohnenspielMove> moves = a.getLegalMoves();
	    		if (moves.size() == 0) {
	    			return evaluateWinnerWhenThereAreNoMovesPossibleYetForSomeReasonTheGameIsNotFinished(a);
	    		}
	    		a.move(moves.get(rand.nextInt(moves.size())));
	    	}
    	}
    }
    
    private double monte(BohnenspielBoardState a) {
    	int total = 0;
    	int count = 25;
    	for (int i=0; i<count; i++) {
    		BohnenspielBoardState cloned = (BohnenspielBoardState) a.clone();
    		int winner = randomDescent(cloned);
    		if (winner == player_id){
    			total ++;
    		}
    		else if (winner == opponent_id) {
    			total --;
    		}
    	}
    	return ((double)(total)/(double)(count));
    }
    
    private int distance(BohnenspielBoardState a, BohnenspielBoardState b) {
    	return Math.abs( (a.getScore(0) - a.getScore(1)) - (b.getScore(0) - b.getScore(1)) );
    }
    
    
    /** Evaluates a game state using minimax and returns a score based on the evaluation function **/
    private double minimax(final BohnenspielBoardState board_state, int remaining_depth, double alpha, double beta) {
    	
    	int winner = board_state.getWinner();
    	if (winner == opponent_id || board_state.getScore(opponent_id) > 36) {
    		return -1;
    	}
    	if (winner == player_id || board_state.getScore(player_id) > 36) {
    		return 1;
    	}
    	if (winner == BohnenspielBoardState.DRAW) {
    		return 0;
    	}

    	if (remaining_depth == 0) {
    		return monte(board_state);
    	}
    	
    	ArrayList<BohnenspielMove> moves = board_state.getLegalMoves();
    	Collections.shuffle(moves);
    	
    	
    	final BohnenspielBoardState[] next_states = new BohnenspielBoardState[moves.size()];
    	
    	for (int i=0; i<moves.size(); i++) {
    		BohnenspielBoardState cloned_board_state = (BohnenspielBoardState) board_state.clone();
    		cloned_board_state.move(moves.get(i));
    		next_states[i] = cloned_board_state;
    	}
    	
    	
    	Arrays.sort(next_states, new Comparator<BohnenspielBoardState>() {
    		public int compare(BohnenspielBoardState a, BohnenspielBoardState b) {
    			return distance(b, board_state) - distance(a, board_state);
    		}
    	});
    	
    	
		if (board_state.getTurnPlayer() == player_id) {
    	
	    	double max_score = -100000;
	    	for (BohnenspielBoardState cloned_board_state: next_states) {
	            double score = minimax(cloned_board_state, remaining_depth-1, alpha, beta);
	            if (score == 1) {
	            	return score;
	            }
	            if (score > max_score){
	            	max_score = score;
	            }
	            if (max_score > alpha) {
	            	alpha = max_score;
	            }
	            if (beta <= alpha) {
	            	break;
	            }
	
	    	}
	    	return max_score;
		}
		else {
			double min_score = 100000;
	    	for (BohnenspielBoardState cloned_board_state: next_states) {
	
	            double score = minimax(cloned_board_state, remaining_depth-1, alpha, beta);
	            if (score == -1) {
	            	return score;
	            }
	            if (score < min_score){
	            	min_score = score;
	            }
	            if (min_score < beta) {
	            	beta = min_score;
	            }
	            if (beta <= alpha) {
	            	break;
	            }
	
	    	}
	    	return min_score;
		}
    }
    

    public BohnenspielMove chooseMove(final BohnenspielBoardState board_state)
    {
    	long startTime = System.nanoTime();
    	
        // Get the legal moves for the current board state.
        final ArrayList<BohnenspielMove> moves = board_state.getLegalMoves();
        Collections.shuffle(moves);
     
        int[][] pits = board_state.getPits();
        
        int sum = 0;
        for (int i=0; i<2; i++) {
        	for (int j=0; j<6; j++) {
        		sum += pits[i][j];
        	}
        }
        
        
        BohnenspielMove previous_best_move = null;
        
        for (int i=2; i<150; i++) {
        
        	
	        //http://stackoverflow.com/questions/1164301/how-do-i-call-some-blocking-method-with-a-timeout-in-java
	        ExecutorService executor = Executors.newCachedThreadPool();
	        
	        final int final_i = i;
	        Callable<Object> task = new Callable<Object>() {
	           public Object call() {
	        	   
					BohnenspielMove best_move = null;
					double best_score = -10000;
					
					for (BohnenspielMove move : moves) {
						BohnenspielBoardState cloned_board_state = (BohnenspielBoardState) board_state.clone();
						cloned_board_state.move(move);
						double score = minimax(cloned_board_state, final_i, -100000, 100000);
						if (score > best_score) {
							best_score = score;
							best_move = move;
						}
	
					}
					
					return best_move;
	           }
	        };
	        
	        Future<Object> future = executor.submit(task);
	        try {
	           BohnenspielMove result = (BohnenspielMove) future.get(675*1000000 - (System.nanoTime() - startTime), TimeUnit.NANOSECONDS);
	           previous_best_move = result;
	        } catch (TimeoutException ex) {
	        	//System.out.println("Looked "+(i+1)+" moves ahead with "+sum+" beans on the board.");
	        	return previous_best_move;
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

    	//System.out.println("Looked "+150+" moves ahead with "+sum+" beans on the board.");
        return previous_best_move;
    }
}