/*
 * File: Yahtzee.java
 * ------------------
 * This program plays the Yahtzee game.
 */

import java.util.*;
import acm.io.*;
import acm.program.*;
import acm.util.*;

public class Yahtzee extends GraphicsProgram implements YahtzeeConstants {
	

	
	public void run() {
		IODialog dialog = getDialog();
		nPlayers = dialog.readInt("Enter number of players");
		playerNames = new String[nPlayers];
		for (int i = 1; i <= nPlayers; i++) {
			playerNames[i - 1] = dialog.readLine("Enter name for player " + i);
		}
		display = new YahtzeeDisplay(getGCanvas(), playerNames);
		playGame();
	}

	private void playGame() {
		scoresArray = initialiseScoresArray();
		for (int turn = 0; turn < N_SCORING_CATEGORIES; turn++) {
			for (int player = 0; player < nPlayers; player++) {
				// initial roll of the dice after player clicks the button
				display.waitForPlayerToClickRoll(player+1);
				int[] dice = rollDice();
				display.displayDice(dice);
				// subsequent two rolls of the dice after player selects dice
				for (int i = 0; i < 2; i++) {
					display.waitForPlayerToSelectDice();
					dice = reRollSelectedDice(dice);
					display.displayDice(dice);
				}
				// calculates and updates the score based on user selected category
				while (true) {
					int category = display.waitForPlayerToSelectCategory();
					if ( scoresArray[category][player] == -1 ) {
						int score = calculateScore(category,dice);						
						updateScoresArray(category,player,score);
						updateScoresDisplay();
						break;
					}
				}
			}
		}
	}
	
	/** returns a new 2 dimensional array used to keep track of the scores for each player */
	private int[][] initialiseScoresArray() {
		int[][] array = new int[N_CATEGORIES + 1][nPlayers];
		for (int i = 0; i <= N_CATEGORIES; i++) {
			for (int j = 0; j < nPlayers; j++) {
				array[i][j] = -1;
			}
		}
		return array;
	}

	/** returns a newly rolled set of dice */
	private int[] rollDice() {
		int[] dice = new int[N_DICE];
		for (int i = 0; i < N_DICE; i++){
			dice[i] = rgen.nextInt(1, 6);
		}
		return dice;
	}
	
	/** re-rolls the user selected dice */
	private int[] reRollSelectedDice(int[] dice) {
		for (int i = 0; i < N_DICE; i++) {
			if(display.isDieSelected(i)) {
				dice[i] = rgen.nextInt(1, 6);
			}
		}
		return dice;
	}

	/** calculates the score according to the user-selected category */
	private int calculateScore(int category, int[] dice) {
		int result;
		if (category <= SIXES) {
			result = calculateScoreForUpperCategory(dice, category);
		}
		else switch (category) {
			case THREE_OF_A_KIND: result = calculateScoreOfAKind(dice, 3);
				break;
			case FOUR_OF_A_KIND: result = calculateScoreOfAKind(dice, 4);
				break;
			case FULL_HOUSE: result = calculateScoreForFullHouse(dice);
				break;
			case SMALL_STRAIGHT: result = calculateScoreForStraight(dice, SMALL_STRAIGHT);
				break;
			case LARGE_STRAIGHT: result = calculateScoreForStraight(dice, LARGE_STRAIGHT);
				break;
			case YAHTZEE: result = calculateScoreOfAKind(dice, 5);	
				break;
			case CHANCE: result = sumOfDice(dice);
				break;
			default: result = 0; 
				break;
		}
		return result;
	}
	
	/** calculates the score for the upper categories */
	private int calculateScoreForUpperCategory(int[] dice, int category) {
		int score = 0;
		for (int number: dice) {
			if (number == category) {
				score += number;
			}
		}
		return score;
	}

	/** calculates the score for the straight categories */
	private int calculateScoreForStraight(int[] dice, int straightLength) {
		int numberOfSequentialDice = 0;
		Arrays.sort(dice);
		for(int i = 1; i < dice.length; i++) {
			if (dice[i] == dice[i-1] + 1)
				numberOfSequentialDice++;
		}
		if (straightLength == SMALL_STRAIGHT && numberOfSequentialDice >= 3) {
			return SMALL_STRAIGHT_SCORE_AMOUNT;
		} else if (straightLength == LARGE_STRAIGHT && numberOfSequentialDice == 4) {
			return LARGE_STRAIGHT_SCORE_AMOUNT;
		} else 
			return 0;
	}

