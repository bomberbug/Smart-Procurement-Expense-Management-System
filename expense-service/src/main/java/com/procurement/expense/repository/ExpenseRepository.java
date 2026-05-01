package com.procurement.expense.repository;

import com.procurement.expense.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByEmpId(Long empId);

    List<Expense> findByStatus(Expense.Status status);

    List<Expense> findByCategory(Expense.Category category);

    @Query("SELECT e FROM Expense e WHERE e.fraudScore > :threshold")
    List<Expense> findFlaggedExpenses(BigDecimal threshold);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.empId = :empId AND e.status = 'APPROVED'")
    BigDecimal getTotalApprovedByEmployee(Long empId);

    @Query("SELECT e.category, COUNT(e), SUM(e.amount) FROM Expense e GROUP BY e.category")
    List<Object[]> getExpenseSummaryByCategory();
}
