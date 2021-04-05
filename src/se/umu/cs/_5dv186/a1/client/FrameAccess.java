package se.umu.cs._5dv186.a1.client;

import ki.types.ds.Block;
import ki.types.ds.StreamInfo;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class FrameAccess implements FrameAccessor{

    protected StreamInfo stream;
    protected StreamServiceClient client;
    protected Frame[] frames;
    protected PerformanceStatistics ps;
    int nrFrames;

    public FrameAccess(StreamServiceClient client, StreamInfo stream){
        this.client = client;
        this.stream = stream;
        this.frames = new Frame[stream.getWidthInBlocks()*stream.getHeightInBlocks()];
        this.ps = new PerformanceStatistics();
        this.nrFrames = 0;
    }

    @Override
    public StreamInfo getStreamInfo() throws IOException, SocketTimeoutException {
        return stream;
    }

    @Override
    public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
        return frames[frame];
    }

    // Receive and store a frame.
    public void receiveFrame(int frame){
        int maxX = stream.getWidthInBlocks();
        int maxY = stream.getHeightInBlocks();

        Frame f = new Frame();

        long ft1 = System.currentTimeMillis();
        for(int blockY = 0; blockY < maxY; blockY++){
            for(int blockX = 0; blockX < maxX; blockX++){
                try {
                    long t1 = System.currentTimeMillis();
                    client.getBlock(stream.getName(), frame, blockX, blockY);
                    long t2 = System.currentTimeMillis();
                    f.blockTime[blockY*maxY+blockX] = t2-t1;
                    System.out.println("Block retrieved in: " + f.blockTime[blockY*maxY+blockX] + "ms.");
                }
                catch (SocketTimeoutException e)
                {
                    f.packetDrops++;
                    System.out.println("block drop.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        long ft2 = System.currentTimeMillis();
        f.frameTime = ft2-ft1;
        frames[frame] = f;
        nrFrames++;
    }

    @Override
    public PerformanceStatistics getPerformanceStatistics() {
        return ps;
    }

    class Frame implements FrameAccessor.Frame{
        Block[] blocks;
        long[] blockTime;
        long frameTime;
        int packetDrops;

        public Frame(){
            blocks = new Block[stream.getWidthInBlocks()*stream.getHeightInBlocks()];
            blockTime = new long[stream.getWidthInBlocks()*stream.getHeightInBlocks()];
            packetDrops = 0;
        }

        @Override
        public Block getBlock(int blockX, int blockY) throws IOException, SocketTimeoutException {
            return blocks[blockY*stream.getHeightInBlocks()+blockX];
        }
    }

    class PerformanceStatistics implements FrameAccessor.PerformanceStatistics{

        public PerformanceStatistics(){

        }

        @Override
        public double getPacketDropRate(String host) {
            int totalDrops = 0;

            for(int i = 0; i < nrFrames; i++)
                totalDrops += frames[i].packetDrops;

            return (double)totalDrops/(nrFrames*stream.getHeightInBlocks()*stream.getWidthInBlocks());
        }

        @Override
        public double getPacketLatency(String host) {
            long totalLatency = 0;

            for(int i = 0; i < nrFrames; i++)
                 totalLatency += frames[i].frameTime;

            return (double)totalLatency/(nrFrames*stream.getHeightInBlocks()*stream.getWidthInBlocks());
        }

        @Override
        public double getFrameThroughput() {
            double totalFrameTime = 0;

            for(int i = 0; i < nrFrames; i++)
                totalFrameTime += frames[i].frameTime;

            return (nrFrames*1000)/totalFrameTime;
        }

        @Override
        public double getBandwidthUtilization() {
            int bitsInFrame = stream.getHeightInBlocks()*stream.getWidthInBlocks()*24*16*16;

            return bitsInFrame*getFrameThroughput();
        }
    }
}
