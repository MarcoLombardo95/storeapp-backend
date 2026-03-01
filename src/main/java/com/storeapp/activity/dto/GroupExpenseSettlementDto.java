package com.storeapp.activity.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Risposta completa dell'endpoint GET /api/groups/{id}/expenses/settlement
 */
public class GroupExpenseSettlementDto {
    public Long groupId;
    public BigDecimal totalExpenses;   // somma di tutte le spese del gruppo
    public int expenseCount;           // numero totale di spese
    public List<MemberBalanceDto> balances;          // bilancio per ogni membro
    public List<SettlementTransactionDto> settlements; // transazioni ottimizzate
    public int transactionCount;       // = settlements.size()
}
