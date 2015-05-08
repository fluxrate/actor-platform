package im.actor.model.api;
/*
 *  Generated by the Actor API Scheme generator.  DO NOT EDIT!
 */

import java.io.IOException;

public enum Sex {

    UNKNOWN(1),
    MALE(2),
    FEMALE(3),
    UNSUPPORTED_VALUE(-1);

    private int value;

    Sex(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Sex parse(int value) throws IOException {
        switch(value) {
            case 1: return Sex.UNKNOWN;
            case 2: return Sex.MALE;
            case 3: return Sex.FEMALE;
            default: return Sex.UNSUPPORTED_VALUE;
        }
    }
}
