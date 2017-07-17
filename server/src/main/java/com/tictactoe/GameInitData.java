package com.tictactoe;

public class GameInitData {
    private int userType;
    private int userPoints;
    private int opponentPoints;

    public GameInitData(int userType, int userPoints, int opponentPoints){
        this.userType = userType;
        this.userPoints = userPoints;
        this.opponentPoints = opponentPoints;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public int getUserPoints() {
        return userPoints;
    }

    public void setUserPoints(int userPoints) {
        this.userPoints = userPoints;
    }

    public int getOpponentPoints() {
        return opponentPoints;
    }

    public void setOpponentPoints(int opponentPoints) {
        this.opponentPoints = opponentPoints;
    }
}
