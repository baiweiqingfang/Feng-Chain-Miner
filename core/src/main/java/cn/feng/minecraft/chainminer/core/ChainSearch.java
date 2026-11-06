package cn.feng.minecraft.chainminer.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Loader-independent breadth-first search for connected blocks. */
public final class ChainSearch {
    private ChainSearch() {
    }

    /**
     * Finds matching positions connected to {@code origin}. The origin is not
     * included in the returned list, but it counts toward {@code maxTotal}.
     */
    public static <P> List<P> findConnected(
            P origin,
            Function<P, Iterable<P>> neighbors,
            Predicate<P> matches,
            int maxTotal
    ) {
        if (maxTotal < 1) {
            throw new IllegalArgumentException("maxTotal must be positive");
        }

        ArrayDeque<P> queue = new ArrayDeque<>();
        Set<P> visited = new HashSet<>();
        List<P> result = new ArrayList<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && result.size() < maxTotal - 1) {
            P center = queue.removeFirst();
            for (P next : neighbors.apply(center)) {
                if (result.size() >= maxTotal - 1) {
                    break;
                }
                if (visited.add(next) && matches.test(next)) {
                    result.add(next);
                    queue.addLast(next);
                }
            }
        }

        return result;
    }
}

