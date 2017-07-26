package com.angjiutech.bmw.statusbar.policy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import com.angjiutech.bmw.statusbar.R;
import com.angjiutech.bmw.statusbar.util.LogUtils;
import com.angjiutech.bmw.statusbar.util.ToolUtils;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.AnimationDrawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class StatusBarNetworkController extends BroadcastReceiver{

	private static final String TAG = "StatusBarNetworkController";
	
	private static final int INET_CONDITION_THRESHOLD = 50;
	
	private Context mContext;
	
	String mLastTime = "";
    int mLastPhoneSignalIconId = -1;
    int mLastCombinedSignalIconId = -1;
    int mLastDataTypeIconId = -1;
    int mLastWimaxIconId = -1;
    int mLastDataDirectionOverlayIconId = -1;
    int mLastDataDirectionIconId = -1;				// 上网数据上下传显示图标
    int mLastBluetoothTetherIconId = -1;			// 显示蓝牙
    int mLastWifiIconId = -1;						// wifi显示图标
    int mLastUMediaIconId = -1;						// U盘显示图标
    int mLastGpsStarIconId = -1;					// Gps显示图标
    int mLastBtIconId = -1;							// 蓝牙显示图标
	
    private boolean mAirplaneMode = false;

	private ConnectivityManager mConnectivityManager;
	
	// 蓝牙状态
	private static final String BT_SETTINGS_KEY= "bluetooth_setting";//蓝牙连接状态数据库
	private static final int BT_CLOSE = 0;//蓝牙关闭
	private static final int BT_OPEN = 1;//蓝牙关闭
	private int mBtIconId = 0;
	class BtContentObserver extends ContentObserver{

		public BtContentObserver(){
			super(new Handler());
		}
		
		@Override
		public void onChange(boolean selfChange) {
			// TODO Auto-generated method stub
			super.onChange(selfChange);
			updateBtState();
			refreshViews();
		}
	}
    
    // wifi
    private final WifiManager mWifiManager;
    private boolean mWifiEnabled, mWifiConnected;
    private String mWifiSsid;
    private int mWifiRssi, mWifiLevel;
    private int mWifiIconId = 0;
    private int mWifiActivityIconId = 0; // overlay arrows for wifi direction
    private boolean mDataAndWifiStacked = true;
//    int mWifiActivity = WifiManager.DATA_ACTIVITY_NONE;
    int mWifiActivity = 0x00;
    
    // telephony
//    String mNetworkName;
//    String mNetworkNameDefault;
    IccCard.State mSimState = IccCard.State.READY;
//    String mNetworkNameSeparator;
    boolean mShowPhoneRSSIForData = false;
    
    int mLastSignalLevel;
    
    private int mWimaxState = 0;
    private int mWimaxExtraState = 0;
    private boolean mHasMobileDataFeature;	// 硬件是否支持网络
    
    // 时间
    private String mTime = "";
    
    // sim卡状态
    private int mPhoneSignalIconId; 										// 电话信号强度图标id
    private int mDataSignalIconId; 											// ?sim卡信号强度
    private int mMobileActivityIconId; // overlay arrows for data direction	// sim卡数据上下传图标
    private int mInetCondition = 0;											// 表示当前网络连接状态是否好：1 : 连接状态好 ; 0 : 连接状态不好
    private boolean mIsWimaxEnabled = false;								// 是否是Wimax类型
    private boolean mWimaxConnected = false;								// Wimax类型是否连接
    private int mWimaxSignal = 0;											// Wimax信号强度
    private boolean mWimaxIdle = false;										// Wimax处在空闲状态?
    private int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;		// sim卡类型,2G,3G,4G
    private int mWimaxIconId = 0;								            // Wimax使用的图标
    PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
    	
    	 // network signal 强度改变时回调，例如：1： EMERGENCY_ONLY ； 2：IN_SERVICE ； 3：OUT_OF_SERVICE ； 4：POWER_OFF
    	 @Override
         public void onSignalStrengthsChanged(SignalStrength signalStrength) {
    		 LogUtils.d(TAG, "yzp statusbar sim onSignalStrengthsChanged signalStrength=" + signalStrength +
//                 ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
                 ((signalStrength == null) ? "" : (" level=" + ToolUtils.getLevel(signalStrength))));
             mSignalStrength = signalStrength;
             updateTelephonySignalStrength();
             refreshViews();
         }

    	 // 设备服务状态改变时回调，例如：1：EMERGENCY_ONLY ； 2：IN_SERVICE ； 3：OUT_OF_SERVICE ； 4：POWER_OFF
         @Override
         public void onServiceStateChanged(ServiceState state) {
             LogUtils.d(TAG, "yzp statusbar sim onServiceStateChanged state=" + state.getState());
             mServiceState = state;
             updateTelephonySignalStrength();
             updateDataNetType();
             updateDataIcon();
             refreshViews();
         }

         // 当设备呼叫状态改变时回调，例如：1：STATE_IDLE； 2：STATE_RINGING； 3：STATE_OFFHOOK
         @Override
         public void onCallStateChanged(int state, String incomingNumber) {
             LogUtils.d(TAG, "yzp statusbar sim onCallStateChanged state=" + state);
             // In cdma, if a voice call is made, RSSI should switch to 1x.
             if (isCdma()) {
                 updateTelephonySignalStrength();
                 refreshViews();
             }
         }

         //  当连接状态改变时回调,1：DISCONNECTED；2：CONNECTING；3：CONNECTED；4：SUSPENDED
         @Override
         public void onDataConnectionStateChanged(int state, int networkType) {
             LogUtils.d(TAG, "yzp statusbar sim onDataConnectionStateChanged: state=" + state + " type=" + networkType);
             mDataState = state;
             mDataNetType = networkType;
             updateDataNetType();
             updateDataIcon();
             refreshViews();
         }

         // 数据活动改变时触发，例如：1:ACTIVITY_NONE; 2:ACTIVITY_IN ; 3:ACTIVITY_OUT ; 4:ACTIVITY_INOUT; 5:ACTIVITY_DORMANT
         @Override
         public void onDataActivity(int direction) {
             LogUtils.d(TAG, "yzp statusbar sim onDataActivity: direction=" + direction);
             mDataActivity = direction;
             updateDataIcon();
             refreshViews();
         }
    };

    // 网络连接状态
    private int mDataTypeIconId;											// 数据类型图标
    private int mDataDirectionIconId; // data + data direction on phones	// 当前上网数据的方向图标ID
//    private int[] mDataIconList = TelephonyIcons.DATA_G[0];					// 数据上下传的图标类型,根据不同sim卡制式改变
    private int[] mDataIconList;					// 数据上下传的图标类型,根据不同sim卡制式改变
    private boolean mDataConnected; 										// 当前上网数据的图标是否可见
    private int mDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;		// 上网数据方向
    private int mDataState = TelephonyManager.DATA_DISCONNECTED;			// 上网数据状态

    // bluetooth状态
    private boolean mBluetoothTethered = false;		// 蓝牙是否连接
    private int mBluetoothTetherIconId = 0;

    // U盘状态
    int mUMediaIconId = 0;
    
    // gps状态
    private static final int GPS_AVAILABLE_STAR_NUM = 3;
    private AnimationDrawable mAnimationGps;
    private int mGpsStarNum;
	private int mGpsStarIconId ;
    private GpsStatus.Listener gpsStateListener = new GpsStatus.Listener(){
		@Override
		public void onGpsStatusChanged(int event) {
			// TODO Auto-generated method stub
			switch(event){
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					Log.d(TAG , "yzp statusbar gps onGpsStatusChanged() GPS_EVENT_SATELLITE_STATUS");
					GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
					//创建一个迭代器保存所有卫星
	                Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();  
	                //获取卫星颗数的默认最大值
	                int maxSatellites = gpsStatus.getMaxSatellites();  
	                int count = 0;
	                while (iters.hasNext() && count <= maxSatellites) {       
	                    GpsSatellite s = iters.next();
	                    if(s.usedInFix()){
	                    	count++;       	                    	
	                    }
	                }
//	                Toast.makeText(mContext, "GPS_EVENT_SATELLITE_STATUS mGpsStarNum:" + mGpsStarNum, 0).show();
	                Log.d(TAG,"gpsStateListener: available count: " + count);
	                mGpsStarNum = count;
	                updateGpsState();
	                refreshViews();
					break;
			}
		}
    };
    
    final TelephonyManager mPhone;
    private LocationManager mLocationManager;	// Gps管理者
    
    boolean mShowAtLeastThreeGees = false;
    boolean mAlwaysShowCdmaRssi = false;
    
    // telephony
    boolean mHspaDataDistinguishable;
    
    SignalStrength mSignalStrength;
    ServiceState mServiceState;
    // yuck -- stop doing this here and put it in the framework
