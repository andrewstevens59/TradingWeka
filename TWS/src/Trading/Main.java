package Trading;

import java.io.IOException;
import java.text.ParseException;
import java.util.Vector;

import samples.base.ComboContract;
import samples.base.FutContract;
import samples.base.OptContract;
import samples.base.SimpleWrapper;
import samples.base.StkContract;
import samples.rfq.SampleRfq;
import samples.rfq.SampleRfq.Status;

import MoveParticle.Classify;
import MoveParticle.FXData;
import MoveParticle.SampleGraphSet;

import com.ib.client.ComboLeg;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Order;
import com.ib.client.TagValue;
import com.ib.client.TickType;
import com.ib.client.UnderComp;

public class Main extends Wrapper {

	   public enum Status { None, SecDef, SecDefFMF, Rfq, Ticks, Done, Error };

	   private Object m_mutex = new Object();
	   private Status m_status = Status.None;

	   private int m_rfqId;
	   private int m_mode;

	   private Contract m_contract = null;

	   private double m_bidPrice = 0;
	   private double m_askPrice = 0;

	   private int m_bidSize = 0;
	   private int m_askSize = 0;
	   private FXData fxdata = new FXData();

	   public Main(int clientId, int rfqId, int mode) {
	      m_rfqId = rfqId;
	   }
	   
	   public void testOrder() throws Exception {

	      int clientId = 2;		
	      connect(clientId);

	      if (client() != null && client().isConnected()) {
	    	 

	    	  fxdata.ReadFXData("../RegimeSwitching/FXData");
				fxdata.GenerateTrainingSet();
				fxdata.BuildHierarchy();
				
			   for(int i=0; i<1; i++) {
			      Contract con = new Contract();
			      con.m_localSymbol = "";
			      con.m_symbol = fxdata.Currency1(i);
			      con.m_currency = fxdata.Currency2(i);
			      con.m_exchange = "IDEALPRO";
			      con.m_secType = "CASH";
		
			      int ticket = i + 1000;
			      fxpair_map.put(ticket, new FXPair(new SampleGraphSet(), 
			      	fxdata.Currency1(i), fxdata.Currency2(i), client()));
			      client().reqMktData(ticket, con, "", false);
			   }
			  
			   client().reqAccountUpdates(true, "");

	         try {

	            synchronized (m_mutex) {

	               if (client().serverVersion() < 42) {
	                  error ("Sample will not work with TWS older that 877");
	               }

	               while (m_status != Status.Done &&
	                     m_status != Status.Error) {

	                  if (m_status == Status.None) {
	                     obtainContract();
	                     if (m_status != Status.Error &&
	                         m_status != Status.SecDef) {
	                        submitRfq();
	                     }
	                  }
	                  m_mutex.wait();
	               }
	            }
	         }

	         finally {
	            disconnect();
	         } 

	         if (m_status == Status.Done) {

	            String msg = "Done, bid=" + m_bidSize + "@" + m_bidPrice +
	                              " ask=" + m_askSize + "@" + m_askPrice;

	            UnderComp underComp = m_contract.m_underComp;
	            if (underComp != null) {
	               msg += " DN: conId=" + underComp.m_conId
	                        + " price=" + underComp.m_price
	                        + " delta=" + underComp.m_delta; 
	            } 

	            consoleMsg(msg);
	         }
	      }
	   }
	   
	   private void obtainContract() {

	      switch (m_mode) {
	      case 0:
	         {
	            m_contract = new StkContract("IBM");
	            m_contract.m_currency = "EUR";
	            break;
	         }
	      case 1:
	         {
	            m_contract = new FutContract("IBM", "200809");
	            break;
	         }		
	      case 2:
	         {
	            m_contract = new OptContract("IBM", "200809", 120, "CALL");
	            break;
	         }		
	      case 3:
	         {
	            m_contract = new OptContract("Z", "LIFFE", "200809", 54.75, "CALL");
	            m_contract.m_currency = "GBP";
	            break;
	         }		
	      case 4:
	         {
	            m_contract = new ComboContract("Z", "GBP", "LIFFE");
	            m_contract.m_comboLegs = new Vector(2);
	            m_contract.m_comboLegs.setSize(2);

	            {
	               Contract l1 = new OptContract(
	                     "Z", "LIFFE", "200809", 54.75, "CALL");
	               l1.m_currency = "GBP";
	               submitSecDef(1, l1);
	            }

	            {
	               Contract l2 = new OptContract(
	                     "Z", "LIFFE", "200810", 55.00, "CALL");
	               l2.m_currency = "GBP";
	               submitSecDef(2, l2);
	            }

	            m_status = Status.SecDef;
	            break;
	         }
	      case 5:
	         {
	            m_contract = new ComboContract("IBM");
	            m_contract.m_comboLegs = new Vector(1);
	            m_contract.m_comboLegs.setSize(1);

	            m_contract.m_underComp = new UnderComp();
	            //m_contract.m_underComp.m_delta = 0.8;
	            //m_contract.m_underComp.m_price = 120;

	            {
	               Contract l1 = new OptContract("IBM", "200809", 120, "CALL");
	               submitSecDef(1, l1);
	            }

	            m_status = Status.SecDef;
	            break;
	         }
	      case 6:
	         {
	            m_contract = new ComboContract("RUT");
	            m_contract.m_comboLegs = new Vector(1);
	            m_contract.m_comboLegs.setSize(1);

	            m_contract.m_underComp = new UnderComp();
	            {
	               Contract l1 = new OptContract("RUT", "200809", 740, "CALL");
	               submitSecDef(1, l1);
	            }

	            m_status = Status.SecDef;
	            break;
	         }
	      case 7:
	         {
	            m_contract = new ComboContract("Z", "GBP", "LIFFE");
	            m_contract.m_comboLegs = new Vector(1);
	            m_contract.m_comboLegs.setSize(1);

	            m_contract.m_underComp = new UnderComp();

	            {
	               Contract l1 = new OptContract(
	                     "Z", "LIFFE", "200808", 55.00, "CALL");
	               l1.m_currency = "GBP";
	               submitSecDef(1, l1);
	            }

	            m_status = Status.SecDef;
	            break;
	         }
	      }
	   }
	   
	   private void submitSecDef(int reqId, Contract contract) {

	      consoleMsg("REQ: secDef " + reqId);

	      client().reqContractDetails(reqId, contract);
	   }

	   private void submitRfq() throws IOException, ParseException {
		   
		
	   }

	   public void error(String str) {
	      consoleMsg("Error=" + str);
	      synchronized (m_mutex) {
	         m_mutex.notify();
	      }
	   }

	   public void error(int id, int errorCode, String errorMsg) {
	      consoleMsg("Error id=" + id + " code=" + errorCode + " msg=" + errorMsg);
	      if (errorCode >= 2100 && errorCode < 2200) {
	         return;
	      }
	      synchronized (m_mutex) {
	         m_mutex.notify();
	      }
	   }

	   /* ***************************************************************
	    * Main Method
	    *****************************************************************/

	   public static void main(String[] args) throws IOException, ParseException {
			
	      try {
	         int rfqId = (int) (System.currentTimeMillis() / 1000);
	         int mode = (args.length > 0) ? Integer.parseInt(args[0]) : 0;
	         Main ut = new Main(/* clientId */ 0, rfqId, mode);
	         ut.testOrder();
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	   }
	}
