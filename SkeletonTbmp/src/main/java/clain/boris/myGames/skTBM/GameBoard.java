package clain.boris.myGames.skTBM;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import android.graphics.Color;
import android.widget.ImageView;

import java.util.HashSet;

/**
 * Created by Boris on 2015-08-09.
 */

/**
 *
 * Pour la génération des coups de base, je me suis inspiré du tutoriel de l'utilisateur youtube LogicCrazy,
 * tutoriel disponible  à l'adresse: https://www.youtube.com/user/jonathanwarkentin
 * Ce tutoriel m'a surtout été utile pour la génération des coups de base et de la détection de l'échec.
 *
 *
 * J'ai fait les roques, les prises en passant , les promotions , ainsi que les fonctions makeAMove(), undoMove(), et lockMove()
 * sans aucune aide extérieure. Toutes les fonctions de dessin (incluant drawPossibleSquaresAsSelected() dans  MainActivity) aussi ont été
 * faites sans aucune aide extérieure
 */

/*
Représentation interne du board.
 */
public class GameBoard {

    public static final String TAG = "EBTurn";

    public String data = "";
    public int turnCounter;
    public boolean isWhiteTurn;

    public int winner = -1;
    public boolean gameOver = false;
    public boolean mustEnd = false;
    public boolean forfeit = false;

    public boolean whiteCanStillCastleKingSide = true;
    public boolean whiteCanStillCastleQueenSide = true;
    public boolean blackCanStillCastleKingSide = true;
    public boolean blackCanStillCastleQueenSide = true;

    public int columnEnPassantBlackPawn = -1;
    public int columnEnPassantWhitePawn = -1;

    public static ImageView gameImageViews[][];

    private char gameBoard[][];
    private int whiteKingRowPosition = 7;
    private int whiteKingColumnPosition = 4; //position du roi blanc
    private int blackKingRowPosition = 0;
    private int blackKingColumnPosition = 4; //position du roi noir

    public static final char BLACK_ROOK = 'r';
    public static final char BLACK_KNIGHT = 'k';
    public static final char BLACK_BISHOP = 'b';
    public static final char BLACK_QUEEN = 'q';
    public static final char BLACK_KING = 'a';
    public static final char BLACK_PAWN = 'p';
    public static final char WHITE_ROOK = 'R';
    public static final char WHITE_KNIGHT = 'K';
    public static final char WHITE_BISHOP = 'B';
    public static final char WHITE_QUEEN = 'Q';
    public static final char WHITE_KING = 'A';
    public static final char WHITE_PAWN = 'P';
    public static final char EMPTY_SQUARE = ' ';



    public static enum PlayerColor {WHITE, BLACK}

    public GameBoard(){
        initializeBoard();
    }


    /**
     * Initialise le board
     *
     * majuscule/minuscule : BLANC/noir
     *
     * R/r : Rook (Tour)
     * K/k : Knight (Cavalier)
     * B/b : Bishop (Fou)
     * Q/q : Queen (Reine)
     * A/a : King (Roi)
     * P/p : Pawn (Pion)
     *
     */
    public void initializeBoard(){
        gameBoard = new char[][]{
                {'r','k','b','q','a','b','k','r'},
                {'p','p','p','p','p','p','p','p'},
                {' ',' ',' ',' ',' ',' ',' ',' '},
                {' ',' ',' ',' ',' ',' ',' ',' '},
                {' ',' ',' ',' ',' ',' ',' ',' '},
                {' ',' ',' ',' ',' ',' ',' ',' '},
                {'P','P','P','P','P','P','P','P'},
                {'R','K','B','Q','A','B','K','R'}};


        /*
            { [0,0], [0,1] , ... [0,7] }
            { [1,0], [1,1] , ... [1,7] }
            ...
            { [7,0], [7,1] , ... [7,7] }
        */
    }


    public void encodeFromBoardToData(){
        data = "";
        for(int i = 0; i < 8; i++){
            for(int j=0; j<8; j++){
                data = data + gameBoard[i][j];
            }
        }
    }

    public void decodeFromDataToBoard(){
        if(data.length() == 64){
            for(int i = 0; i < 64; i++){
                gameBoard[i/8][i%8] = data.charAt(i);
            }
        }else
            Log.d(TAG, "Data missing: not 64 characters long");
        updateBlackKingPosition();
        updateWhiteKingPosition();
    }