//    IBatteryStats mBatteryStats;	// 统计电池电量
    
    private ArrayList<ImageView> mPhoneSignalIconViews = new ArrayList<ImageView>();	// 更新sim卡信号强度的控件
    private ArrayList<ImageView> mDataDirectionIconViews = new ArrayList<ImageView>();	// 更新sim卡信号类型的控件
    private ArrayList<ImageView> mCombinedSignalIconViews = new ArrayList<ImageView>();	// 混合信号类型强度,显示的也是sim卡信号强度
    private ArrayList<ImageView> mDataTypeIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mWimaxIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mDataDirectionOverlayIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mWifiIconViews = new ArrayList<ImageView>();			// 更新wifi状态
    private ArrayList<ImageView> mBtStateIconViews = new ArrayList<ImageView>();			// 更新wifi状态    
    private ArrayList<ImageView> mUMediaStateIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mGpsStateIconViews = new ArrayList<ImageView>();
    private ArrayList<SignalCluster> mSignalClusters = new ArrayList<SignalCluster>();
    private TextView mTimeHourTextView = null;
    private TextView mTimeMinTextView = null;
    
    public StatusBarNetworkController(Context context){
    	LogUtils.d(TAG,"StatusBarNetworkController()");
    	mContext = context;
    	
    	final Resources res = context.getResources();
//    	mShowAtLeastThreeGees = res.getBoolean(R.bool.config_showMin3G);
//        mHspaDataDistinguishable = res.getBoolean(R.bool.config_hspa_data_distinguishable);
//        mAlwaysShowCdmaRssi = res.getBoolean(R.bool.config_alwaysUseCdmaRssi);
//        mShowPhoneRSSIForData = res.getBoolean(R.bool.config_showPhoneRSSIForData);
        mShowAtLeastThreeGees = res.getBoolean(ToolUtils.getResourceId(mContext, "config_showMin3G","bool"));
        LogUtils.d("jarerror==null?","mShowAtLeastThreeGees == null? " + (mShowAtLeastThreeGees));
        mHspaDataDistinguishable = res.getBoolean(ToolUtils.getResourceId(mContext, "config_hspa_data_distinguishable","bool"));
        LogUtils.d("jarerror==null?","mHspaDataDistinguishable == null? " + (mHspaDataDistinguishable));
        mAlwaysShowCdmaRssi = res.getBoolean(ToolUtils.getResourceId(mContext, "config_alwaysUseCdmaRssi","bool"));
        LogUtils.d("jarerror==null?","mAlwaysShowCdmaRssi == null? " + (mAlwaysShowCdmaRssi));
        mShowPhoneRSSIForData = res.getBoolean(ToolUtils.getResourceId(mContext, "config_showPhoneRSSIForData","bool"));
        LogUtils.d("jarerror==null?","mShowPhoneRSSIForData == null? " + (mShowPhoneRSSIForData));
        
        mDataIconList = TelephonyIcons.getDataG(mContext)[0];
        LogUtils.d("jarerror==null?","mDataIconList == null? " + (mDataIconList == null));
        mGpsStarIconId = ToolUtils.getResourceId(mContext, "statusbar_gps_animation","drawable");
        LogUtils.d("jarerror==null?","mGpsStarIconId == null? " + (mGpsStarIconId));
        
        // telephony
        mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mPhone.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
              | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
              | PhoneStateListener.LISTEN_CALL_STATE
              | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
              | PhoneStateListener.LISTEN_DATA_ACTIVITY);
//        mNetworkNameSeparator = mContext.getString(R.string.status_bar_network_name_separator);
        
        mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//        Method isNetworkSupportedMethod = ReflectUtil.getMethodBypackageName("android.net.ConnectivityManager", "isNetworkSupported", int.class);
//        LogUtils.d(TAG , "-------------------yzp nullpoint:" + (isNetworkSupportedMethod == null));
//        mHasMobileDataFeature = (Boolean)ReflectUtil.invoke(isNetworkSupportedMethod, cm, ConnectivityManager.TYPE_MOBILE);
        mHasMobileDataFeature = ToolUtils.isNetworkSupported(mConnectivityManager, ConnectivityManager.TYPE_MOBILE);
//        mNetworkNameDefault = mContext.getString(R.string.lockscreen_carrier_default);
//        mNetworkName = mNetworkNameDefault;	// 用来显示网络的名字,比如中国联通,中国移动等

        // wifi
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiConnected = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        
        // gps
        mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.addGpsStatusListener(gpsStateListener);
//        mAnimationGps = (AnimationDrawable) mGpsStateIconViews.get(0).getDrawable();
//		mAnimationGps.start();
        
        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode();

        // set up the default wifi icon, used when no radios have ever appeared
        updateWifiIcons();
        updateWimaxIcons();
        
        // u盘Id
        mUMediaIconId = 0;
        
        // 注册蓝牙改变的回调
        updateBtState();
        registerBtStateObserver();
        // yuck
//        mBatteryStats = BatteryStatsService.getService();
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		LogUtils.d(TAG, "yzp statusbar wifi onReceive action: " + action);

		// 时间更新
		if(action.equals(Intent.ACTION_TIME_TICK)
				|| action.equals(Intent.ACTION_TIME_CHANGED)
				|| action.equals(Intent.ACTION_CONFIGURATION_CHANGED)
				|| action.equals(Intent.ACTION_TIMEZONE_CHANGED)){
			
//			Toast.makeText(mContext, "time show", 0).show();
			LogUtils.d(TAG, "yzp statusbar time onReceive() time change");

			updateTime();
			refreshViews();
		}

		// wifi 等网络连接状态改变
		else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)
//				|| action.equals(ConnectivityManager.INET_CONDITION_ACTION)){
				||action.equals("android.net.conn.INET_CONDITION_ACTION")){
			
//			Toast.makeText(mContext, "网络状态改变", 0).show();
			LogUtils.d(TAG , "yzp statusbar Connectivity onReceive() CONNECTIVITY_ACTION || INET_CONDITION_ACTION");
			
			updateConnectivity(intent);
			refreshViews();

		}else if(action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)){	// 接收到飞行模式改变的广播
			
//			Toast.makeText(mContext, "接收到飞行模式", 0).show();
			LogUtils.d(TAG,"yzp statusbar airplane onReceiver() ACTION_AIRPLANE_MODE_CHANGED:");

			updateAirplaneMode();
			refreshViews();

//		}else if(action.equals(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION)){
//		}else if(action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")){
////            updateNetworkName(intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
//			LogUtils.d(TAG,"yzp statusbar wifi onReceiver() SPN_STRINGS_UPDATED:");
//            updateNetworkName(intent.getBooleanExtra("showSpn", false),
////                    intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
//                    intent.getStringExtra("spn"),
////                    intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
//                    intent.getBooleanExtra("showPlmn", false),
////                    intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
//                    intent.getStringExtra("plmn"));
//            refreshViews();

//		}else if(action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION) ||	// sim卡网络状态 状态发生改变
		}else if(action.equals("android.net.fourG.NET_4G_STATE_CHANGED") ||	
//                action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||	// Wimax signal信号强度更改
                action.equals("android.net.wimax.SIGNAL_LEVEL_CHANGED") ||				
//                action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)){	// Wimax state change状态改变
				action.equals("android.net.fourG.wimax.WIMAX_NETWORK_STATE_CHANGED")){
			
//			Toast.makeText(mContext, "更新4GWimax", 0).show();
			LogUtils.d(TAG,"yzp statusbar net onReceiver() NET_4G_STATE_CHANGED || SIGNAL_LEVEL_CHANGED || WIMAX_NETWORK_STATE_CHANGED");

            updateWimaxState(intent);
            refreshViews();

//    	}else if(action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)){
		}else if(action.equals("android.intent.action.SIM_STATE_CHANGED")){		// sim卡状态改变后,更改sim状态和上网数据状态
			LogUtils.d(TAG,"yzp statusbar wifi onReceiver() SIM_STATE_CHANGED:");
			
			updateSimState(intent);
            updateDataIcon();
            refreshViews();
            
		}else if(action.equals(WifiManager.RSSI_CHANGED_ACTION)		// wifi 信号强度改变
                || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			
//			Toast.makeText(mContext, "接收到wifi改变的广播", 0).show();
			LogUtils.d(TAG , "yzp statusbar wifi onReceive() RSSI_CHANGED_ACTION || WIFI_STATE_CHANGED_ACTION || NETWORK_STATE_CHANGED_ACTION");

            updateWifiState(intent);
            refreshViews();   
            
		}else if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){	// 蓝牙连接
//			Toast.makeText(mContext, "蓝牙连接成功", 0).show();
			LogUtils.d(TAG,"yzp statusbar bt onReceive() BluetoothDevice.ACTION_ACL_CONNECTED");
			
			updateBtState(true);
			refreshViews();
		}else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){	// 蓝牙断开
//			Toast.makeText(mContext, "蓝牙断开连接", 0).show();
			LogUtils.d(TAG,"yzp statusbar bt onReceive() BluetoothDevice.ACTION_ACL_CONNECTED");
			
			updateBtState(false);
			refreshViews();
		}

		else if(action.equals(Intent.ACTION_MEDIA_MOUNTED)){ 	// 插入u盘
//			Toast.makeText(mContext, "插入u盘", 0).show();
			LogUtils.d(TAG,"yzp statusbar udisk onReceive() ACTION_MEDIA_MOUNTED");

			updateMediaState(true);
			refreshViews();
		}else if(action.equals(Intent.ACTION_MEDIA_UNMOUNTED)){	// 拔出u盘
//			Toast.makeText(mContext, "拔出u盘", 0).show();
			LogUtils.d(TAG,"yzp statusbar udisk onReceive() ACTION_MEDIA_UNMOUNTED");
			
			updateMediaState(false);
			refreshViews();
		}
	}
	
	private void updateTime(){
		long Time = System.currentTimeMillis();
		LogUtils.d(TAG , " Time:" + Time);
		Calendar calendar = Calendar.getInstance();  
		calendar.setTimeInMillis(Time); 
		int hour = calendar.get(Calendar.HOUR);
		int ispm = calendar.get(Calendar.AM_PM);
		if(ispm == Calendar.PM){
			hour = hour + 12;                    
		}
		String strHour = String.valueOf(hour);
		if(hour < 10){
			strHour = "0" + hour;
		}

		int min = calendar.get(Calendar.MINUTE);
		String strMin = String.valueOf(min);
		if(min < 10){
			strMin = "0" + min;
		}
		mTime = strHour + ":" + strMin;
		LogUtils.d(TAG , "yzp statusbar time : " + mTime);
	}
	
    private void updateConnectivity(Intent intent) {
    	LogUtils.d(TAG, "updateConnectivity ");

        NetworkInfo info = (NetworkInfo)(intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO));
