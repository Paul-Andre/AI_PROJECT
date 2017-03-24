package student_player;

import java.util.ArrayList;
import java.util.Collections;

import bohnenspiel.BohnenspielBoardState;
import bohnenspiel.BohnenspielMove;
import bohnenspiel.BohnenspielPlayer;
import bohnenspiel.BohnenspielMove.MoveType;
import student_player.mytools.MyTools;

/** A Bohnenspiel player submitted by a student. */
public class StudentPlayerMinimax extends BohnenspielPlayer {

    /** You must modify this constructor to return your student number.
     * This is important, because this is what the code that runs the
     * competition uses to associate you with your agent.
     * The constructor should do nothing else. */
    public StudentPlayerMinimax() { super("Top Memer"); }

    /** This is the primary method that you need to implement.
     * The ``board_state`` object contains the current state of the game,
     * which your agent can use to make decisions. See the class
bohnenspiel.RandomPlayer
     * for another example agent. */
    
    
    private int evaluationFunction(BohnenspielBoardState board_state) {
    	return board_state.getScore(player_id) - board_state.getScore(opponent_id);
    }
    
    /** Evaluates a game state using minimax and returns a score based on the evaluation function **/
    private int minimax(BohnenspielBoardState board_state, int remaining_depth, int alpha, int beta) {
    	
    	int winner = board_state.getWinner();
    	if (winner == opponent_id || board_state.getScore(opponent_id) > 36) {
    		return -1000;
    	}
    	if (winner == player_id || board_state.getScore(player_id) > 36) {
    		return 1000;
    	}
    	if (winner == BohnenspielBoardState.DRAW) {
    		return 0;
    	}

    	if (remaining_depth == 0) {
    		return evaluationFunction(board_state);
    	}
    	
    	ArrayList<BohnenspielMove> moves = board_state.getLegalMoves();
    	Collections.shuffle(moves);
    	
		if (board_state.getTurnPlayer() == player_id) {
    	
	    	int max_score = -100000;
	    	for (BohnenspielMove move: moves) {
	    		BohnenspielBoardState cloned_board_state = (BohnenspielBoardState) board_state.clone();
	    		
	            cloned_board_state.move(move);
	
	            int score = minimax(cloned_board_state, remaining_depth-1, alpha, beta);
	            if (score == 1000) {
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
			int min_score = 100000;
	    	for (BohnenspielMove move: moves) {
	    		BohnenspielBoardState cloned_board_state = (BohnenspielBoardState) board_state.clone();
	    		
	            cloned_board_state.move(move);
	
	            int score = minimax(cloned_board_state, remaining_depth-1, alpha, beta);
	            if (score == -1000) {
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
    

    public BohnenspielMove chooseMove(BohnenspielBoardState board_state)
    {
        // Get the contents of the pits so we can use it to make decisions.
        int[][] pits = board_state.getPits();

        // Use ``player_id`` and ``opponent_id`` to get my pits and opponent pits.
        int[] my_pits = pits[player_id];
        int[] op_pits = pits[opponent_id];

        // Get the legal moves for the current board state.
        ArrayList<BohnenspielMove> moves = board_state.getLegalMoves();
        Collections.shuffle(moves);
     
        BohnenspielMove best_move = null;
        int best_score = -10000;
        for (BohnenspielMove move: moves) {
    		BohnenspielBoardState cloned_board_state = (BohnenspielBoardState) board_state.clone();
    		cloned_board_state.move(move);
            int score = minimax(cloned_board_state, 9, -100000,  100000);
            if (score > best_score) {
            	best_score = score;
            	best_move = move;
            }
            //System.out.println("Considering move "+move.toPrettyString()+" with score "+score);

    	}

        return best_move;
    }
}