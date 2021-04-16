package se.umu.cs._5dv186.a1.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;

import ki.types.ds.StreamInfo;

public final class Client {
    public static final int DEFAULT_TIMEOUT = 1000;

    //----------------------------------------------------------
    //----------------------------------------------------------
    public static void listStreamInfo(StreamServiceClient client)
            throws IOException {
        StreamInfo[] streams = client.listStreams();
        System.out.println("found " + streams.length + " streams");
        for (StreamInfo stream : streams) {
            System.out.println("  '" + stream.getName() + "': " + stream.getLengthInFrames() + " frames, " +
                    stream.getWidthInBlocks() + " x " + stream.getHeightInBlocks() + " blocks");
        }
    }

    //----------------------------------------------------------
    public static void main(String[] args) {
        try {
            final String host = (args.length > 0) ? args[0] : "scratchy.cs.umu.se";
            final int timeout = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_TIMEOUT;
            final String username = (args.length > 2) ? args[2] : "c17con";
            final int nrClients = (args.length > 3) ? Integer.parseInt(args[3]) : 27;
            final int nrFrames = (args.length > 4) ? Integer.parseInt(args[4]) : 1;
            final String streamName = (args.length > 5) ? args[5] : "stream7";
            final String filePath = (args.length > 6) ? args[6] : "res.csv";

            StreamServiceClient[] clients = new StreamServiceClient[nrClients];
            for (int i = 0; i < nrClients; i++) {
                clients[i] = DefaultStreamServiceClient.bind(host, timeout, username);
            }

            Factory factory = new Factory();
            FrameAccess fa = factory.getFrameAccessor(clients, streamName);

            int frameCount = 0;
            for (int i = 0; i < nrFrames; i++) {
                fa.getFrame(i);
                frameCount++;
                System.out.println(frameCount);
            }

            // Write to file only if path is specified.
            if(filePath != null)
                writeResult(fa,filePath, frameCount);

            System.out.println("Received " + frameCount + " / " + nrFrames);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void writeResult(FrameAccess fa, String filePath, int frameCount){
        try {
            File f = new File(filePath);
            f.createNewFile();

            DecimalFormat df = new DecimalFormat("#.#####");

            // Write in the order: drop rate, fps, bandwidth utilization.
            String csvEntry = df.format(fa.getPerformanceStatistics().getPacketDropRate(""))+","
                            + df.format(fa.getPerformanceStatistics().getFrameThroughput())+","
                            + df.format(fa.getPerformanceStatistics().getBandwidthUtilization())+","
                            + df.format(fa.getPerformanceStatistics().getPacketLatency(""))+",\n";

            Files.write(Paths.get(filePath), csvEntry.getBytes(), StandardOpenOption.APPEND);

            File fl = new File("l"+filePath);
            fl.createNewFile();

            // Append to block file.
            for(int frame = 0; frame < frameCount; frame++){
                StringBuilder stringBuilder = new StringBuilder();
                for(int blockY = 0; blockY < fa.stream.getHeightInBlocks(); blockY++){
                    for(int blockX = 0; blockX < fa.stream.getWidthInBlocks(); blockX++){
                        stringBuilder.append(df.format(fa.getPerformanceStatistics().
                                getBlockTime(frame,blockY*fa.stream.getHeightInBlocks()+blockX))+",");
                    }
                }
                Files.write(Paths.get("l"+filePath), (stringBuilder+"\n").getBytes(), StandardOpenOption.APPEND);
            }

            // Extra credit.
            if(frameCount > 99){
                File fp = new File("p"+filePath);
                fp.createNewFile();
                StringBuilder stringBuilder = new StringBuilder();
                int[] percentages = {50, 80, 95, 99, 100};
                for (int percent : percentages) {
                    stringBuilder.append(fa.getPerformanceStatistics().retrieveLatency(percent)+",");
                }
                Files.write(Paths.get("p"+filePath), (stringBuilder+"\n").getBytes(), StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printResult(FrameAccess fa, int frameCount){
        if(frameCount > 99){
            int[] percentages = {50, 80, 95, 99, 100};
            for (int percent : percentages) {
                System.out.println("Latency for " + percent + ": " + fa.getPerformanceStatistics().retrieveLatency(percent));
                System.out.println("Throughput for " + percent + ": " + fa.getPerformanceStatistics().retrieveThroughput(percent));
            }
        }
        System.out.println("Drop rate: " + fa.getPerformanceStatistics().getPacketDropRate("placeholder"));
        System.out.println("FPS: " + fa.getPerformanceStatistics().getFrameThroughput());
        System.out.println("Latency: " + fa.getPerformanceStatistics().getPacketLatency("placeholder"));
        System.out.println("Bandwidth utilization: " + fa.getPerformanceStatistics().getBandwidthUtilization() + " bps");
    }
}