//        int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);
        int connectionStatus = intent.getIntExtra("inetCondition", 0);		// “inetCondition” 表示的是连接网络的强度，0 表示没有连接，100表示一个great的连接

        LogUtils.d(TAG , "yzp statusbar updateConnectivity: networkInfo == null ?" + (info == null) + " ,connectionStatus ==  " + connectionStatus);

        mInetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);

        if (info != null && info.getType() == ConnectivityManager.TYPE_BLUETOOTH) {	// 通过蓝牙的方式连接网络
            mBluetoothTethered = info.isConnected();
        } else {
            mBluetoothTethered = false;
        }
        LogUtils.d(TAG , "yzp statusbar bluetooth updateConnectivity() mBluetoothTethered == " + mBluetoothTethered);
        
        // We want to update all the icons, all at once, for any condition change
        updateDataNetType();
        updateWimaxIcons();
        updateDataIcon();
        updateTelephonySignalStrength();
        updateWifiIcons();
    }
    
    private final void updateDataNetType() {		// 更新手机信号类型		mDataTypeIconId : 手机信号类型
        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is a special 4g network not handled by telephony
        	LogUtils.d(TAG,"updateDataNetType() mIsWimaxEnabled && mWimaxConnected");
//            mDataIconList = TelephonyIcons.DATA_4G[mInetCondition];
            mDataIconList = TelephonyIcons.getData4G(mContext)[mInetCondition];
            LogUtils.d("jarerror==null?","mDataIconList == null? " + (mDataIconList == null));
//            mDataTypeIconId = R.drawable.status_sim_4g;
            mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_4g","drawable");
            LogUtils.d("jarerror==null?","mDataTypeIconId == null? " + (mDataTypeIconId));
//            mContentDescriptionDataType = mContext.getString(
//                    R.string.accessibility_data_connection_4g);
        } else {
        	LogUtils.d(TAG,"updateDataNetType() mIsWimaxEnabled is false ||  mWimaxConnected is false ,  mDataNetType: " + mDataNetType);
            switch (mDataNetType) {
            
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    if (!mWimaxConnected){
                    	LogUtils.d(TAG,"updateDataNetType() !mWimaxConnected");
//                        mDataIconList = TelephonyIcons.DATA_G[mInetCondition];
                        mDataIconList = TelephonyIcons.getDataG(mContext)[mInetCondition];
                        mDataTypeIconId = 0;
//                        mContentDescriptionDataType = mContext.getString(
//                                R.string.accessibility_data_connection_gprs);
                        break;
                    } else {
                        // fall through
                    	LogUtils.d(TAG,"updateDataNetType() mWimaxConnected");
                    }
                    
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    if (!mShowAtLeastThreeGees) {
                    	LogUtils.d(TAG,"updateDataNetType() !mShowAtLeastThreeGees");
                    	
//                        mDataIconList = TelephonyIcons.DATA_E[mInetCondition];
                        mDataIconList = TelephonyIcons.getDataE(mContext)[mInetCondition];
//                        mDataTypeIconId = R.drawable.status_conn_e;
                        mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_conn_e","drawable");
//                        mContentDescriptionDataType = mContext.getString(
//                                R.string.accessibility_data_connection_edge);
                        break;
                    } else {
                        // fall through
                    	LogUtils.d(TAG,"updateDataNetType() mShowAtLeastThreeGees");
                    }
                    
                case TelephonyManager.NETWORK_TYPE_UMTS:
                	LogUtils.d(TAG,"updateDataNetType() NETWORK_TYPE_UMTS");
//                    mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                    mDataIconList = TelephonyIcons.getData3G(mContext)[mInetCondition];
//                    mDataTypeIconId = R.drawable.status_sim_3g;
                    mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_3g","drawable");
//                    mContentDescriptionDataType = mContext.getString(
//                            R.string.accessibility_data_connection_3g);
                    break;
                    
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    if (mHspaDataDistinguishable) {
                    	LogUtils.d(TAG,"updateDataNetType() mHspaDataDistinguishable");
//                        mDataIconList = TelephonyIcons.DATA_H[mInetCondition];
                        mDataIconList = TelephonyIcons.getDataH(mContext)[mInetCondition];
//                        mDataTypeIconId = R.drawable.status_conn_h;
                        mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_conn_h","drawable");
//                        mContentDescriptionDataType = mContext.getString(
//                                R.string.accessibility_data_connection_3_5g);
                    } else {
                    	LogUtils.d(TAG,"updateDataNetType() !mHspaDataDistinguishable");
//                        mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                        mDataIconList = TelephonyIcons.getData3G(mContext)[mInetCondition];
//                        mDataTypeIconId = R.drawable.status_sim_3g;
                        mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_3g","drawable");
//                        mContentDescriptionDataType = mContext.getString(
//                                R.string.accessibility_data_connection_3g);
                    }
                    break;
                    
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    // display 1xRTT for IS95A/B
                	LogUtils.d(TAG,"updateDataNetType() NETWORK_TYPE_CDMA");
//                    mDataIconList = TelephonyIcons.DATA_1X[mInetCondition];
                    mDataIconList = TelephonyIcons.getData1X(mContext)[mInetCondition];
//                    mDataTypeIconId = R.drawable.status_sim_1x;
                    mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_1x","drawable");
//                    mContentDescriptionDataType = mContext.getString(
//                            R.string.accessibility_data_connection_cdma);
                    break;
                    
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                	LogUtils.d(TAG,"updateDataNetType() NETWORK_TYPE_1xRTT");
//                    mDataIconList = TelephonyIcons.DATA_1X[mInetCondition];
                    mDataIconList = TelephonyIcons.getData1X(mContext)[mInetCondition];
//                    mDataTypeIconId = R.drawable.status_sim_1x;
                    mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_1x","drawable");
//                    mContentDescriptionDataType = mContext.getString(
//                            R.string.accessibility_data_connection_cdma);
                    break;
                    
                case TelephonyManager.NETWORK_TYPE_EVDO_0: //fall through
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                	LogUtils.d(TAG,"updateDataNetType() NETWORK_TYPE_EVDO_0 || NETWORK_TYPE_EVDO_A || NETWORK_TYPE_EVDO_B || NETWORK_TYPE_EHRPD");
//                    mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                    mDataIconList = TelephonyIcons.getData3G(mContext)[mInetCondition];
//                    mDataTypeIconId = R.drawable.status_sim_3g;
                    mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_3g","drawable");
//                    mContentDescriptionDataType = mContext.getString(
//                            R.string.accessibility_data_connection_3g);
                    break;

                case TelephonyManager.NETWORK_TYPE_LTE:
                	LogUtils.d(TAG,"updateDataNetType() NETWORK_TYPE_LTE");
//                    mDataIconList = TelephonyIcons.DATA_4G[mInetCondition];
                    mDataIconList = TelephonyIcons.getData4G(mContext)[mInetCondition];
//                    mDataTypeIconId = R.drawable.status_sim_4g;
                    mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_4g","drawable");
                    break;
                    
                default:
                	LogUtils.d(TAG,"updateDataNetType() default");
                    if (!mShowAtLeastThreeGees) {
//                        mDataIconList = TelephonyIcons.DATA_G[mInetCondition];
                        mDataIconList = TelephonyIcons.getDataG(mContext)[mInetCondition];
//                        mDataTypeIconId = R.drawable.status_conn_g;
                        mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_conn_g","drawable");
                    } else {
//                        mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                        mDataIconList = TelephonyIcons.getData3G(mContext)[mInetCondition];
//                        mDataTypeIconId = R.drawable.status_sim_3g;
                        mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_sim_3g","drawable");
                    }
                    break;
            }
        }

        // 为了显示网络级别,把判断漫游取消掉
//        if ((isCdma() && isCdmaEri()) || mPhone.isNetworkRoaming()) {
//        	LogUtils.d(TAG,"updateDataNetType() (isCdma() && isCdmaEri()) || mPhone.isNetworkRoaming()");
//            mDataTypeIconId = R.drawable.status_conn_roam;
//        }
    }
    
