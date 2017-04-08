package simulate;

import bohnenspiel.*;
import student_player.*;

public class Simulate {

	static BohnenspielPlayer make_A() {
		return new StudentPlayerMinimaxImproved();
	}
	
	static BohnenspielPlayer make_B() {
		return new StudentPlayerMinimaxImprovedTest();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello World");
		int A_wins = 0;
		int B_wins = 0;
		int draws = 0;
		for (int i=0; i<10; i++) {
			System.out.print("A");
			BohnenspielPlayer p0 = make_A();
			BohnenspielPlayer p1 = make_B();
			p0.setColor(0);
			p1.setColor(1);
			int result = simulateGame(p0,p1);
			
			if (result == BohnenspielBoard.DRAW) {
				draws++;
			}
			else if (result == 0) {
				A_wins++;
			}
			else if (result == 1) {
				B_wins++;
			}

			System.out.println(" A: " + A_wins +" B: " +  B_wins + " draws: "+ draws);
			
			System.out.print("B");
			p0 = make_B();
			p1 = make_A();
			
			p0.setColor(0);
			p1.setColor(1);
			
			result = simulateGame(p0,p1);
			
			if (result == BohnenspielBoard.DRAW) {
				draws++;
			}
			else if (result == 0) {
				B_wins++;
			}
			else if (result == 1) {
				A_wins++;
			}
			System.out.println(" A: " + A_wins +" B: " +  B_wins + " draws: "+ draws);
		}
	}

	public static int simulateGame(BohnenspielPlayer p0, BohnenspielPlayer p1) {
		BohnenspielBoard board = new BohnenspielBoard();

		while (board.getWinner() == BohnenspielBoard.NOBODY) {

			if (board.getTurnPlayer() == 0) {
				board.move(p0.chooseMove(board.getBoardState()));

			} else {
				board.move(p1.chooseMove(board.getBoardState()));
			}
			//System.out.println(board.toString());
			System.out.print(".");
		}

		int w = board.getWinner();

		return w;
	}
}
