package application;

import chess.ChessPiece;

public class UI {

	public static void printBoard(ChessPiece[][] piece) {
		for (int r=0; r<piece.length; r++) {
			System.out.print((8 - r) + " ");
			
			for (int c=0; c<piece.length; c++) {
				printPiece(piece[r][c]);
			}
			System.out.println();
		}
		System.out.println("  a b c d e f g h");

	}

	private static void printPiece(ChessPiece piece) {
		if (piece == null) {
			System.out.print("-");
		}
		else {
			System.out.print(piece);
		}
		System.out.print(" ");		
	}
	
}