//    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
//        LogUtils.d("CarrierLabel" +  "updateNetworkName showSpn=" + showSpn + " spn=" + spn
//                + " showPlmn=" + showPlmn + " plmn=" + plmn);
//        StringBuilder str = new StringBuilder();
//        boolean something = false;
//        if (showPlmn && plmn != null) {
//            str.append(plmn);
//            something = true;
//        }
//        if (showSpn && spn != null) {
//            if (something) {
//                str.append(mNetworkNameSeparator);
//            }
//            str.append(spn);
//            something = true;
//        }
//        if (something) {
////            mNetworkName = str.toString();
//        } else {
////            mNetworkName = mNetworkNameDefault;
//        }
//    }
    
    private boolean isCdma(){
        return (mSignalStrength != null) && !mSignalStrength.isGsm();
    }

    boolean isCdmaEri() {
        if (mServiceState != null) {
//            final int iconIndex = mServiceState.getCdmaEriIconIndex();
//            Method getCdmaEriIconIndexMethod = ReflectUtil.getMethodBypackageName("android.telephony.ServiceState", "getCdmaEriIconIndex", null);
//            final int iconIndex = (Integer)ReflectUtil.invoke(getCdmaEriIconIndexMethod, mServiceState, null);
            final int iconIndex = ToolUtils.getCdmaEriIconIndex(mServiceState);
            
            if (iconIndex != EriInfo.ROAMING_INDICATOR_OFF) {
//                final int iconMode = mServiceState.getCdmaEriIconMode();
//                Method getCdmaEriIconModeMethod = ReflectUtil.getMethodBypackageName("android.telephony.ServiceState", "getCdmaEriIconMode", null);
//                final int iconMode = (Integer)ReflectUtil.invoke(getCdmaEriIconModeMethod, mServiceState, null);
                final int iconMode = ToolUtils.getCdmaEriIconMode(mServiceState);
                if (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                        || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateGpsState(){
    	if(mGpsStarNum <= GPS_AVAILABLE_STAR_NUM){		// 未到达有效星数
//    		mGpsStarIconId = R.drawable.statusbar_gps_animation;
    		mGpsStarIconId = ToolUtils.getResourceId(mContext, "statusbar_gps_animation","drawable");
    	}else{			// 到达有效星数
//    		mGpsStarIconId = R.drawable.gps_on;
    		mGpsStarIconId = ToolUtils.getResourceId(mContext, "gps_on","drawable");
    	}
    }
    
    // =========update simcard state=================================================
    private final void updateSimState(Intent intent) {
//        String stateExtra = intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE);
        String stateExtra = intent.getStringExtra("ss");
//        if (IccCard.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
        if ("ABSENT".equals(stateExtra)) {
            mSimState = IccCard.State.ABSENT;
        }
//        else if (IccCard.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
        else if ("READY".equals(stateExtra)) {
            mSimState = IccCard.State.READY;
        }
//        else if (IccCard.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
        else if ("LOCKED".equals(stateExtra)) {
//            final String lockedReason = intent.getStringExtra(IccCard.INTENT_KEY_LOCKED_REASON);
            final String lockedReason = intent.getStringExtra("reason");
//            if (IccCard.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
            if ("PIN".equals(lockedReason)) {
                mSimState = IccCard.State.PIN_REQUIRED;
            }
//            else if (IccCard.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
            else if ("PUK".equals(lockedReason)) {
                mSimState = IccCard.State.PUK_REQUIRED;
            }
            else {
                mSimState = IccCard.State.NETWORK_LOCKED;
            }
        } else {
            mSimState = IccCard.State.UNKNOWN;
        }
    }
    
    private final void updateWimaxState(Intent intent) {
        final String action = intent.getAction();
        boolean wasConnected = mWimaxConnected;
//        if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION)) {
        if (action.equals("android.net.fourG.NET_4G_STATE_CHANGED")) {
        	
//            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.EXTRA_4G_STATE,WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            int wimaxStatus = intent.getIntExtra("4g_state",4);
//            mIsWimaxEnabled = (wimaxStatus ==WimaxManagerConstants.NET_4G_STATE_ENABLED);
            mIsWimaxEnabled = (wimaxStatus == 3);
            
//        } else if (action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION)) {
        } else if (action.equals("android.net.wimax.SIGNAL_LEVEL_CHANGED")) {
//            mWimaxSignal = intent.getIntExtra(WimaxManagerConstants.EXTRA_NEW_SIGNAL_LEVEL, 0);
            mWimaxSignal = intent.getIntExtra("newSignalLevel", 0);
            
//        } else if (action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
        } else if (action.equals("android.net.fourG.wimax.WIMAX_NETWORK_STATE_CHANGED")){
//            mWimaxState = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATE,WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxState = intent.getIntExtra("WimaxState",4);

//            mWimaxExtraState = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATE_DETAIL,WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxExtraState = intent.getIntExtra("WimaxStateDetail",4);
//            mWimaxConnected = (mWimaxState == WimaxManagerConstants.WIMAX_STATE_CONNECTED);
            mWimaxConnected = (mWimaxState == 7);
            
//            mWimaxIdle = (mWimaxExtraState == WimaxManagerConstants.WIMAX_IDLE);
            mWimaxIdle = (mWimaxExtraState == 6);
        }
        updateDataNetType();
        updateWimaxIcons();
    }
    
    private void updateWimaxIcons() {
    	LogUtils.d(TAG,"updateWimaxIcons()");

        if (mIsWimaxEnabled) {
        	LogUtils.d(TAG,"updateWimaxIcons() mIsWimaxEnabled is true ,  mWimaxConnected = " + mWimaxConnected + " ,mWimaxIdle = " + mWimaxIdle);
            if (mWimaxConnected) {
                if (mWimaxIdle)
//                    mWimaxIconId = WimaxIcons.WIMAX_IDLE;
                	mWimaxIconId = WimaxIcons.getWimaxIdle(mContext);
                else
//                    mWimaxIconId = WimaxIcons.WIMAX_SIGNAL_STRENGTH[mInetCondition][mWimaxSignal];
                	mWimaxIconId = WimaxIcons.getWimaxSignalStrength(mContext)[mInetCondition][mWimaxSignal];
            } else {
//                mWimaxIconId = WimaxIcons.WIMAX_DISCONNECTED;
                mWimaxIconId = WimaxIcons.getWimaxDisconnected(mContext);
            }
        } else {
        	LogUtils.d(TAG,"updateWimaxIcons() mIsWimaxEnabled is false");
            mWimaxIconId = 0;
        }
    }
    
    private final void updateTelephonySignalStrength() {	// 更新电话信号强度
    	LogUtils.d(TAG , "updateTelephonySignalStrength()");
        if (!hasService()) {
            LogUtils.d(TAG , "updateTelephonySignalStrength() !hasService() is true");
//            mPhoneSignalIconId = R.drawable.status_sim_singal_null;
            mPhoneSignalIconId = ToolUtils.getResourceId(mContext, "status_sim_singal_null","drawable");
//            mDataSignalIconId = R.drawable.status_sim_singal_null;
            mDataSignalIconId = ToolUtils.getResourceId(mContext, "status_sim_singal_null","drawable");
        } else {
        	LogUtils.d(TAG , "updateTelephonySignalStrength() hasService() is true");
            if (mSignalStrength == null) {
            	LogUtils.d(TAG , "updateTelephonySignalStrength() mSignalStrength == null");
//                mPhoneSignalIconId = R.drawable.status_sim_singal_null;
                mPhoneSignalIconId = ToolUtils.getResourceId(mContext, "status_sim_singal_null","drawable");
//                mDataSignalIconId = R.drawable.status_sim_singal_null;
                mDataSignalIconId = ToolUtils.getResourceId(mContext, "status_sim_singal_null","drawable");
            } else {
            	LogUtils.d(TAG , "updateTelephonySignalStrength() mSignalStrength != null");
                int iconLevel = 0;
                int[] iconList;
                if (isCdma() && mAlwaysShowCdmaRssi) {
                	LogUtils.d(TAG,"isCdma() && mAlwaysShowCdmaRssi");
                	// 系统中mSignalStrength.getCdmaLevel() 是隐藏方法,用反射获取
//                    mLastSignalLevel = iconLevel = mSignalStrength.getCdmaLevel();
//                	Method getCdmaLevelMethod = ReflectUtil.getMethodBypackageName("android.telephony.SignalStrength", "getCdmaLevel", null);
//                	mLastSignalLevel = iconLevel = (Integer)ReflectUtil.invoke(getCdmaLevelMethod, mSignalStrength, null);
                	mLastSignalLevel = iconLevel = (Integer)ToolUtils.getCdmaLevel(mSignalStrength);
                	LogUtils.d(TAG,"mAlwaysShowCdmaRssi=" + mAlwaysShowCdmaRssi
                            + " set to cdmaLevel=" + mLastSignalLevel);
//                            + " instead of level=" + mSignalStrength.getLevel());
                } else {
                	LogUtils.d(TAG,"isCdma() is false? : " + isCdma() + " or mAlwaysShowCdmaRssi is false");
                	// mSignalStrength.getLevel() 隐藏方法,用反射获取
//                    mLastSignalLevel = iconLevel = mSignalStrength.getLevel();
//                	Method getLevelMethod = ReflectUtil.getMethodBypackageName("android.telephony.SignalStrength", "getLevel", null);
//                	mLastSignalLevel = iconLevel = (Integer)ReflectUtil.invoke(getLevelMethod, mSignalStrength, null);
                	mLastSignalLevel = iconLevel = (Integer)ToolUtils.getLevel(mSignalStrength);
                    LogUtils.d(TAG ,"mAlwaysShowCdmaRssi=" + mAlwaysShowCdmaRssi
                            + " instead of level=" + mLastSignalLevel);
                }

                if (isCdma()) {
                	LogUtils.d(TAG,"isCdma() is true");
                    if (isCdmaEri()) {
                    	LogUtils.d(TAG,"isCdmaEri() is true");
//                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[mInetCondition];
                        iconList = TelephonyIcons.getTelephonySignalStrengthRoaming(mContext)[mInetCondition];
                    } else {
                    	LogUtils.d(TAG,"isCdmaEri() is false");
//                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[mInetCondition];
                        iconList = TelephonyIcons.getTelephonySignalStrength(mContext)[mInetCondition];
                    }
                } else {
                	LogUtils.d(TAG,"isCdma() is false");
                    // Though mPhone is a Manager, this call is not an IPC
                	// both of this iconList is same;
                    if (mPhone.isNetworkRoaming()) {
                    	LogUtils.d(TAG,"mPhone.isNetworkRoaming() is true");
//                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[mInetCondition];
                        iconList = TelephonyIcons.getTelephonySignalStrengthRoaming(mContext)[mInetCondition];
                    } else {
                    	LogUtils.d(TAG,"mPhone.isNetworkRoaming() is false");
//                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[mInetCondition];
                        iconList = TelephonyIcons.getTelephonySignalStrength(mContext)[mInetCondition];
                    }
                }
                mPhoneSignalIconId = iconList[iconLevel];
//                mContentDescriptionPhoneSignal = mContext.getString(
//                        AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[iconLevel]);
//                mDataSignalIconId = TelephonyIcons.DATA_SIGNAL_STRENGTH[mInetCondition][iconLevel];
                mDataSignalIconId = TelephonyIcons.getDataSignalStrength(mContext)[mInetCondition][iconLevel];
            }
        }
    }
    
    private void registerBtStateObserver(){
     	BtContentObserver weatherObserver = new BtContentObserver();
    	mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(BT_SETTINGS_KEY), true, weatherObserver);
    }
    
    private void updateBtState(){
    	int btState = Settings.System.getInt(mContext.getContentResolver(), BT_SETTINGS_KEY,BT_CLOSE);
    	if(btState == BT_OPEN){
    		mBtIconId = R.drawable.bt_on;
    	}else{
    		mBtIconId = R.drawable.bt_off;
    	}
    }

    private boolean hasService() {
        if (mServiceState != null) {
        	LogUtils.d(TAG,"hasService() mServiceState != null");
            switch (mServiceState.getState()) {
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    return false;
                default:
                    return true;
            }
        } else {
        	LogUtils.d(TAG,"hasService() mServiceState == null");
            return false;
        }
    }

    private final void updateDataIcon() {	// 更新上网信号方向
    	LogUtils.d(TAG , "yzp statusbar updateDataIcon()");
        int iconId;
        boolean visible = true;

        if (!isCdma()){		// 非2G
            // GSM case, we have to check also the sim state
        	LogUtils.d(TAG,"!isCdma()()");
            if (mSimState == IccCard.State.READY || mSimState == IccCard.State.UNKNOWN) {
            	LogUtils.d(TAG,"mSimState == IccCard.State.READY || mSimState == IccCard.State.UNKNOWN");
                if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
                	LogUtils.d(TAG,"hasService() && mDataState == TelephonyManager.DATA_CONNECTED");
                    switch (mDataActivity) {
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            iconId = mDataIconList[1];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            iconId = mDataIconList[2];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            iconId = mDataIconList[3];
                            break;
                        default:
                            iconId = mDataIconList[0];
                            break;
                    }
                    mDataDirectionIconId = iconId;
                } else {
                	LogUtils.d(TAG,"hasService() is false && mDataState != TelephonyManager.DATA_CONNECTED");
                    iconId = 0;
                    visible = false;
                }
            } else {
            	LogUtils.d(TAG,"mSimState != IccCard.State.READY && mSimState != IccCard.State.UNKNOWN");
//                iconId = R.drawable.status_no_sim;
                iconId = ToolUtils.getResourceId(mContext, "status_no_sim","drawable");
                visible = false; // no SIM? no data
            }
        } else {	// 2G
            // CDMA case, mDataActivity can be also DATA_ACTIVITY_DORMANT
        	LogUtils.d(TAG,"isCdma()()");
            if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
            	LogUtils.d(TAG,"hasService() && mDataState == TelephonyManager.DATA_CONNECTED");
                switch (mDataActivity) {
                    case TelephonyManager.DATA_ACTIVITY_IN:
                        iconId = mDataIconList[1];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT:
                        iconId = mDataIconList[2];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT:
                        iconId = mDataIconList[3];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_DORMANT:
                    default:
                        iconId = mDataIconList[0];
                        break;
                }
            } else {
            	LogUtils.d(TAG,"!hasService() || mDataState != TelephonyManager.DATA_CONNECTED");
                iconId = 0;
                visible = false;
            }
        }

        // 下面的是跟电池相关的，不需要
        // yuck - this should NOT be done by the status bar
        //long ident = Binder.clearCallingIdentity();
//        try {
//            mBatteryStats.notePhoneDataConnectionState(mPhone.getNetworkType(), visible);
//        } catch (RemoteException e) {
//        } finally {
//            Binder.restoreCallingIdentity(ident);
//        }

        mDataDirectionIconId = iconId;
        mDataConnected = visible;
    }
    
    /**
     * 更新wifi状态
     * @param intent
     */
    private void updateWifiState(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {		// wifi是否打开

            mWifiEnabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
            LogUtils.d(TAG, "yzp statusbar wifi updateWifiState() action: WIFI_STATE_CHANGED_ACTION mWifiEnabled: " + mWifiEnabled);

        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {	// wifi是否连接上
            final NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean wasConnected = mWifiConnected;
            mWifiConnected = networkInfo != null && networkInfo.isConnected();
            LogUtils.d(TAG, "yzp statusbar wifi updateWifiState() action: NETWORK_STATE_CHANGED_ACTION mWifiConnected: " + mWifiConnected + " ,wasConnected: " + wasConnected);
            // If we just connected, grab the inintial signal strength and ssid
            if (mWifiConnected && !wasConnected){
            	LogUtils.d(TAG, "yzp statusbar wifi updateWifiState() wifi connect");
                // try getting it out of the intent first
                WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                if (info == null) {
                    info = mWifiManager.getConnectionInfo();
                }
                if (info != null) {
                    mWifiSsid = huntForSsid(info);
                    
                    // yzp add
                    mWifiRssi = info.getRssi();
//                    mWifiLevel = WifiManager.calculateSignalLevel(mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
                    mWifiLevel = WifiManager.calculateSignalLevel(mWifiRssi, WifiIcons.getWifiLevelCount(mContext));
                    LogUtils.d(TAG, "yzp statusbar wifi updateWifiState() wifi mWifiSsid: " + mWifiSsid + " ,mWifiRssi: " + mWifiRssi + " ,mWifiLevel: "+ mWifiLevel);
                } else {
                    mWifiSsid = null;
                    mWifiLevel = 0;
                    mWifiRssi = -200;
                }
            } else if (!mWifiConnected) {
            	LogUtils.d(TAG, "yzp statusbar wifi updateWifiState() wifi disconnect");
                mWifiSsid = null;
                mWifiLevel = 0;
                mWifiRssi = -200;
            }
            // Apparently the wifi level is not stable at this point even if we've just connected to
            // the network; we need to wait for an RSSI_CHANGED_ACTION for that. So let's just set
            // it to 0 for now
            
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {		// wifi 信号强度改变广播
        	LogUtils.d(TAG, "yzp statusbar wifi updateWifiState() action: RSSI_CHANGED_ACTION");
            if (mWifiConnected) {
                mWifiRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
//                mWifiLevel = WifiManager.calculateSignalLevel(mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
                mWifiLevel = WifiManager.calculateSignalLevel(mWifiRssi, WifiIcons.getWifiLevelCount(mContext));
                LogUtils.d(TAG, "yzp statusbar wifi updateWifiState() action: RSSI_CHANGED_ACTION mWifiLevel: " + mWifiLevel + ",mWifiRssi");
//                Toast.makeText(mContext, "wifi mWifiRssi: " + mWifiRssi + " ,mWifiLevel: " + mWifiLevel, 0).show();
            }
        }

        updateWifiIcons();
    }
    
    private String huntForSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        // OK, it's not in the connectionInfo; we have to go hunting for it
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration net : networks) {
            if (net.networkId == info.getNetworkId()) {
                return net.SSID;
            }
        }
        return null;
    }
    
    private void updateWifiIcons() {
        if (mWifiConnected) {
//            mWifiIconId = WifiIcons.WIFI_SIGNAL_STRENGTH[mWifiLevel];
            mWifiIconId = WifiIcons.getWifiSignalStrength(mContext)[mWifiLevel];
        } else {
            if (mDataAndWifiStacked) {
                mWifiIconId = 0;
            } else {
//                mWifiIconId = mWifiEnabled ? WifiIcons.WIFI_SIGNAL_STRENGTH[0] : 0;
                mWifiIconId = mWifiEnabled ? WifiIcons.getWifiSignalStrength(mContext)[0] : 0;
            }
        }
    }

    /**
     * 更新蓝牙状态
     * @param inConnect
     */
    
    private void updateBtState(boolean inConnect){
    	LogUtils.d(TAG, "yzp statusbar bt updateBtState() inConnect: " + inConnect);
    	if(inConnect){
    		mBluetoothTethered = true;
//    		mBluetoothTetherIconId = R.drawable.bt_on;
    		mBluetoothTetherIconId = ToolUtils.getResourceId(mContext, "bt_on","drawable");
    	}else{
    		mBluetoothTethered = false;    		
    		mBluetoothTetherIconId = 0;
    	}
    }
    
    private void updateMediaState(boolean isMount){
    	LogUtils.d(TAG , "yzp statusbar upan updateMediaState() isMount: " + isMount);
    	if(isMount){
//    		mUMediaIconId = R.drawable.udisk;
    		mUMediaIconId = ToolUtils.getResourceId(mContext, "udisk","drawable");
    	}else{
    		mUMediaIconId = 0;
    	}
    }

    /**
     * 更新飞行模式状态
     */
    private void updateAirplaneMode() {
        mAirplaneMode = (Settings.System.getInt(mContext.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) == 1);
        LogUtils.d(TAG,"yzp statusbar airplane updateAirplaneMode() mAirplaneMode:" + mAirplaneMode);
    }
    
    /**
     * 状态栏图标刷新    mDataIconList
     */
    private void refreshViews(){
//    	Toast.makeText(mContext, "刷新界面", 0).show();
    	LogUtils.d(TAG , "yzp statusbar refreshViews()");
//        Context context = mContext;

        int combinedSignalIconId = 0;		// 显示sim卡信号强度
        int combinedActivityIconId = 0;		// 显示当前网络连接状态的网络数据上下行状态
//        String combinedLabel = "";		// 用于在状态栏显示label的内容
//        String wifiLabel = "";
//        String mobileLabel = "";
        int N;								// 临时变量

        if (!mHasMobileDataFeature) {		// 硬件不支持网络
        	LogUtils.d(TAG,"yzp statusbar refreshViews() mHasMobileDataFeature is false");
            mDataSignalIconId = mPhoneSignalIconId = 0;
//            mobileLabel = "";
        } else {							// 硬件支持网络
            // We want to show the carrier name if in service and either:
            //   - We are connected to mobile data, or
            //   - We are not connected to mobile data, as long as the *reason* packets are not
            //     being routed over that link is that we have better connectivity via wifi.
            // If data is disconnected for some other reason but wifi is connected, we show nothing.
            // Otherwise (nothing connected) we show "No internet connection".

//            if (mDataConnected) {			// 用来判断显示的label名字 是中国移动,中国联通还是其他的
////                mobileLabel = mNetworkName;
//            } else if (mWifiConnected) {
//                if (hasService()) {
////                    mobileLabel = mNetworkName;
//                } else {
////                    mobileLabel = "";
//                }
//            } else {
////                mobileLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
//            }

            // Now for things that should only be shown when actually using mobile data.
            if (mDataConnected){
            	LogUtils.d(TAG,"refreshViews() mDataConnected connected");
                combinedSignalIconId = mDataSignalIconId;
                
                switch (mDataActivity) {
                    case TelephonyManager.DATA_ACTIVITY_IN:
//                        mMobileActivityIconId = R.drawable.status_conn_signal_in;
                        mMobileActivityIconId = ToolUtils.getResourceId(mContext, "status_conn_signal_in","drawable");
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT:
//                        mMobileActivityIconId = R.drawable.status_conn_signal_out;
                        mMobileActivityIconId = ToolUtils.getResourceId(mContext, "status_conn_signal_out","drawable");
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT:
//                        mMobileActivityIconId = R.drawable.status_conn_signal_inout;
                        mMobileActivityIconId = ToolUtils.getResourceId(mContext, "status_conn_signal_inout","drawable");
                        break;
                    default:
//                        mMobileActivityIconId = R.drawable.status_conn_signal_noinout;
                        mMobileActivityIconId = ToolUtils.getResourceId(mContext, "status_conn_signal_noinout","drawable");
                        break;
                }

//                combinedLabel = mobileLabel;
                combinedActivityIconId = mMobileActivityIconId;
                combinedSignalIconId = mDataSignalIconId; // set by updateDataIcon()
//                mContentDescriptionCombinedSignal = mContentDescriptionDataType;
                
                LogUtils.d(TAG,"yzp statusbar refreshViews() mDataConnected is connect,  combinedActivityIconId: " + combinedActivityIconId + " ,combinedSignalIconId: " + combinedSignalIconId);
            }
        }

        // 更新wifi数据
        if (mWifiConnected) {
        	LogUtils.d(TAG,"yzp statusbar refreshViews() mWifiConnected");
            if (mWifiSsid == null) {
                mMobileActivityIconId = 0; // no wifis, no bits
            } else {
                switch (mWifiActivity) {
//                    case WifiManager.DATA_ACTIVITY_IN:
                    case 0x01:
//                        mMobileActivityIconId = R.drawable.status_conn_signal_in;
                        mMobileActivityIconId = ToolUtils.getResourceId(mContext, "status_conn_signal_in","drawable");
                        break;
//                    case WifiManager.DATA_ACTIVITY_OUT:
                    case 0x02:
//                        mMobileActivityIconId = R.drawable.status_conn_signal_out;
                        mMobileActivityIconId = ToolUtils.getResourceId(mContext, "status_conn_signal_out","drawable");
                        break;
//                    case WifiManager.DATA_ACTIVITY_INOUT:
                    case 0x03:
//                        mMobileActivityIconId = R.drawable.status_conn_signal_inout;
                        mMobileActivityIconId = ToolUtils.getResourceId(mContext, "status_conn_signal_inout","drawable");
                        break;
//                    case WifiManager.DATA_ACTIVITY_NONE:
                    case 0x00:
                        mMobileActivityIconId = 0;
                        break;
                }
            }

            combinedActivityIconId = mMobileActivityIconId;
//            combinedLabel = mobileLabel;
            combinedSignalIconId = mDataSignalIconId; // set by updateWifiIcons()
//            mContentDescriptionCombinedSignal = mContentDescriptionDataType;
            LogUtils.d(TAG,"yzp statusbar refreshViews() mDataConnected is connect,  combinedActivityIconId: " + combinedActivityIconId + " ,combinedSignalIconId: " + combinedSignalIconId);
        } else {
//            if (mHasMobileDataFeature) {
////                wifiLabel = "";
//            } else {
////                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
//            }
        }

        if(!mDataConnected && !mWifiConnected) {
            mMobileActivityIconId = 0;
        }

        if (mBluetoothTethered) {
//            combinedLabel = mContext.getString(R.string.bluetooth_tethered);
            combinedSignalIconId = mBluetoothTetherIconId;
        }
        
//        Method method = ReflectUtil.getMethodBypackageName("android.telephony.ServiceState", "isEmergencyOnly",new Class[]{});
        
//        boolean isEmergencyOnly = (Boolean)ReflectUtil.invoke(method, mServiceState, new Class[]{});
        if (mAirplaneMode &&
//                (mServiceState == null || (!hasService() && !mServiceState.isEmergencyOnly()))) {
        		(mServiceState == null || (!hasService() && !ToolUtils.isEmergencyOnly(mServiceState)))){
        	
            // Only display the flight-mode icon if not in "emergency calls only" mode.
            // look again; your radios are now airplanes
        	LogUtils.d(TAG,"yzp statusbar refreshViews() mAirplaneMode && (mServiceState == null || (!hasService() && !ToolUtil.isEmergencyOnly(mServiceState))");
//            mPhoneSignalIconId = mDataSignalIconId = R.drawable.status_flightmode;
            mPhoneSignalIconId = mDataSignalIconId = ToolUtils.getResourceId(mContext, "status_flightmode","drawable");
            mDataTypeIconId = 0;

            // yzp add
            combinedSignalIconId = 0;
            mDataTypeIconId = 0;
            mWimaxIconId = 0;
            combinedActivityIconId = 0;

            // combined values from connected wifi take precedence over airplane mode
            if (mWifiConnected) {
                // Suppress "No internet connection." from mobile if wifi connected.
//                mobileLabel = "";
            } else {
                if (mHasMobileDataFeature) {
                    // let the mobile icon show "No internet connection."
//                    wifiLabel = "";
                } else {
//                    wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
//                    combinedLabel = wifiLabel;
                }
//                mContentDescriptionCombinedSignal = mContentDescriptionPhoneSignal;
                combinedSignalIconId = mDataSignalIconId;
            }
            
        }else if (!mDataConnected && !mWifiConnected && !mBluetoothTethered && !mWimaxConnected) {
        	
            // pretty much totally disconnected

            // On devices without mobile radios, we want to show the wifi icon
            combinedSignalIconId = mHasMobileDataFeature ? mDataSignalIconId : mWifiIconId;

//            if ((isCdma() && isCdmaEri()) || mPhone.isNetworkRoaming()) {
////                mDataTypeIconId = R.drawable.status_conn_roam;
//                mDataTypeIconId = ToolUtils.getResourceId(mContext, "status_conn_roam","drawable");
//            } else {
                mDataTypeIconId = 0;
//            }
            
        	LogUtils.d(TAG,"yzp statusbar refreshViews() !mDataConnected && !mWifiConnected && !mBluetoothTethered && !mWimaxConnected , mDataTypeIconId : " + mDataTypeIconId);
        }

        Log.d(TAG, "statusbar refreshViews connected={"
                + (mWifiConnected?" wifi":"")
                + (mDataConnected?" data":"")
                + " } level="
                + ((mSignalStrength == null)?"??":Integer.toString(ToolUtils.getLevel(mSignalStrength)))
                + " combinedSignalIconId=0x"
                + Integer.toHexString(combinedSignalIconId)
                + "/" + getResourceName(combinedSignalIconId)
                + " combinedActivityIconId=0x" + Integer.toHexString(combinedActivityIconId)
                + " mAirplaneMode=" + mAirplaneMode
                + " mDataActivity=" + mDataActivity
                + " mPhoneSignalIconId=0x" + Integer.toHexString(mPhoneSignalIconId)
                + " mDataDirectionIconId=0x" + Integer.toHexString(mDataDirectionIconId)
                + " mDataSignalIconId=0x" + Integer.toHexString(mDataSignalIconId)
                + " mDataTypeIconId=0x" + Integer.toHexString(mDataTypeIconId)
                + " mWifiIconId=0x" + Integer.toHexString(mWifiIconId)
                + " mBluetoothTetherIconId=0x" + Integer.toHexString(mBluetoothTetherIconId)
                + " time:" + mTime 
                + " mUMediaIconId=0x" + Integer.toHexString(mUMediaIconId)
                + " mGpsStarIconId=0x" + Integer.toHexString(mGpsStarIconId)
                + " mBtIconId=0x" + Integer.toHexString(mBtIconId));

        if (mLastPhoneSignalIconId          != mPhoneSignalIconId
         || mLastDataDirectionOverlayIconId != combinedActivityIconId
         || mLastWifiIconId                 != mWifiIconId
         || mLastWimaxIconId                != mWimaxIconId
         || mLastDataTypeIconId             != mDataTypeIconId){
            // NB: the mLast*s will be updated later
            for (SignalCluster cluster : mSignalClusters) {
                refreshSignalCluster(cluster);
            }
        }

        // 更新时间
        LogUtils.d(TAG, "yzp statusbar time mLastTime: " + mLastTime + " ,mTime:" + mTime);
        if(!mLastTime.equals(mTime)){
        	mLastTime = mTime;
        	if(!TextUtils.isEmpty(mTime)){
        		if(mTimeHourTextView != null){
            		String hour = mTime.split(":")[0];
            		mTimeHourTextView.setText(hour);
            	}
            	if(mTimeMinTextView != null){
            		String min = mTime.split(":")[1];
            		mTimeMinTextView.setText(min);
            	}
        	}
        }

        // 更新sim卡信号强度
        if (mLastPhoneSignalIconId != mPhoneSignalIconId) {
            mLastPhoneSignalIconId = mPhoneSignalIconId;
            N = mPhoneSignalIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mPhoneSignalIconViews.get(i);
                if (mPhoneSignalIconId == 0) {		// 硬件不支持sim卡的时候才会消失
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mPhoneSignalIconId);
                }
            }
        }
        
        // 混合的数据网络信号强度图标,比如说有手机网络,wifi网络,和bt网络,
        if (mLastCombinedSignalIconId != combinedSignalIconId) {
            mLastCombinedSignalIconId = combinedSignalIconId;
            N = mCombinedSignalIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mCombinedSignalIconViews.get(i);
                v.setImageResource(combinedSignalIconId);
            }
        }

        // 数据网络的类型,例如:h,e...
        if (mLastDataTypeIconId != mDataTypeIconId) {
            mLastDataTypeIconId = mDataTypeIconId;
            N = mDataTypeIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mDataTypeIconViews.get(i);
                if (mDataTypeIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mDataTypeIconId);
                }
            }
        }

        // Wimax信号图标
        if (mLastWimaxIconId != mWimaxIconId) {		
            mLastWimaxIconId = mWimaxIconId;
            N = mWimaxIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mWimaxIconViews.get(i);
                if (mWimaxIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mWimaxIconId);
                }
           }
        }

        // 返回数据上传下传状态
        if (mLastDataDirectionOverlayIconId != combinedActivityIconId) {
            LogUtils.d(TAG ,"changing data overlay icon id to " + combinedActivityIconId);
            mLastDataDirectionOverlayIconId = combinedActivityIconId;
            N = mDataDirectionOverlayIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mDataDirectionOverlayIconViews.get(i);
                if (combinedActivityIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(combinedActivityIconId);
//                    v.setContentDescription(mContentDescriptionDataType);
                }
            }
        }
        
        // 更新上网数据方向
        if (mLastDataDirectionIconId != mDataDirectionIconId) {
            mLastDataDirectionIconId = mDataDirectionIconId;
            N = mDataDirectionIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mDataDirectionIconViews.get(i);
                v.setImageResource(mDataDirectionIconId);
            }
        }

        // 更新wifi状态
