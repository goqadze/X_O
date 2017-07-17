package com.tictactoe;

public enum MoveType {
    NONE(0), X(1), O(2), DRAW(3);

    private final int value;
    MoveType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MoveType FromInt(int value){
        if (value == 1)
            return MoveType.X;
        if (value == 2)
            return MoveType.O;

        return MoveType.NONE;
    }
}
