package ParticleFilter;

//This stores a training sample
public class Train {
	// This stores the volume for btc vs usd
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
	float price;
}
