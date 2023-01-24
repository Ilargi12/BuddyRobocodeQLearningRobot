package BuddyRobocode;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class QTable {
    private int numStates = State.statesCount;
    private int numActions = Action.actionsCount;
    public double[][] qTable;

    public QTable(){
        this.qTable = new double[this.numStates][this.numActions];
        for (int i = 0; i < this.numStates; ++i)
            for (int j = 0; j < this.numActions; ++j)
                this.qTable[i][j] = 0.0;
    }

    public double getMaxQValue(int state){
        return Arrays.stream(this.qTable[state]).max().getAsDouble();
    }

    public int getOptimalAction(int state){
        int idx = 0;
        for (double QValue: this.qTable[state]){
            if (QValue == this.getMaxQValue(state)) return idx;
            ++idx;
        }
        return 0;
    }

    public void set(int state, int action, double value){
        this.qTable[state][action] = value;
    }

    public double get(int state, int action){
        return this.qTable[state][action];
    }

}