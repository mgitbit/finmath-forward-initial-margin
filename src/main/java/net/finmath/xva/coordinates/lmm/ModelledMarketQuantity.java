package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

/**
 * Bundles a product with a coordinate of the ISDA SIMM v2 framework.
 * The product will deliver the market rate with regard to which the sensitivity is calculated.
 */
public interface ModelledMarketQuantity {
	/**
	 * The SIMM coordinate for the sensitivities in the
	 * @return The coordinate as an {@link Simm2Coordinate} instance.
	 */
	Simm2Coordinate getCoordinate();

	/**
	 * The product that will deliver the market quantity.
	 * @param evaluationTime The evaluation time as {@link net.finmath.time.FloatingpointDate}.
	 *                       This is needed as the product itself might depend on the time, e. g. a swap rate will have
	 *                       a different schedule at different times.
	 * @return An {@link AbstractMonteCarloProduct} able to calculate the quantity at given times and for given simulations.
	 */
	AbstractMonteCarloProduct getProduct(double evaluationTime);
}
