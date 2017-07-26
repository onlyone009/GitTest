package com.angjiutech.bmw.statusbar.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.content.Context;
import android.util.Log;
/**
 * @author 作者 :yuanzhenpeng
 * @version 创建时间：2017-3-2 上午9:18:20
 * 类说明:
 */
public class ToolUtils {
	
	private static final String TAG = "ToolUtilss";
	
	public static int getLevel(Object receiver){
		Class clazz = ToolUtils.getClass("android.telephony.SignalStrength");
		if(clazz != null){
			Method getLevelMethod = ToolUtils.getMethod(clazz, "getLevel", new Class[]{});
			if(getLevelMethod != null){
				Object obj = ToolUtils.invoke(getLevelMethod,receiver,new Object[]{});
				return (Integer)obj;
			}
		}
		return -1;
	}

	public static int getCdmaLevel(Object receiver){
		Class clazz = ToolUtils.getClass("android.telephony.SignalStrength");
		if(clazz != null){
			Method getCdmaLevelMethod = ToolUtils.getMethod(clazz, "getCdmaLevel", new Class[]{});
			if(getCdmaLevelMethod != null){
				return (Integer)ToolUtils.invoke(getCdmaLevelMethod,receiver,new Object(){});
			}
		}
		return -1;
	}
	
	public static int getCdmaEriIconMode(Object receiver){
		LogUtils.d(TAG , "yzp statusbar simtype getCdmaEriIconMode");
		Class clazz = ToolUtils.getClass("android.telephony.ServiceState");
		if(clazz != null){
			Method getCdmaEriIconModeMethod = ToolUtils.getMethod(clazz, "getCdmaEriIconMode", new Class[]{});
			if(getCdmaEriIconModeMethod != null){
				return (Integer)ToolUtils.invoke(getCdmaEriIconModeMethod,receiver,new Object[]{});
			}
		}
		
		return -1;
	}
	
	public static int getCdmaEriIconIndex(Object receiver){
		LogUtils.d(TAG , "yzp statusbar simtype getCdmaEriIconIndex");
		Class clazz = ToolUtils.getClass("android.telephony.ServiceState");
		if(clazz != null){
			Method getCdmaEriIconIndexMethod = ToolUtils.getMethod(clazz,"getCdmaEriIconIndex",new Class[]{});
			if(getCdmaEriIconIndexMethod != null){
				return (Integer)ToolUtils.invoke(getCdmaEriIconIndexMethod,receiver,new Object[]{});
			}
		}
		return -1;
	}
	
	public static boolean isNetworkSupported(Object receiver, int networkType){
		LogUtils.d(TAG , "yzp statusbar simtype isNetworkSupported");
		Class clazz = ToolUtils.getClass("android.net.ConnectivityManager");
		if(clazz != null){
			Method isNetworkSupportedMethod = null;;
			Method[] methods = clazz.getMethods();
			for(Method method : methods){
				if(method.getName().contains("isNetworkSupported")){
					LogUtils.d(TAG , "yzp statusbar isNetworkSupported: " + method.getName());
					isNetworkSupportedMethod = method;
					break;
				}
			}
			if(isNetworkSupportedMethod != null){
				return (Boolean)ToolUtils.invoke(isNetworkSupportedMethod, receiver, networkType);
			}
		}
		return false;
	}
	
	public static boolean isEmergencyOnly(Object receiver){
		Class clazz = ToolUtils.getClass("android.telephony.ServiceState");
		if(clazz != null){
			Method isEmergencyOnlyMethod = ToolUtils.getMethod(clazz, "isEmergencyOnly", new Class[]{});
			if(isEmergencyOnlyMethod != null){
				return (Boolean)ToolUtils.invoke(isEmergencyOnlyMethod, receiver, new Object[]{});
			}
		}
		return false;
	}
	
	private static Class getClass(String packageName){
		try {
			Class clazz = Class.forName(packageName);
			return clazz;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static Method getMethod(Class clazz,String methodName, Class<?>... parameterTypes){
		try {
			Method method = clazz.getMethod(methodName, parameterTypes);
			return method;
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static Object invoke(Method method, Object target,Object... args){
		try {
			return method.invoke(target, args);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static int getResourceId(Context context, String resCode, String type){
//		Log.d("jarerror","context == null? " + (context == null) + " ,resCode: " + resCode + " ,type: " + type);
		int resId = context.getResources().getIdentifier(resCode, type, context.getPackageName());
//		Log.d("jarerror","context == null? " + (context == null) + " ,resCode: " + resCode + " ,type: " + type + " ,resId: " + resId);
		return resId;
	}
}
