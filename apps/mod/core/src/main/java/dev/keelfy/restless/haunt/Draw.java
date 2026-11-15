package dev.keelfy.restless.haunt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/**
 * A weighted draw that asks whether the drawn item can be used only after drawing it: a drawn item that cannot is put
 * aside and the draw is repeated among the rest. The odds are the same as a weighted draw among the usable items only,
 * but the costly check runs on a few items instead of all of them. Pure, so it is unit-testable.
 */
public final class Draw {
    private Draw() {}

    /**
     * @param weights one per item, each above 0
     * @param usable the costly check, called at most once per item
     * @param random uniform in [0, 1)
     * @return the drawn usable item, empty when none is
     */
    public static <T> Optional<T> first(
            List<T> items, List<Double> weights, Predicate<T> usable, DoubleSupplier random) {
        List<T> left = new ArrayList<>(items);
        List<Double> odds = new ArrayList<>(weights);
        double total = 0;
        for (double weight : odds) {
            total += weight;
        }
        while (!left.isEmpty()) {
            double roll = random.getAsDouble() * total;
            int i = left.size() - 1;
            for (int j = 0; j < left.size() - 1; j++) {
                roll -= odds.get(j);
                if (roll < 0) {
                    i = j;
                    break;
                }
            }
            T item = left.get(i);
            if (usable.test(item)) {
                return Optional.of(item);
            }
            total -= odds.get(i);
            int last = left.size() - 1;
            left.set(i, left.get(last));
            odds.set(i, odds.get(last));
            left.remove(last);
            odds.remove(last);
        }
        return Optional.empty();
    }
}
