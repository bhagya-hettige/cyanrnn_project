/**   
 * @package	com.kingwang.netattrnn
 * @File		CyanRNNWithOnlyTimeAndTimeTest.java
 * @Crtdate	Feb 9, 2017
 *
 * Copyright (c) 2017 by <a href="mailto:wangyongqing.casia@gmail.com">King Wang</a>.   
 */
package com.kingwang.netattrnn;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.junit.Before;
import org.junit.Test;

import com.kingwang.netattrnn.cells.impl.AttentionWithCov;
import com.kingwang.netattrnn.cells.impl.GRUWithCov;
import com.kingwang.netattrnn.cells.impl.InputLayerWithCov;
import com.kingwang.netattrnn.cells.impl.OutputLayerWithHSoftmax;
import com.kingwang.netattrnn.cons.AlgConsHSoftmax;
import com.kingwang.netattrnn.cons.MultiThreadCons;
import com.kingwang.netattrnn.utils.MatIniter;
import com.kingwang.netattrnn.utils.MatIniter.Type;

/**
 *
 * @author King Wang
 * 
 * Feb 9, 2017 12:48:29 AM
 * @version 1.0
 */
public class CyanRNNWithCovTest {
	private int inDynSize;
	private int inFixedSize;
	private int outSize; 
	private int outoutSize;
	private int covSize;
	private int nodeSize;
	private int[] clsRcd;
	private InputLayerWithCov input;
	private GRUWithCov gru;
	private AttentionWithCov att;
	private OutputLayerWithHSoftmax output;
	private Map<String, DoubleMatrix> nodeCode;
	private Map<String, DoubleMatrix> acts = new HashMap<>();
	private List<Double> y1_arr = new ArrayList<>();
	private List<Double> y0_arr = new ArrayList<>();
	
	@Before
	public void setCell() {
		// set basic parameters
		inDynSize = 10;
		inFixedSize = 1;
		outSize = 8;
		outoutSize = 7;
		covSize = 3;
		nodeSize = 5;
		clsRcd = new int[2];
		clsRcd[0] = 0;
		clsRcd[1] = 1;
		AlgConsHSoftmax.cNum = 2;
		AlgConsHSoftmax.nodeSizeInCls = new int[2];
		AlgConsHSoftmax.nodeSizeInCls[0] = 2;
		AlgConsHSoftmax.nodeSizeInCls[1] = 3;
		AlgConsHSoftmax.biasInitVal = 0;
		
		AlgConsHSoftmax.rnnType="gru";
		input = new InputLayerWithCov(nodeSize, inDynSize, new MatIniter(Type.Test, 0, 0, 0));
        att = new AttentionWithCov(inDynSize, inFixedSize, outSize, outoutSize
        							, covSize, new MatIniter(Type.Test, 0, 0, 0));
		gru = new GRUWithCov(inDynSize, inFixedSize, outSize, new MatIniter(Type.Test, 0, 0, 0)); // set cell
		output = new OutputLayerWithHSoftmax(inDynSize, inFixedSize, outSize, outoutSize
									, AlgConsHSoftmax.cNum, new MatIniter(Type.Test, 0, 0, 0));
		
		DoubleMatrix Wx = new DoubleMatrix(nodeSize, inDynSize);
		nodeCode = new HashMap<>();
		DoubleMatrix code = new DoubleMatrix(1, nodeSize);
		nodeCode.put("0", code.put(0, 1));
		code = new DoubleMatrix(1, nodeSize);
		nodeCode.put("1", code.put(1, 1));
		code = new DoubleMatrix(1, nodeSize);
		nodeCode.put("2", code.put(2, 1));
	}
	
