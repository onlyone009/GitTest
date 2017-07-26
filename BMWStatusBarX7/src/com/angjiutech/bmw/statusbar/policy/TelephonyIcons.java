/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.angjiutech.bmw.statusbar.policy;

import android.content.Context;
import com.angjiutech.bmw.statusbar.util.ToolUtils;


class TelephonyIcons {
    //***** Signal strength icons
	private static int[][] mArrTelephonySignalStrength = null;
	
	//GSM/UMTS
	static int[][] getTelephonySignalStrength(Context mContext){
		if(mArrTelephonySignalStrength == null){
			mArrTelephonySignalStrength = new int[][]{
			        { ToolUtils.getResourceId(mContext, "status_sim_signal_0","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_1","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_2","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_3","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_4","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_5","drawable")},
			          { ToolUtils.getResourceId(mContext, "status_sim_signal_0","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_1","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_2","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_3","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_4","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_5","drawable")}
			      };
		}
		 return mArrTelephonySignalStrength;
	}

	
	private static int[][] mArrTelephonySignalStrengthRoaming = null;
	
	static int[][] getTelephonySignalStrengthRoaming(Context mContext){
		if(mArrTelephonySignalStrengthRoaming == null){
			mArrTelephonySignalStrengthRoaming = new int[][]{
			    	{   ToolUtils.getResourceId(mContext, "status_sim_signal_0","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_1","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_2","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_3","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_4","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_5","drawable")},
			        {   ToolUtils.getResourceId(mContext, "status_sim_signal_0","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_1","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_2","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_3","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_4","drawable"),
			            ToolUtils.getResourceId(mContext, "status_sim_signal_5","drawable")}
			    };
		}
		return mArrTelephonySignalStrengthRoaming;
	}

	static final int[][] getDataSignalStrength(Context context){
		return getTelephonySignalStrength(context);
	}

	//***** Data connection icons
	//GSM/UMTS
	private static int[][] mArrDataG = null;
    static final int[][] getDataG(Context mContext){
    	if(mArrDataG == null){
    		mArrDataG = new int[][]{
                    { ToolUtils.getResourceId(mContext, "status_conn_g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_g","drawable") },
                    { ToolUtils.getResourceId(mContext, "status_conn_g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_g","drawable") }
                };
    	}
    	return mArrDataG;
    }

    private static int[][] mArrData3G = null;
    static final int[][] getData3G(Context mContext){
    	if(mArrData3G == null){
    		mArrData3G = new int[][]{
                    { ToolUtils.getResourceId(mContext, "status_sim_3g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_3g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_3g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_3g","drawable") },
                    { ToolUtils.getResourceId(mContext, "status_sim_3g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_3g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_3g","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_3g","drawable") }
                };
    	}
    	return mArrData3G;
    }

    private static int[][] mArrDataE = null;
    static final int[][] getDataE(Context mContext){
    	if(mArrDataE == null){
    		mArrDataE = new int[][]{
                    { ToolUtils.getResourceId(mContext, "status_conn_e","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_e","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_e","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_e","drawable") },
                    { ToolUtils.getResourceId(mContext, "status_conn_e","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_e","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_e","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_e","drawable") }
             };
    	}
    	return mArrDataE;
    }

    //3.5G
    private static int[][] mArrDataH = null;
    static final int[][] getDataH(Context mContext){
    	if(mArrDataH == null){
    		mArrDataH = new int[][]{
                    { ToolUtils.getResourceId(mContext, "status_conn_h","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_h","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_h","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_h","drawable") },
                    { ToolUtils.getResourceId(mContext, "status_conn_h","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_h","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_h","drawable"),
                      ToolUtils.getResourceId(mContext, "status_conn_h","drawable") }
            };
    	}
    	return mArrDataH;
    }

    //CDMA
    private static int[][] mArrData1X = null;
    static final int[][] getData1X(Context mContext){
    	if(mArrData1X == null){
    		mArrData1X = new int[][]{
                    { ToolUtils.getResourceId(mContext, "status_sim_1x","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_1x","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_1x","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_1x","drawable") },
                    { ToolUtils.getResourceId(mContext, "status_sim_1x","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_1x","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_1x","drawable"),
                      ToolUtils.getResourceId(mContext, "status_sim_1x","drawable") }
            };
    	}
    	return mArrData1X;
    }
    
    
    // LTE and eHRPD
    private static int[][] mArrData4G = null;
    static final int[][] getData4G(Context mContext){
    	if(mArrData4G == null){
    		mArrData4G = new int[][]{
        			{ ToolUtils.getResourceId(mContext, "status_conn_signal_in","drawable"),
                         ToolUtils.getResourceId(mContext, "status_conn_signal_out","drawable"),
                         ToolUtils.getResourceId(mContext, "status_conn_signal_inout","drawable"),
                         ToolUtils.getResourceId(mContext, "status_sim_4g","drawable") },
                    { ToolUtils.getResourceId(mContext, "status_conn_signal_in","drawable"),
                         ToolUtils.getResourceId(mContext, "status_conn_signal_out","drawable"),
                         ToolUtils.getResourceId(mContext, "status_conn_signal_inout","drawable"),
                         ToolUtils.getResourceId(mContext, "status_sim_4g","drawable") }
               };
    	}
    	return mArrData4G;
    }
}

