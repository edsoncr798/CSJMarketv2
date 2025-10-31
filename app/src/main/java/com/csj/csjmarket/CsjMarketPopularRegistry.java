package com.csj.csjmarket;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class CsjMarketPopularRegistry {
    private static final int MAX_SIZE = 200;
    private static final LinkedHashSet<Integer> popular = new LinkedHashSet<>();

    public static synchronized void add(Integer id) {
        if (id == null || id <= 0) return;
        popular.remove(id);
        popular.add(id);
        while (popular.size() > MAX_SIZE) {
            Integer first = popular.iterator().next();
            popular.remove(first);
        }
    }

    public static synchronized Set<Integer> get() {
        return Collections.unmodifiableSet(popular);
    }
}