package se.umu.cs._5dv186.a1.client;

import ki.types.ds.StreamInfo;
import java.io.IOException;

public class Factory implements FrameAccessor.Factory {

    public Factory(){}

    @Override
    public FrameAccess getFrameAccessor(StreamServiceClient client, String stream) {
        StreamInfo s = null;

        try {
            StreamInfo[] streams = client.listStreams();
            for(StreamInfo streamInfo : streams){
                if(streamInfo.getName().equals(stream)){
                    s = streamInfo;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert to match the constructor of FrameAccess.
        StreamServiceClient[] clients = new StreamServiceClient[1];
        clients[0] = client;

        assert s != null;
        return new FrameAccess(clients, s);
    }

    @Override
    public FrameAccess getFrameAccessor(StreamServiceClient[] clients, String stream) {
        StreamInfo s = null;

        try {
            StreamInfo[] streams = clients[0].listStreams();
            for(StreamInfo streamInfo : streams){
                if(streamInfo.getName().equals(stream)){
                    s = streamInfo;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert s != null;
        return new FrameAccess(clients, s);
    }
}
