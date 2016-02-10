package models;

public class OddOrEven {
    boolean isEven = true;

    public boolean getAndToggle() {
        boolean value = isEven;
        isEven = !isEven;
        return value;
    }
}

