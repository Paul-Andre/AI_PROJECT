package student_player.mytools;

import bohnenspiel.BohnenspielMove;

public class MyTools {
	
	/// Compares two moves. Used when reordering moves.
	static public boolean movesAreEqual(BohnenspielMove a, BohnenspielMove b) {
		if (a == null || b == null) {
			return a == b;
		}
		BohnenspielMove.MoveType aType = a.getMoveType();
		BohnenspielMove.MoveType bType = b.getMoveType();

		if (aType == BohnenspielMove.MoveType.PIT && bType == BohnenspielMove.MoveType.PIT) {
			return a.getPit() == b.getPit();
		} else {
			return aType == bType;
		}
	}

}
