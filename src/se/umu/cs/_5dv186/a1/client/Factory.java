package se.umu.cs._5dv186.a1.client;

import ki.types.ds.StreamInfo;
import java.io.IOException;

public class Factory implements FrameAccessor.Factory {

    public Factory(){}

    /**
     * Create and get a FrameAccess object using only one client.
     * @param client - client for FrameAccess object.
     * @param stream - stream for FrameAccess object to gather data from.
     * @return - FrameAccess object.
     */
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

        StreamServiceClient[] clients = new StreamServiceClient[1];
        clients[0] = client;

        assert s != null;
        return new FrameAccess(clients, s);
    }

    /**
     * Create and get a FrameAccess object using an array of clients.
     * @param clients - clients for FrameAccess object.
     * @param stream - stream for FrameAccess object to gather data from.
     * @return - FrameAccess object.
     */
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
