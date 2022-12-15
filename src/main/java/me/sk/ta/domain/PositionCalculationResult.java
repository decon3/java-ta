package me.sk.ta.domain;

public record PositionCalculationResult(double totalPrice, double charges, int position) {
    @Override
    public String toString() {
        return String.format("Price:%.2f charges:%.2f position:%d", totalPrice,charges,position);
    }
}