	/**
	 * Test method for {@link com.kingwang.ctsrnn.rnn.impl.LSTM.lstm.Cell#active(int, java.util.Map)}.
	 */
	@Test
	public void testActive() {
		// settings
		
		System.out.println("active function test");
//		DoubleMatrix code = nodeCode.get("1");
		DoubleMatrix code = new DoubleMatrix(1);
		code.put(0, 1);
		acts.put("code"+1, code);
		input.Wx.put(1, 2, 1);
		acts.put("fixedFeat"+1, DoubleMatrix.ones(1));
		
		DoubleMatrix prevS = DoubleMatrix.ones(1, outSize);
		acts.put("s"+0, prevS.mul(0.1));
		
		acts.put("r" + 0, DoubleMatrix.ones(1, 8).mul(0.1));
        acts.put("z" + 0, DoubleMatrix.ones(1, 8).mul(0.2));
        acts.put("gh" + 0, DoubleMatrix.ones(1, 8).mul(0.3));
        acts.put("h" + 0, DoubleMatrix.ones(1, 8).mul(0.1));
        
        input.active(1, acts);
        
		gru.active(1, acts);
		System.out.println(Math.pow(1+Math.exp(-.45), -1)+","+acts.get("r"+1).get(1, 2));
		assertEquals(Math.pow(1+Math.exp(-.45), -1), acts.get("r"+1).get(1, 2), 10e-3);
		System.out.println(Math.pow(1+Math.exp(-.45), -1)+","+acts.get("z"+1).get(1, 2));
		assertEquals(Math.pow(1+Math.exp(-.45), -1), acts.get("z"+1).get(1, 2), 10e-3);
		System.out.println(Math.tanh(.29+.16*Math.pow(1+Math.exp(-.45), -1))
							+","+acts.get("gh"+1).get(1, 2));
		assertEquals(Math.tanh(.29+.16*Math.pow(1+Math.exp(-.45), -1)), acts.get("gh"+1).get(1, 2), 10e-3);
		double z = Math.pow(1+Math.exp(-.45), -1);
		double gh = Math.tanh(.29+.16*Math.pow(1+Math.exp(-.45), -1));
		double h1 = z*.1+(1-z)*gh;
		System.out.println(h1);
		assertEquals(h1, acts.get("h"+1).get(1,2), 10e-3);
	}
	
	private void calcOneTurn(List<String> nodes) {
		
		for (int t=0; t<nodes.size()-1; t++) {
	    	String ndId = nodes.get(t);
	    	String nxtNdId = nodes.get(t+1);
	    	int nxtNdIdx = -1;
	
	    	DoubleMatrix fixedFeat = DoubleMatrix.zeros(1, inFixedSize);
			fixedFeat.put(0, 1);
	    	
			DoubleMatrix code = new DoubleMatrix(1);
			code.put(0, Double.parseDouble(ndId));
	    	acts.put("code"+t, code);
	    	acts.put("fixedFeat"+t, fixedFeat);
	    	
	    	int nodeCls = clsRcd[0];
	    	
	    	input.active(t, acts);
	        gru.active(t, acts);
	        att.active(t, acts);
	        output.active(t, acts, nodeCls);
	       
	        DoubleMatrix y = new DoubleMatrix(1, AlgConsHSoftmax.nodeSizeInCls[nodeCls]);
	        if(nxtNdId.equalsIgnoreCase("1")) {
	        	nxtNdIdx = 0;
	        }
	        if(nxtNdId.equalsIgnoreCase("0")) {
	        	nxtNdIdx = 1;
	        }
	        y.put(nxtNdIdx, 1);
	        DoubleMatrix cls = new DoubleMatrix(1, AlgConsHSoftmax.cNum);
	        cls.put(nodeCls, 1);
	        acts.put("y" + t, y);
	        acts.put("cls"+t, cls);
		}
	}
	
