package student_player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import student_player.mytools.MyTools;

/** A Bohnenspiel player submitted by a student. */
public class StudentPlayer extends BohnenspielPlayer {

	/**
	 * You must modify this constructor to return your student number. This is
	 * important, because this is what the code that runs the competition uses
	 * to associate you with your agent. The constructor should do nothing else.
	 */
	public StudentPlayer() {
		super("260689725");
	}

	private static final int MAX_SCORE = 1000000;

	/// Weights for the evaluation function.
	/// I thought about doing some kind of genetic algorithm or machine learning
	/// so that's why these are here.
	public int a = 2, b = -2, c = 1;

	private int evaluationFunction(BohnenspielBoardState boardState) {

		int winner = boardState.getWinner();
		if (winner == BohnenspielBoardState.NOBODY) {

			int playerScore = boardState.getScore(player_id);
			int opponentScore = boardState.getScore(opponent_id);
			if (playerScore > 36) {
				return MAX_SCORE;
			}
			if (opponentScore > 36) {
				return -MAX_SCORE;
			}

			int[][] pits = boardState.getPits();

			int sum = 0;
			for (int j = 0; j < 6; j++) {
				sum += pits[player_id][j];
			}

			return a * playerScore + b * opponentScore + c * sum;
		} else if (winner == player_id) {
			return MAX_SCORE;
		} else if (winner == opponent_id) {
			return -MAX_SCORE;
		}

		return 0;
	}

	/// Represents a linked list of optimal moves
	private static class MoveChain {
		public BohnenspielMove currentMove;
		public MoveChain nextMoves;

		public MoveChain(BohnenspielMove m, MoveChain n) {
			m.getMoveType();
			this.currentMove = m;
			this.nextMoves = n;
		}
	}

	/// The return value of negamax. Represents a chain of moves and a score.
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

	/// Used to cache the evaluation function when reordering moves
	private static class BoardStateAndMoveAndOrderingScore {
		BohnenspielBoardState boardState;
		BohnenspielMove move;
		int orderingScore;

		public BoardStateAndMoveAndOrderingScore(BohnenspielBoardState b, BohnenspielMove m, int s) {
			this.boardState = b;
			this.move = m;
			this.orderingScore = s;
		}
	}

