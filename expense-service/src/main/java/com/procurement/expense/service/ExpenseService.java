package com.procurement.expense.service;

import com.procurement.expense.model.Expense;
import com.procurement.expense.repository.ExpenseRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Submit new expense — calls ML service for fraud score
    @CacheEvict(value = "expenses", allEntries = true)
    public Expense submitExpense(Expense expense) {
        // Call ML service for fraud detection + category prediction
        try {
            Map<String, Object> mlRequest = new HashMap<>();
            mlRequest.put("amount", expense.getAmount());
            mlRequest.put("description", expense.getDescription());

            Map mlResponse = restTemplate.postForObject(
                mlServiceUrl + "/predict", mlRequest, Map.class);

            if (mlResponse != null) {
                double fraudScore = Double.parseDouble(
                    mlResponse.getOrDefault("fraud_score", "0.0").toString());
                String category = mlResponse.getOrDefault("category", "OTHER").toString();

                expense.setFraudScore(BigDecimal.valueOf(fraudScore));
                expense.setCategory(Expense.Category.valueOf(category));

                // Auto-flag if fraud score > 0.7
                if (fraudScore > 0.7) {
                    expense.setStatus(Expense.Status.FLAGGED);
                    // Send async notification via RabbitMQ
                    rabbitTemplate.convertAndSend("expense.exchange",
                        "expense.flagged",
                        "Expense flagged for employee " + expense.getEmpId()
                        + " amount: " + expense.getAmount());
                }
            }
        } catch (Exception e) {
            // ML service unavailable — continue without fraud score
            expense.setCategory(Expense.Category.OTHER);
        }

        Expense saved = expenseRepository.save(expense);

        // Notify via RabbitMQ
        rabbitTemplate.convertAndSend("expense.exchange",
            "expense.submitted",
            "New expense submitted by employee " + expense.getEmpId());

        return saved;
    }

    @Cacheable("expenses")
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public List<Expense> getExpensesByEmployee(Long empId) {
        return expenseRepository.findByEmpId(empId);
    }

    public List<Expense> getFlaggedExpenses() {
        return expenseRepository.findFlaggedExpenses(new BigDecimal("0.7"));
    }

    @CacheEvict(value = "expenses", allEntries = true)
    public Expense updateExpenseStatus(Long expId, String status, String approverComment) {
        Expense expense = expenseRepository.findById(expId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expId));

        expense.setStatus(Expense.Status.valueOf(status));
        Expense updated = expenseRepository.save(expense);

        // Notify employee via RabbitMQ
        rabbitTemplate.convertAndSend("expense.exchange",
            "expense.status.updated",
            "Expense " + expId + " status updated to " + status);

        return updated;
    }

    public Map<String, Object> getExpenseAnalytics() {
        List<Object[]> summary = expenseRepository.getExpenseSummaryByCategory();
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("categoryBreakdown", summary);
        analytics.put("totalExpenses", expenseRepository.count());
        analytics.put("flaggedCount",
            expenseRepository.findByStatus(Expense.Status.FLAGGED).size());
        return analytics;
    }
}