//        Toast.makeText(mContext, "yzp statusbar mWifiIconId: " + mWifiIconId + " ,mLastWifiIconId: " + mLastWifiIconId, 0).show();
        LogUtils.d(TAG, "yzp statusbar wifi refreshView() mLastWifiIconId :" + mLastWifiIconId + " ,mWifiIconId: " + mWifiIconId);
        if (mLastWifiIconId != mWifiIconId) {
            mLastWifiIconId = mWifiIconId;
            N = mWifiIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mWifiIconViews.get(i);
                if (mWifiIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mWifiIconId);
                }
            }
        }
        
        // 更新蓝牙状态
//        LogUtils.d(TAG , "yzp statusbar bt update");
//        if(mLastBluetoothTetherIconId != mBluetoothTetherIconId){
//        	mLastBluetoothTetherIconId = mBluetoothTetherIconId;
//        	N = mBtStateIconViews.size();
//        	for(int i = 0 ; i < N ; i++){
//        		final ImageView v = mUMediaStateIconViews.get(i);
//        		if(mLastBluetoothTetherIconId == 0){
//        			v.setVisibility(View.GONE);
//        		}else{
//        			v.setVisibility(View.VISIBLE);
//        			v.setImageResource(mBluetoothTetherIconId);
//        		}
//        	}
//        }
        if(mLastBtIconId != mBtIconId){
        	mLastBtIconId = mBtIconId;
        	N = mBtStateIconViews.size();
        	for(int i = 0 ; i < N ; i++){
        		final ImageView v = mBtStateIconViews.get(i);
        		if(mLastBtIconId == 0){
        			v.setVisibility(View.GONE);
        		}else{
        			v.setVisibility(View.VISIBLE);
        			v.setImageResource(mBtIconId);
        		}
        	}
        }
        
        // 更新u盘状态
        LogUtils.d(TAG , "yzp statusbar upan refreshViews() mUMediaIconId: " + mUMediaIconId );
