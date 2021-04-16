package se.umu.cs._5dv186.a1.client;

import ki.types.ds.Block;
import ki.types.ds.StreamInfo;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

public class FrameAccess implements FrameAccessor {

    protected StreamInfo stream;
    protected StreamServiceClient[] client;
    protected Frame[] frames;
    protected PerformanceStatistics ps;
    private int[] localN;
    int nrFrames;


    public FrameAccess(StreamServiceClient[] client, StreamInfo stream){
        this.client = client;
        this.stream = stream;
        this.frames = new Frame[stream.getWidthInBlocks()*stream.getHeightInBlocks()];
        this.ps = new PerformanceStatistics();
        this.nrFrames = 0;

        // Block partition.
        this.localN = new int[client.length];
        int rest = (stream.getWidthInBlocks()*stream.getHeightInBlocks()) % client.length;
        for(int i = 0; i < client.length; i++){
            if (rest > i) {
                localN[i] = (stream.getWidthInBlocks()*stream.getHeightInBlocks())/client.length + 1;
            }
            else {
                localN[i] = (stream.getWidthInBlocks()*stream.getHeightInBlocks())/client.length;
            }
        }
    }

    @Override
    public StreamInfo getStreamInfo() throws IOException, SocketTimeoutException {
        return stream;
    }

    @Override
    public Frame getFrame(int frame) throws IOException, SocketTimeoutException {
        int maxX = stream.getWidthInBlocks();
        Frame f = new Frame();

        // Start time.
        long ft1 = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(client.length);
        CountDownLatch latch = new CountDownLatch(client.length);

        int startingBlock = 0;
        for(int i = 0; i < client.length; i++){

            int finalI = i;
            int finalStartingBlock = startingBlock;

            executor.execute(()->{
                for(int block = finalStartingBlock; block < finalStartingBlock + localN[finalI]; block++){
                    // Implemented resend mechanic.
                    boolean blockSent = false;
                    // blockTime affected by drops.
                    long t1 = System.currentTimeMillis();

                    int blockY = block/maxX;
                    int blockX = block % maxX;

                    while(!blockSent){
                        try {
                            client[finalI].getBlock(stream.getName(), frame, blockX, blockY);
                            long t2 = System.currentTimeMillis();
                            //System.out.println("BLOCK: " + block + " FINAL STARTING BLOCK: " + finalStartingBlock);
                            f.blockTime[block] = t2-t1;
                            //System.out.println("BLOCK Y: " + blockY + " BLOCK X: " + blockX + " Block retrieved in: " + f.blockTime[block] + "ms.");
                            blockSent = true;

                        }
                        catch (SocketTimeoutException e)
                        {
                            f.packetDrops++;
                            //System.out.println("Block drop.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                latch.countDown();
            });
            startingBlock += localN[i];
        }

        try {
            latch.await();
            executor.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long ft2 = System.currentTimeMillis();
        f.frameTime = ft2-ft1;
        frames[frame] = f;
        nrFrames++;
        return f;
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

        public PerformanceStatistics(){}

        public double retrieveLatency(int percentage){
            double totalLatency = 0;

            for(int i = 0; i < percentage; i++)
                totalLatency += frames[i].frameTime;

            return totalLatency/percentage;
        }

        public double retrieveThroughput(int percentage){
            double totalFrameTime = 0;

            for(int i = 0; i < percentage; i++)
                totalFrameTime += frames[i].frameTime;

            return (percentage*1000)/totalFrameTime;
        }

        public double getBlockTime(int frame, int block){
            return frames[frame].blockTime[block];
        }

        @Override
        public double getPacketDropRate(String host) {
            int totalDrops = 0;

            for(int i = 0; i < nrFrames; i++)
                totalDrops += frames[i].packetDrops;

            return (double)totalDrops/(totalDrops+nrFrames*stream.getHeightInBlocks()*stream.getWidthInBlocks());
        }

        @Override
        public double getPacketLatency(String host) {
            double totalLatency = 0;

            for(int i = 0; i < nrFrames; i++)
                for(long blockTime : frames[i].blockTime)
                    totalLatency += blockTime;

            return totalLatency/(nrFrames*stream.getHeightInBlocks()*stream.getWidthInBlocks());
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
            int bitsInFrame = stream.getHeightInBlocks()*stream.getWidthInBlocks()*24*16*16+64;

            return bitsInFrame*getFrameThroughput();
        }
    }
}
