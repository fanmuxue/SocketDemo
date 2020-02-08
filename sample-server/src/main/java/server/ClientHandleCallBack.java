package server;


import server.handle.ClientHandle;

public interface ClientHandleCallBack {
    //移除list中的自己
    void closeBySelf(ClientHandle clientHandle);

    //发送消息
    void onNewMessageArrived(ClientHandle clientHandle,String msg);

}
