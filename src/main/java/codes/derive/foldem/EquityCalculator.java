package codes.derive.foldem;

import static codes.derive.foldem.Foldem.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import codes.derive.foldem.board.Board;
import codes.derive.foldem.board.Boards;
import codes.derive.foldem.eval.DefaultEvaluator;
import codes.derive.foldem.eval.Evaluator;
import codes.derive.foldem.hand.Hand;
import codes.derive.foldem.hand.HandGroup;

/**
 * A type that can be used to calculate equity for hands and groups of hands
 * using Monte Carlo simulations.
 */
public class EquityCalculator {

	/* The default sample size to use for simulations. */
	private static final int DEFAULT_SAMPLE_SIZE = 10000;
	
	/* The default evaluator to use for simulations */
	private static final Evaluator DEFAULT_EVALUATOR = new DefaultEvaluator();
	
	/* The sample size to use for simulations. */
	private int sampleSize = DEFAULT_SAMPLE_SIZE;
	
	/* The evaluator to use for simulations */
	private Evaluator evaluator = DEFAULT_EVALUATOR;

	// TODO calculate(HandGroup...)
	// TODO consider renaming
	// TODO refactor

	/**
	 * Performs an equity calculation for the specified hands and return map
	 * containing each hand mapped to its calculated equity.
	 * 
	 * @param hands
	 *            The hands to calculate equity for.
	 * @return A map containing the specified hands mapped to their calculated
	 *         equity.
	 */
	public Map<Hand, Equity> calculate(Hand... hands) {
		
		// map base equities to respective hands
		Map<Hand, Equity> equities = new HashMap<>();
		for (Hand hand : hands) {
			equities.put(hand, new Equity());
		}
		
		// seed our RNG using the input hands for output continuity
		long seed = 31L;
		for (Hand hand : hands) {
			seed *= hand.hashCode();
		}
		Random random = new Random(seed);
		
		// begin simulating boards and sampling results
		for (int i = 0; i < sampleSize; i++) {
			
			// take our hands and board from a randomized deck
			Deck deck = deck().shuffle(random);
			for (Hand hand : hands) {
				deck.pop(hand);
			}
			Board board = Boards.river(deck);
			
			// rank our hands in order for the sample
			List<Hand> best = new LinkedList<>();
			int rank = Integer.MAX_VALUE;
			for (Hand hand : hands) {
				int r = evaluator.rank(hand, board);
				if (r < rank) {
					best.clear();
					best.add(hand);
					rank = r;
				} else if (r == rank) {
					best.add(hand);
				}
			}
			
			// apply our sample to our equity map
			for (Hand hand : hands) {
				if (!best.contains(hand)) {
					equities.get(hand).addLoss();
				}
			}
			if (best.size() > 1) {
				for (Hand hand : best) {
					equities.get(hand).addSplit();
				}
			} else {
				equities.get(best.get(0)).addWin();
			}
		}
		return Collections.unmodifiableMap(equities);
	}
	
	public Map<Hand, Equity> calculate(Board board, Hand... hands) {
		return null; // TODO
	}
	
	public Map<HandGroup, Equity> calculate(HandGroup... groups) {
		return null; // TODO
	}
	
	public Map<HandGroup, Equity> calculate(Board board, HandGroup... groups) {
		return null; // TODO
	}
	
	/**
	 * Sets the number of boards to simulate for equity calculations. By default
	 * this value is specified by <code>DEFAULT_SAMPLE_SIZE</code>.
	 * 
	 * @param sampleSize
	 *            The number of boards to simulate for equity calculations.
	 * @return the EquityCalculator instance, for chaining.
	 */
	public EquityCalculator setUseSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
		return this;
	}
	
	/**
	 * Sets the evaluator to be used to evaluate hand values during simulations.
	 * By default this value is specified by <code>DEFAULT_EVALUATOR</code>.
	 * 
	 * @param evaluator
	 *            The evaluator to be used to evaluate hands during simulations.
	 * @return The EquityCalculator instance, for chaining.
	 */
	public EquityCalculator setUseEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
		return this;
	}

	/**
	 * Represents a hand's equity in a pot. With decimals representing win, loss,
	 * and split pot rates.
	 */
	public class Equity {

		private double win = 0.0, lose = 0.0, split = 0.0;
		
		private Equity() { }

		private void addWin() {
			this.win += (1.0 / sampleSize);
		}
		
		private void addLoss() {
			this.lose += (1.0 / sampleSize);
		}

		private void addSplit() {
			this.split += (1.0 / sampleSize);
		}

		/**
		 * Obtains how often the hand or hand group associated with this equity
		 * will win, as a decimal.
		 * 
		 * @return How often the hand or hand group associated with this equity
		 *         will win, as a decimal.
		 */
		public double win() {
			return win;
		}

		/**
		 * Obtains how often the hand or hand group associated with this equity
		 * will lose, as a decimal.
		 * 
		 * @return How often the hand or hand group associated with this equity
		 *         will lose, as a decimal.
		 */
		public double lose() {
			return lose;
		}

		/**
		 * Obtains how often the hand or hand group associated with this equity
		 * will split the pot, as a decimal.
		 * 
		 * @return How often the hand or hand group associated with this equity
		 *         will split the pot, as a decimal.
		 */
		public double split() {
			return split;
		}
		
		@Override
		public String toString() {
			return new StringBuilder().append("[win=").append(win).append(" lose=")
					.append(lose).append(" split=").append(split).append("]")
					.toString();
		}

	}
	
}