	private void gradientTestAndretActualGradient(List<String> nodes, DoubleMatrix mat
													, int reviseLoc, int targetT, double delta) {
		
		DoubleMatrix tmList = acts.get("tmList");
		
		y0_arr = new ArrayList<>();
		y1_arr = new ArrayList<>();
		
		mat = mat.put(reviseLoc, mat.get(reviseLoc)-delta); // reset Wxi
		calcOneTurn(nodes);
		for(int t=0; t<targetT+1; t++) {
			String nxtNdId = nodes.get(t+1);
			double tmGap = tmList.get(t);
	    	int nxtNdIdx = -1;
	    	int nodeCls = clsRcd[0];
	    	if(nxtNdId.equalsIgnoreCase("1")) {
	        	nxtNdIdx = 0;
	        }
	        if(nxtNdId.equalsIgnoreCase("0")) {
	        	nxtNdIdx = 1;
	        }
	        
			DoubleMatrix py = acts.get("py" + t);
			DoubleMatrix pc = acts.get("pc" + t);
			
			DoubleMatrix decD = acts.get("decD"+t);
	        DoubleMatrix lambda = MatrixFunctions.exp(decD);
	        double logft = decD.add(output.Wd.mul(tmGap))
								.add(MatrixFunctions.pow(output.Wd, -1).mul(lambda.mul(-1))
										.mul(MatrixFunctions.exp(output.Wd.mul(tmGap)).sub(1))).get(0);
			y1_arr.add(Math.log(py.get(nxtNdIdx))+Math.log(pc.get(nodeCls))+logft);
		}
		//original
		mat = mat.put(reviseLoc, mat.get(reviseLoc)+2*delta);
		calcOneTurn(nodes);
		for(int t=0; t<targetT+1; t++) {
			String nxtNdId = nodes.get(t+1);
			double tmGap = tmList.get(t);
	    	int nxtNdIdx = -1;
	    	int nodeCls = clsRcd[0];
	    	if(nxtNdId.equalsIgnoreCase("1")) {
	        	nxtNdIdx = 0;
	        }
	        if(nxtNdId.equalsIgnoreCase("0")) {
	        	nxtNdIdx = 1;
	        }
			DoubleMatrix py = acts.get("py" + t);
			DoubleMatrix pc = acts.get("pc" + t);
			
			DoubleMatrix decD = acts.get("decD"+t);
	        DoubleMatrix lambda = MatrixFunctions.exp(decD);
	        double logft = decD.add(output.Wd.mul(tmGap))
								.add(MatrixFunctions.pow(output.Wd, -1).mul(lambda.mul(-1))
										.mul(MatrixFunctions.exp(output.Wd.mul(tmGap)).sub(1))).get(0);
			y0_arr.add(Math.log(py.get(nxtNdIdx))+Math.log(pc.get(nodeCls))+logft);
		}
		 
		// test
		mat = mat.put(reviseLoc, mat.get(reviseLoc)-delta); // set back to the original Wxi
		calcOneTurn(nodes);
		output.bptt(acts, targetT);
		att.bptt(acts, targetT, output);
		gru.bptt(acts, targetT, att);
		input.bptt(acts, targetT, output, att, gru);
	}
	
	/**
	 * Test method for {@link com.kingwang.ctsrnn.rnn.impl.LSTM.lstm.Cell#bptt(java.util.List, java.util.Map, java.util.Map, int, double)}.
	 */
	@Test
	public void testBptt() {
		
		inDynSize = 3;
		inFixedSize = 1;
		outSize = 2;
		outoutSize = 3;
		covSize = 4;
		nodeSize = 5;
		
		double delta = 10e-7;
		
		// set input
		List<String> ndList = new ArrayList<>();
		ndList.add("2");
		ndList.add("1");
		ndList.add("0");
		ndList.add("1");
		
		// set tmList
		DoubleMatrix tmList = new DoubleMatrix(3);
		tmList.put(0, 2.);
		tmList.put(1, 3.);
		tmList.put(2, 4.);
		acts.put("tmList", tmList);
		
		DoubleMatrix fixedFeat = DoubleMatrix.zeros(1, inFixedSize);
		fixedFeat.put(0, 1);
		
		MatIniter initer = new MatIniter(Type.Uniform, 1, 0, 0);
		
		input = new InputLayerWithCov(nodeSize, inDynSize, initer);
		gru = new GRUWithCov(inDynSize, inFixedSize, outSize, initer);
		att = new AttentionWithCov(inDynSize, inFixedSize, outSize, outoutSize, covSize, initer);
		output = new OutputLayerWithHSoftmax(inDynSize, inFixedSize, outSize, outoutSize, AlgConsHSoftmax.cNum, initer);
		int reviseLoc = 1;
		int targetT = 2;
		
		int attRevLoc=0;
		
		AlgConsHSoftmax.windowSize = 2;
		
		/**
		 * Wxd
		 */
		System.out.println("Wxd test");
		gradientTestAndretActualGradient(ndList, output.Wxd, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWxd_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWxd_2 += tmp/2/delta;
		}
		System.out.println(deltaWxd_2+","+(-acts.get("dWxd").get(reviseLoc)));
		assertEquals(deltaWxd_2, -acts.get("dWxd").get(reviseLoc), 10e-7);
		
		/**
		 * Wtd
		 */
		System.out.println("Wtd test");
		gradientTestAndretActualGradient(ndList, output.Wtd, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWtd_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWtd_2 += tmp/2/delta;
		}
		System.out.println(deltaWtd_2+","+(-acts.get("dWtd").get(reviseLoc)));
		assertEquals(deltaWtd_2, -acts.get("dWtd").get(reviseLoc), 10e-7);
		
		/**
		 * bd
		 */
		System.out.println("bd test");
		gradientTestAndretActualGradient(ndList, output.bd, 0, targetT, delta);
		// get the actual partial y/partial x
		double deltabd_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltabd_2 += tmp/2/delta;
		}
		System.out.println(deltabd_2+","+(-acts.get("dbd").get(0)));
		assertEquals(deltabd_2, -acts.get("dbd").get(0), 10e-7);
		
