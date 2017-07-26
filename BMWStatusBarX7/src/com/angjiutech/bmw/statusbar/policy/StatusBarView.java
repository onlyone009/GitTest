package com.angjiutech.bmw.statusbar.policy;

import com.angjiutech.bmw.statusbar.util.LogUtils;
import com.angjiutech.bmw.statusbar.util.ToolUtils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author 作者 :yuanzhenpeng
 * @version 创建时间：2017-2-15 下午4:50:22
 * 类说明:
 */
public class StatusBarView extends LinearLayout{
	
	private static final String TAG = "StatusBarView";
	private Context mContext;
	
	private TextView mTimeHour;
	private TextView mTimeMin;
	private ImageView mSimType;
	private ImageView mIvSimStrength;
	private ImageView mIvWifiStrength;
	private ImageView mIvDataDirection;
	private ImageView mIvUDisk;
	private ImageView mIvGps;
	private ImageView mIvBt;
	private View rootView;
	private StatusBarNetworkController mStatusBarNetworkController;
	
	public StatusBarView(Context context) {
		super(context);
		mContext = context;
//		rootView = inflate(context, R.layout.layout_statusbar, null);
		rootView = inflate(context, ToolUtils.getResourceId(mContext, "layout_statusbar","layout"), null);
		addView(rootView);
		initView();
	}
	
	public StatusBarView(Context context, AttributeSet attrs) {
		this(context);
	}
	
	public StatusBarView(Context context, AttributeSet attrs, int defStyle) {
		this(context,attrs);
	}

	private void registerStatusBarBroadcast() {
		// TODO Auto-generated method stub
		LogUtils.d(TAG,"statusbar wifi registerNetworkControllerReceiver()");
		
		mStatusBarNetworkController = new StatusBarNetworkController(getContext());

		mStatusBarNetworkController.addTimeHourView(mTimeHour);
		mStatusBarNetworkController.addTimeMinView(mTimeMin);
		mStatusBarNetworkController.addDataTypeIconView(mSimType);
		mStatusBarNetworkController.addPhoneSignalIconView(mIvSimStrength);
		mStatusBarNetworkController.addDataDirectionOverlayIconView(mIvDataDirection);
//		mStatusBarNetworkController.addBtStateIconView(mIvBt);
		mStatusBarNetworkController.addWifiIconView(mIvWifiStrength);
		mStatusBarNetworkController.addUMediaStateIconView(mIvUDisk);
		mStatusBarNetworkController.addGpsStateIconView(mIvGps);

    	// 更新时间的广播
    	IntentFilter timeFilter = new IntentFilter();
    	timeFilter.addAction(Intent.ACTION_TIME_TICK);
    	timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
    	timeFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
    	timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

    	// 更新链接广播
    	IntentFilter networkFilter = new IntentFilter();
    	networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    	networkFilter.addAction("android.net.conn.INET_CONDITION_ACTION");
    	networkFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
    	networkFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    	networkFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    	networkFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//    	networkFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
    	networkFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
//    	
////    	statusFilter.addAction(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION);
//    	statusFilter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED");

    	IntentFilter stateFilter = new IntentFilter();
//    	stateFilter.addAction(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION);
    	stateFilter.addAction("android.net.fourG.NET_4G_STATE_CHANGED");
//    	stateFilter.addAction(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION);
    	stateFilter.addAction("android.net.wimax.SIGNAL_LEVEL_CHANGED");
//    	stateFilter.addAction(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION);
    	stateFilter.addAction("android.net.fourG.wimax.WIMAX_NETWORK_STATE_CHANGED");

    	IntentFilter sdCardFilter = new IntentFilter();
    	sdCardFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);	// 插入SD卡并且已正确安装（识别）时发出的广播
    	sdCardFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);	// 广播：扩展介质存在，但是还没有被挂载 (mount)。 
    	sdCardFilter.addDataScheme("file");

    	getContext().registerReceiver(mStatusBarNetworkController,timeFilter);
    	getContext().registerReceiver(mStatusBarNetworkController,networkFilter);
    	getContext().registerReceiver(mStatusBarNetworkController,stateFilter);
    	getContext().registerReceiver(mStatusBarNetworkController,sdCardFilter);
    	
//    	// 初始化状态栏信息
    	mStatusBarNetworkController.initAllDateState();
	}

	private void initView() {
		// TODO Auto-generated method stub
		
//		mTimeHour=(TextView) rootView.findViewById(R.id.tv_time_hour);
//		mTimeMin=(TextView) rootView.findViewById(R.id.tv_time_min);
//		mSimType = (ImageView) rootView.findViewById(R.id.iv_sim_type);
//		mIvSimStrength = (ImageView)rootView.findViewById(R.id.iv_sim_strength);
//		mIvWifiStrength = (ImageView)rootView.findViewById(R.id.iv_wifi_strength);
//		mIvDataDirection = (ImageView)rootView.findViewById(R.id.iv_data_direction);
//		mIvUDisk = (ImageView) rootView.findViewById(R.id.iv_udisk);
//		mIvGps = (ImageView) rootView.findViewById(R.id.iv_gps);
//		mIvBt = (ImageView) rootView.findViewById(R.id.iv_bt);
		
		mTimeHour=(TextView) rootView.findViewById(ToolUtils.getResourceId(mContext, "tv_time_hour", "id"));
		mTimeMin=(TextView) rootView.findViewById(ToolUtils.getResourceId(mContext, "tv_time_min", "id"));
		mSimType = (ImageView) rootView.findViewById(ToolUtils.getResourceId(mContext, "iv_sim_type","id"));
		mIvSimStrength = (ImageView)rootView.findViewById(ToolUtils.getResourceId(mContext, "iv_sim_strength","id"));
		mIvWifiStrength = (ImageView)rootView.findViewById(ToolUtils.getResourceId(mContext, "iv_wifi_strength","id"));
		mIvDataDirection = (ImageView)rootView.findViewById(ToolUtils.getResourceId(mContext, "iv_data_direction","id"));
		mIvUDisk = (ImageView) rootView.findViewById(ToolUtils.getResourceId(mContext, "iv_udisk","id"));
		mIvGps = (ImageView) rootView.findViewById(ToolUtils.getResourceId(mContext, "iv_gps","id"));
		mIvBt = (ImageView) rootView.findViewById(ToolUtils.getResourceId(mContext, "iv_bt","id"));
	}
    
	/**
	 *  当 view 加载到界面上的时候调用该方法
	 */
    @Override
    protected void onAttachedToWindow() {
    	// TODO Auto-generated method stub
    	super.onAttachedToWindow();
    	
    	registerStatusBarBroadcast();
    }
    
    /**
     *  当view 从界面上分离的时候就调用该方法
     */
    @Override
    protected void onDetachedFromWindow() {
    	// TODO Auto-generated method stub
    	super.onDetachedFromWindow();
    	
    	if(mStatusBarNetworkController != null){
    		getContext().unregisterReceiver(mStatusBarNetworkController);    		
    	}
    }
}
