package nachos.kv;

public class KVConstants {
	public static final String PUT_REQ = "putreq";
	public static final String GET_REQ = "getreq";
	public static final String DEL_REQ = "delreq";
	public static final String RESP = "resp";
	
	public static final String ERROR_INVAILD_KEY = "errorInvalidKey";
	public static final String ERROR_INVAILD_VALUE = "errorInvalidValue";
	
	public static KVMessage errorInvalidKey() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = ERROR_INVAILD_KEY;
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorInvalidValue() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = ERROR_INVAILD_VALUE;
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorInvalidFormat() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = "errorInvalidFormat";
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorSocketTimeout() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = "errorSocketTimeout";
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorCouldNotCreateSocket() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = "errorCouldNotCreateSocket";
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorParser() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = "errorParser";
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorCouldNotReceiveData() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = "errorCouldNotReceiveData";
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorCouldNotSendData() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = "errorCouldNotSendData";
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
	
	public static KVMessage errorCouldNotConnect() {
		KVMessage kvm = null;
		
		String iMsgType = "error";
		String iKey = "-1";
		String iValue = "-1";
		String iMessage = "errorCouldNotConnect";
		try {
			kvm = new KVMessage(iMsgType, iMessage);
			kvm.setKey(iKey);
			kvm.setValue(iValue);
		} catch (KVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kvm;
	}
}