//        Toast.makeText(context, "upan mUMediaIconId: " + mUMediaIconId + " mLastUMediaIconId: " + mLastUMediaIconId, 0).show();
        if(mLastUMediaIconId != mUMediaIconId){
        	LogUtils.d(TAG, "yzp statusbar upan mUMediaState icon id to:" + mUMediaIconId);
        	mLastUMediaIconId = mUMediaIconId;
        	N = mUMediaStateIconViews.size();
        	for(int i = 0 ; i < N; i++){
        		final ImageView v = mUMediaStateIconViews.get(i);
        		if(mUMediaIconId == 0){
//        			Toast.makeText(context, "yzp statusbar upan not connect ",0).show();
        			v.setVisibility(View.GONE);
        		}else{
//        			Toast.makeText(context, "yzp statusbar upan connect ",0).show();
        			v.setVisibility(View.VISIBLE);
        			v.setImageResource(mUMediaIconId);
        		}
        	}
        }

        // 更新gps状态
//        Toast.makeText(context, "yzp statusbar gps update mLastGpsStarIconId: " + mLastGpsStarIconId + " mGpsStarIconId: " + mGpsStarIconId, 0).show();
        LogUtils.d(TAG , "yzp statusbar gps update mLastGpsStarIconId:" + mLastGpsStarIconId + " mLastGpsStarIconId:" + mLastGpsStarIconId);
        if(mLastGpsStarIconId != mGpsStarIconId){
        	mLastGpsStarIconId = mGpsStarIconId;
        	N = mGpsStateIconViews.size();
//        	Toast.makeText(context, "gps update mLastGpsStarIconId N: " + N, 0).show();
        	for(int i = 0 ; i < N ; i++){
        		ImageView v = mGpsStateIconViews.get(i);
//        		if(mGpsStarIconId != R.drawable.gps_on){	// 播放闪烁动画
        		if(mGpsStarIconId != ToolUtils.getResourceId(mContext, "gps_on","drawable")){	// 播放闪烁动画
//        			Toast.makeText(context, "yzp statusbar gps start animation", 0).show();
        			v.setImageResource(mLastGpsStarIconId);
       				mAnimationGps = (AnimationDrawable) v.getDrawable();        				
        			mAnimationGps.start();
        		}else{
        			// 回收动画资源
//        			if(mAnimationGps != null){			// 播放gps打开动画
//        				mAnimationGps.stop();
//        				for (int j = 0; j < mAnimationGps.getNumberOfFrames(); j++) {
//        		            Drawable frame = mAnimationGps.getFrame(j);
//        		            if (frame instanceof BitmapDrawable) {
//        		                ((BitmapDrawable) frame).getBitmap().recycle();
//        		            }
//        		            frame.setCallback(null);
//        		        }
//        			}
//        			Toast.makeText(mContext, "yzp statusbar gps get location", 0).show();
        			v.setImageResource(mLastGpsStarIconId);        			
        		}
        	}
        }
        
        
        // the combinedLabel in the notification panel
