#!/usr/bin/perl

my $SIM_CMD = "java CacheSim";

foreach my $trace (qw/hadoop-bayes hadoopKMeans hadoop-terasort-sort hadoopWordCount-sort hadoopDfsio hadoopPageRank hadoopSort/) {
  foreach my $policy (qw/1 3 4 5 7 8/) {
    foreach my $size (qw/1 8 16 32 64 128 256 512 1024 2048/) {
      print "Trace: $trace; beginning $policy sim with cache size $size\n";
      print "$SIM_CMD oneRouter.txt $trace $policy $size\n";
      system("$SIM_CMD oneRouter.txt $trace $policy $size");
    }
  }
}
 
