package net.finmath.xva.coordinates.lmm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.products.SimpleSwap;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

@RunWith(Theories.class)
public class SwapMarketRateProductTest {

	@DataPoints
	public static TimeDiscretizationInterface[] discretizations = new TimeDiscretizationInterface[] {
			new TimeDiscretization(0.0, 5.0, 1.0, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END)
	};

	@DataPoints
	public static double[] periodLengths = new double[] { 0.25, 0.5 };

	@DataPoints("forwardRates")
	public static double[][] rates = new double[][] { { 0.01, 0.03, 0.025, 0.02, 0.015 }, { 0.01, 0.02, 0.025, 0.02, 0.015 }};

	@DataPoints("discountFactors")
	public static double[][] discountFactors = new double[][] { { 0.98, 0.95, 0.94, 0.92, 0.9 }};

	@Theory
	public void testGetValue(TimeDiscretizationInterface floatTenor, TimeDiscretizationInterface fixTenor,
			@FromDataPoints("discountFactors") double[] discountFactors,
			@FromDataPoints("forwardRates") double[] forwardRates, double periodLength)
					throws CalculationException {

		final SwapMarketRateProduct parRate = new SwapMarketRateProduct(floatTenor, fixTenor);

		final double lastTime = Math.max(floatTenor.getTime(floatTenor.getNumberOfTimeSteps()), fixTenor.getTime(fixTenor.getNumberOfTimeSteps()));

		TimeDiscretizationInterface periodTenor = new TimeDiscretization(0.0, lastTime, periodLength, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END);
		TimeDiscretizationInterface processTenor = periodTenor.union(
				new TimeDiscretization(0.0, lastTime, 0.1, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END));

		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards("",
				Arrays.stream(floatTenor.getAsDoubleArray()).skip(1).toArray(), forwardRates, periodLength);
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("",
				Arrays.stream(fixTenor.getAsDoubleArray()).skip(1).toArray(), discountFactors);
		AbstractLIBORCovarianceModel covariance = new LIBORCovarianceModelExponentialForm5Param(processTenor, periodTenor, 1, new double[] { 0.1, 0.1, 0.1, 0.1, 0.1});

		LIBORModelMonteCarloSimulation simulation = new LIBORModelMonteCarloSimulation(
				new LIBORMarketModel(periodTenor, forwardCurve, discountCurve, covariance),
				new ProcessEulerScheme(new BrownianMotion(processTenor, 1, 100, 42)));

		final double numerator = IntStream.range(0, floatTenor.getNumberOfTimeSteps()).
				mapToDouble(i -> forwardRates[i] * discountFactors[i] * floatTenor.getTimeStep(i)).
				sum();

		final double denominator = IntStream.range(0, fixTenor.getNumberOfTimeSteps()).
				mapToDouble(i -> discountFactors[i] * fixTenor.getTimeStep(i)).
				sum();

		assertThat(parRate.getValue(0.0, simulation).getAverage(),
				is(closeTo(numerator/denominator, 1E-3)));
	}

	@Theory
	public void testGetValuePutIntoSwap(TimeDiscretizationInterface uniTenor,
			@FromDataPoints("discountFactors") double[] discountFactors,
			@FromDataPoints("forwardRates") double[] forwardRates, double periodLength)
					throws CalculationException {

		final SwapMarketRateProduct parRate = new SwapMarketRateProduct(uniTenor, uniTenor);

		final double lastTime = uniTenor.getTime(uniTenor.getNumberOfTimeSteps());

		TimeDiscretizationInterface periodTenor = new TimeDiscretization(0.0, lastTime, periodLength, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END);
		TimeDiscretizationInterface processTenor = periodTenor.union(
				new TimeDiscretization(0.0, lastTime, 0.1, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END));

		final double[] timesFromStart = Arrays.stream(uniTenor.getAsDoubleArray()).limit(uniTenor.getNumberOfTimeSteps()).toArray();
		final double[] timesFromEnd = Arrays.stream(uniTenor.getAsDoubleArray()).skip(1).toArray();
		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards("",
				timesFromEnd, forwardRates, periodLength);
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("",
				timesFromEnd, discountFactors);
		AbstractLIBORCovarianceModel covariance = new LIBORCovarianceModelExponentialForm5Param(processTenor, periodTenor, 1, new double[] { 0.1, 0.1, 0.1, 0.1, 0.1});

		LIBORModelMonteCarloSimulation simulation = new LIBORModelMonteCarloSimulation(
				new LIBORMarketModel(periodTenor, forwardCurve, discountCurve, covariance),
				new ProcessEulerScheme(new BrownianMotion(processTenor, 1, 10000, 42)));

		double parRateToday = parRate.getValue(0.0, simulation).getAverage();

		double[] notionals = new double[timesFromEnd.length];
		double[] ratesForSwap = new double[timesFromEnd.length];
		Arrays.fill(ratesForSwap, parRateToday);
		Arrays.fill(notionals, 1.0);

		assertThat(new SimpleSwap(timesFromStart, timesFromEnd, ratesForSwap, notionals).getValue(0.0, simulation).getAverage(),
				is(closeTo(0.0, 1E-3)));
	}
}