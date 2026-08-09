package com.whis.app.msg.model;

/**
 * Six-category triage enum for MSG module — MSG_PLAN.md Section 3 Decision 3 & 4.1.
 * <p>
 * Used for supplementary content triage and Android notification channel priority.
 */
public enum MsgCategory {

    /** Personal contact or explicitly allowed sender */
    ALLOWED,

    /** Bank OTPs, debit/credit alerts, account statements */
    TRANSACTION,

    /** Service alerts, delivery updates, appointment reminders */
    NOTIFICATION,

    /** Marketing, promotional offers, discounts */
    PROMOTION,

    /** General unclassified message */
    GENERAL,

    /** Confirmed scam or junk message */
    JUNK,

    /** Urgent family emergency, accident, or hospital alert */
    EMERGENCY
}
