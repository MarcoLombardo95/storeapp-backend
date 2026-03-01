package com.storeapp.activity.repository;

import java.math.BigDecimal;

/**
 * Proiezione raw per il bilancio aggregato di un membro a livello gruppo.
 * Costruita via JPQL constructor expression.
 */
public class MemberBalanceProjection {
    public final Long groupMemberId;
    public final String memberName;
    public final String memberAvatarUrl;
    public final BigDecimal totalPaid;
    public final BigDecimal totalOwed;

    public MemberBalanceProjection(Long groupMemberId, String memberName, String memberAvatarUrl,
                                   BigDecimal totalPaid, BigDecimal totalOwed) {
        this.groupMemberId = groupMemberId;
        this.memberName = memberName;
        this.memberAvatarUrl = memberAvatarUrl;
        this.totalPaid = totalPaid;
        this.totalOwed = totalOwed;
    }
}
