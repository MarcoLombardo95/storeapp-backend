package com.storeapp.activity.dto;

import java.math.BigDecimal;

/**
 * Una transazione del settlement ottimizzato.
 * "from deve pagare amount a to"
 */
public class SettlementTransactionDto {
    public Long fromMemberId;
    public String fromMemberName;
    public String fromMemberAvatarUrl;
    public Long toMemberId;
    public String toMemberName;
    public String toMemberAvatarUrl;
    public BigDecimal amount;
}
