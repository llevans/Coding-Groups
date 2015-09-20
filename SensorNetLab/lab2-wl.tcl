# ======================================================================
# Define options
# ======================================================================

set opt(chan)	Channel/WirelessChannel
set opt(prop)	Propagation/TwoRayGround
set opt(netif)	Phy/WirelessPhy
set opt(mac)	Mac/802_11
set opt(ifq)	Queue/DropTail/PriQueue
set opt(ll)		LL
set opt(ant)        Antenna/OmniAntenna
set opt(x)		670   ;# X dimension of the topography
set opt(y)		670   ;# Y dimension of the topography
set opt(ifqlen)	50	      ;# max packet in ifq
set opt(seed)	0.0
set opt(tr)		wirelesslab.tr    ;# trace file
set opt(nam)            wirelesslab.nam   ;# nam trace file
set opt(adhocRouting)   DSDV
set opt(nn)             3             ;# how many nodes are simulated
set opt(nm)             "./scen-3"    ;# node movement file
set opt(cp)             "./traf-3"    ;# traffic connection file
set opt(stop)		200.0		;# simulation time
# =====================================================================
# Other default settings

LL set mindelay_		50us
LL set delay_			25us
LL set bandwidth_		0	;# not used

Agent/Null set sport_		0
Agent/Null set dport_		0

Agent/CBR set sport_		0
Agent/CBR set dport_		0

Agent/TCPSink set sport_	0
Agent/TCPSink set dport_	0

Agent/TCP set sport_		0
Agent/TCP set dport_		0
Agent/TCP set packetSize_	512

Queue/DropTail/PriQueue set Prefer_Routing_Protocols    1

# unity gain, omni-directional antennas
# set up the antennas to be centered in the node and 1.5 meters above it
Antenna/OmniAntenna set X_ 0
Antenna/OmniAntenna set Y_ 0
Antenna/OmniAntenna set Z_ 1.5
Antenna/OmniAntenna set Gt_ 1.0
Antenna/OmniAntenna set Gr_ 1.0

# Initialize the SharedMedia interface with parameters to make
# it work like the 914MHz Lucent WaveLAN DSSS radio interface
Phy/WirelessPhy set CPThresh_ 10.0
Phy/WirelessPhy set CSThresh_ 1.559e-11
Phy/WirelessPhy set RXThresh_ 3.652e-10
Phy/WirelessPhy set Rb_ 2*1e6
Phy/WirelessPhy set Pt_ 0.2818
Phy/WirelessPhy set freq_ 914e+6 
Phy/WirelessPhy set L_ 1.0


# ======================================================================
# Main Program
# ======================================================================

ErrorModel set rate_ 0.1

proc UniformErrorProc {} {
    global opt
	
    set	errObj [new ErrorModel]
    $errObj unit packet
    return $errObj
}


#
# Initialize Global Variables
#

# create simulator instance

-->

# set wireless topography object

-->

# create trace object for ns and nam
-->

-->

$ns_ trace-all $tracefd
$ns_ namtrace-all-wireless $namtrace $opt(x) $opt(y)

# define topology
$wtopo load_flatgrid $opt(x) $opt(y)

#
# Create God
#
set god_ [create-god $opt(nn)]

#
# define how node should be created
#

#global node setting

$ns_ node-config -adhocRouting $opt(adhocRouting) \
		 -llType $opt(ll) \
		 -macType $opt(mac) \
		 -ifqType $opt(ifq) \
		 -ifqLen $opt(ifqlen) \
		 -antType $opt(ant) \
		 -propType $opt(prop) \
		 -phyType $opt(netif) \
		 -channelType $opt(chan) \
		 -topoInstance $wtopo \
		 -agentTrace ON \
                 -routerTrace ON \
                 -macTrace OFF 


#
#  Create the specified number of nodes [$opt(nn)] and "attach" them
#  to the channel. 

for {set i 0} {$i < $opt(nn) } {incr i} {
--> 
	$node_($i) random-motion 0		;# disable random motion
}

# 
# Define node movement model
#
puts "Loading node movement..."
#source $opt(nm)

$node_(0) set Z_ 0.0
$node_(0) set Y_ 60.0
$node_(0) set X_ 83.0
$node_(1) set Z_ 0.0
$node_(1) set Y_ 180.3
$node_(1) set X_ 200.0
$node_(2) set Z_ 0.0
$node_(2) set Y_ 400.0
$node_(2) set X_ 450.2

$ns_ at 3.000000000000 "$node_(2) setdest 277.663708107313 281.494644426442 5.153832288917"
$ns_ at 80.000000000000 "$node_(1) setdest 369.463244915743 170.519203111152 3.371785899154"
$ns_ at 100.0 "$node_(0) setdest 201.663708107313 186.494644426442 15.153832288917"

# 
# Define traffic model
#
puts "Loading connection pattern..."
#source $opt(cp)

set udp_(0) [new Agent/UDP]
$ns_ attach-agent $node_(0) $udp_(0)
set null_(0) [new Agent/Null]
$ns_ attach-agent $node_(2) $null_(0)
set cbr_(0) [new Application/Traffic/CBR]
$cbr_(0) set packetSize_ 512
$cbr_(0) set interval_ 0.5
$cbr_(0) set random_ 1
$cbr_(0) set maxpkts_ 10000
$cbr_(0) set fid_ 0
$cbr_(0) attach-agent $udp_(0)
$ns_ connect $udp_(0) $null_(0)
$ns_ at 50.0 "$cbr_(0) start"

# Define node initial position in nam

for {set i 0} {$i < $opt(nn)} {incr i} {

    # 20 defines the node size in nam, must adjust it according to your scenario
    # The function must be called after mobility model is defined
    
    $ns_ initial_node_pos $node_($i) 20
}

$ns_ set-animation-rate 100.0ms

#
# Tell nodes when the simulation ends
#
for {set i 0} {$i < $opt(nn) } {incr i} {
    $ns_ at $opt(stop).000000001 "$node_($i) reset";
}
# tell nam the simulation stop time
$ns_ at  $opt(stop)	"$ns_ nam-end-wireless $opt(stop)"

$ns_ at  $opt(stop).000000001 "puts \"NS EXITING...\" ; $ns_ halt"


puts "Starting Simulation..."
$ns_ run