//        if (!mLastCombinedLabel.equals(combinedLabel)) {
//            mLastCombinedLabel = combinedLabel;
//            N = mCombinedLabelViews.size();
//            for (int i=0; i<N; i++) {
//                TextView v = mCombinedLabelViews.get(i);
//                v.setText(combinedLabel);
//            }
//        }

        // wifi label
//        N = mWifiLabelViews.size();
//        for (int i=0; i<N; i++) {
//            TextView v = mWifiLabelViews.get(i);
//            if ("".equals(wifiLabel)) {
//                v.setVisibility(View.GONE);
//            } else {
//                v.setVisibility(View.VISIBLE);
//                v.setText(wifiLabel);
//            }
//        }

        // mobile label
//        N = mMobileLabelViews.size();
//        for (int i=0; i<N; i++) {
//            TextView v = mMobileLabelViews.get(i);
//            if ("".equals(mobileLabel)) {
//                v.setVisibility(View.GONE);
//            } else {
//                v.setVisibility(View.VISIBLE);
//                v.setText(mobileLabel);
//            }
//        }
    }
    
    private String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }
    
    public interface SignalCluster {
        void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
                String contentDescription);
        void setMobileDataIndicators(boolean visible, int strengthIcon, int activityIcon,
                int typeIcon, String contentDescription, String typeContentDescription);
        void setIsAirplaneMode(boolean is);
    }
    
    public void refreshSignalCluster(SignalCluster cluster) {
        cluster.setWifiIndicators(
                mWifiConnected, // only show wifi in the cluster if connected
                mWifiIconId,
                mWifiActivityIconId,
//                mContentDescriptionWifi);
                null);

        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is special
            cluster.setMobileDataIndicators(
                    true,
                    mAlwaysShowCdmaRssi ? mPhoneSignalIconId : mWimaxIconId,
                    mMobileActivityIconId,
                    mDataTypeIconId,
//                    mContentDescriptionWimax,
                    null,
//                    mContentDescriptionDataType);
                    null);
        } else {
            // normal mobile data
            cluster.setMobileDataIndicators(
                    mHasMobileDataFeature,
                    mShowPhoneRSSIForData ? mPhoneSignalIconId : mDataSignalIconId,
                    mMobileActivityIconId,
                    mDataTypeIconId,
//                    mContentDescriptionPhoneSignal,
//                    mContentDescriptionDataType);
		            null,
		            null);
        }
        cluster.setIsAirplaneMode(mAirplaneMode);
    }
    
    /**
     * 初始化数据状态
     */
    public void initAllDateState(){
    	// 更新时间
    	updateTime();

    	// 获取sim卡信号强度
    	
    	// 获取wifi信号强度
    	getWifiStrength();
    	
    	// 获取蓝牙状态
    	getBtState();

    	// 获取u盘状态
    	getUdiskState();
    	
    	// 获取GPS是否定位

    	refreshViews();
    }
    
    @SuppressLint("NewApi")
	private void getBtState(){
    	LogUtils.d(TAG,"getBtState()");
    	
    	BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
    	if(ba != null){
    		if(mBluetoothTethered = ba.isEnabled()){
    			int a2dp = ba.getProfileConnectionState(BluetoothProfile.A2DP); //可操控蓝牙设备，如带播放暂停功能的蓝牙耳机  
    			int headset = ba.getProfileConnectionState(BluetoothProfile.HEADSET);        //蓝牙头戴式耳机，支持语音输入输出  
    			int health = ba.getProfileConnectionState(BluetoothProfile.HEALTH);          //蓝牙穿戴式设备 
    			
    		   //查看是否蓝牙是否连接到三种设备的一种，以此来判断是否处于连接状态还是打开并没有连接的状态  
     		   int flag = -1;  
     		   if (a2dp == BluetoothProfile.STATE_CONNECTED) {  
     		      flag = a2dp;  
     		   } else if (headset == BluetoothProfile.STATE_CONNECTED) {  
     		      flag = headset;  
     		   } else if (health == BluetoothProfile.STATE_CONNECTED) {  
     		      flag = health;  
     		   }  
     		   
     		   // 说明连接上三种设备中的一种
     		   if(flag != -1){
     			   mBluetoothTethered = true;
//     			   mBluetoothTetherIconId = R.drawable.bt_on;
     			   mBluetoothTetherIconId = ToolUtils.getResourceId(mContext, "bt_on","drawable");
     			   return;
     		   }
    		}
    	}
    	mBluetoothTethered = false;
		mBluetoothTetherIconId = 0;

    }
    
    /**
     * 获取wifi是否打开已经对应的强度
     */
    private void getWifiStrength(){
    	mWifiConnected = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
    	LogUtils.d(TAG,"getWifiStrength() mWifiConnected: " + mWifiConnected);
    	if(mWifiConnected){
    		WifiInfo wifiInfo = mWifiManager.getConnectionInfo();    		
    		LogUtils.d(TAG,"getWifiStrength() wifiInfo == null? " + (wifiInfo == null));
    		if(wifiInfo != null){
    			mWifiSsid = huntForSsid(wifiInfo);
    			mWifiRssi = wifiInfo.getRssi();
//    			mWifiLevel = WifiManager.calculateSignalLevel(mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
    			mWifiLevel = WifiManager.calculateSignalLevel(mWifiRssi, WifiIcons.getWifiLevelCount(mContext));
    		}else{
    			mWifiSsid = null;
                mWifiLevel = 0;
                mWifiRssi = -200;
    		}
    	}else{
    		mWifiSsid = null;
            mWifiLevel = 0;
            mWifiRssi = -200;
    	}

    	updateWifiIcons();
    	LogUtils.d(TAG,"getWifiStrength() mWifiSsid: " + mWifiSsid + " ,mWifiRssi: " + mWifiRssi + " ,mWifiLevel: " + mWifiLevel);
    }
    
    /**
     * 获取u盘状态
     */
    private void getUdiskState(){
    	
    	String udiskPath = "/mnt/media_rw/udisk";
    	
    	InputStream is = null;
    	InputStreamReader isr = null;
    	BufferedReader bufferedReader = null;
    	
    	try {
    		Process process = Runtime.getRuntime().exec("mount");
    		is = process.getInputStream();
    		isr = new InputStreamReader(is);
    		bufferedReader = new BufferedReader(isr);
    		String line  = null ;
    		while((line = bufferedReader.readLine()) !=null){
    			if(line.contains(udiskPath)){
//    				mUMediaIconId = R.drawable.udisk;
    				mUMediaIconId = ToolUtils.getResourceId(mContext, "udisk","drawable");
    				return;
    			}
    		}
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}finally{
    		
    		// 只关闭外层流，里面的就会关闭了
    		if(bufferedReader != null){
    			try {
					bufferedReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	mUMediaIconId = 0;

    }
    
    public static final int NETWORK_CLASS_UNKNOWN = 0;
    public static final int NETWORK_CLASS_2_G = 1;
    public static final int NETWORK_CLASS_3_G = 2;
    public static final int NETWORK_CLASS_4_G = 3;

    /**
     * 获取sim卡制式
     * @return sim卡制式
     */
//    private int getNetworkType(){
//    	
//    	mConnectivityManager.
//    	switch (networkType) {
//    		case TelephonyManager.NETWORK_TYPE_GPRS:
//    		case TelephonyManager.NETWORK_TYPE_EDGE:
//    		case TelephonyManager.NETWORK_TYPE_CDMA:
//    		case TelephonyManager.NETWORK_TYPE_1xRTT:
//    		case TelephonyManager.NETWORK_TYPE_IDEN:
//    			return NETWORK_CLASS_2_G;
//    		case TelephonyManager.NETWORK_TYPE_UMTS:
//    		case TelephonyManager.NETWORK_TYPE_EVDO_0:
//    		case TelephonyManager.NETWORK_TYPE_EVDO_A:
//    		case TelephonyManager.NETWORK_TYPE_HSDPA:
//	        case TelephonyManager.NETWORK_TYPE_HSUPA:
//	        case TelephonyManager.NETWORK_TYPE_HSPA:
//	        case TelephonyManager.NETWORK_TYPE_EVDO_B:
//	        case TelephonyManager.NETWORK_TYPE_EHRPD:
//	        case TelephonyManager.NETWORK_TYPE_HSPAP:
//	        	return NETWORK_CLASS_3_G;
//	        case TelephonyManager.NETWORK_TYPE_LTE:
//	        	return NETWORK_CLASS_4_G;
//	        default:
//	        	return NETWORK_CLASS_UNKNOWN;
//    	}
//    }
    
    public void addPhoneSignalIconView(ImageView v) {
        mPhoneSignalIconViews.add(v);
    }
    
    public void addDataDirectionIconView(ImageView v) {
        mDataDirectionIconViews.add(v);
    }
    
    public void addCombinedSignalIconView(ImageView v) {
        mCombinedSignalIconViews.add(v);
    }
    
    public void setStackedMode(boolean stacked) {
        mDataAndWifiStacked = stacked;
    }
    
    public void addWifiIconView(ImageView v) {
        mWifiIconViews.add(v);
    }
    
    public void addSignalCluster(SignalCluster cluster) {
        mSignalClusters.add(cluster);
        refreshSignalCluster(cluster);
    }
    
    public void addDataDirectionOverlayIconView(ImageView v) {
        mDataDirectionOverlayIconViews.add(v);
    }
    
    public void addBtStateIconView(ImageView v){
    	mBtStateIconViews.add(v);
    }
    
    /**
     * 添加U盘状态控件
     * @param v
     */
    public void addUMediaStateIconView(ImageView v){
    	mUMediaStateIconViews.add(v);
    }
    
    /**
     * 添加gps状态控件
     * @param v
     */
    public void addGpsStateIconView(ImageView v){
    	mGpsStateIconViews.add(v);
    }
    
    /**
     * 添加sim卡制式控件
     * @param 
     */
    public void addDataTypeIconView(ImageView v){
    	mDataTypeIconViews.add(v);
    }
    
    public void addTimeHourView(TextView v){
    	mTimeHourTextView = v;
    }
    
    public void addTimeMinView(TextView v){
    	mTimeMinTextView = v;
    }
}
