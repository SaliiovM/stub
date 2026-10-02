import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class InventoryService {

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> productLocks = new ConcurrentHashMap<>();

    public void registerProduct(Product product) {
        Objects.requireNonNull(product, "product must not be null");
        products.put(product.id(), product);
        productLocks.putIfAbsent(product.id(), new ReentrantLock());
    }

    public Optional<Product> findProduct(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    public Map<String, Product> allProducts() {
        return Collections.unmodifiableMap(products);
    }

    public OrderResult reserveStock(String productId, int quantity) {
        if (quantity <= 0) {
            return OrderResult.rejected("Quantity must be positive");
        }

        ReentrantLock lock = productLocks.get(productId);
        if (lock == null) {
            return OrderResult.rejected("Unknown product: " + productId);
        }

        lock.lock();
        try {
            Product current = products.get(productId);
            if (current == null) {
                return OrderResult.rejected("Unknown product: " + productId);
            }
            if (current.quantity() < quantity) {
                return OrderResult.rejected("Insufficient stock for " + productId);
            }
            Product updated = current.withQuantity(current.quantity() - quantity);
            products.put(productId, updated);
            return OrderResult.accepted(updated);
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal calculateShippingCost(BigDecimal weightKg, int distanceMiles) {
        Objects.requireNonNull(weightKg, "weightKg must not be null");
        ShippingTier tier = ShippingTier.forWeight(weightKg);
        BigDecimal distanceCharge = tier.perMileRate().multiply(BigDecimal.valueOf(distanceMiles));
        return distanceCharge.add(tier.baseFee()).setScale(2, RoundingMode.HALF_UP);
    }

    private enum ShippingTier {
        LIGHT(BigDecimal.valueOf(20), new BigDecimal("0.25"), new BigDecimal("4.99")),
        MEDIUM(BigDecimal.valueOf(50), new BigDecimal("0.45"), new BigDecimal("12.50")),
        HEAVY(null, new BigDecimal("0.75"), new BigDecimal("25.99"));

        private final BigDecimal maxWeightKg;
        private final BigDecimal perMileRate;
        private final BigDecimal baseFee;

        ShippingTier(BigDecimal maxWeightKg, BigDecimal perMileRate, BigDecimal baseFee) {
            this.maxWeightKg = maxWeightKg;
            this.perMileRate = perMileRate;
            this.baseFee = baseFee;
        }

        BigDecimal perMileRate() {
            return perMileRate;
        }

        BigDecimal baseFee() {
            return baseFee;
        }

        static ShippingTier forWeight(BigDecimal weightKg) {
            for (ShippingTier tier : values()) {
                if (tier.maxWeightKg == null || weightKg.compareTo(tier.maxWeightKg) <= 0) {
                    return tier;
                }
            }
            return HEAVY;
        }
    }

    public record Product(String id, String name, BigDecimal price, int quantity) {

        public Product {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(price, "price must not be null");
            if (quantity < 0) {
                throw new IllegalArgumentException("quantity must not be negative");
            }
        }

        public Product withQuantity(int newQuantity) {
            return new Product(id, name, price, newQuantity);
        }
    }

    public record OrderResult(boolean accepted, String message, Product product) {

        public static OrderResult accepted(Product product) {
            return new OrderResult(true, "OK", product);
        }

        public static OrderResult rejected(String reason) {
            return new OrderResult(false, reason, null);
        }
    }
}
