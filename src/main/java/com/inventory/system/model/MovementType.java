package com.inventory.system.model;

/**
 * The reason a StockMovement was written. Quantity on a movement is always
 * positive — this enum, not the sign of the number, tells you direction.
 */
public enum MovementType {
    /** Stock coming in: a restock, a return, an initial load. */
    IN,
    /** Stock going out because of a completed sale. */
    OUT,
    /** A manual correction — damage, recount, shrinkage. */
    ADJUSTMENT
}
