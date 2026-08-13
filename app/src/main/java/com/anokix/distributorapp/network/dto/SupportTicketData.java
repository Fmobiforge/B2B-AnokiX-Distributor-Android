package com.anokix.distributorapp.network.dto;

/**
 * Envelope shared by {@code support/tickets} (create), {@code support/tickets/{id}},
 * {@code support/tickets/reply} and {@code support/tickets/close} — every one of them
 * answers with the ticket in its new state, messages included.
 */
public class SupportTicketData {
    public SupportOverviewData.Ticket ticket;
}
