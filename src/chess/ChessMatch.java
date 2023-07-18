package chess;

import boardgame.Board;
import boardgame.Position;
import chess.pieces.King;
import chess.pieces.Rook;

public class ChessMatch {

	private Board board;
	
	public ChessMatch() {
		board = new Board(8, 8);
		inicialSetup();
	}
	
	public ChessPiece[][] getPiece(){
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for (int r=0; r<board.getRows(); r++) {
			for (int c=0; c<board.getColumns(); c++) {
				mat[r][c] = (ChessPiece) board.piece(r, c);
			}
		}
		return mat; 
	}
	
	public void inicialSetup() {
		board.placePiece(new Rook(board, Color.Black), new Position(2, 1) );
		board.placePiece(new King(board, Color.White), new Position(2, 1));
	}
}
