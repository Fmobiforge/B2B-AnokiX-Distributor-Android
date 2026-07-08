package com.anokix.distributorapp.network.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code data} block of {@code GET api/common/vas/categories}. The categories form a
 * tree; only leaf nodes (no children) that {@code hasProducts} are purchasable, so
 * {@link #leaves()} flattens the tree depth-first (respecting {@code displayOrder}) into
 * the flat list the UI shows.
 */
public class VasCategoriesData {
    public boolean configured;
    public double commission_rate;
    public List<Node> categories;

    public static class Node {
        public String id;
        public String name;
        public int displayOrder;
        public List<Node> children;
        public int productCount;
        public boolean hasProducts;

        boolean isLeaf() {
            return children == null || children.isEmpty();
        }
    }

    /** Depth-first flatten to the selectable leaf categories, ordered by {@code displayOrder}. */
    public List<Node> leaves() {
        List<Node> out = new ArrayList<>();
        collect(categories, out);
        return out;
    }

    private void collect(List<Node> nodes, List<Node> out) {
        if (nodes == null) return;
        List<Node> sorted = new ArrayList<>(nodes);
        Collections.sort(sorted, (a, b) -> Integer.compare(a.displayOrder, b.displayOrder));
        for (Node n : sorted) {
            if (n == null) continue;
            if (n.isLeaf()) {
                if (n.hasProducts || n.productCount > 0) out.add(n);
            } else {
                collect(n.children, out);
            }
        }
    }
}
