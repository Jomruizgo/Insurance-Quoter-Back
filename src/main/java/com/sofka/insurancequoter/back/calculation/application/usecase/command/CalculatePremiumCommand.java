package com.sofka.insurancequoter.back.calculation.application.usecase.command;

// Command object to trigger premium calculation for a specific quote version
public record CalculatePremiumCommand(String folioNumber, long version) {}
