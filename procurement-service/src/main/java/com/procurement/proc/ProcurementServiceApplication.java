package com.procurement.proc;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ─── Main Application ─────────────────────────────────────────────────────────

@SpringBootApplication
@EnableCaching
public class ProcurementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcurementServiceApplication.class, args);
    }

    @Bean TopicExchange procExchange()  { return new TopicExchange("procurement.exchange"); }
    @Bean Queue         procQueue()     { return new Queue("procurement.queue", true); }
    @Bean Binding       procBinding(Queue q, TopicExchange e) {
        return BindingBuilder.bind(q).to(e).with("procurement.#");
    }
    @Bean Jackson2JsonMessageConverter msgConverter() { return new Jackson2JsonMessageConverter(); }
}

// ─── Vendor Entity ────────────────────────────────────────────────────────────

@Entity @Table(name = "vendor") @Data @NoArgsConstructor
class Vendor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorId;
    private String name;
    private String email;
    private String contact;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

// ─── Purchase Order Entity ────────────────────────────────────────────────────

@Entity @Table(name = "purchase_order") @Data @NoArgsConstructor
class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long poId;
    private Long vendorId;
    private Long deptId;
    private Long requestedBy;
    private BigDecimal amount;
    private String description;
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist  void onCreate()  { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   void onUpdate()  { updatedAt = LocalDateTime.now(); }
    public enum Status { DRAFT, PENDING, APPROVED, REJECTED, COMPLETED }
}

// ─── Repositories ─────────────────────────────────────────────────────────────

@Repository
interface VendorRepository extends JpaRepository<Vendor, Long> {
    boolean existsByEmail(String email);
}

@Repository
interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByStatus(PurchaseOrder.Status status);
    List<PurchaseOrder> findByRequestedBy(Long empId);
    @Query("SELECT SUM(po.amount) FROM PurchaseOrder po WHERE po.deptId = :deptId AND po.status = 'APPROVED'")
    BigDecimal getTotalApprovedByDept(Long deptId);
}

// ─── Vendor Service ───────────────────────────────────────────────────────────

@Service
class VendorService {
    @Autowired VendorRepository vendorRepo;

    @Cacheable("vendors")
    public List<Vendor> getAllVendors() { return vendorRepo.findAll(); }

    @CacheEvict(value = "vendors", allEntries = true)
    public Vendor createVendor(Vendor v) {
        if (vendorRepo.existsByEmail(v.getEmail()))
            throw new RuntimeException("Vendor email already exists");
        return vendorRepo.save(v);
    }
}

// ─── Purchase Order Service ───────────────────────────────────────────────────

@Service
class PurchaseOrderService {
    @Autowired PurchaseOrderRepository poRepo;
    @Autowired RabbitTemplate rabbitTemplate;

    @Cacheable("orders")
    public List<PurchaseOrder> getAllOrders() { return poRepo.findAll(); }

    @CacheEvict(value = "orders", allEntries = true)
    public PurchaseOrder createOrder(PurchaseOrder po) {
        po.setStatus(PurchaseOrder.Status.PENDING);
        PurchaseOrder saved = poRepo.save(po);
        rabbitTemplate.convertAndSend("procurement.exchange", "procurement.order.created",
            "New PO created: " + saved.getPoId() + " for ₹" + saved.getAmount());
        return saved;
    }

    @CacheEvict(value = "orders", allEntries = true)
    public PurchaseOrder updateStatus(Long poId, String status, Long approverId) {
        PurchaseOrder po = poRepo.findById(poId)
            .orElseThrow(() -> new RuntimeException("PO not found: " + poId));
        po.setStatus(PurchaseOrder.Status.valueOf(status));
        PurchaseOrder updated = poRepo.save(po);
        rabbitTemplate.convertAndSend("procurement.exchange", "procurement.order.status",
            "PO " + poId + " " + status + " by " + approverId);
        return updated;
    }
}

// ─── Vendor Controller ────────────────────────────────────────────────────────

@RestController @RequestMapping("/api/procurement/vendors") @CrossOrigin(origins = "*")
class VendorController {
    @Autowired VendorService vendorService;

    @GetMapping
    public ResponseEntity<List<Vendor>> getAll() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Vendor vendor) {
        try { return ResponseEntity.ok(vendorService.createVendor(vendor)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}

// ─── Purchase Order Controller ────────────────────────────────────────────────

@RestController @RequestMapping("/api/procurement/orders") @CrossOrigin(origins = "*")
class PurchaseOrderController {
    @Autowired PurchaseOrderService poService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAll() {
        return ResponseEntity.ok(poService.getAllOrders());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PurchaseOrder po) {
        try { return ResponseEntity.ok(poService.createOrder(po)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, Object> req) {
        try {
            Long approverId = req.get("approverId") != null
                ? Long.parseLong(req.get("approverId").toString()) : 1L;
            return ResponseEntity.ok(
                poService.updateStatus(id, (String) req.get("status"), approverId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "procurement-service"));
    }
}
