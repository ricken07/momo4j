/**
 * Core abstractions and interfaces for the momo4j unified Mobile Money client.
 * <p>
 * This package provides the fundamental building blocks for Mobile Money integrations:
 * <ul>
 *   <li>{@link com.rickenbazolo.momo4j.core.MobileMoneyClient} - Base client interface</li>
 *   <li>{@link com.rickenbazolo.momo4j.core.MobileMoneyConfig} - Configuration interface</li>
 *   <li>{@link com.rickenbazolo.momo4j.core.http} - HTTP abstraction layer</li>
 *   <li>{@link com.rickenbazolo.momo4j.core.operations} - Common operation interfaces</li>
 * </ul>
 * <p>
 * Operator-specific modules (e.g., momo4j-congo-airtel, momo4j-congo-airtel) implement these
 * interfaces to provide concrete implementations for each Mobile Money provider.
 *

 * @author Ricken Bazolo * @since 0.1.0
 */
package com.rickenbazolo.momo4j.core;
