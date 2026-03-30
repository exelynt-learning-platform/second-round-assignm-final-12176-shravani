package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class EcommerceBackend {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceBackend.class, args);
    }
}

// ===================== ENTITIES =====================

@Entity
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String imageUrl;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

@Entity
class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;

    @ManyToMany
    private List<Product> products = new ArrayList<>();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}

// ===================== REPOSITORIES =====================

interface ProductRepository extends JpaRepository<Product, Long> {}
interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUsername(String username);
}

// ===================== SERVICES =====================

@Service
class ProductService {
    private final ProductRepository productRepository;
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    public Product save(Product product) { return productRepository.save(product); }
    public List<Product> findAll() { return productRepository.findAll(); }
    public Product update(Long id, Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        existing.setImageUrl(product.getImageUrl());
        return productRepository.save(existing);
    }
    public void delete(Long id) { productRepository.deleteById(id); }
}

@Service
class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }
    public Cart addProduct(String username, Long productId) {
        Cart cart = cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUsername(username);
                    return newCart;
                });
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        cart.getProducts().add(product);
        return cartRepository.save(cart);
    }
    public Cart removeProduct(String username, Long productId) {
        Cart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        cart.getProducts().removeIf(p -> p.getId() != null && p.getId().equals(productId));
        return cartRepository.save(cart);
    }
    public Cart getCart(String username) {
        return cartRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }
}

// ===================== CONTROLLERS =====================

@RestController
@RequestMapping("/products")
class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }

    @PostMapping
    public Product create(@RequestBody Product product) { return productService.save(product); }

    @GetMapping
    public List<Product> getAll() { return productService.findAll(); }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        return productService.update(id, product);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { productService.delete(id); }
}

@RestController
@RequestMapping("/cart")
class CartController {
    private final CartService cartService;
    public CartController(CartService cartService) { this.cartService = cartService; }

    @PostMapping("/add/{productId}")
    public Cart addProduct(@PathVariable Long productId, @RequestParam String username) {
        return cartService.addProduct(username, productId);
    }

    @DeleteMapping("/remove/{productId}")
    public Cart removeProduct(@PathVariable Long productId, @RequestParam String username) {
        return cartService.removeProduct(username, productId);
    }

    @GetMapping
    public Cart viewCart(@RequestParam String username) {
        return cartService.getCart(username);
    }
}
