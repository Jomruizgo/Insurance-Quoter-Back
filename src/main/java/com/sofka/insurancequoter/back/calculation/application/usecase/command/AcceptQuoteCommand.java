package com.sofka.insurancequoter.back.calculation.application.usecase.command;

// Command carrying the data needed to accept a quote
public record AcceptQuoteCommand(
        String folioNumber,
        String acceptedBy,
        long version
) {}
