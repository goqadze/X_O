package com.tictactoe;

public class Move {
    private int i;
    private int j;
    private int type;
    private int isWinner;

    public Move() {
    }

    public Move(int i, int j) {
        super();
        this.i = i;
        this.j = j;
        this.isWinner = MoveType.NONE.getValue();
        this.type = MoveType.NONE.getValue();
    }

    public int getJ() {
        return j;
    }

    public void setJ(int j) {
        this.j = j;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public int getType() {
        return type;
    }

    public void setType(MoveType type) {
        this.type = type.getValue();
    }

    public int getIsWinner() {
        return isWinner;
    }

    public void setIsWinner(MoveType winner) {
        isWinner = winner.getValue();
    }
}