	/** calculates the score for the "of a kind" and yahtzee categories */
	private int calculateScoreOfAKind( int[] dice, int numOfKind) {
		// get hashmap to compute counts of each kind rolled
		Map<Integer,Integer> kindCounter = createHashmapFromDiceArray(dice);
		// see if any of the dice results has the required number of a kind, return sum of dice if so
		for (int key: kindCounter.keySet()) {
			if (kindCounter.get(key) >= numOfKind ) {
				if (numOfKind == 5) {
					return YAHTZEE_SCORE_AMOUNT;
				}
				else return sumOfDice(dice);
			}
		}
		// return zero if not enough of a kind
		return 0;
	}
	
	/** calculates the score for the full house category */
	private int calculateScoreForFullHouse(int[] dice) {
		// get hashmap to compute counts of each kind rolled
		Map<Integer,Integer> kindCounter = createHashmapFromDiceArray(dice);
		if (kindCounter.containsValue(2) && kindCounter.containsValue(3)) {
			return FULL_HOUSE_SCORE_AMOUNT;
		}
		else return 0;
	}
	
	/** returns a hashmap based on the dice array, with values equal to the count of each kind (and key equal to the number rolled) */
	private Map<Integer, Integer> createHashmapFromDiceArray(int[] dice) {
		Map<Integer,Integer> kindCounter = new HashMap<Integer,Integer>();		
		for (int i = 0; i < dice.length; i++) {
			if ( kindCounter.containsKey(dice[i]) ) {
				kindCounter.put(dice[i], kindCounter.get(dice[i])+1);
			}
			else {
				kindCounter.put(dice[i],1);
			}
		}
		return kindCounter;
	}


	/** returns the sum of all the dice rolled */
	private int sumOfDice(int[] dice) {
		int sum = 0;
		for (int i = 0; i < dice.length; i++) {
			sum += dice[i];
		}
		return sum;
	}

	/** updates the scores array, including totals and upper bonus */
	private void updateScoresArray(int category, int player, int score) {
		// update individual score category
		scoresArray[category][player] = score;
		// update the player's upper total
		scoresArray[UPPER_SCORE][player] = 0;
		for (int i = ONES; i <= SIXES; i++) {
			if (scoresArray[i][player] >= 0) {
				scoresArray[UPPER_SCORE][player] += scoresArray[i][player];
			}	
		}
		// update the player's lower score
		scoresArray[LOWER_SCORE][player] = 0;
		for (int i = THREE_OF_A_KIND; i <= CHANCE; i++) {
			if (scoresArray[i][player] >= 0) {
				scoresArray[LOWER_SCORE][player] += scoresArray[i][player];
			}
		}
		// update the player's total score
		scoresArray[TOTAL][player] = ( scoresArray[UPPER_SCORE][player] + 
				scoresArray[LOWER_SCORE][player] );
		// update the player's upper bonus if necessary
		if (scoresArray[UPPER_SCORE][player] >= 63) {
			scoresArray[UPPER_BONUS][player] = UPPER_BONUS_SCORE_AMOUNT;
			scoresArray[TOTAL][player] += UPPER_BONUS_SCORE_AMOUNT;
		}
	}
	
	/** updates the scores displayed in the game */
	private void updateScoresDisplay() {
		for (int category = 0; category < scoresArray.length; category++) {
			for (int player = 0; player < scoresArray[0].length; player++) {
				int score = scoresArray[category][player];
				if( score >= 0) {
					display.updateScorecard(category, player+1, score);
				}
			}
		}
	}
	
	/* Java main method to ensure that this program starts correctly */
	public static void main(String[] args) {
		new Yahtzee().start(args);
	}
	
	
	/* Private instance variables */
	private int nPlayers;
	private String[] playerNames;
	private YahtzeeDisplay display;
	private RandomGenerator rgen = RandomGenerator.getInstance();
	private int[][] scoresArray;
	
}
