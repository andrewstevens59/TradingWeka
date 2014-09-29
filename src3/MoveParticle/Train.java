package MoveParticle;

//This stores a training sample
public class Train {
	/*// This stores the volume for btc vs usd
	float vol_diff[][] = new float[4][4];
	// This stores the volume for usd vs usd
	float usd_vol_diff[][] = new float[4][4];
	// This stores the volume for btc vs btc
	float btc_vol_diff[][] = new float[4][4];
	// This stores the volume for usd 
	float price_diff[][] = new float[4][4];
	// This stores the price jump the next day
	float price_jump;
	// This sotres the current price
	float price;*/
	// This stores the buy sell volume csum difference 
	float ba_vol_diff[] = new float[4];
	// This stores the (exchange_rate - min_bid) / (max_ask - min_bid)
	float gap_rat[] = new float[8];
	// THIS stores the volatility between the upper and lower bound
	float volatility_gap[] = new float[8];
	// This stores the exchange rate difference between different currency pairs
	float exch_rate_comp[][] = new float[9][4];
	// This stores the exchange rate
	float exch_rate;
	// This stores the exchange rate jump
	float exch_rate_jump;
}
