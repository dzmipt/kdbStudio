package studio.kdb;

public interface ToDouble {

    double toDouble();
    boolean isNull();
    boolean isPositiveInfinity();
    boolean isNegativeInfinity();

    default boolean isInfinity() {
        return isPositiveInfinity() || isNegativeInfinity();
    }
}
