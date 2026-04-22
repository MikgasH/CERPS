package com.example.cerpshashkin.dto;

public record BankCommissionResponse(Double commission, boolean found) {

    public static BankCommissionResponse found(final Double commission) {
        return new BankCommissionResponse(commission, true);
    }

    public static BankCommissionResponse notFound() {
        return new BankCommissionResponse(null, false);
    }
}