    public byte[] persist() {
        JSONObject retVal = new JSONObject();

        try {
            this.encodeFromBoardToData();
            retVal.put("data", data);
            retVal.put("turnCounter", turnCounter);
            retVal.put("isWhiteTurn", isWhiteTurn);
            retVal.put("winner", winner);
            retVal.put("forfeit", forfeit);
            retVal.put("gameOver", gameOver);
            retVal.put("mustEnd", mustEnd);
            retVal.put("whiteCanStillCastleKingSide", whiteCanStillCastleKingSide);
            retVal.put("whiteCanStillCastleQueenSide", whiteCanStillCastleQueenSide);
            retVal.put("blackCanStillCastleKingSide", blackCanStillCastleKingSide);
            retVal.put("blackCanStillCastleQueenSide", blackCanStillCastleQueenSide);
            retVal.put("columnEnPassantWhitePawn", columnEnPassantWhitePawn);
            retVal.put("columnEnPassantBlackPawn", columnEnPassantBlackPawn);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String st = retVal.toString();

        Log.d(TAG, "==== PERSISTING\n" + st);

        return st.getBytes(Charset.forName("UTF-8"));
    }

    //Crée une nouvelle instance de GameBoard
    static public GameBoard unpersist(byte[] byteArray){

        if (byteArray == null){
            Log.d(TAG, "Empty array --- possible bug.");
            return new GameBoard();
        }

        String st = null;
        try{
            st = new String(byteArray, "UTF-8");
        } catch (UnsupportedEncodingException e1){
            e1.printStackTrace();
            return null;
        }
        Log.d(TAG, "===UNPERSIST \n" + st);

        GameBoard retVal = new GameBoard();

        try{
            JSONObject obj = new JSONObject(st);

            if (obj.has("data")){
                retVal.data = obj.getString("data");
            }
            if (obj.has("turnCounter")){
                retVal.turnCounter = obj.getInt("turnCounter");
            }
            if (obj.has("isWhiteTurn")){
                retVal.isWhiteTurn = obj.getBoolean("isWhiteTurn");
            }
            if (obj.has("gameOver")){
                retVal.gameOver = obj.getBoolean("gameOver");
            }
            if (obj.has("winner")){
                retVal.winner = obj.getInt("winner");
            }
            if (obj.has("forfeit")) {
                retVal.forfeit = obj.getBoolean("forfeit");
            }
            if (obj.has("mustEnd")){
                retVal.mustEnd = obj.getBoolean("mustEnd");
            }
            if (obj.has("whiteCanStillCastleKingSide")){
                retVal.whiteCanStillCastleKingSide = obj.getBoolean("whiteCanStillCastleKingSide");
            }
            if (obj.has("whiteCanStillCastleQueenSide")){
                retVal.whiteCanStillCastleQueenSide = obj.getBoolean("whiteCanStillCastleQueenSide");
            }
            if (obj.has("blackCanStillCastleKingSide")){
                retVal.blackCanStillCastleKingSide = obj.getBoolean("blackCanStillCastleKingSide");
            }
            if (obj.has("blackCanStillCastleQueenSide")){
                retVal.blackCanStillCastleQueenSide = obj.getBoolean("blackCanStillCastleQueenSide");
            }
            if(obj.has("columnEnPassantWhitePawn")){
                retVal.columnEnPassantWhitePawn = obj.getInt("columnEnPassantWhitePawn");
            }
            if(obj.has("columnEnPassantBlackPawn")){
                retVal.columnEnPassantBlackPawn = obj.getInt("columnEnPassantBlackPawn");
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
        return retVal;
    }


    public void displayForBlack(){
        for(int i = 0; i < 8 ; i++){
            for(int j = 0 ; j < 8 ; j++){
                if( true){
                    if (gameBoard[i][j]==BLACK_ROOK) {
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.black_rook);
                    }
                    else if (gameBoard[i][j]==BLACK_KNIGHT){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.black_knight);
                    }
                    else if (gameBoard[i][j]==BLACK_BISHOP){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.black_bishop);
                    }
                    else if (gameBoard[i][j]==BLACK_QUEEN){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.black_queen);
                    }
                    else if (gameBoard[i][j]==BLACK_KING){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.black_king);
                    }
                    else if (gameBoard[i][j]==BLACK_PAWN){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.black_pawn);
                    }

                    else if (gameBoard[i][j]==WHITE_ROOK) {
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.white_rook);
                    }
                    else if (gameBoard[i][j]==WHITE_KNIGHT){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.white_knight);
                    }
                    else if (gameBoard[i][j]==WHITE_BISHOP){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.white_bishop);
                    }
                    else if (gameBoard[i][j]==WHITE_QUEEN){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.white_queen);
                    }
                    else if (gameBoard[i][j]==WHITE_KING){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.white_king);
                    }
                    else if (gameBoard[i][j]==WHITE_PAWN){
                        gameImageViews[7-i][7-j].setImageResource(R.drawable.white_pawn);
                    }

                    else{
                        gameImageViews[7-i][7-j].setImageResource(0);
                    }


                }

            }
        }
    }

    private void drawPieces(int i, int j){
        if (gameBoard[i][j]==BLACK_ROOK) {
            gameImageViews[i][j].setImageResource(R.drawable.black_rook);
        }
        else if (gameBoard[i][j]==BLACK_KNIGHT){
            gameImageViews[i][j].setImageResource(R.drawable.black_knight);
        }
        else if (gameBoard[i][j]==BLACK_BISHOP){
            gameImageViews[i][j].setImageResource(R.drawable.black_bishop);
        }
        else if (gameBoard[i][j]==BLACK_QUEEN){
            gameImageViews[i][j].setImageResource(R.drawable.black_queen);
        }
        else if (gameBoard[i][j]==BLACK_KING){
            gameImageViews[i][j].setImageResource(R.drawable.black_king);
        }
        else if (gameBoard[i][j]==BLACK_PAWN){
            gameImageViews[i][j].setImageResource(R.drawable.black_pawn);
        }


        else if (gameBoard[i][j]==WHITE_ROOK) {
            gameImageViews[i][j].setImageResource(R.drawable.white_rook);
        }
        else if (gameBoard[i][j]==WHITE_KNIGHT){
            gameImageViews[i][j].setImageResource(R.drawable.white_knight);
        }
        else if (gameBoard[i][j]==WHITE_BISHOP){
            gameImageViews[i][j].setImageResource(R.drawable.white_bishop);
        }
        else if (gameBoard[i][j]==WHITE_QUEEN){
            gameImageViews[i][j].setImageResource(R.drawable.white_queen);
        }
        else if (gameBoard[i][j]==WHITE_KING){
            gameImageViews[i][j].setImageResource(R.drawable.white_king);
        }
        else if (gameBoard[i][j]==WHITE_PAWN){
            gameImageViews[i][j].setImageResource(R.drawable.white_pawn);
        }

        else{
            gameImageViews[i][j].setImageResource(0);
        }
    }

    /**
     * Affiche l'échiquier
     */
    public void displayForWhite(){
        for(int i = 0; i<8; i++){
            for(int j = 0; j<8; j++) {
                drawPieces(i,j);
            }
        }
    }

    public void resetColorOfSquares() {
        for(int i = 0; i<8; i++){
            for(int j = 0; j<8; j++) {
                if((i+j)%2 == 0){
                    gameImageViews[i][j].setBackgroundColor(Color.parseColor("#ffffff"));
                }
                else{
                    gameImageViews[i][j].setBackgroundColor(Color.parseColor("#999966"));
                }
            }
        }

    }


    public boolean isABlackPiece(int r, int c){
        return Character.isLowerCase(gameBoard[r][c]);
    }

    public boolean isAWhitePiece(int r, int c){
        return Character.isUpperCase(gameBoard[r][c]);
    }

    public boolean isABlank(int r, int c) {
        return gameBoard[r][c] == EMPTY_SQUARE;
    }

    public char getPiece(int r, int c) {
        return gameBoard[r][c];
    }

    public void lockMove(String move){
        if(Character.isDigit(move.charAt(0)))
        {
            int x1 = Character.getNumericValue(move.charAt(0));
            int y1 = Character.getNumericValue(move.charAt(1));
            int x2 = Character.getNumericValue(move.charAt(2));
            int y2 = Character.getNumericValue(move.charAt(3));
            if (getPiece(x2, y2) == WHITE_KING && x1 == 7 && y1 == 4) {
                whiteCanStillCastleKingSide = false;
                whiteCanStillCastleQueenSide = false;
            } else if (getPiece(x2, y2) == BLACK_KING && x1 == 0 && y1 == 4) {
                blackCanStillCastleKingSide = false;
                blackCanStillCastleQueenSide = false;
            } else if (getPiece(x2, y2) == WHITE_ROOK && x1 == 7 && y1 == 7) {
                whiteCanStillCastleKingSide = false;
            } else if (getPiece(x2, y2) == WHITE_ROOK && x1 == 7 && y1 == 0) {
                whiteCanStillCastleQueenSide = false;
            } else if (getPiece(x2, y2) == BLACK_ROOK && x1 == 0 && y1 == 7) {
                blackCanStillCastleKingSide = false;
            } else if (getPiece(x2, y2) == BLACK_ROOK && x1 == 0 && y1 == 0) {
                blackCanStillCastleQueenSide = false;
            }
            //un pas de deux par un pion blanc entraîne une possible prise en passant
            else if (getPiece(x2,y2) == WHITE_PAWN && x1 == 6 && x2 == 4 ){
                columnEnPassantWhitePawn = y1;
                columnEnPassantBlackPawn = -1;
                updateWhiteKingPosition();
                updateBlackKingPosition();
                return;
            }
            //un pas de deux par un pion noir entraîne une possible prise en passant
            else if (getPiece(x2,y2) == BLACK_PAWN && x1 == 1 && x2 == 3){
                columnEnPassantBlackPawn = y1;
                columnEnPassantWhitePawn = -1;
                updateWhiteKingPosition();
                updateBlackKingPosition();
                return;
            }
        }
        else if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'K' && move.charAt(3) == 'S')
        {
            whiteCanStillCastleKingSide = false;
            whiteCanStillCastleQueenSide = false;
        }
        else if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'Q' && move.charAt(3) == 'S')
        {
            whiteCanStillCastleKingSide = false;
            whiteCanStillCastleQueenSide = false;
        }
        else if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'K' && move.charAt(3) == 'S')
        {
            blackCanStillCastleKingSide = false;
            blackCanStillCastleQueenSide = false;
        }
        else if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'Q' && move.charAt(3) == 'S')
        {
            blackCanStillCastleKingSide = false;
            blackCanStillCastleQueenSide = false;
        }
        columnEnPassantBlackPawn = -1;
        columnEnPassantWhitePawn = -1;
        updateWhiteKingPosition();
        updateBlackKingPosition();
    }

    public void makeAMove(String move) {
        if(Character.isDigit(move.charAt(0)) && Character.isDigit(move.charAt(1)) && Character.isDigit(move.charAt(2)) && Character.isDigit(move.charAt(3)))
        {
            int x1 = Character.getNumericValue(move.charAt(0));
            int y1 = Character.getNumericValue(move.charAt(1));
            int x2 = Character.getNumericValue(move.charAt(2));
            int y2 = Character.getNumericValue(move.charAt(3));
            gameBoard[x2][y2] = gameBoard[x1][y1];
            gameBoard[x1][y1] = EMPTY_SQUARE;
        }
        else if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'K' && move.charAt(3) == 'S')
        {
            gameBoard[7][4] = EMPTY_SQUARE;
            gameBoard[7][5] = WHITE_ROOK;
            gameBoard[7][6] = WHITE_KING;
            gameBoard[7][7] = EMPTY_SQUARE;
        }
        else if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'Q' && move.charAt(3) == 'S')
        {
            gameBoard[7][4] = EMPTY_SQUARE;
            gameBoard[7][3] = WHITE_ROOK;
            gameBoard[7][2] = WHITE_KING;
            gameBoard[7][1] = EMPTY_SQUARE;
            gameBoard[7][0] = EMPTY_SQUARE;
        }
        else if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'K' && move.charAt(3) == 'S')
        {
            gameBoard[0][4] = EMPTY_SQUARE;
            gameBoard[0][5] = BLACK_ROOK;
            gameBoard[0][6] = BLACK_KING;
            gameBoard[0][7] = EMPTY_SQUARE;
        }
        else if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'Q' && move.charAt(3) == 'S')
        {
            gameBoard[0][4] = EMPTY_SQUARE;
            gameBoard[0][3] = BLACK_ROOK;
            gameBoard[0][2] = BLACK_KING;
            gameBoard[0][1] = EMPTY_SQUARE;
            gameBoard[0][0] = EMPTY_SQUARE;
        }
        else if(move.charAt(0) == 'E' && move.charAt(1) == 'P'){
            int x1 = Character.getNumericValue(move.charAt(2));
            int y1 = Character.getNumericValue(move.charAt(3));
            int x2 = Character.getNumericValue(move.charAt(4));
            int y2 = Character.getNumericValue(move.charAt(5));
            //prise en passant par un pion blanc
            if(x1 == 3){
                gameBoard[x1][y1] = EMPTY_SQUARE;
                gameBoard[x1][y2] = EMPTY_SQUARE;
                gameBoard[x2][y2] = WHITE_PAWN;
            }
            //prise en passant par un pion noir
            else if(x1 == 4){
                gameBoard[x1][y1] = EMPTY_SQUARE;
                gameBoard[x1][y2] = EMPTY_SQUARE;
                gameBoard[x2][y2] = BLACK_PAWN;
            }
        }
        //promotion des blancs
        else if(move.charAt(0) == 'X')
        {
            gameBoard[0][Character.getNumericValue(move.charAt(4))] = move.charAt(6);
            gameBoard[1][Character.getNumericValue(move.charAt(2))] = EMPTY_SQUARE;
        }
        //promotion des noirs
        else if(move.charAt(0) == 'x')
        {
            gameBoard[7][Character.getNumericValue(move.charAt(4))] = move.charAt(6);
            gameBoard[6][Character.getNumericValue(move.charAt(2))] = EMPTY_SQUARE;
        }
        updateWhiteKingPosition();
        updateBlackKingPosition();
    }

    public void undoAMove(String move){
        if(Character.isDigit(move.charAt(0)) && Character.isDigit(move.charAt(1)) && Character.isDigit(move.charAt(2)) && Character.isDigit(move.charAt(3)))
        {
            int x1 = Character.getNumericValue(move.charAt(0));
            int y1 = Character.getNumericValue(move.charAt(1));
            int x2 = Character.getNumericValue(move.charAt(2));
            int y2 = Character.getNumericValue(move.charAt(3));
            char c = move.charAt(4);
            gameBoard[x1][y1] = getPiece(x2, y2);
            gameBoard[x2][y2] = c;
        }
        else if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'K' && move.charAt(3) == 'S')
        {
            gameBoard[7][4] = WHITE_KING;
            gameBoard[7][5] = EMPTY_SQUARE;
            gameBoard[7][6] = EMPTY_SQUARE;
            gameBoard[7][7] = WHITE_ROOK;
        }
        else if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'Q' && move.charAt(3) == 'S')
        {
            gameBoard[7][4] = WHITE_KING;
            gameBoard[7][3] = EMPTY_SQUARE;
            gameBoard[7][2] = EMPTY_SQUARE;
            gameBoard[7][1] = EMPTY_SQUARE;
            gameBoard[7][0] = WHITE_ROOK;
        }
        else if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'K' && move.charAt(3) == 'S')
        {
            gameBoard[0][4] = BLACK_KING;
            gameBoard[0][5] = EMPTY_SQUARE;
            gameBoard[0][6] = EMPTY_SQUARE;
            gameBoard[0][7] = BLACK_ROOK;
        }
        else if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'Q' && move.charAt(3) == 'S')
        {
            gameBoard[0][4] = BLACK_KING;
            gameBoard[0][3] = EMPTY_SQUARE;
            gameBoard[0][2] = EMPTY_SQUARE;
            gameBoard[0][1] = EMPTY_SQUARE;
            gameBoard[0][0] = BLACK_ROOK;
        }
        else if(move.charAt(0) == 'E' && move.charAt(1) == 'P')
        {
            int x1 = Character.getNumericValue(move.charAt(2));
            int y1 = Character.getNumericValue(move.charAt(3));
            int x2 = Character.getNumericValue(move.charAt(4));
            int y2 = Character.getNumericValue(move.charAt(5));
            //défaire la prise en passant par un pion blanc
            if(x1 == 3){
                gameBoard[x1][y1] = WHITE_PAWN;
                gameBoard[x1][y2] = BLACK_PAWN;
                gameBoard[x2][y2] = EMPTY_SQUARE;
            }
            //défaire la prise en passant par un pion noir
            else if(x1 == 4){
                gameBoard[x1][y1] = BLACK_PAWN;
                gameBoard[x1][y2] = WHITE_PAWN;
                gameBoard[x2][y2] = EMPTY_SQUARE;
            }
        }
        //promotion des blancs
        else if(move.charAt(0) == 'X')
        {
            char oldPiece = move.charAt(5);
            gameBoard[0][Character.getNumericValue(move.charAt(4))] = oldPiece;
            gameBoard[1][Character.getNumericValue(move.charAt(2))] = WHITE_PAWN;
        }
        //promotion des noirs
        else if(move.charAt(0) == 'x')
        {
            char oldPiece = move.charAt(5);
            gameBoard[7][move.charAt(4)] = oldPiece;
            gameBoard[6][move.charAt(2)] = BLACK_PAWN;
        }


        updateWhiteKingPosition();
        updateBlackKingPosition();
    }


    private void updateWhiteKingPosition(){
        int index = 0;
        while(gameBoard[index/8][index%8] != WHITE_KING){
            index++;
        }
        whiteKingRowPosition = index/8;
        whiteKingColumnPosition = index%8;
    }

    private void updateBlackKingPosition(){
        int index = 0;
        while(gameBoard[index/8][index%8] != BLACK_KING){
            index++;
        }
        blackKingRowPosition = index/8;
        blackKingColumnPosition = index%8;
    }





    /** Renvoie la liste de tous les coups possibles sur l'échiquier pour le joueur de couleur color
     *
     * Format d'un move standard: "x1y1x2y2c", où x1y1 sont les coordonnées de la case de départ
     * et x2y2 sont les coordonnées de la case d'arrivée, et où c est la pièce capturée (et un
     * caractère " " représente aucune capture)
     * @return la liste des moves possibles sur l'échiquier
     */
    public HashSet<String> giveAllPossibleMoves(PlayerColor color) {
        HashSet<String> setOfMoves = new HashSet<String>();
        if (color == PlayerColor.WHITE) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    switch (gameBoard[i][j]) {
                        case WHITE_PAWN:
                            setOfMoves.addAll(possibleWhitePawn(i, j));
                            break;
                        case WHITE_ROOK:
                            setOfMoves.addAll(possibleWhiteRook(i, j));
                            break;
                        case WHITE_KNIGHT:
                            setOfMoves.addAll(possibleWhiteKnight(i, j));
                            break;
                        case WHITE_BISHOP:
                            setOfMoves.addAll(possibleWhiteBishop(i, j));
                            break;
                        case WHITE_QUEEN:
                            setOfMoves.addAll(possibleWhiteQueen(i, j));
                            break;
                        case WHITE_KING:
                            setOfMoves.addAll(possibleWhiteKing(i, j));
                            break;
                    }//fin du switch
                }
            }
            setOfMoves.addAll(whitePossibleCastlingMoves());
        } else if (color == PlayerColor.BLACK) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    switch (gameBoard[i][j]) {
                        case BLACK_PAWN:
                            setOfMoves.addAll(possibleBlackPawn(i, j));
                            break;
                        case BLACK_ROOK:
                            setOfMoves.addAll(possibleBlackRook(i, j));
                            break;
                        case BLACK_KNIGHT:
                            setOfMoves.addAll(possibleBlackKnight(i, j));
                            break;
                        case BLACK_BISHOP:
                            setOfMoves.addAll(possibleBlackBishop(i, j));
                            break;
                        case BLACK_QUEEN:
                            setOfMoves.addAll(possibleBlackQueen(i, j));
                            break;
                        case BLACK_KING:
                            setOfMoves.addAll(possibleBlackKing(i, j));
                            break;
                    }//fin du switch
                }
            }
            setOfMoves.addAll(blackPossibleCastlingMoves());
        }
        return setOfMoves;
    }

    private HashSet<String> whitePossibleCastlingMoves() {
        HashSet<String> set = new HashSet<String>();
        boolean canGoToFirstCase, canGoToSecondCase;
        if (whiteCanStillCastleKingSide && !whiteKingIsInCheck() &&
            gameBoard[7][4] == WHITE_KING &&
            gameBoard[7][5] == EMPTY_SQUARE &&
            gameBoard[7][6] == EMPTY_SQUARE &&
            gameBoard[7][7] == WHITE_ROOK)
            {
                whiteKingColumnPosition = 5;
                canGoToFirstCase = !whiteKingIsInCheck();
                whiteKingColumnPosition = 6;
                canGoToSecondCase = !whiteKingIsInCheck();
                if(canGoToFirstCase && canGoToSecondCase)
                    set.add("" + 'W' + 'C' + 'K' + 'S');
                whiteKingColumnPosition = 4;
            }
        if (whiteCanStillCastleQueenSide && !whiteKingIsInCheck() &&
                gameBoard[7][4] == WHITE_KING &&
                gameBoard[7][3] == EMPTY_SQUARE &&
                gameBoard[7][2] == EMPTY_SQUARE &&
                gameBoard[7][1] == EMPTY_SQUARE &&
                gameBoard[7][0] == WHITE_ROOK)
            {
                whiteKingColumnPosition = 3;
                canGoToFirstCase = !blackKingIsInCheck();
                whiteKingColumnPosition = 2;
                canGoToSecondCase = !blackKingIsInCheck();
                if (canGoToFirstCase && canGoToSecondCase)
                    set.add("" + 'W' + 'C' + 'Q' + 'S');
                whiteKingColumnPosition = 4;
            }
        return set;
    }

    private HashSet<String> blackPossibleCastlingMoves() {
        HashSet<String> set = new HashSet<String>();
        boolean canGoToFirstCase, canGoToSecondCase;
        if (blackCanStillCastleKingSide && !blackKingIsInCheck() &&
                gameBoard[0][4] == BLACK_KING &&
                gameBoard[0][5] == EMPTY_SQUARE &&
                gameBoard[0][6] == EMPTY_SQUARE &&
                gameBoard[0][7] == BLACK_ROOK)
        {
            blackKingColumnPosition = 5;
            canGoToFirstCase = !blackKingIsInCheck();
            blackKingColumnPosition = 6;
            canGoToSecondCase = !blackKingIsInCheck();
            if (canGoToFirstCase && canGoToSecondCase)
                set.add("" + 'B' + 'C' + 'K' + 'S');
            blackKingColumnPosition = 4;
        }
        if (blackCanStillCastleQueenSide && !blackKingIsInCheck() &&
                gameBoard[0][4] == BLACK_KING &&
                gameBoard[0][3] == EMPTY_SQUARE &&
                gameBoard[0][2] == EMPTY_SQUARE &&
                gameBoard[0][1] == EMPTY_SQUARE &&
                gameBoard[0][0] == BLACK_ROOK) {
            blackKingColumnPosition = 3;
            canGoToFirstCase = !blackKingIsInCheck();
            blackKingColumnPosition = 2;
            canGoToSecondCase = !blackKingIsInCheck();
            if (canGoToFirstCase && canGoToSecondCase)
                set.add("" + 'B' + 'C' + 'Q' + 'S');
            blackKingColumnPosition = 4;
        }
        return set;
    }



    private HashSet<String> possibleBlackPawn(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;

        //Capture d'une pièce en diagonale
        for (int k=-1; k<=1; k+=2){
            //capture d'une pièce en diagonale sans promotion
            try{
                if(isAWhitePiece(i+1,j+k) && i <= 5){
                    oldPiece = gameBoard[i+1][j+k];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i+1][j+k] = BLACK_PAWN;
                    if (!blackKingIsInCheck()){
                        set.add("" + i + j + (i + 1) + (j + k) + oldPiece);
                    }
                    gameBoard[i][j] = BLACK_PAWN;
                    gameBoard[i+1][j+k] = oldPiece;
                }
                if(i == 4 && gameBoard[4][j+k] == WHITE_PAWN &&
                        columnEnPassantWhitePawn == (j+k) && columnEnPassantWhitePawn != -1)
                {
                    oldPiece = gameBoard[i][j+k];
                    gameBoard[i][j+k] = EMPTY_SQUARE;
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i+1][j+k] = BLACK_PAWN;
                    if(!blackKingIsInCheck()){
                        set.add("" + 'E' + 'P' + '4' + j + '5' + (j+k) + oldPiece);
                    }
                    gameBoard[i][j] = BLACK_PAWN ;
                    gameBoard[i][j+k] = oldPiece ;
                    gameBoard[i+1][j+k] = EMPTY_SQUARE;
                }
            } catch (Exception e){
            }
            //promotion par la capture d'une pièce en diagonale
            try{
                if(isAWhitePiece(i+1,j+k) && i > 5){
                    char[] promotions = {'k','q','b','r'};
                    for (char promotion : promotions){
                        oldPiece = gameBoard[i+1][j+k];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i+1][j+k] = promotion;
                        if(!blackKingIsInCheck()){
                            //x,  rangée, colonne, rangée, colonne, oldPiece, promotion
                            set.add("" + 'x' + i + j + (i+1) + (j+k) + oldPiece+ promotion);
                        }
                        gameBoard[i][j] = BLACK_PAWN;
                        gameBoard[i+1][j+k] = oldPiece;
                    }
                }
            } catch(Exception e){
            }
        }//fin de la boucle pour générer les moves de capture

        //Simple mouvement d'une seule case
        try{
            if(isABlank(i+1,j) && i <= 5){
                oldPiece = gameBoard[i+1][j];
                gameBoard[i][j] = EMPTY_SQUARE;
                gameBoard[i+1][j] = BLACK_PAWN;
                if(!blackKingIsInCheck()){
                    set.add("" + i + j + (i+1) + j + oldPiece);
                }
                gameBoard[i][j] = BLACK_PAWN;
                gameBoard[i+1][j] = oldPiece;
            }
        } catch (Exception e){
        }
        //Promotion par le mouvement sans capture
        try{
            if(isABlank(i+1,j) && i > 5){
                char [] promotions = {'k','q','b','r'};
                for(char promotion : promotions){
                    oldPiece = gameBoard[i+1][j];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i+1][j] = promotion;
                    if(!blackKingIsInCheck()){
                        //X,  rangée, colonne, rangée, colonne, oldPiece, promotion
                        set.add("" + 'x' + i + j + (i+1) + j + oldPiece+ promotion);
                    }
                    gameBoard[i][j] = BLACK_PAWN;
                    gameBoard[i+1][j] = oldPiece;
                }
            }
        } catch(Exception e){
        }

        //Mouvement de deux cases
        try{
            if(isABlank(i+1,j) && isABlank(i+2,j) && i == 1){
                oldPiece = gameBoard[i+2][j];
                gameBoard[i][j] = EMPTY_SQUARE;
                gameBoard[i+2][j] = BLACK_PAWN;
                if(!blackKingIsInCheck()){
                    set.add("" + i + j + (i+2) + j + oldPiece);
                }
                gameBoard[i][j] = BLACK_PAWN;
                gameBoard[i+2][j] = oldPiece;
            }
        } catch (Exception e){
        }
        return set;
    }
    private HashSet<String> possibleBlackRook(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        int s;
        for(int k = -1; k <= 1; k+=2){
            s = 1;
            //Génération des coups horizontaux
            try{
                while(isABlank(i, j + s*k)){
                    oldPiece = gameBoard[i][j + s*k];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i][j+ s*k] = BLACK_ROOK;
                    if (!blackKingIsInCheck()){
                        set.add("" + i + j + i + (j + s*k) + oldPiece);
                    }
                    gameBoard[i][j] = BLACK_ROOK;
                    gameBoard[i][j + s * k] = oldPiece;
                    s++;
                }
                if(isAWhitePiece(i, j + s * k)){
                    oldPiece = gameBoard[i][j + s*k];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i][j+ s*k] = BLACK_ROOK;
                    if (!blackKingIsInCheck()){
                        set.add("" + i + j + i + (j + s*k) + oldPiece);
                    }
                    gameBoard[i][j] = BLACK_ROOK;
                    gameBoard[i][j + s * k] = oldPiece;
                }
            }catch(Exception e){
            }
            s = 1;
            //Génération des coups verticaux
            try{
                while(isABlank(i + s*k, j)){
                    oldPiece = gameBoard[i + s*k][j];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i + s*k][j] = BLACK_ROOK;
                    if(!blackKingIsInCheck()){
                        set.add("" + i + j + (i + s*k) + j + oldPiece);
                    }
                    gameBoard[i][j] = BLACK_ROOK;
                    gameBoard[i + s*k][j] = oldPiece;
                    s++;
                }
                if (isAWhitePiece(i + s * k, j)){
                    oldPiece = gameBoard[i + s*k][j];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i + s*k][j] = BLACK_ROOK;
                    if(!blackKingIsInCheck()){
                        set.add("" + i + j + (i + s*k) + j + oldPiece);
                    }
                    gameBoard[i][j] = BLACK_ROOK;
                    gameBoard[i + s*k][j] = oldPiece;
                }
            }catch(Exception e){
            }
        }
        return set;
    }
    private HashSet<String> possibleBlackKnight(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        for (int k=-1; k<=1; k+=2) {
            for (int l=-1; l<=1; l+=2) {
                try {
                    if (isAWhitePiece(i + k, j + l * 2) || isABlank(i+k,j+l*2)) {
                        oldPiece=gameBoard[i+k][j+l*2];
                        gameBoard[i][j]=EMPTY_SQUARE;
                        gameBoard[i+k][j+l*2]=BLACK_KNIGHT;
                        if (!blackKingIsInCheck()) {
                            set.add("" + i + j + (i+k) + (j+l*2) + oldPiece);
                        }
                        gameBoard[i][j]=BLACK_KNIGHT;
                        gameBoard[i+k][j+l*2]=oldPiece;
                    }
                } catch (Exception e) {}
                try {
                    if (isAWhitePiece(i + k * 2, j + l) || isABlank(i + k * 2, j + l)) {
                        oldPiece=gameBoard[i+k*2][j + l];
                        gameBoard[i][j]=EMPTY_SQUARE;
                        gameBoard[i+k*2][j + l]=BLACK_KNIGHT;
                        if (!blackKingIsInCheck()) {
                            set.add("" + i + j + (i+k*2) + (j+l) + oldPiece);
                        }
                        gameBoard[i][j]=BLACK_KNIGHT;
                        gameBoard[i+k*2][j + l]=oldPiece;
                    }
                } catch (Exception e) {}
            }

        }
        return set;
    }

    private HashSet<String> possibleBlackBishop(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        int s;
        for(int k = -1; k<= 1; k+=2){
            for(int l = -1; l<= 1; l+=2){
                s = 1;
                try{
                    while(isABlank(i + s*k, j + s*l)){
                        oldPiece = gameBoard[i + s*k][j + s*l];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i + s*k][j + s*l] = BLACK_BISHOP;
                        if(!blackKingIsInCheck()){
                            set.add("" + i + j + (i + s*k) + (j + s*l) + oldPiece);
                        }
                        gameBoard[i][j] = BLACK_BISHOP;
                        gameBoard[i + s*k][j + s*l] = oldPiece;
                        s++;
                    }
                    if(isAWhitePiece(i + s*k, j + s*l)){
                        oldPiece = gameBoard[i + s*k][j + s*l];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i + s*k][j + s*l] = BLACK_BISHOP;
                        if(!blackKingIsInCheck()){
                            set.add("" + i + j + (i + s*k) + (j + s*l) + oldPiece);
                        }
                        gameBoard[i][j] = BLACK_BISHOP;
                        gameBoard[i + s*k][j + s*l] = oldPiece;
                    }

                } catch (Exception e){
                }
            }
        }
        return set;
    }
    private HashSet<String> possibleBlackQueen(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        int s;
        for(int k = -1; k <= 1; k++){
            for(int l = -1 ; l <= 1 ; l++){
                if(k != 0 || l != 0) {
                    s = 1;
                    try {
                        while (isABlank(i + s * k, j + s * l)) {
                            oldPiece = gameBoard[i + s * k][j + s * l];
                            gameBoard[i][j] = EMPTY_SQUARE;
                            gameBoard[i + s * k][j + s * l] = BLACK_QUEEN;
                            if (!blackKingIsInCheck()) {
                                set.add("" + i + j + (i + s * k) + (j + s * l) + oldPiece);
                            }
                            gameBoard[i][j] = BLACK_QUEEN;
                            gameBoard[i + s * k][j + s * l] = oldPiece;
                            s++;
                        }
                        if (isAWhitePiece(i + s * k, j + s * l)) {
                            oldPiece = gameBoard[i + s * k][j + s * l];
                            gameBoard[i][j] = EMPTY_SQUARE;
                            gameBoard[i + s * k][j + s * l] = BLACK_QUEEN;
                            if (!blackKingIsInCheck()) {
                                set.add("" + i + j + (i + s * k) + (j + s * l) + oldPiece);
                            }
                            gameBoard[i][j] = BLACK_QUEEN;
                            gameBoard[i + s * k][j + s * l] = oldPiece;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return set;
    }
    private HashSet<String> possibleBlackKing(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        for (int k = 0; k <= 8; k++){
            if (k != 4){
                try {
                    if (isAWhitePiece(i - 1 + k / 3, j - 1 + k % 3) || isABlank(i - 1 + k / 3,j - 1 + k % 3)) {
                        oldPiece = gameBoard[i - 1 + k / 3][j - 1 + k % 3];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i - 1 + k / 3][j - 1 + k % 3] = BLACK_KING;
                        int kingOriginalRowPosition = blackKingRowPosition;
                        int kingOriginalColumnPosition = blackKingColumnPosition;
                        blackKingRowPosition = i - 1 + k / 3;
                        blackKingColumnPosition = j - 1 + k % 3;
                        if (!blackKingIsInCheck()) {
                            set.add("" + i + "" + j + "" + (i - 1 + k / 3) + "" + (j - 1 + k % 3) + "" + oldPiece);
                        }
                        gameBoard[i][j] = BLACK_KING;
                        gameBoard[i - 1 + k / 3][j - 1 + k % 3] = oldPiece;
                        blackKingRowPosition = kingOriginalRowPosition;
                        blackKingColumnPosition = kingOriginalColumnPosition;
                    }
                } catch (Exception e){
                }
            }
        }
        return set;
    }

    //TODO: Revenir dans ces methodes et utiliser nos methodes utilitaires (isWhite())
    private HashSet<String> possibleWhitePawn(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;

        //Capture d'une pièce en diagonale
        for (int k=-1; k<=1; k+=2){
            //capture d'une pièce en diagonale sans promotion
            try{
                if(isABlackPiece(i-1,j+k) && i >= 2 ){
                    oldPiece = gameBoard[i-1][j+k];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i-1][j+k] = WHITE_PAWN;
                    if (!whiteKingIsInCheck()){
                        set.add("" + i + j + (i - 1) + (j + k) + oldPiece);
                    }
                    gameBoard[i][j] = WHITE_PAWN;
                    gameBoard[i-1][j+k] = oldPiece;
                }
                if(i == 3 && gameBoard[3][j+k] == BLACK_PAWN &&
                        columnEnPassantBlackPawn == (j+k) && columnEnPassantBlackPawn != -1)
                {
                    oldPiece = gameBoard[i][j+k];
                    gameBoard[i][j+k] = EMPTY_SQUARE;
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i-1][j+k] = WHITE_PAWN;
                    if(!whiteKingIsInCheck()){
                        set.add("" + 'E' + 'P' + '3' + j + '2' + (j+k) + oldPiece);
                    }
                    gameBoard[i][j] = WHITE_PAWN ;
                    gameBoard[i][j+k] = oldPiece ;
                    gameBoard[i-1][j+k] = EMPTY_SQUARE;
                }
            } catch (Exception e){
            }
            //promotion par la capture d'une pièce en diagonale
            try{
                if(isABlackPiece(i-1,j+k) && i < 2){
                    char[] promotions = {'K','Q','B','R'};
                    for (char promotion : promotions){
                        oldPiece = gameBoard[i-1][j+k];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i-1][j+k] = promotion;
                        if(!whiteKingIsInCheck()){
                            //X,  rangée, colonne, rangée, colonne, oldPiece, promotion
                            set.add("" + 'X' + i + j + (i-1) + (j+k) + oldPiece+ promotion);
                        }
                        gameBoard[i][j] = WHITE_PAWN;
                        gameBoard[i-1][j+k] = oldPiece;
                    }
                }
            } catch(Exception e){
            }
        }//fin de la boucle pour générer les moves de capture

        //Simple mouvement d'une seule case
        try{
            if(isABlank(i-1,j) && i >= 2){
                oldPiece = gameBoard[i-1][j];
                gameBoard[i][j] = EMPTY_SQUARE;
                gameBoard[i-1][j] = WHITE_PAWN;
                if(!whiteKingIsInCheck()){
                    set.add("" + i + j + (i-1) + j + oldPiece);
                }
                gameBoard[i][j] = WHITE_PAWN;
                gameBoard[i-1][j] = oldPiece;
            }
        } catch (Exception e){
        }
        //Promotion par le mouvement sans capture
        try{
            if(isABlank(i-1,j) && i < 2){
                char [] promotions = {'K','Q','B','R'};
                for(char promotion : promotions){
                    oldPiece = gameBoard[i-1][j];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i-1][j] = promotion;
                    if(!whiteKingIsInCheck()){
                        set.add("" + 'X' + i + j + (i-1) + j + oldPiece+ promotion);
                    }
                    gameBoard[i][j] = WHITE_PAWN;
                    gameBoard[i-1][j] = oldPiece;
                }
            }
        } catch(Exception e){
        }

        //Mouvement de deux cases
        try{
            if(isABlank(i-1,j) && isABlank(i-2,j) && i == 6){
                oldPiece = gameBoard[i-2][j];
                gameBoard[i][j] = EMPTY_SQUARE;
                gameBoard[i-2][j] = WHITE_PAWN;
                if(!whiteKingIsInCheck()){
                    set.add("" + i + j + (i-2) + j + oldPiece);
                }
                gameBoard[i][j] = WHITE_PAWN;
                gameBoard[i-2][j] = oldPiece;
            }
        } catch (Exception e){
        }
        return set;
    }


    private HashSet<String> possibleWhiteRook(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        int s;
        for(int k = -1; k <= 1; k+=2){
            s = 1;
            //Génération des coups horizontaux
            try{
                while(isABlank(i, j + s*k)){
                    oldPiece = gameBoard[i][j + s*k];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i][j+ s*k] = WHITE_ROOK;
                    if (!whiteKingIsInCheck()){
                        set.add("" + i + j + i + (j + s*k) + oldPiece);
                    }
                    gameBoard[i][j] = WHITE_ROOK;
                    gameBoard[i][j + s * k] = oldPiece;
                    s++;
                }
                if(isABlackPiece(i, j + s*k)){
                    oldPiece = gameBoard[i][j + s*k];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i][j+ s*k] = WHITE_ROOK;
                    if (!whiteKingIsInCheck()){
                        set.add("" + i + j + i + (j + s*k) + oldPiece);
                    }
                    gameBoard[i][j] = WHITE_ROOK;
                    gameBoard[i][j + s * k] = oldPiece;
                }
            }catch(Exception e){
            }
            s = 1;
            //Génération des coups verticaux
            try{
                while(isABlank(i + s*k, j)){
                    oldPiece = gameBoard[i + s*k][j];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i + s*k][j] = WHITE_ROOK;
                    if(!whiteKingIsInCheck()){
                        set.add("" + i + j + (i + s*k) + j + oldPiece);
                    }
                    gameBoard[i][j] = WHITE_ROOK;
                    gameBoard[i + s*k][j] = oldPiece;
                    s++;
                }
                if (isABlackPiece(i + s*k, j)){
                    oldPiece = gameBoard[i + s*k][j];
                    gameBoard[i][j] = EMPTY_SQUARE;
                    gameBoard[i + s*k][j] = WHITE_ROOK;
                    if(!whiteKingIsInCheck()){
                        set.add("" + i + j + (i + s*k) + j + oldPiece);
                    }
                    gameBoard[i][j] = WHITE_ROOK;
                    gameBoard[i + s*k][j] = oldPiece;
                }
            }catch(Exception e){
            }
        }
        return set;
    }

    private HashSet<String> possibleWhiteKnight(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        for (int k=-1; k<=1; k+=2) {
            for (int l=-1; l<=1; l+=2) {
                try {
                    if (isABlackPiece(i+k,j+l*2) || isABlank(i+k,j+l*2)) {
                        oldPiece=gameBoard[i+k][j+l*2];
                        gameBoard[i][j]=EMPTY_SQUARE;
                        gameBoard[i+k][j+l*2]=WHITE_KNIGHT;
                        if (!whiteKingIsInCheck()) {
                            set.add("" + i + j + (i+k) + (j+l*2) + oldPiece);
                        }
                        gameBoard[i][j]=WHITE_KNIGHT;
                        gameBoard[i+k][j+l*2]=oldPiece;
                    }
                } catch (Exception e) {}
                try {
                    if (isABlackPiece(i + k * 2, j + l) || isABlank(i + k * 2, j + l)) {
                        oldPiece=gameBoard[i+k*2][j + l];
                        gameBoard[i][j]=EMPTY_SQUARE;
                        gameBoard[i+k*2][j + l]=WHITE_KNIGHT;
                        if (!whiteKingIsInCheck()) {
                            set.add("" + i + j + (i+k*2) + (j+l) + oldPiece);
                        }
                        gameBoard[i][j]=WHITE_KNIGHT;
                        gameBoard[i+k*2][j + l]=oldPiece;
                    }
                } catch (Exception e) {}
            }

        }
        return set;
    }
    private HashSet<String> possibleWhiteBishop(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        int s;
        for(int k = -1; k<= 1; k+=2){
            for(int l = -1; l<= 1; l+=2){
                s = 1;
                try{
                    while(isABlank(i + s*k, j + s*l)){
                        oldPiece = gameBoard[i + s*k][j + s*l];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i + s*k][j + s*l] = WHITE_BISHOP;
                        if(!whiteKingIsInCheck()){
                            set.add("" + i + j + (i + s*k) + (j + s*l) + oldPiece);
                        }
                        gameBoard[i][j] = WHITE_BISHOP;
                        gameBoard[i + s*k][j + s*l] = oldPiece;
                        s++;
                    }
                    if(isABlackPiece(i + s*k, j + s*l)){
                        oldPiece = gameBoard[i + s*k][j + s*l];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i + s*k][j + s*l] = WHITE_BISHOP;
                        if(!whiteKingIsInCheck()){
                            set.add("" + i + j + (i + s*k) + (j + s*l) + oldPiece);
                        }
                        gameBoard[i][j] = WHITE_BISHOP;
                        gameBoard[i + s*k][j + s*l] = oldPiece;
                    }

                } catch (Exception e){
                }
            }
        }
        return set;
    }
    private HashSet<String> possibleWhiteQueen(int i, int j) {
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        int s;
        for(int k = -1; k <= 1; k++){
            for(int l = -1 ; l <= 1 ; l++){
                if(k != 0 || l != 0){
                    s = 1;
                    try{
                        while(isABlank(i + s * k, j + s * l))
                        {
                            oldPiece = gameBoard[i + s*k][j + s*l];
                            gameBoard[i][j] = EMPTY_SQUARE;
                            gameBoard[i + s*k][j + s*l] = WHITE_QUEEN;
                            if(!whiteKingIsInCheck()){
                                set.add("" + i + j + (i + s*k) + (j + s*l) + oldPiece);
                            }
                            gameBoard[i][j] = WHITE_QUEEN;
                            gameBoard[i + s*k][j + s*l] = oldPiece;
                            s++;
                        }
                        if(isABlackPiece(i + s * k, j + s * l)){
                            oldPiece = gameBoard[i + s*k][j + s*l];
                            gameBoard[i][j] = EMPTY_SQUARE;
                            gameBoard[i + s*k][j + s*l] = WHITE_QUEEN;
                            if(!whiteKingIsInCheck()){
                                set.add("" + i + j + (i + s*k) + (j + s*l) + oldPiece);
                            }
                            gameBoard[i][j] = WHITE_QUEEN;
                            gameBoard[i + s*k][j + s*l] = oldPiece;
                        }
                    }catch(Exception e){
                    }
                }
            }
        }
        return set;
    }

    /**
     * @param i est la rangée du roi blanc (entre 0 et 7)
     * @param j est la colonne du roi blanc (entre 0 et 7)
     * @return
     */
    private HashSet<String> possibleWhiteKing(int i, int j){
        HashSet<String> set  = new HashSet<String>();
        char oldPiece;
        for (int k = 0; k <= 8; k++){
            if (k != 4){
                try {
                    if (isABlackPiece(i - 1 + k / 3,j - 1 + k % 3) || isABlank(i - 1 + k / 3,j - 1 + k % 3)) {
                        oldPiece = gameBoard[i - 1 + k / 3][j - 1 + k % 3];
                        gameBoard[i][j] = EMPTY_SQUARE;
                        gameBoard[i - 1 + k / 3][j - 1 + k % 3] = WHITE_KING;
                        int kingOriginalRowPosition = whiteKingRowPosition;
                        int kingOriginalColumnPosition = whiteKingColumnPosition;
                        whiteKingRowPosition = i - 1 + k / 3;
                        whiteKingColumnPosition = j - 1 + k % 3;
                        if (!whiteKingIsInCheck()) {
                            set.add("" + i + "" + j + "" + (i - 1 + k / 3) + "" + (j - 1 + k % 3) + "" + oldPiece);
                        }
                        gameBoard[i][j] = WHITE_KING;
                        gameBoard[i - 1 + k / 3][j - 1 + k % 3] = oldPiece;
                        whiteKingRowPosition = kingOriginalRowPosition;
                        whiteKingColumnPosition = kingOriginalColumnPosition;
                    }
                } catch (Exception e){
                }
            }
        }
        return set;
    }


    //Todo: à finir (échec par cavalier et échec par roi)
    public boolean whiteKingIsInCheck(){
        //Échec par les diagonales (Fou ou Reine)
        int s = 1;
        for (int i = -1; i <= 1; i+=2){
            for(int j = -1; j <= 1; j+=2){
                try{
                    while(isABlank(whiteKingRowPosition + s*i,whiteKingColumnPosition + s*j))
                        s++;
                    if (BLACK_BISHOP == gameBoard[whiteKingRowPosition + s*i][whiteKingColumnPosition + s*j] ||
                            BLACK_QUEEN == gameBoard[whiteKingRowPosition + s*i][whiteKingColumnPosition + s*j])
                        return true;
                }catch (Exception e){
                }
                s = 1;
            }
        }

        //Échec par les rangées ou colonnes (Tour ou Reine)
        s = 1;
        for (int i = -1; i<=1; i+=2){
            try{
                while(isABlank(whiteKingRowPosition, whiteKingColumnPosition + s*i)){
                    s++;
                }
                if(BLACK_ROOK == gameBoard[whiteKingRowPosition][whiteKingColumnPosition + s*i] ||
                        BLACK_QUEEN == gameBoard[whiteKingRowPosition][whiteKingColumnPosition + s*i]){
                    return true;
                }
            } catch ( Exception e ){
            }
            s = 1;
            try{
                while(isABlank(whiteKingRowPosition + s*i, whiteKingColumnPosition)){
                    s++;
                }
                if(BLACK_ROOK == gameBoard[whiteKingRowPosition + s*i][whiteKingColumnPosition] ||
                        BLACK_QUEEN == gameBoard[whiteKingRowPosition + s*i][whiteKingColumnPosition]){
                    return true;
                }
            } catch ( Exception e){
            }
            s = 1;
        }

        //Échec par pion
        if (whiteKingRowPosition >= 2){
            try{
                if(BLACK_PAWN == gameBoard[whiteKingRowPosition-1][whiteKingColumnPosition-1]){
                    return true;
                }
            }catch (Exception e){
            }
            try{
                if(BLACK_PAWN == gameBoard[whiteKingRowPosition-1][whiteKingColumnPosition+1]){
                    return true;
                }
            }catch(Exception e){
            }
        }

        //Échec par roi
        for(int i = -1 ; i <= 1; i++ ){
            for(int j = -1; j <= 1; j++){
                if(i != 0 || j != 0){
                    try{
                        if(BLACK_KING == gameBoard[whiteKingRowPosition + i][whiteKingColumnPosition + j])
                            return true;
                    }catch(Exception e){
                    }
                }
            }
        }

        //Échec par cavalier
        for(int i = -1 ; i <= 1 ; i += 2){
            for(int j = -1 ; j <= 1 ; j += 2){
                try{
                    if(BLACK_KNIGHT == gameBoard[whiteKingRowPosition + i][whiteKingColumnPosition + j*2])
                        return true;
                } catch (Exception e){
                }
                try{
                    if(BLACK_KNIGHT == gameBoard[whiteKingRowPosition + i*2][whiteKingColumnPosition + j])
                        return true;
                }catch(Exception e){
                }
            }
        }

        return false;
    }

    public boolean blackKingIsInCheck(){
        //Échec par les diagonales (Fou ou Reine)
        int s = 1;
        for (int i = -1; i <= 1; i+=2){
            for(int j = -1; j <= 1; j+=2){
                try{
                    while(isABlank(blackKingRowPosition + s*i,blackKingColumnPosition + s*j))
                        s++;
                    if (WHITE_BISHOP == gameBoard[blackKingRowPosition + s*i][blackKingColumnPosition + s*j] ||
                            WHITE_QUEEN == gameBoard[blackKingRowPosition + s*i][blackKingColumnPosition + s*j])
                        return true;
                }catch (Exception e){
                }
                s = 1;
            }
        }

        //Échec par les rangées ou colonnes (Tour ou Reine)
        s = 1;
        for (int i = -1; i<=1; i+=2){
            try{
                while(isABlank(blackKingRowPosition, blackKingColumnPosition + s*i)){
                    s++;
                }
                if(WHITE_ROOK == gameBoard[blackKingRowPosition][blackKingColumnPosition + s*i] ||
                        WHITE_QUEEN == gameBoard[blackKingRowPosition][blackKingColumnPosition + s*i]){
                    return true;
                }
            } catch ( Exception e ){
            }
            s = 1;
            try{
                while(isABlank(blackKingRowPosition + s*i, blackKingColumnPosition)){
                    s++;
                }
                if(WHITE_ROOK == gameBoard[blackKingRowPosition + s*i][blackKingColumnPosition] ||
                        WHITE_QUEEN == gameBoard[blackKingRowPosition + s*i][blackKingColumnPosition]){
                    return true;
                }
            } catch(Exception e){
            }
            s=1;
        }

        //Échec par pion
        if (blackKingRowPosition <= 5){
            try{
                if(WHITE_PAWN == gameBoard[blackKingRowPosition+1][blackKingColumnPosition-1]){
                    return true;
                }
            }catch (Exception e){
            }
            try{
                if(WHITE_PAWN == gameBoard[blackKingRowPosition+1][blackKingColumnPosition+1]){
                    return true;
                }
            }catch(Exception e){
            }
        }

        //Échec par roi
        for(int i = -1 ; i <= 1; i++ ){
            for(int j = -1; j <= 1; j++){
                if(i != 0 || j != 0){
                    try{
                        if(WHITE_KING == gameBoard[blackKingRowPosition + i][blackKingColumnPosition + j])
                            return true;
                    }catch(Exception e){
                    }
                }
            }
        }

        //Échec par cavalier
        for(int i = -1 ; i <= 1 ; i += 2){
            for(int j = -1 ; j <= 1 ; j += 2){
                try{
                    if(WHITE_KNIGHT == gameBoard[blackKingRowPosition + i][blackKingColumnPosition + j*2])
                        return true;
                } catch (Exception e){
                }
                try{
                    if(WHITE_KNIGHT == gameBoard[blackKingRowPosition + i*2][blackKingColumnPosition + j])
                        return true;
                }catch(Exception e){
                }
            }
        }


        return false;

    }

}//fin de la classe GameBoard