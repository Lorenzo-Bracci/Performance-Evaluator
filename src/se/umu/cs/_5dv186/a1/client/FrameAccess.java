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
    private final int[] localN;
    int nrFrames;

    /**
     * Set variables and compute an array containing the sizes of each block partition for every thread.
     * @param client - clients to run experiments with. The number of clients equal the thread count to use.
     * @param stream - stream to gather data from.
     */
    public FrameAccess(StreamServiceClient[] client, StreamInfo stream){
        this.client = client;
        this.stream = stream;
        this.frames = new Frame[stream.getWidthInBlocks()*stream.getHeightInBlocks()];
        this.ps = new PerformanceStatistics();
        this.nrFrames = 0;

        // Set array containing size for block partition across computing threads.
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

    /**
     * Get StreamInfo object.
     * @return - StreamInfo object.
     */
    @Override
    public StreamInfo getStreamInfo() {
        return stream;
    }

    /**
     * Retrieve an entire frame. Parallelized using ExecutorService with a thread pool of size equal to the number of
     * clients. During the gathering process data is being stored regarding latency for blocks and frame and the drop
     * rate. Packets dropped are re-retrieved until successfully retrieved.
     * @param frame -which frame in stream to retrieve.
     * @return - a Frame object.
     */
    @Override
    public Frame getFrame(int frame) {
        int maxX = stream.getWidthInBlocks();
        Frame f = new Frame();

        // Start time for frame retrieval.
        long ft1 = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(client.length);
        CountDownLatch latch = new CountDownLatch(client.length);

        int startingBlock = 0;
        for(int i = 0; i < client.length; i++){

            int finalI = i;
            int finalStartingBlock = startingBlock;

            executor.execute(()->{
                for(int block = finalStartingBlock; block < finalStartingBlock + localN[finalI]; block++){

                    // Start time for block retrieval.
                    long t1 = System.currentTimeMillis();

                    int blockY = block/maxX;
                    int blockX = block % maxX;

                    boolean blockSent = false;
                    while(!blockSent){
                        try {
                            client[finalI].getBlock(stream.getName(), frame, blockX, blockY);
                            // End time for block retrieval.
                            long t2 = System.currentTimeMillis();
                            f.blockTime[block] = t2-t1;
                            //System.out.println("Block retrieved in: " + f.blockTime[block] + "ms.");
                            blockSent = true;

                        }
                        catch (SocketTimeoutException e) {
                            f.packetDrops++;
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
        // End time for frame retrieval.
        long ft2 = System.currentTimeMillis();
        f.frameTime = ft2-ft1;
        frames[frame] = f;
        nrFrames++;
        return f;
    }

    /**
     * Get PerformanceStatistics object.
     * @return - PerformanceStatistics object.
     */
    @Override
    public PerformanceStatistics getPerformanceStatistics() {
        return ps;
    }

    class Frame implements FrameAccessor.Frame{
        Block[] blocks;
        long[] blockTime;
        long frameTime;
        int packetDrops;

        /**
         * Set size of array containing all blocks in frame, array containing latency for each block and an initial
         * number of dropped packets of zero.
         */
        public Frame(){
            blocks = new Block[stream.getWidthInBlocks()*stream.getHeightInBlocks()];
            blockTime = new long[stream.getWidthInBlocks()*stream.getHeightInBlocks()];
            packetDrops = 0;
        }

        /**
         * Get a specific block.
         * @param blockX - x-value of the block.
         * @param blockY - y-value of the block.
         * @return - the requested Block object.
         */
        @Override
        public Block getBlock(int blockX, int blockY) {
            return blocks[blockY*stream.getHeightInBlocks()+blockX];
        }
    }

    class PerformanceStatistics implements FrameAccessor.PerformanceStatistics{

        public PerformanceStatistics(){}

        //TODO: remove?
        public double retrieveLatency(int percentage){
            double totalLatency = 0;

            for(int i = 0; i < percentage; i++)
                totalLatency += frames[i].frameTime;

            return totalLatency/percentage;
        }

        //TODO: remove?
        public double retrieveThroughput(int percentage){
            double totalFrameTime = 0;

            for(int i = 0; i < percentage; i++)
                totalFrameTime += frames[i].frameTime;

            return (percentage*1000)/totalFrameTime;
        }

        /**
         * Get block latency for specific block in specified frame.
         * @param frame - frame the block is stored in.
         * @param block - block position.
         * @return - exact latency for specified block.
         */
        public double getBlockTime(int frame, int block){
            return frames[frame].blockTime[block];
        }

        /**
         *
         * @param host
         * @return
         */
        @Override
        public double getPacketDropRate(String host) {
            int totalDrops = 0;

            for(int i = 0; i < nrFrames; i++)
                totalDrops += frames[i].packetDrops;

            return (double)totalDrops*100/(totalDrops+nrFrames*stream.getHeightInBlocks()*stream.getWidthInBlocks());
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