	/// A alpha-beta pruning negamax implementation of minimax
	private ScoreAndMoveChain negamax(final BohnenspielBoardState boardState, int alpha, int beta, int color,
			int remainingDepth, MoveChain previousOptimalMoves) {
		
		// If time's up, just return. The calculation was cancelled anyway.
		if (Thread.currentThread().isInterrupted()) {
			return new ScoreAndMoveChain(0, null);
		}

		ArrayList<BohnenspielMove> moves = boardState.getLegalMoves();

		// If terminal node, return its value
		if (moves.size() == 0) {
			return new ScoreAndMoveChain(evaluationFunction(boardState), null);
		}

		Collections.shuffle(moves);

		// If the depth is 1, instead of using the evaluation function to order
		// the moves and then recalculating the evaluation function at depth 0
		// and returning it, I just calculate it once and return the best one
		if (remainingDepth == 1) {

			BohnenspielMove bestMove = null;
			int bestScore = -MAX_SCORE;
			for (BohnenspielMove move : moves) {
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

		// First thing we do at this point is try to repeat the optimal move
		// from the previous iteration of iterative deepening, so that we end up
		// pruning more values.
		if (previousOptimalMoves != null) {
			BohnenspielBoardState clonedBoard = (BohnenspielBoardState) boardState.clone();
			clonedBoard.move(previousOptimalMoves.currentMove);
			bestScoreAndMoveChain = new ScoreAndMoveChain(previousOptimalMoves.currentMove, negamax(clonedBoard, -beta,
					-alpha, -1 * color, remainingDepth - 1, previousOptimalMoves.nextMoves));
			if (bestScoreAndMoveChain.score == MAX_SCORE) {
				return bestScoreAndMoveChain;
			}
			if (bestScoreAndMoveChain.score > alpha) {
				alpha = bestScoreAndMoveChain.score;
			}
		}

		// We create an array that contains states, moves and cached evaluation
		// function scores.
		int movesArraySize = moves.size();
		if (previousOptimalMoves != null) {
			movesArraySize -= 1;
		}
		BoardStateAndMoveAndOrderingScore[] nextStates = new BoardStateAndMoveAndOrderingScore[movesArraySize];

		// Populate the array
		int j = 0;
		for (BohnenspielMove move : moves) {
			// If it's the optimal move that we have already tried, ignore it
			if (previousOptimalMoves == null || !MyTools.movesAreEqual(move, previousOptimalMoves.currentMove)) {
				BohnenspielBoardState clonedBoard = (BohnenspielBoardState) boardState.clone();
				clonedBoard.move(move);
				int score = color * evaluationFunction(clonedBoard);
				// If a score is MAX_SCORE we might as well return it.
				if (score == MAX_SCORE) {
					return new ScoreAndMoveChain(MAX_SCORE, new MoveChain(move, null));
				}
				nextStates[j] = new BoardStateAndMoveAndOrderingScore(clonedBoard, move, score);
				j++;
			}
		}

		// Reorder moves based on evaluation function
		Arrays.sort(nextStates, new Comparator<BoardStateAndMoveAndOrderingScore>() {
			public int compare(BoardStateAndMoveAndOrderingScore a, BoardStateAndMoveAndOrderingScore b) {
				return b.orderingScore - a.orderingScore;
			}
		});

		// The actual alpha-beta part of the algorithm
		for (BoardStateAndMoveAndOrderingScore childState : nextStates) {

			ScoreAndMoveChain negaMaxResult = negamax(childState.boardState, -beta, -alpha, -1 * color,
					remainingDepth - 1, null);
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

		return bestScoreAndMoveChain;
	}

	public BohnenspielMove chooseMove(final BohnenspielBoardState boardState) {
		long startTime = System.nanoTime();

		// First moves were based on running the algorithm for a bit longer than
		// usual.
		// This is more for making everythig go faster than anything else.
		if (boardState.getTurnNumber() == 0) {
			if (player_id == 0) {
				return new BohnenspielMove(2, player_id);
			} else {
				int[] op_pits = boardState.getPits()[opponent_id];
				if (op_pits[0] == 0) {
					return new BohnenspielMove(2, player_id);
				} else if (op_pits[1] == 0) {
					return new BohnenspielMove(1, player_id);
				} else if (op_pits[2] == 0) {
					return new BohnenspielMove(1, player_id);
				} else if (op_pits[3] == 0) {
					return new BohnenspielMove(1, player_id);
				} else if (op_pits[4] == 0) {
					return new BohnenspielMove(2, player_id);
				} else if (op_pits[5] == 0) {
					return new BohnenspielMove(1, player_id);
				} else {
					// it was a skip
					return new BohnenspielMove(2, player_id);
				}
			}
		}

		final long timeout = 690;

		// It does iterative deepening and every step, it remembers the chain of
		// best moves down to the leaf.

		MoveChain previousBestMoves = null;

		int baseDepth = 12;
		for (int i = baseDepth; i < 250; i++) {

			// I create a new thread in which I calculate this iterations value for iterating deepening.
			// http://stackoverflow.com/questions/1164301/how-do-i-call-some-blocking-method-with-a-timeout-in-java

			ExecutorService executor = Executors.newSingleThreadExecutor();

			final int final_i = i;
			final MoveChain finalPreviousBestMoves = previousBestMoves;
			Callable<ScoreAndMoveChain> task = new Callable<ScoreAndMoveChain>() {
				public ScoreAndMoveChain call() {

					int alpha = -MAX_SCORE;
					int beta = MAX_SCORE;
					int color = 1;

					ScoreAndMoveChain result = negamax(boardState, alpha, beta, color, final_i, finalPreviousBestMoves);

					return result;
				}
			};

			Future<ScoreAndMoveChain> future = executor.submit(task);
			try {
				ScoreAndMoveChain newScoreAndMoves = future.get(timeout * 1_000_000l - (System.nanoTime() - startTime),
						TimeUnit.NANOSECONDS);
				
				// If it's a guaranteed win or a guaranteed fail assuming the
				// opponent is smart, stop looking because there is no more
				// point since minimax won't return anything more.
				if (newScoreAndMoves.score == MAX_SCORE) {
					return newScoreAndMoves.moveChain.currentMove;
				}
				if (newScoreAndMoves.score == -MAX_SCORE) {
					return newScoreAndMoves.moveChain.currentMove;
				}
				// Set the remembered move chain to be the returned move chain.
				previousBestMoves = newScoreAndMoves.moveChain;

			} catch (TimeoutException ex) {
				return previousBestMoves.currentMove;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				if (!executor.isTerminated()) {
					executor.shutdownNow();
				}
				future.cancel(true);
			}
		}
		return previousBestMoves.currentMove;
	}
}
