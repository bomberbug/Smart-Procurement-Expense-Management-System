package com.procurement.expense;

import com.procurement.expense.model.Expense;
import com.procurement.expense.repository.ExpenseRepository;
import com.procurement.expense.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ExpenseService expenseService;

    private Expense mockExpense;

    @BeforeEach
    void setUp() {
        mockExpense = new Expense();
        mockExpense.setExpId(1L);
        mockExpense.setEmpId(3L);
        mockExpense.setAmount(new BigDecimal("2500.00"));
        mockExpense.setCategory(Expense.Category.TRAVEL);
        mockExpense.setStatus(Expense.Status.PENDING);
        mockExpense.setFraudScore(new BigDecimal("0.05"));
    }

    @Test
    void getAllExpenses_ReturnsExpenseList() {
        when(expenseRepository.findAll()).thenReturn(List.of(mockExpense));
        List<Expense> result = expenseService.getAllExpenses();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getExpensesByEmployee_ReturnsCorrectList() {
        when(expenseRepository.findByEmpId(3L)).thenReturn(List.of(mockExpense));
        List<Expense> result = expenseService.getExpensesByEmployee(3L);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getEmpId());
    }

    @Test
    void updateExpenseStatus_Approve_UpdatesStatus() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(mockExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(mockExpense);

        Expense updated = expenseService.updateExpenseStatus(1L, "APPROVED", "Looks good");

        assertEquals(Expense.Status.APPROVED, updated.getStatus());
        verify(rabbitTemplate, times(1))
            .convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void updateExpenseStatus_InvalidId_ThrowsException() {
        when(expenseRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
            expenseService.updateExpenseStatus(999L, "APPROVED", ""));
    }

    @Test
    void getFlaggedExpenses_ReturnsFraudulentExpenses() {
        Expense flagged = new Expense();
        flagged.setFraudScore(new BigDecimal("0.89"));
        flagged.setStatus(Expense.Status.FLAGGED);

        when(expenseRepository.findFlaggedExpenses(any(BigDecimal.class)))
            .thenReturn(List.of(flagged));

        List<Expense> result = expenseService.getFlaggedExpenses();
        assertEquals(1, result.size());
        assertEquals(Expense.Status.FLAGGED, result.get(0).getStatus());
    }
}
