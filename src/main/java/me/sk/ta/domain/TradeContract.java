package me.sk.ta.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;
import java.util.Objects;

@Getter @Setter
@Accessors(fluent = true, chain = true)
public class TradeContract {
    private int id;
    private LocalDate date;
    private int size;
    private double averagePrice;
    private double totalPrice;
    private double charges;
    private boolean isSale;
    private boolean isIntraDay;

    public TradeContract() {}
    public TradeContract(int id, LocalDate date, int size, double averagePrice, boolean isIntraDay, TradingChargesCalculator calculator)
    {
        this.id = id;
        this.date = date;
        this.size = size;
        this.averagePrice = averagePrice;
        this.totalPrice = Utils.round(size * averagePrice, 2);
        this.isIntraDay = isIntraDay;
        this.charges = calculator.estimateCostOfTrade(totalPrice, isIntraDay).total();
    }

    @JsonIgnore
    public boolean IsValid() {
        return true;
        // TODO call validater
        // return new TradeContractValidator().validate(this, ValidationUtils.);
    }

    @Override
    public String toString() {
        return "TradeContract{" +
                "id=" + id +
                ", date=" + date +
                ", size=" + size +
                ", averagePrice=" + averagePrice +
                ", totalPrice=" + totalPrice +
                ", charges=" + charges +
                ", sale=" + isSale +
                ", intraDay=" + isIntraDay +
                '}';
    }

    private class TradeContractValidator implements Validator
    {

        @Override
        public boolean supports(Class<?> clazz) {
            return TradeContract.class.isAssignableFrom(clazz);
        }

        @Override
        public void validate(Object target, Errors e) {
            var t = (TradeContract) target;
            if (t.size <= 0) {
                e.rejectValue("size", "Mandatory");
            }
            if (t.averagePrice <= 0) {
                e.rejectValue("averagePrice", "Mandatory");
            }
            if (t.totalPrice <= 0) {
                e.rejectValue("totalPrice", "Mandatory");
            }
            if (t.charges <= 0) {
                e.rejectValue("charges", "Mandatory");
            }
            if (t.date.isBefore(LocalDate.of(2000, 1, 1))) {
                e.rejectValue("date", "Mandatory", "Should be greater than 2000-01-01");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeContract that = (TradeContract) o;
        return id == that.id &&
                size == that.size &&
                Double.compare(that.averagePrice, averagePrice) == 0 &&
                Double.compare(that.totalPrice, totalPrice) == 0 &&
                Double.compare(that.charges, charges) == 0 &&
                isSale == that.isSale &&
                isIntraDay == that.isIntraDay &&
                date.equals(that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, size, averagePrice, totalPrice, charges, isSale, isIntraDay);
    }

}
