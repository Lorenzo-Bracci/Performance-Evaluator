package se.umu.cs._5dv186.a1.client;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLOutput;

import ki.types.ds.Block;
import ki.types.ds.StreamInfo;

public final class ExampleClient
{
  public static final int DEFAULT_TIMEOUT = 1000;


  //----------------------------------------------------------
  //----------------------------------------------------------
  public static void listStreamInfo (StreamServiceClient client)
    throws IOException
  {
    StreamInfo[] streams = client.listStreams();
    System.out.println("found " + streams.length + " streams");
    for (StreamInfo stream : streams)
    {
      System.out.println("  '" + stream.getName() + "': " + stream.getLengthInFrames() + " frames, " +
                         stream.getWidthInBlocks() + " x " + stream.getHeightInBlocks() + " blocks");
    }
  }

  //----------------------------------------------------------
  public static void main (String[] args)
  {
    try
    {
      final String host     = (args.length > 0) ? args[0] : "itchy.cs.umu.se";
      final int timeout     = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_TIMEOUT;
      final String username = (args.length > 2) ? args[2] : "c17con";

      StreamServiceClient[] clients = new StreamServiceClient[12];

      for(int i = 0; i < 12;  i++){
        clients[i] = DefaultStreamServiceClient.bind(host,timeout,username);
      }


      int cores = Runtime.getRuntime().availableProcessors();
      System.out.println("nr cores: " + cores);

      //listStreamInfo(client);

      int nr = 100;
      int count = 0;
      //String stream = "stream10";
      StreamInfo[] streams = clients[0].listStreams();
      StreamInfo stream = streams[7];
      //FrameAccess fa = new FrameAccess(client, stream);

      FrameAccessParallel fa = new FrameAccessParallel(clients, stream);

      for (int i=0; i<nr; i++)
      {
        fa.receiveFrame(i);
        count++;
        System.out.println(count);
      }

      int[] percentages = {50, 80, 95, 99, 100};

      for(int percent : percentages){
        System.out.println("Latency for " + percent + ": " + fa.getPerformanceStatistics().retrieveLatency(percent));
        System.out.println("Throughput for " + percent + ": " + fa.getPerformanceStatistics().retrieveThroughput(percent));
      }

      System.out.println("Drop rate: " + fa.getPerformanceStatistics().getPacketDropRate("place_holder"));
      System.out.println("FPS: " + fa.getPerformanceStatistics().getFrameThroughput());
      System.out.println("Latency: " + fa.getPerformanceStatistics().getPacketLatency("place_holder"));
      System.out.println("Bandwidth utilization: " + fa.getPerformanceStatistics().getBandwidthUtilization() + " bps");
      System.out.println("received " + count + " / " + nr);
    }
	catch (Exception e)
	{
	  e.printStackTrace();
	}

  }
}
