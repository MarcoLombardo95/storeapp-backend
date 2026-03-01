package com.storeapp.activity.dto;

import java.math.BigDecimal;

/**
 * Bilancio di un membro del gruppo su tutte le spese.
 * balance = totalPaid - totalOwed
 *   > 0 → creditore (deve ricevere)
 *   < 0 → debitore  (deve pagare)
 */
public class MemberBalanceDto {
    public Long groupMemberId;
    public String memberName;
    public String memberAvatarUrl;
    public BigDecimal totalPaid;   // quanto ha anticipato
    public BigDecimal totalOwed;   // la sua quota totale
    public BigDecimal balance;     // totalPaid - totalOwed
}
