package com.procurement.expense.controller;

import com.procurement.expense.model.Expense;
import com.procurement.expense.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    // POST /api/expenses - Submit new expense
    @PostMapping
    public ResponseEntity<?> submitExpense(@RequestBody Expense expense) {
        try {
            Expense saved = expenseService.submitExpense(expense);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/expenses - Get all expenses (Manager/Admin)
    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    // GET /api/expenses/employee/{empId} - Get expenses by employee
    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<Expense>> getByEmployee(@PathVariable Long empId) {
        return ResponseEntity.ok(expenseService.getExpensesByEmployee(empId));
    }

    // GET /api/expenses/flagged - Get fraud-flagged expenses
    @GetMapping("/flagged")
    public ResponseEntity<List<Expense>> getFlagged() {
        return ResponseEntity.ok(expenseService.getFlaggedExpenses());
    }

    // PUT /api/expenses/{id}/status - Approve or reject
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> request) {
        try {
            Expense updated = expenseService.updateExpenseStatus(
                id,
                request.get("status"),
                request.getOrDefault("comment", "")
            );
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/expenses/analytics - Dashboard analytics
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        return ResponseEntity.ok(expenseService.getExpenseAnalytics());
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "expense-service"));
    }
}
