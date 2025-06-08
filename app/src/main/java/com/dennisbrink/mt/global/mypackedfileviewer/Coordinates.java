package com.dennisbrink.mt.global.mypackedfileviewer;

import java.util.ArrayList;
import java.util.List;

public class Coordinates {
    private List<Coordinate> coordinates = new ArrayList<>();
    private boolean set = false;
    private int deviation = 30;

    public Coordinates(List<Coordinate> coordinates, boolean set, int deviation) {
        this.coordinates = coordinates;
        this.set = set;
        this.deviation = deviation;
    }

    public Coordinates() {

    }

    // Getters and setters
    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isSet() {
        return set;
    }

    public void setSet(boolean set) {
        this.set = set;
    }

    public int getDeviation() {
        return deviation;
    }

    public void setDeviation(int deviation) {
        this.deviation = deviation;
    }

}