		/**
		 * Wd
		 */
		System.out.println("Wd test");
		gradientTestAndretActualGradient(ndList, output.Wd, 0, targetT, delta);
		// get the actual partial y/partial x
		double deltaWd_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWd_2 += tmp/2/delta;
		}
		System.out.println(deltaWd_2+","+(-acts.get("dWd").get(0)));
		assertEquals(deltaWd_2, -acts.get("dWd").get(0), 10e-7);
		
		/**
		 * Wav
		 */
		System.out.println("Wav test");
		gradientTestAndretActualGradient(ndList, att.Wav, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWav_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWav_2 += tmp/2/delta;
		}
		System.out.println(deltaWav_2+","+(-acts.get("dWav").get(reviseLoc)));
		assertEquals(deltaWav_2, -acts.get("dWav").get(reviseLoc), 10e-7);
		
		/**
		 * Wvv
		 */
		System.out.println("Wvv test");
		gradientTestAndretActualGradient(ndList, att.Wvv, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWvv_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWvv_2 += tmp/2/delta;
		}
		System.out.println(deltaWvv_2+","+(-acts.get("dWvv").get(reviseLoc)));
		assertEquals(deltaWvv_2, -acts.get("dWvv").get(reviseLoc), 10e-7);
		
		/**
		 * Whv
		 */
		System.out.println("Whv test");
		gradientTestAndretActualGradient(ndList, att.Whv, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWhv_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWhv_2 += tmp/2/delta;
		}
		System.out.println(deltaWhv_2+","+(-acts.get("dWhv").get(reviseLoc)));
		assertEquals(deltaWhv_2, -acts.get("dWhv").get(reviseLoc), 10e-7);
		
		/**
		 * Wtv
		 */
		System.out.println("Wtv test");
		gradientTestAndretActualGradient(ndList, att.Wtv, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWtv_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWtv_2 += tmp/2/delta;
		}
		System.out.println(deltaWtv_2+","+(-acts.get("dWtv").get(reviseLoc)));
		assertEquals(deltaWtv_2, -acts.get("dWtv").get(reviseLoc), 10e-7);
		
		/**
		 * bv
		 */
		System.out.println("bv test");
		gradientTestAndretActualGradient(ndList, att.bv, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltabv_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltabv_2 += tmp/2/delta;
		}
		System.out.println(deltabv_2+","+(-acts.get("dbv").get(reviseLoc)));
		assertEquals(deltabv_2, -acts.get("dbv").get(reviseLoc), 10e-7);
		
		/**
		 * V
		 */
		System.out.println("V test");
		gradientTestAndretActualGradient(ndList, att.V, attRevLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaV_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaV_2 += tmp/2/delta;
		}
		System.out.println(deltaV_2+","+(-acts.get("dV").get(attRevLoc)));
		assertEquals(deltaV_2, -acts.get("dV").get(attRevLoc), 10e-7);
		
		/**
		 * W
		 */
		System.out.println("W test");
		gradientTestAndretActualGradient(ndList, att.W, attRevLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaW_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaW_2 += tmp/2/delta;
		}
		System.out.println(deltaW_2+","+(-acts.get("dW").get(attRevLoc)));
		assertEquals(deltaW_2, -acts.get("dW").get(attRevLoc), 10e-7);
		
		/**
		 * U
		 */
		System.out.println("U test");
		gradientTestAndretActualGradient(ndList, att.U, attRevLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaU_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaU_2 += tmp/2/delta;
		}
		System.out.println(deltaU_2+","+(-acts.get("dU").get(attRevLoc)));
		assertEquals(deltaU_2, -acts.get("dU").get(attRevLoc), 10e-7);
		
		/**
		 * Z
		 */
		System.out.println("Z test");
		gradientTestAndretActualGradient(ndList, att.Z, attRevLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaZ_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaZ_2 += tmp/2/delta;
		}
		System.out.println(deltaZ_2+","+(-acts.get("dZ").get(attRevLoc)));
		assertEquals(deltaZ_2, -acts.get("dZ").get(attRevLoc), 10e-7);
		
		/**
		 * bs
		 */
		System.out.println("bs test");
		gradientTestAndretActualGradient(ndList, att.bs, attRevLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltabs_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltabs_2 += tmp/2/delta;
		}
		System.out.println(deltabs_2+","+(-acts.get("dbs").get(attRevLoc)));
		assertEquals(deltabs_2, -acts.get("dbs").get(attRevLoc), 10e-7);
		
		/**
		 * Wxr
		 */
		System.out.println("Wxr test");
		gradientTestAndretActualGradient(ndList, gru.Wxr, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWxr_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWxr_2 += tmp/2/delta;
		}
		System.out.println(deltaWxr_2+","+(-acts.get("dWxr").get(reviseLoc)));
		assertEquals(deltaWxr_2, -acts.get("dWxr").get(reviseLoc), 10e-7);
		
		/**
		 * Wdr
		 */
		reviseLoc = 0;
		System.out.println("Wdr test");
		gradientTestAndretActualGradient(ndList, gru.Wdr, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWdr_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWdr_2 += tmp/2/delta;
		}
		System.out.println(deltaWdr_2+","+(-acts.get("dWdr").get(reviseLoc)));
		assertEquals(deltaWdr_2, -acts.get("dWdr").get(reviseLoc), 10e-7);
		
		/**
		 * Whr
		 */
		reviseLoc = 0;
		System.out.println("Whr test");
		gradientTestAndretActualGradient(ndList, gru.Whr, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWhr_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWhr_2 += tmp/2/delta;
		}
		System.out.println(deltaWhr_2+","+(-acts.get("dWhr").get(reviseLoc)));
		assertEquals(deltaWhr_2, -acts.get("dWhr").get(reviseLoc), 10e-7);
		
		/**
		 * br
		 */
		reviseLoc = 0;
		System.out.println("br test");
		gradientTestAndretActualGradient(ndList, gru.br, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltabr_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltabr_2 += tmp/2/delta;
		}
		System.out.println(deltabr_2+","+(-acts.get("dbr").get(reviseLoc)));
		assertEquals(deltabr_2, -acts.get("dbr").get(reviseLoc), 10e-7);
		
		/**
		 * Wxh
		 */
		System.out.println("Wxh test");
		gradientTestAndretActualGradient(ndList, gru.Wxh, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWxh_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWxh_2 += tmp/2/delta;
		}
		System.out.println(deltaWxh_2+","+(-acts.get("dWxh").get(reviseLoc)));
		assertEquals(deltaWxh_2, -acts.get("dWxh").get(reviseLoc), 10e-7);
		
		/**
		 * Wdh
		 */
		System.out.println("Wdh test");
		gradientTestAndretActualGradient(ndList, gru.Wdh, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWdh_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWdh_2 += tmp/2/delta;
		}
		System.out.println(deltaWdh_2+","+(-acts.get("dWdh").get(reviseLoc)));
		assertEquals(deltaWdh_2, -acts.get("dWdh").get(reviseLoc), 10e-7);
		
		/***
		 * Whh
		 */
		System.out.println("Whh test");
		gradientTestAndretActualGradient(ndList, gru.Whh, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWhh_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWhh_2 += tmp/2/delta;
		}
		System.out.println(deltaWhh_2+","+(-acts.get("dWhh").get(reviseLoc)));
		assertEquals(deltaWhh_2, -acts.get("dWhh").get(reviseLoc), 10e-7);
		
		/**
		 * bh
		 */
		System.out.println("bh test");
		gradientTestAndretActualGradient(ndList, gru.bh, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltabh_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltabh_2 += tmp/2/delta;
		}
		System.out.println(deltabh_2+","+(-acts.get("dbh").get(reviseLoc)));
		assertEquals(deltabh_2, -acts.get("dbh").get(reviseLoc), 10e-7);
		
		/***
		 * Wxz
		 */
		System.out.println("Wxz test");
		gradientTestAndretActualGradient(ndList, gru.Wxz, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWxz_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWxz_2 += tmp/2/delta;
		}
		System.out.println(deltaWxz_2+","+(-acts.get("dWxz").get(reviseLoc)));
		assertEquals(deltaWxz_2, -acts.get("dWxz").get(reviseLoc), 10e-7);
		
		/***
		 * Wdz
		 */
		System.out.println("Wdz test");
		gradientTestAndretActualGradient(ndList, gru.Wdz, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWdz_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWdz_2 += tmp/2/delta;
		}
		System.out.println(deltaWdz_2+","+(-acts.get("dWdz").get(reviseLoc)));
		assertEquals(deltaWdz_2, -acts.get("dWdz").get(reviseLoc), 10e-7);
		
		/***
		 * Whz
		 */
		System.out.println("Whz test");
		gradientTestAndretActualGradient(ndList, gru.Whz, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWhz_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWhz_2 += tmp/2/delta;
		}
		System.out.println(deltaWhz_2+","+(-acts.get("dWhz").get(reviseLoc)));
		assertEquals(deltaWhz_2, -acts.get("dWhz").get(reviseLoc), 10e-7);
		
		/**
		 * bz
		 */
		System.out.println("bz test");
		gradientTestAndretActualGradient(ndList, gru.bz, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltabz_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltabz_2 += tmp/2/delta;
		}
		System.out.println(deltabz_2+","+(-acts.get("dbz").get(reviseLoc)));
		assertEquals(deltabz_2, -acts.get("dbz").get(reviseLoc), 10e-7);
		
		/**
		 * Wxt
		 */
		System.out.println("Wxt test");
		gradientTestAndretActualGradient(ndList, att.Wxt, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWxt = 0;
		for(int t=0; t<targetT+1; t++) {
			deltaWxt += y0_arr.get(t)-y1_arr.get(t);
		}
		deltaWxt = deltaWxt/2/delta;
		System.out.println(deltaWxt+","+(-acts.get("dWxt").get(reviseLoc)));
		assertEquals(deltaWxt, -acts.get("dWxt").get(reviseLoc), 10e-7);
		
		/**
		 * Wdt
		 */
		System.out.println("Wdt test");
		gradientTestAndretActualGradient(ndList, att.Wdt, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWdt = 0;
		for(int t=0; t<targetT+1; t++) {
			deltaWdt += y0_arr.get(t)-y1_arr.get(t);
		}
		deltaWdt = deltaWdt/2/delta;
		System.out.println(deltaWdt+","+(-acts.get("dWdt").get(reviseLoc)));
		assertEquals(deltaWdt, -acts.get("dWdt").get(reviseLoc), 10e-7);
		
		/**
		 * Wtt
		 */
		System.out.println("Wtt test");
		gradientTestAndretActualGradient(ndList, att.Wtt, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWtt = 0;
		for(int t=0; t<targetT+1; t++) {
			deltaWtt += y0_arr.get(t)-y1_arr.get(t);
		}
		deltaWtt = deltaWtt/2/delta;
		System.out.println(deltaWtt+","+(-acts.get("dWtt").get(reviseLoc)));
		assertEquals(deltaWtt, -acts.get("dWtt").get(reviseLoc), 10e-7);
		
		/**
		 * Wst
		 */
		System.out.println("Wst test");
		gradientTestAndretActualGradient(ndList, att.Wst, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWst = 0;
		for(int t=0; t<targetT+1; t++) {
			deltaWst += y0_arr.get(t)-y1_arr.get(t);
		}
		deltaWst = deltaWst/2/delta;
		System.out.println(deltaWst+","+(-acts.get("dWst").get(reviseLoc)));
		assertEquals(deltaWst, -acts.get("dWst").get(reviseLoc), 10e-7);
		
		/**
		 * bt
		 */
		System.out.println("bt test");
		gradientTestAndretActualGradient(ndList, att.bt, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltabt = 0;
		for(int t=0; t<targetT+1; t++) {
			deltabt += y0_arr.get(t)-y1_arr.get(t);
		}
		deltabt = deltabt/2/delta;
		System.out.println(deltabt+","+(-acts.get("dbt").get(reviseLoc)));
		assertEquals(deltabt, -acts.get("dbt").get(reviseLoc), 10e-7);
		
		/**
		 * Wx
		 */
		System.out.println("Wx test");
		reviseLoc = 2;
		gradientTestAndretActualGradient(ndList, input.Wx, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltaWx_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltaWx_2 += tmp/2/delta;
		}
		System.out.println(deltaWx_2+","+(-acts.get("dWx").get(reviseLoc)));
		assertEquals(deltaWx_2, -acts.get("dWx").get(reviseLoc), 10e-7);
		
		/**
		 * bx
		 */
		System.out.println("bx test");
		gradientTestAndretActualGradient(ndList, input.bx, reviseLoc, targetT, delta);
		// get the actual partial y/partial x
		double deltabx_2 = 0;
		for(int t=0; t<targetT+1; t++) {
			double tmp = y0_arr.get(t)-y1_arr.get(t);
			deltabx_2 += tmp/2/delta;
		}
		System.out.println(deltabx_2+","+(-acts.get("dbx").get(reviseLoc)));
		assertEquals(deltabx_2, -acts.get("dbx").get(reviseLoc), 10e-7);
	}
}
