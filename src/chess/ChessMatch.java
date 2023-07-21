package chess;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {

	private int turn;
	private Color currentPlayer;
	private Board board;
	private boolean check;
	private boolean checkMate;
	private ChessPiece enPassantVunerable;
	private ChessPiece promotedPawn;
	
	List<Piece> piecesOnBoard = new ArrayList<>();
	List<Piece> capturedPieces = new ArrayList<>();
	
	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		inicialSetup();
	}
	
	public ChessPiece[][] getPieces(){
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for (int r=0; r<board.getRows(); r++) {
			for (int c=0; c<board.getColumns(); c++) {
				mat[r][c] = (ChessPiece) board.piece(r, c);
			}
		}
		return mat; 
	}
	
	public int getTurn() {
		return turn;
	}
	
	public Color getCurrentPlayer() {
		return currentPlayer;
	}
	
	public boolean getCheck() {
		return check;
	}
	
	public boolean getCheckMate() {
		return checkMate;
	}
	
	public ChessPiece getEnPassantVunerable() {
		return enPassantVunerable;
	}
	
	public ChessPiece getPromotedPawn() {
		return promotedPawn;
	}
	
	private void nextTurn() {
		turn++;
		currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}
	
	public boolean[][] possibleMoves(ChessPosition sourcePosition){
		Position position = sourcePosition.toPosition();
		validadeSourcePosition(position);
		return board.piece(position).possibleMoves();
	}
	
	public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
		Position source = sourcePosition.toPosition();
		Position target = targetPosition.toPosition();
		validadeSourcePosition(source);
		validadeTargetPosition(source, target);
		
		Piece capturedPiece = makeMove(source, target);
		
		if (testCheck(currentPlayer)) {
			undoMove(source, target, capturedPiece);
			throw new ChessException("Illigal Move: cannot put yourself in check");
		}
		
		ChessPiece movedPiece = (ChessPiece)board.piece(target);
		
		//Promoted
		promotedPawn = null;
		if (movedPiece instanceof Pawn) {
			if ((movedPiece.getColor() == Color.WHITE && target.getRow() == 0) || (movedPiece.getColor() == Color.BLACK && target.getRow() == 0) ) {
				promotedPawn = (ChessPiece)board.piece(target);
				promotedPawn = removePromotedPiece("Q");
			}
		}
		
		check = (testCheck(opponent(currentPlayer))) ? true : false;
		if (testCheckMate(opponent(currentPlayer))) {
			 checkMate = true;
		}
		else {
			nextTurn();
		}
		// En Passant
		if (movedPiece instanceof Pawn && (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)) {
			enPassantVunerable = movedPiece;
		}
		else {
			enPassantVunerable = null;
		}
		
		return (ChessPiece)capturedPiece;
	}
	
	public ChessPiece removePromotedPiece(String type) {
		if (promotedPawn == null) {
			throw new IllegalStateException("There is no piece to be promoted");
		}
		if(!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) {
			throw new InvalidParameterException("Invalid type of promotion");
		}
		
		Position pos = promotedPawn.getChessPosition().toPosition();
		Piece p = board.removePiece(pos);
		piecesOnBoard.remove(p);
		
		ChessPiece newPiece = newPiece(type, promotedPawn.getColor());
		board.placePiece(newPiece, pos);
		piecesOnBoard.add(newPiece);
		return newPiece;
				
		
		
	}
	
	private ChessPiece newPiece(String type, Color color) {
		if (type.equals("B")) return new Bishop(board, color);
		if (type.equals("N")) return new Knight(board, color);
		if (type.equals("Q")) return new Queen(board, color);
		return new Rook(board, color);
		
	}
	
	private Piece makeMove(Position source, Position target) {
		ChessPiece p = (ChessPiece)board.removePiece(source);
		p.increaseMoveCount();
		Piece capturedPiece = board.removePiece(target);
		board.placePiece(p, target);
		if (capturedPiece != null) {
			piecesOnBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}
		//Castling King side
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceN = new Position(source.getRow(), source.getColumn() + 3);
			Position targetN = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece)board.removePiece(sourceN);
			board.placePiece(rook, targetN);
			rook.increaseMoveCount();
		}		
		//Castling Queen side
		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceN = new Position(source.getRow(), source.getColumn() - 4);
			Position targetN = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece)board.removePiece(sourceN);
			board.placePiece(rook, targetN);
			rook.increaseMoveCount();
		}
		//En passant
		if (p instanceof Pawn) {
			if(source.getColumn() != target.getColumn() && capturedPiece == null) {
				Position pawnCaptured;
				if(p.getColor() == Color.WHITE) {
					pawnCaptured = new Position(target.getRow() + 1, target.getColumn());
				}
				else {
					pawnCaptured = new Position(target.getRow() - 1, target.getColumn());
				}
				capturedPiece = board.removePiece(pawnCaptured);
				capturedPieces.add(capturedPiece);
				piecesOnBoard.remove(capturedPiece);
			}
		}
		
		return capturedPiece;
	}
	
	private void undoMove(Position source, Position target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece)board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);
		if (capturedPiece != null) {
			board.placePiece(capturedPiece, target);
			capturedPieces.remove(capturedPiece);
			piecesOnBoard.add(capturedPiece);
		}
		//Castling King side
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceN = new Position(source.getRow(), source.getColumn() + 3);
			Position targetN = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece)board.removePiece(targetN);
			board.placePiece(rook, sourceN);
			rook.decreaseMoveCount();
		}
		
		//Castling Queen side
		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceN = new Position(source.getRow(), source.getColumn() - 4);
			Position targetN = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece)board.removePiece(targetN);
			board.placePiece(rook, sourceN);
			rook.decreaseMoveCount();
		}
		//En passant
		if (p instanceof Pawn) {
			if(source.getColumn() != target.getColumn() && capturedPiece == enPassantVunerable) {
				ChessPiece pawn = (ChessPiece)board.removePiece(target);
				Position pawnCaptured;
				if(p.getColor() == Color.WHITE) {
					pawnCaptured = new Position(3, target.getColumn());
				}
				else {
					pawnCaptured = new Position(4, target.getColumn());
				}
				board.placePiece(pawn, pawnCaptured);
			}
		}
	}
	
	private void validadeSourcePosition(Position position) {
		if (!board.thereIsAPiece(position)) {
			throw new ChessException("There is no piece on source position");
		}
		if (currentPlayer != ((ChessPiece)board.piece(position)).getColor()) {
			throw new ChessException("You can't move other player pieces");
		}
		if (!board.piece(position).isThereAnyPossibleMove()) {
			throw new ChessException("There is no possible moves for this piece");
		}
	}
	
	private void validadeTargetPosition(Position source, Position Target) {
		if (!board.piece(source).possibleMove(Target)) {
			throw new ChessException("Chosen piece cannot move to that position");
		}
	}
	
	private Color opponent(Color color) {
		return (color == Color.WHITE) ? Color.BLACK : Color.WHITE; 
	}
	
	private ChessPiece king (Color color) {
		List<Piece> list = piecesOnBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
		for (Piece p : list) {
			if (p instanceof King) {
				return (ChessPiece)p;
			}
		}
		throw new IllegalStateException("There is no " + color + " king on board");
	}
	
	private boolean testCheck(Color color) {
		Position kingPosition = king(color).getChessPosition().toPosition();
		List<Piece> opponentPieces = piecesOnBoard.stream().filter(x -> ((ChessPiece)x).getColor() == opponent(color)).collect(Collectors.toList());
		for (Piece p : opponentPieces) {
			boolean[][] mat = p.possibleMoves();
				if(mat[kingPosition.getRow()][kingPosition.getColumn()]) {
					return true;
				}
		}
		return false;
	}
	
	private boolean testCheckMate(Color color) {
		if(!testCheck(color)) {
			return false;
		}
		List<Piece> list = piecesOnBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
		for (Piece p : list) {
			boolean[][] mat = p.possibleMoves();
			for (int r=0; r<mat.length; r++) {
				for (int c=0; c<mat.length; c++) {
					if(mat[r][c]) {
						Position source = ((ChessPiece)p).getChessPosition().toPosition();
						Position target = new Position(r, c);
						Piece capturedPiece = makeMove(source, target);
						boolean testCheck = testCheck(color);
						undoMove(source, target, capturedPiece);
						if(!testCheck) {
							return false;
						}
					}
				}
			}
		}
		return true;
		
	}
	
	private void placeNewPiece(char column, int row, ChessPiece piece) {
		board.placePiece(piece, new ChessPosition(column, row).toPosition());
		piecesOnBoard.add(piece);
	}
	
	public void inicialSetup() {
		placeNewPiece('a', 1, new Rook(board, Color.WHITE));
        placeNewPiece('b', 1, new Knight(board, Color.WHITE));
        placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
        placeNewPiece('d', 1, new Queen(board, Color.WHITE));
        placeNewPiece('e', 1, new King(board, Color.WHITE, this));
        placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
        placeNewPiece('g', 1, new Knight(board, Color.WHITE));
        placeNewPiece('h', 1, new Rook(board, Color.WHITE));
        placeNewPiece('a', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('b', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('c', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('d', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('e', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('f', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('g', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('h', 2, new Pawn(board, Color.WHITE, this));
        
        placeNewPiece('a', 8, new Rook(board, Color.BLACK));
        placeNewPiece('b', 8, new Knight(board, Color.BLACK));
        placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
        placeNewPiece('d', 8, new Queen(board, Color.BLACK));
        placeNewPiece('e', 8, new King(board, Color.BLACK, this));
        placeNewPiece('f', 8, new Bishop(board, Color.BLACK));
        placeNewPiece('g', 8, new Knight(board, Color.BLACK));
        placeNewPiece('h', 8, new Rook(board, Color.BLACK));
        placeNewPiece('a', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('b', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('c', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('d', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('e', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('f', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('g', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('h', 7, new Pawn(board, Color.BLACK, this));
        
        
	}
}
