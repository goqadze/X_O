package com.tictactoe;

public class Game {
    private MoveType nextMove;

    private MoveType currentMove;

    private int[][] board;

    Game(){
        init();
    }

    private void setCurrentMove() {
        MoveType type = this.currentMove;
        this.currentMove = this.nextMove;
        this.nextMove = type;
    }

    public MoveType getCurrentMove(){
        return this.currentMove;
    }

    public boolean makeMove(Move playerMove){
        int i = playerMove.getI();
        int j = playerMove.getJ();

        if (this.board[i][j] != 0){
            return false;
        }

        this.board[i][j] = this.nextMove.getValue();

        setCurrentMove();

        return true;
    }

    public MoveType checkWinner(){
        for (int i = 0; i < 3; i++){
            if (this.board[i][0] == this.board[i][1] && this.board[i][1] == this.board[i][2]){
                MoveType type = MoveType.values()[this.board[i][0]];

                if (type != MoveType.NONE)
                    return type;
            }
        }

        for (int i = 0; i < 3; i++){
            if (this.board[0][i] == this.board[1][i] && this.board[1][i] == this.board[2][i]){
                MoveType type = MoveType.values()[this.board[0][i]];

                if (type != MoveType.NONE)
                    return type;
            }
        }

        if (this.board[0][0] == this.board[1][1] && this.board[1][1] == this.board[2][2]){
            MoveType type = MoveType.values()[this.board[0][0]];

            if (type != MoveType.NONE)
                return type;
        }

        if (this.board[0][2] == this.board[1][1] && this.board[1][1] == this.board[2][0]){
            MoveType type = MoveType.values()[this.board[0][2]];

            if (type != MoveType.NONE)
                return type;
        }

        if (movesCount() == 9){
            return  MoveType.DRAW;
        }

        return MoveType.NONE;
    }

    public void reset(){
        this.init();
    }

    private void init(){
        this.board = new int[3][3];
        this.currentMove = MoveType.O;
        this.nextMove = MoveType.X;
    }

    private int movesCount(){
        int count = 0;
        for (int i = 0; i < 3; i ++){
            for (int j = 0; j < 3; j ++){
                if (this.board[i][j] > 0){
                    count += 1;
                }
            }
        }
        return count;
    }
}
